export type DetectionType = 'BOX' | 'MASQUE';
export type DetectionStatut = 'EN_ATTENTE' | 'ACCEPTEE' | 'REJETEE';

export interface DetectionBbox {
  x: number;
  y: number;
  largeur: number;
  hauteur: number;
}

export interface Detection {
  id: number;
  imageId: number;
  type: DetectionType;
  anomalie: string | null;
  confiance: number;
  statut: DetectionStatut;
  coupe: number | null;
  bbox: DetectionBbox | null;
  apercuMasqueDisponible: boolean;
  validateurEmail: string | null;
  valideLe: string | null;
}

export type AnalyseStatut = 'EN_ATTENTE' | 'EN_COURS' | 'TERMINEE' | 'ECHOUEE';

export interface AnalyseLanceeResponse {
  examenId: number;
  statut: AnalyseStatut;
}

export interface AnalyseStatutResponse {
  examenId: number;
  statut: AnalyseStatut;
  message: string | null;
  finieLe: string | null;
}
