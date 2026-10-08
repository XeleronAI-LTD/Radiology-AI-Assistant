import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../shared/models/page-response.model';
import { ExamenDetail, ExamenSummary, ExamenUploadResult } from '../models/examen.model';

@Injectable({ providedIn: 'root' })
export class ExamenService {
  private readonly http = inject(HttpClient);

  lister(mrn: string | null, page: number, size: number): Observable<PageResponse<ExamenSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (mrn) {
      params = params.set('mrn', mrn);
    }
    return this.http.get<PageResponse<ExamenSummary>>(`${environment.apiUrl}/v1/examens`, { params });
  }

  detail(examenId: number): Observable<ExamenDetail> {
    return this.http.get<ExamenDetail>(`${environment.apiUrl}/v1/examens/${examenId}`);
  }

  apercu(examenId: number, imageId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/v1/examens/${examenId}/images/${imageId}/apercu`, {
      responseType: 'blob',
    });
  }

  uploadStandard(formData: FormData): Observable<ExamenUploadResult> {
    return this.http.post<ExamenUploadResult>(`${environment.apiUrl}/v1/examens/upload/standard`, formData);
  }

  uploadDicom(formData: FormData): Observable<ExamenUploadResult> {
    return this.http.post<ExamenUploadResult>(`${environment.apiUrl}/v1/examens/upload/dicom`, formData);
  }
}
