import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AnalyseLanceeResponse, AnalyseStatutResponse, Detection } from '../models/detection.model';

@Injectable({ providedIn: 'root' })
export class DetectionService {
  private readonly http = inject(HttpClient);

  lister(examenId: number): Observable<Detection[]> {
    return this.http.get<Detection[]>(`${environment.apiUrl}/v1/examens/${examenId}/detections`);
  }

  masque(examenId: number, detectionId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/v1/examens/${examenId}/detections/${detectionId}/masque`, {
      responseType: 'blob',
    });
  }

  lancerAnalyse(examenId: number): Observable<AnalyseLanceeResponse> {
    return this.http.post<AnalyseLanceeResponse>(
      `${environment.apiUrl}/v1/examens/${examenId}/detections/analyser`,
      {}
    );
  }

  statutAnalyse(examenId: number): Observable<AnalyseStatutResponse> {
    return this.http.get<AnalyseStatutResponse>(`${environment.apiUrl}/v1/examens/${examenId}/detections/statut`);
  }

  validerStatut(examenId: number, detectionId: number, statut: 'ACCEPTEE' | 'REJETEE'): Observable<Detection> {
    return this.http.patch<Detection>(
      `${environment.apiUrl}/v1/examens/${examenId}/detections/${detectionId}/statut`,
      { statut }
    );
  }
}
