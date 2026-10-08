import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ExamenUploadResult } from '../../models/examen.model';
import { ExamenService } from '../../services/examen.service';

type ModeUpload = 'STANDARD' | 'DICOM';

const EXTENSIONS_STANDARD = ['png', 'jpg', 'jpeg'];
const EXTENSION_DICOM = 'dcm';

@Component({
  selector: 'app-examen-upload',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './examen-upload.html',
})
export class ExamenUpload {
  private readonly fb = inject(FormBuilder);
  private readonly examenService = inject(ExamenService);

  // Deux inputs distincts (un par mode, seul celui du mode actif est rendu
  // via @if/@else dans le template) plutôt qu'un seul input dont on
  // basculerait les attributs : webkitdirectory ne doit exister que sur
  // l'input DICOM, jamais sur l'input Standard.
  private readonly fileInputStandard = viewChild<ElementRef<HTMLInputElement>>('fileInputStandard');
  private readonly fileInputDicom = viewChild<ElementRef<HTMLInputElement>>('fileInputDicom');

  protected readonly mode = signal<ModeUpload>('STANDARD');
  protected readonly fichiers = signal<File[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly resultat = signal<ExamenUploadResult | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    mrn: ['', Validators.required],
    nom: ['', Validators.required],
    dateNaissance: [''],
    sexe: [''],
    type: [''],
    dateExamen: [''],
    modalite: [''],
  });

  // Méthode plutôt que computed() : dépend de form.valid (FormGroup Reactive
  // Forms, pas un signal) — un computed() ne recalculerait pas quand la
  // validité du formulaire change sans qu'un signal suivi ne change aussi.
  protected peutSoumettre(): boolean {
    return !this.loading() && this.fichiers().length > 0 && (this.mode() === 'DICOM' || this.form.valid);
  }

  protected onChangerMode(mode: ModeUpload): void {
    this.mode.set(mode);
    this.viderSelection();
    this.errorMessage.set(null);
    this.resultat.set(null);
  }

  protected onFichiersSelectionnes(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichiers = Array.from(input.files ?? []);

    if (this.mode() === 'STANDARD') {
      const invalide = fichiers.find((f) => !EXTENSIONS_STANDARD.includes(this.extensionDe(f)));
      if (invalide) {
        this.errorMessage.set(`Extension non autorisée : ${invalide.name}`);
        this.viderSelection();
        return;
      }
      this.errorMessage.set(null);
      this.fichiers.set(fichiers);
      return;
    }

    // Mode DICOM : l'utilisateur sélectionne un dossier entier
    // (webkitdirectory) qui peut contenir des fichiers parasites
    // (.DS_Store, Thumbs.db, DICOMDIR, sous-dossiers...). On filtre pour ne
    // garder que les .dcm plutôt que de rejeter tout le dossier — le
    // FileTypeValidator backend, lui, rejette tout le batch au moindre
    // fichier non conforme.
    const dicoms = fichiers.filter((f) => this.extensionDe(f) === EXTENSION_DICOM);

    if (fichiers.length > 0 && dicoms.length === 0) {
      this.errorMessage.set('Aucun fichier DICOM (.dcm) trouvé dans ce dossier.');
    } else {
      this.errorMessage.set(null);
    }
    this.fichiers.set(dicoms);
  }

  protected onSubmit(): void {
    if (!this.peutSoumettre()) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    const formData = new FormData();
    for (const fichier of this.fichiers()) {
      formData.append('files', fichier);
    }

    const upload$ =
      this.mode() === 'STANDARD'
        ? this.examenService.uploadStandard(this.construireFormDataStandard(formData))
        : this.examenService.uploadDicom(formData);

    upload$.subscribe({
      next: (resultat) => {
        this.loading.set(false);
        this.resultat.set(resultat);
        this.form.reset();
        this.viderSelection();
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.messageErreur(err));
      },
    });
  }

  private construireFormDataStandard(formData: FormData): FormData {
    const valeurs = this.form.getRawValue();
    for (const [cle, valeur] of Object.entries(valeurs)) {
      if (valeur) {
        formData.append(cle, valeur);
      }
    }
    return formData;
  }

  private viderSelection(): void {
    this.fichiers.set([]);
    const input = this.mode() === 'STANDARD' ? this.fileInputStandard() : this.fileInputDicom();
    if (input) {
      input.nativeElement.value = '';
    }
  }

  private extensionDe(fichier: File): string {
    const idx = fichier.name.lastIndexOf('.');
    return idx < 0 ? '' : fichier.name.slice(idx + 1).toLowerCase();
  }

  private messageErreur(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Impossible de contacter le serveur, réessayez plus tard.';
    }
    if (err.status === 403) {
      return "Vous n'avez pas les droits pour cette action.";
    }
    if (typeof err.error === 'string' && err.error) {
      return err.error;
    }
    return 'Une erreur est survenue, réessayez plus tard.';
  }
}
