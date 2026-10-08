export interface ExamenSummary {
  examenId: number;
  mrn: string;
  patientNom: string;
  type: string;
  dateExamen: string; // ISO "YYYY-MM-DD", formaté en JJ/MM/AAAA à l'affichage
  modalite: string;
  zone: string;
  nombreImages: number;
}

export interface Patient {
  patientId: number;
  mrn: string;
  nom: string;
  dateNaissance: string; // ISO "YYYY-MM-DD"
  sexe: string;
}

export interface ExamenImage {
  imageId: number;
  format: string;
  apercuDisponible: boolean;
  ordre: number;
}

export interface ExamenDetail {
  examenId: number;
  patient: Patient;
  type: string;
  dateExamen: string; // ISO "YYYY-MM-DD"
  modalite: string;
  zone: string;
  creePar: string;
  images: ExamenImage[];
}

export interface ExamenUploadResult {
  examenId: number;
  patientId: number;
  mrn: string;
  nombreImages: number;
  images: ExamenImage[];
}
