package com.xeleronai.medicalimagingbackend.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite le bruteforce sur POST /api/v1/auth/login : 5 tentatives par
 * minute et par IP cliente, fenêtre fixe (refill intervally) — une fois les
 * 5 jetons consommés, aucun ne revient avant que la minute entière depuis le
 * début de la fenêtre soit écoulée (contrairement à un refill progressif, où
 * un attaquant espaçant ses tentatives d'une douzaine de secondes ne serait
 * jamais bloqué net).
 *
 * Ne s'applique qu'à cet endpoint précis — {@link #shouldNotFilter} laisse
 * passer tout le reste de l'API sans overhead.
 *
 * Stockage en mémoire (Map concurrente IP -> bucket), suffisant pour une
 * instance unique. En multi-instance, chaque instance aurait son propre
 * compteur (un attaquant pourrait donc multiplier la limite réelle par le
 * nombre d'instances) : il faudrait alors un store partagé (Redis via
 * bucket4j-redis, par ex.) — hors scope v1.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final RequestMatcher LOGIN_MATCHER =
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/login");

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static final String MESSAGE_TROP_DE_TENTATIVES =
            "Trop de tentatives, réessayez dans une minute.";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN_MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, unused -> newBucket());

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit login dépassé pour IP={}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(MESSAGE_TROP_DE_TENTATIVES);
            return;
        }

        filterChain.doFilter(request, response);

        // Une tentative réussie ne doit pas rester comptée contre la limite :
        // on rembourse le jeton consommé (addTokens plafonne à la capacité,
        // donc pas de dépassement possible même en cas d'appels concurrents).
        if (response.getStatus() == HttpStatus.OK.value()) {
            bucket.addTokens(1);
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(MAX_ATTEMPTS).refillIntervally(MAX_ATTEMPTS, WINDOW))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Le premier maillon de la chaîne est l'IP cliente d'origine.
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
