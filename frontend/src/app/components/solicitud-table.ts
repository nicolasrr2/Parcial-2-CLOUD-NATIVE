import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Solicitud } from '../core/models';

@Component({
  selector: 'app-solicitud-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './solicitud-table.html',
})
export class SolicitudTable {
  @Input() solicitudes: Solicitud[] = [];
  @Input() puedeEscribir = false;

  @Output() editRequest =
    new EventEmitter<Solicitud>();

  @Output() removeRequest =
    new EventEmitter<Solicitud>();

  edit(solicitud: Solicitud): void {
    this.editRequest.emit(solicitud);
  }

  remove(solicitud: Solicitud): void {
    this.removeRequest.emit(solicitud);
  }

  estadoClass(
    estado: string,
  ): string {
    return `estado-${String(
      estado ?? '',
    ).toLowerCase()}`;
  }
}