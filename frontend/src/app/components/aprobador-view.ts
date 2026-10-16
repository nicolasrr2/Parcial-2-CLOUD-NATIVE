import {
  Component,
  Input,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import {
  DecisionSolicitud,
  Solicitud,
} from '../core/models';

@Component({
  selector: 'app-aprobador-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
  ],
  templateUrl: './aprobador-view.html',
})
export class AprobadorView implements OnInit {
  @Input() scopes: string[] = [];

  solicitudes: Solicitud[] = [];

  comentarios:
    Record<number, string> = {};

  cargando = true;
  procesando: number | null = null;

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

  get puedeAprobar(): boolean {
    return this.scopes.includes(
      'solicitudes/approve',
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
        await this.api.listPendingRequests();

      this.solicitudes =
        Array.isArray(data)
          ? data
          : [];
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No se pudieron cargar las solicitudes pendientes.';
    } finally {
      this.cargando = false;
    }
  }

  async decidir(
    id: number,
    decision: DecisionSolicitud,
  ): Promise<void> {
    const comentario =
      (this.comentarios[id] ?? '').trim();

    if (!comentario) {
      this.error =
        'El comentario es obligatorio para decidir una solicitud.';
      return;
    }

    this.procesando = id;
    this.error = '';
    this.mensaje = '';

    try {
      await this.api.decideRequest(
        id,
        decision,
        comentario,
      );

      this.mensaje =
        `Solicitud #${id} ${decision.toLowerCase()} correctamente.`;

      delete this.comentarios[id];

      await this.cargar();
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No se pudo registrar la decision.';
    } finally {
      this.procesando = null;
    }
  }
}