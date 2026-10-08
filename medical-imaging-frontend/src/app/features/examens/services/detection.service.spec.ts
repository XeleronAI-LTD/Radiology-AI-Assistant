import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { DetectionService } from './detection.service';

describe('DetectionService', () => {
  let service: DetectionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DetectionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lister() appelle GET .../examens/{id}/detections', () => {
    service.lister(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42/detections`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('masque() appelle GET .../detections/{id}/masque en blob', () => {
    service.masque(42, 7).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42/detections/7/masque`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });

  it('lancerAnalyse() appelle POST .../detections/analyser', () => {
    service.lancerAnalyse(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42/detections/analyser`);
    expect(req.request.method).toBe('POST');
    req.flush({ examenId: 42, statut: 'EN_COURS' });
  });

  it('statutAnalyse() appelle GET .../detections/statut', () => {
    service.statutAnalyse(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42/detections/statut`);
    expect(req.request.method).toBe('GET');
    req.flush({ examenId: 42, statut: 'EN_ATTENTE', message: null, finieLe: null });
  });

  it('validerStatut() appelle PATCH .../detections/{id}/statut avec le statut demandé', () => {
    service.validerStatut(42, 7, 'ACCEPTEE').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42/detections/7/statut`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ statut: 'ACCEPTEE' });
    req.flush({
      id: 7,
      imageId: 1,
      type: 'BOX',
      anomalie: 'nodule',
      confiance: 0.9,
      statut: 'ACCEPTEE',
      coupe: 0,
      bbox: null,
      apercuMasqueDisponible: false,
      validateurEmail: 'radio@test.com',
      valideLe: '2026-01-01T00:00:00',
    });
  });
});
