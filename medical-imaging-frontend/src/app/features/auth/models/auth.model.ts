export type Role = 'RADIOLOGUE' | 'TECHNICIEN' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

export interface CurrentUser {
  email: string;
  role: Role;
  exp: number; // timestamp en secondes (claim exp du JWT)
}
