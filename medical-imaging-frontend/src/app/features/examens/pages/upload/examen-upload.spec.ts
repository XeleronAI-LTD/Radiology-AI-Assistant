import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ExamenUploadResult } from '../../models/examen.model';
import { ExamenService } from '../../services/examen.service';
import { ExamenUpload } from './examen-upload';

const RESULTAT: ExamenUploadResult = {
  examenId: 42,
  patientId: 7,
  mrn: 'MRN-001',
  nombreImages: 1,
  images: [{ imageId: 1, format: 'PNG', apercuDisponible: true, ordre: 0 }],
};

function fichier(nom: string): File {
  return new File(['x'], nom);
}

function evenementFichiers(fichiers: File[]): Event {
  return { target: { files: fichiers } } as unknown as Event;
}

describe('ExamenUpload', () => {
  let examenService: Partial<ExamenService>;

  beforeEach(async () => {
    examenService = {
      uploadStandard: vi.fn().mockReturnValue(of(RESULTAT)),
      uploadDicom: vi.fn().mockReturnValue(of(RESULTAT)),
    };

    await TestBed.configureTestingModule({
      imports: [ExamenUpload],
      providers: [provideRouter([]), { provide: ExamenService, useValue: examenService }],
    }).compileComponents();
  });

  function createComponent() {
    const fixture = TestBed.createComponent(ExamenUpload);
    fixture.detectChanges();
    return fixture;
  }

  it('bascule de mode vide fichiers/erreur/résultat', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.png')]));
    expect(c['fichiers']().length).toBe(1);

    c['onChangerMode']('DICOM');

    expect(c['mode']()).toBe('DICOM');
    expect(c['fichiers']()).toEqual([]);
    expect(c['errorMessage']()).toBeNull();
    expect(c['resultat']()).toBeNull();
  });

  it('rejette un fichier avec une extension invalide sans appeler le service', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.gif')]));

    expect(c['errorMessage']()).toContain('Extension non autorisée');
    expect(c['fichiers']()).toEqual([]);
    expect(examenService.uploadStandard).not.toHaveBeenCalled();
  });

  it('mode STANDARD : soumet les fichiers dans l\'ordre et les champs non vides du formulaire', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.png'), fichier('b.jpg')]));
    c['form'].setValue({
      mrn: 'MRN-001',
      nom: 'Dupont',
      dateNaissance: '',
      sexe: '',
      type: '',
      dateExamen: '',
      modalite: '',
    });

    c['onSubmit']();

    expect(examenService.uploadStandard).toHaveBeenCalledTimes(1);
    const formData = (examenService.uploadStandard as ReturnType<typeof vi.fn>).mock.calls[0][0] as FormData;
    const files = formData.getAll('files') as File[];
    expect(files.map((f) => f.name)).toEqual(['a.png', 'b.jpg']);
    expect(formData.get('mrn')).toBe('MRN-001');
    expect(formData.get('nom')).toBe('Dupont');
    expect(formData.get('sexe')).toBeNull();
    expect(formData.get('dateNaissance')).toBeNull();
  });

  it('mode DICOM : filtre les fichiers parasites du dossier, ne garde que les .dcm', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onChangerMode']('DICOM');
    c['onFichiersSelectionnes'](
      evenementFichiers([
        fichier('IM001.dcm'),
        fichier('.DS_Store'),
        fichier('Thumbs.db'),
        fichier('DICOMDIR'),
        fichier('IM002.DCM'), // insensible à la casse
      ])
    );

    expect(c['fichiers']().map((f) => f.name)).toEqual(['IM001.dcm', 'IM002.DCM']);
    expect(c['errorMessage']()).toBeNull();
  });

  it("mode DICOM : affiche une erreur si aucun .dcm n'est trouvé dans le dossier", () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onChangerMode']('DICOM');
    c['onFichiersSelectionnes'](evenementFichiers([fichier('.DS_Store'), fichier('Thumbs.db')]));

    expect(c['fichiers']()).toEqual([]);
    expect(c['errorMessage']()).toBe('Aucun fichier DICOM (.dcm) trouvé dans ce dossier.');
    expect(c['peutSoumettre']()).toBe(false);
  });

  it('mode DICOM : soumet uniquement les fichiers, aucun champ de formulaire', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onChangerMode']('DICOM');
    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.dcm')]));

    c['onSubmit']();

    expect(examenService.uploadDicom).toHaveBeenCalledTimes(1);
    const formData = (examenService.uploadDicom as ReturnType<typeof vi.fn>).mock.calls[0][0] as FormData;
    expect((formData.getAll('files') as File[]).map((f) => f.name)).toEqual(['a.dcm']);
    expect(formData.get('mrn')).toBeNull();
  });

  it('succès : pose le résultat et réinitialise formulaire + fichiers', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.png')]));
    c['form'].patchValue({ mrn: 'MRN-001', nom: 'Dupont' });

    c['onSubmit']();

    expect(c['resultat']()).toEqual(RESULTAT);
    expect(c['fichiers']()).toEqual([]);
    expect(c['form'].value.mrn).toBe('');
  });

  it('erreur 400 : affiche le message brut, conserve fichiers et formulaire', () => {
    (examenService.uploadStandard as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: 'MRN manquant' }))
    );
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.png')]));
    c['form'].patchValue({ mrn: 'MRN-001', nom: 'Dupont' });

    c['onSubmit']();

    expect(c['errorMessage']()).toBe('MRN manquant');
    expect(c['fichiers']().length).toBe(1);
    expect(c['form'].value.mrn).toBe('MRN-001');
  });

  it('erreur réseau (status 0) : message générique', () => {
    (examenService.uploadStandard as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 }))
    );
    const fixture = createComponent();
    const c = fixture.componentInstance;

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.png')]));
    c['form'].patchValue({ mrn: 'MRN-001', nom: 'Dupont' });

    c['onSubmit']();

    expect(c['errorMessage']()).toBe('Impossible de contacter le serveur, réessayez plus tard.');
  });

  it('peutSoumettre est false sans fichier, avec mrn/nom vides, ou pendant loading', () => {
    const fixture = createComponent();
    const c = fixture.componentInstance;

    expect(c['peutSoumettre']()).toBe(false);

    c['onFichiersSelectionnes'](evenementFichiers([fichier('a.png')]));
    expect(c['peutSoumettre']()).toBe(false); // mrn/nom vides

    c['form'].patchValue({ mrn: 'MRN-001', nom: 'Dupont' });
    expect(c['peutSoumettre']()).toBe(true);

    c['loading'].set(true);
    expect(c['peutSoumettre']()).toBe(false);
  });
});
