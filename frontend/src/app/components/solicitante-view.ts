import {
  Component,
  Input,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../core/api.service';
import {
  Solicitud,
  SolicitudInput,
} from '../core/models';
import { SolicitudForm } from './solicitud-form';
import { SolicitudTable } from './solicitud-table';

@Component({
  selector: 'app-solicitante-view',
  standalone: true,
  imports: [
    CommonModule,
    SolicitudForm,
    SolicitudTable,
  ],
  templateUrl: './solicitante-view.html',
})
export class SolicitanteView implements OnInit {
  @Input() scopes: string[] = [];

  solicitudes: Solicitud[] = [];
  editando: Solicitud | null = null;

  cargando = true;
  guardando = false;

  mensaje = '';
  error = '';

  constructor(
    private readonly api: ApiService,
  ) {}

  get puedeLeer(): boolean {
    return this.scopes.includes(
      'solicitudes/read',
    );
  }

  get puedeEscribir(): boolean {
    return this.scopes.includes(
      'solicitudes/write',
    );
  }

  ngOnInit(): void {
    if (this.puedeLeer) {
      void this.cargar();
    } else {
      this.cargando = false;
    }
  }

  async cargar(): Promise<void> {
    this.cargando = true;
    this.error = '';

    try {
      const data =
        await this.api.listMyRequests();

      this.solicitudes =
        Array.isArray(data)
          ? data
          : [];
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No se pudieron cargar tus solicitudes.';
    } finally {
      this.cargando = false;
    }
  }

  async guardar(
    input: SolicitudInput,
  ): Promise<void> {
    this.guardando = true;
    this.error = '';
    this.mensaje = '';

    try {
      if (this.editando) {
        await this.api.updateRequest(
          this.editando.id,
          input,
        );

        this.mensaje =
          'Solicitud actualizada correctamente.';

        this.editando = null;
      } else {
        await this.api.createRequest(input);

        this.mensaje =
          'Solicitud creada correctamente.';
      }

      await this.cargar();
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No se pudo guardar la solicitud.';
    } finally {
      this.guardando = false;
    }
  }

  editar(
    solicitud: Solicitud,
  ): void {
    this.editando = solicitud;

    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  }

  cancelarEdicion(): void {
    this.editando = null;
  }

  async eliminar(
    solicitud: Solicitud,
  ): Promise<void> {
    const confirmed = window.confirm(
      `¿Eliminar la solicitud #${solicitud.id}?`,
    );

    if (!confirmed) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    try {
      await this.api.deleteRequest(
        solicitud.id,
      );

      this.mensaje =
        'Solicitud eliminada correctamente.';

      await this.cargar();
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No se pudo eliminar la solicitud.';
    }
  }
}