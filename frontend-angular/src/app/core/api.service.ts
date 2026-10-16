import { Injectable } from '@angular/core';
import {
  DecisionSolicitud,
  Solicitud,
  SolicitudInput,
} from './models';
import { AuthService } from './auth.service';
import { RuntimeConfigService } from './runtime-config.service';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(
    private readonly auth: AuthService,
    private readonly runtimeConfig: RuntimeConfigService,
  ) {}

  listMyRequests(): Promise<Solicitud[]> {
    return this.request<Solicitud[]>(
      'GET',
      '/solicitudes/mias',
    );
  }

  listPendingRequests(): Promise<Solicitud[]> {
    return this.request<Solicitud[]>(
      'GET',
      '/solicitudes/pendientes',
    );
  }

  getRequest(id: number): Promise<Solicitud> {
    return this.request<Solicitud>(
      'GET',
      `/solicitudes/${encodeURIComponent(id)}`,
    );
  }

  createRequest(
    input: SolicitudInput,
  ): Promise<Solicitud> {
    return this.request<Solicitud>(
      'POST',
      '/solicitudes',
      input,
    );
  }

  updateRequest(
    id: number,
    input: SolicitudInput,
  ): Promise<Solicitud> {
    return this.request<Solicitud>(
      'PUT',
      `/solicitudes/${encodeURIComponent(id)}`,
      input,
    );
  }

  deleteRequest(id: number): Promise<void> {
    return this.request<void>(
      'DELETE',
      `/solicitudes/${encodeURIComponent(id)}`,
    );
  }

  decideRequest(
    id: number,
    decision: DecisionSolicitud,
    comentario: string,
  ): Promise<Solicitud> {
    if (
      decision !== 'APROBADA' &&
      decision !== 'RECHAZADA'
    ) {
      return Promise.reject(
        new Error(
          'La decision debe ser APROBADA o RECHAZADA.',
        ),
      );
    }

    return this.request<Solicitud>(
      'PATCH',
      `/solicitudes/${encodeURIComponent(id)}/decision`,
      {
        decision,
        comentario,
      },
    );
  }

  private async request<T>(
    method: string,
    path: string,
    body?: unknown,
  ): Promise<T> {
    const missing =
      this.runtimeConfig.missingApiValues();

    if (missing.length > 0) {
      throw new Error(
        `Falta configuracion: ${missing.join(', ')}`,
      );
    }

    const token = this.auth.getAccessToken();

    if (!token || this.auth.isExpired(token)) {
      throw new Error(
        'Tu sesion no es valida o ha expirado.',
      );
    }

    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`,
    };

    const options: RequestInit = {
      method,
      headers,
      cache: 'no-store',
    };

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }

    let response: Response;

    try {
      response = await fetch(
        `${this.runtimeConfig.value.API_URL}${path}`,
        options,
      );
    } catch {
      throw new Error(
        'No fue posible comunicarse con la API. Revisa la conexion o CORS.',
      );
    }

    if (response.status === 204) {
      if (!response.ok) {
        throw new Error(
          this.messageForStatus(response.status),
        );
      }

      return undefined as T;
    }

    const text = await response.text();

    let responseBody: unknown = null;

    if (text) {
      try {
        responseBody = JSON.parse(text);
      } catch {
        responseBody = text;
      }
    }

    if (!response.ok) {
      let detail = '';

      if (
        responseBody &&
        typeof responseBody === 'object'
      ) {
        const object =
          responseBody as Record<string, unknown>;

        detail = String(
          object['message'] ??
            object['error'] ??
            '',
        );
      } else if (typeof responseBody === 'string') {
        detail = responseBody;
      }

      throw new Error(
        detail ||
          this.messageForStatus(response.status),
      );
    }

    return responseBody as T;
  }

  private messageForStatus(status: number): string {
    const messages: Record<number, string> = {
      400: 'La solicitud contiene datos invalidos.',
      401: 'Tu sesion no es valida o ha expirado.',
      403: 'No tienes permisos para realizar esta operacion.',
      404: 'La solicitud no fue encontrada.',
      409: 'Existe un conflicto al procesar la solicitud.',
    };

    return messages[status] ??
      `La API respondio con HTTP ${status}.`;
  }
}