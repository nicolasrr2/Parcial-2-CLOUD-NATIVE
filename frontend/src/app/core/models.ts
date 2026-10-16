export interface OAuthTokens {
  access_token: string;
  id_token?: string;
  refresh_token?: string;
  token_type?: string;
  expires_in?: number;
}

export interface JwtClaims {
  exp?: number;
  scope?: string | string[];
  token_use?: string;
  client_id?: string;
  iss?: string;
  email?: string;
  username?: string;
  sub?: string;
  'cognito:groups'?: string[] | string;
  [key: string]: unknown;
}

export interface Solicitud {
  id: number;
  tipo: string;
  fechaInicio: string;
  fechaFin: string;
  motivo: string;
  estado: string;
  comentarioAprobador?: string | null;
  creadoPor?: string | null;
  creadoPorEmail?: string | null;
}

export interface SolicitudInput {
  tipo: string;
  fechaInicio: string;
  fechaFin: string;
  motivo: string;
}

export type DecisionSolicitud = 'APROBADA' | 'RECHAZADA';