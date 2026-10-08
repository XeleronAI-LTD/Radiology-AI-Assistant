import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { ExamenService } from './examen.service';

describe('ExamenService', () => {
  let service: ExamenService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ExamenService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envoie page et size en query params', () => {
    service.lister(null, 2, 20).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/v1/examens`
    );
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('mrn')).toBe(false);
    req.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('ajoute mrn uniquement quand il est fourni', () => {
    service.lister('MRN-001', 0, 20).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/v1/examens`
    );
    expect(req.request.params.get('mrn')).toBe('MRN-001');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('detail() appelle GET /v1/examens/{id}', () => {
    service.detail(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('apercu() appelle GET .../images/{imageId}/apercu en blob', () => {
    service.apercu(42, 7).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/42/images/7/apercu`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });

  it('uploadStandard() appelle POST .../upload/standard avec le FormData tel quel', () => {
    const formData = new FormData();
    formData.append('mrn', 'MRN-001');
    service.uploadStandard(formData).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/upload/standard`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe(formData);
    req.flush({ examenId: 1, patientId: 1, mrn: 'MRN-001', nombreImages: 1, images: [] });
  });

  it('uploadDicom() appelle POST .../upload/dicom avec le FormData tel quel', () => {
    const formData = new FormData();
    formData.append('files', new Blob(['x']), 'a.dcm');
    service.uploadDicom(formData).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens/upload/dicom`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe(formData);
    req.flush({ examenId: 2, patientId: 2, mrn: 'MRN-002', nombreImages: 1, images: [] });
  });
});
