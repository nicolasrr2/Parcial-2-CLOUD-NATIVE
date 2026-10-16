import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Solicitud,
  SolicitudInput,
} from '../core/models';

@Component({
  selector: 'app-solicitud-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
  ],
  templateUrl: './solicitud-form.html',
})
export class SolicitudForm implements OnChanges {
  @Input() initialData: Solicitud | null = null;
  @Input() submitting = false;

  @Output() submitted =
    new EventEmitter<SolicitudInput>();

  @Output() cancelled =
    new EventEmitter<void>();

  model: SolicitudInput = this.empty();
  error = '';

  ngOnChanges(
    changes: SimpleChanges,
  ): void {
    if (changes['initialData']) {
      this.model = this.initialData
        ? {
            tipo: this.initialData.tipo,
            fechaInicio: this.initialData.fechaInicio,
            fechaFin: this.initialData.fechaFin,
            motivo: this.initialData.motivo,
          }
        : this.empty();

      this.error = '';
    }
  }

  submit(): void {
    const clean: SolicitudInput = {
      tipo: this.model.tipo.trim(),
      fechaInicio: this.model.fechaInicio,
      fechaFin: this.model.fechaFin,
      motivo: this.model.motivo.trim(),
    };

    if (
      !clean.tipo ||
      !clean.fechaInicio ||
      !clean.fechaFin ||
      !clean.motivo
    ) {
      this.error =
        'Completa todos los campos obligatorios.';
      return;
    }

    if (
      clean.fechaFin <
      clean.fechaInicio
    ) {
      this.error =
        'La fecha fin no puede ser anterior a la fecha inicio.';
      return;
    }

    this.error = '';
    this.submitted.emit(clean);
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private empty(): SolicitudInput {
    return {
      tipo: '',
      fechaInicio: '',
      fechaFin: '',
      motivo: '',
    };
  }
}