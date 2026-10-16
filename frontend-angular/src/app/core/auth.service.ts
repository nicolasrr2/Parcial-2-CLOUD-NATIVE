import { Injectable } from '@angular/core';
import { JwtClaims, OAuthTokens } from './models';
import { RuntimeConfigService } from './runtime-config.service';

const TOKENS_KEY = 'DSY1107_tokens';
const PKCE_VERIFIER_KEY = 'DSY1107_pkce_verifier';
const OAUTH_STATE_KEY = 'DSY1107_oauth_state';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly scopes = 'openid email profile';

  constructor(
    private readonly runtimeConfig: RuntimeConfigService,
  ) {}

  async login(): Promise<void> {
    const missing = this.runtimeConfig.missingAuthValues();

    if (missing.length > 0) {
      throw new Error(
        `Falta configuracion: ${missing.join(', ')}`,
      );
    }

    const verifier = this.randomText(32);
    const state = this.randomText(32);
    const challenge = await this.createCodeChallenge(verifier);

    sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier);
    sessionStorage.setItem(OAUTH_STATE_KEY, state);

    const config = this.runtimeConfig.value;

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: config.COGNITO_CLIENT_ID,
      redirect_uri: config.REDIRECT_URI,
      scope: this.scopes,
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });

    window.location.assign(
      `${config.COGNITO_DOMAIN}/oauth2/authorize?${params.toString()}`,
    );
  }

  async processReturn(): Promise<OAuthTokens | null> {
    const url = new URL(window.location.href);

    const oauthError = url.searchParams.get('error');

    if (oauthError) {
      const description =
        url.searchParams.get('error_description') ?? oauthError;

      this.cleanOAuthParameters(url);

      throw new Error(
        `Cognito rechazo el inicio de sesion: ${description}`,
      );
    }

    const code = url.searchParams.get('code');

    if (!code) {
      return null;
    }

    const state = url.searchParams.get('state');
    const storedState = sessionStorage.getItem(OAUTH_STATE_KEY);

    if (!state || !storedState || state !== storedState) {
      this.cleanOAuthParameters(url);

      throw new Error(
        'El estado OAuth no coincide. Se detuvo el retorno por seguridad.',
      );
    }

    const verifier =
      sessionStorage.getItem(PKCE_VERIFIER_KEY);

    if (!verifier) {
      this.cleanOAuthParameters(url);

      throw new Error(
        'No se encontro el code_verifier de PKCE en la sesion.',
      );
    }

    const missing = this.runtimeConfig.missingAuthValues();

    if (missing.length > 0) {
      throw new Error(
        `Falta configuracion: ${missing.join(', ')}`,
      );
    }

    const config = this.runtimeConfig.value;

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: config.COGNITO_CLIENT_ID,
      code,
      redirect_uri: config.REDIRECT_URI,
      code_verifier: verifier,
    });

    let response: Response;

    try {
      response = await fetch(
        `${config.COGNITO_DOMAIN}/oauth2/token`,
        {
          method: 'POST',
          headers: {
            'Content-Type':
              'application/x-www-form-urlencoded',
          },
          body,
        },
      );
    } catch {
      throw new Error(
        'No fue posible comunicarse con Cognito para canjear el codigo.',
      );
    }

    const text = await response.text();

    let data: Record<string, unknown>;

    try {
      data = JSON.parse(text) as Record<string, unknown>;
    } catch {
      throw new Error(
        'Cognito devolvio una respuesta invalida durante el canje del codigo.',
      );
    }

    if (!response.ok) {
      throw new Error(
        String(
          data['error_description'] ??
            data['error'] ??
            `Error de Cognito HTTP ${response.status}.`,
        ),
      );
    }

    const tokens = data as unknown as OAuthTokens;

    if (!tokens.access_token) {
      throw new Error(
        'Cognito no devolvio un access_token.',
      );
    }

    sessionStorage.setItem(
      TOKENS_KEY,
      JSON.stringify(tokens),
    );

    sessionStorage.removeItem(PKCE_VERIFIER_KEY);
    sessionStorage.removeItem(OAUTH_STATE_KEY);

    this.cleanOAuthParameters(url);

    return tokens;
  }

  getTokens(): OAuthTokens | null {
    const stored = sessionStorage.getItem(TOKENS_KEY);

    if (!stored) {
      return null;
    }

    try {
      return JSON.parse(stored) as OAuthTokens;
    } catch {
      sessionStorage.removeItem(TOKENS_KEY);
      return null;
    }
  }

  getAccessToken(): string | null {
    return this.getTokens()?.access_token ?? null;
  }

  getIdToken(): string | null {
    return this.getTokens()?.id_token ?? null;
  }

  getAccessTokenClaims(): JwtClaims | null {
    const token = this.getAccessToken();

    if (!token || this.isExpired(token)) {
      return null;
    }

    return this.decodeJwt(token);
  }

  getGroups(): string[] {
    const groups =
      this.getAccessTokenClaims()?.['cognito:groups'];

    if (Array.isArray(groups)) {
      return groups.map(String);
    }

    if (typeof groups === 'string') {
      return groups.split(' ').filter(Boolean);
    }

    return [];
  }

  getScopes(): string[] {
    const scope =
      this.getAccessTokenClaims()?.scope;

    if (Array.isArray(scope)) {
      return scope.map(String);
    }

    if (typeof scope === 'string') {
      return scope.split(' ').filter(Boolean);
    }

    return [];
  }

  hasGroup(group: string): boolean {
    return this.getGroups().includes(group);
  }

  hasScope(scope: string): boolean {
    return this.getScopes().includes(scope);
  }

  decodeJwt(token: string): JwtClaims | null {
    try {
      const encodedPayload = token.split('.')[1];

      if (!encodedPayload) {
        return null;
      }

      const normalized = encodedPayload
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const padding =
        '='.repeat((4 - (normalized.length % 4)) % 4);

      const binary = atob(normalized + padding);

      const bytes = Uint8Array.from(
        binary,
        (character) => character.charCodeAt(0),
      );

      const json = new TextDecoder().decode(bytes);

      return JSON.parse(json) as JwtClaims;
    } catch {
      return null;
    }
  }

  isExpired(token: string): boolean {
    const claims = this.decodeJwt(token);

    return !claims?.exp ||
      claims.exp * 1000 <= Date.now();
  }

  logout(): void {
    sessionStorage.removeItem(TOKENS_KEY);
    sessionStorage.removeItem(PKCE_VERIFIER_KEY);
    sessionStorage.removeItem(OAUTH_STATE_KEY);

    const config = this.runtimeConfig.value;

    if (
      !config.COGNITO_DOMAIN ||
      !config.COGNITO_CLIENT_ID ||
      !config.REDIRECT_URI
    ) {
      window.location.reload();
      return;
    }

    const params = new URLSearchParams({
      client_id: config.COGNITO_CLIENT_ID,
      logout_uri: config.REDIRECT_URI,
    });

    window.location.assign(
      `${config.COGNITO_DOMAIN}/logout?${params.toString()}`,
    );
  }

  private cleanOAuthParameters(url: URL): void {
    [
      'code',
      'state',
      'error',
      'error_description',
      'error_uri',
    ].forEach((key) => {
      url.searchParams.delete(key);
    });

    window.history.replaceState(
      {},
      document.title,
      `${url.pathname}${url.search}${url.hash}`,
    );
  }

  private randomText(byteLength = 32): string {
    const bytes = new Uint8Array(byteLength);

    crypto.getRandomValues(bytes);

    let binary = '';

    bytes.forEach((byte) => {
      binary += String.fromCharCode(byte);
    });

    return btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  private async createCodeChallenge(
    verifier: string,
  ): Promise<string> {
    const data =
      new TextEncoder().encode(verifier);

    const hash =
      await crypto.subtle.digest('SHA-256', data);

    let binary = '';

    new Uint8Array(hash).forEach((byte) => {
      binary += String.fromCharCode(byte);
    });

    return btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }
}