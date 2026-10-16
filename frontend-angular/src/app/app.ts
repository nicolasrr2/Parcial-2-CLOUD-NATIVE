import {
  Component,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth.service';
import { RuntimeConfigService } from './core/runtime-config.service';
import { JwtClaims } from './core/models';
import { SolicitanteView } from './components/solicitante-view';
import { AprobadorView } from './components/aprobador-view';
import { TokenClaims } from './components/token-claims';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    SolicitanteView,
    AprobadorView,
    TokenClaims,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  initializing = true;
  error = '';

  authenticated = false;

  groups: string[] = [];
  scopes: string[] = [];

  claims: JwtClaims | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly runtimeConfig: RuntimeConfigService,
  ) {}

  get showSolicitante(): boolean {
    return (
      this.groups.includes('solicitantes')
      || this.scopes.includes('solicitudes/write')
    );
  }

  get showAprobador(): boolean {
    return (
      this.groups.includes('aprobadores')
      || this.scopes.includes('solicitudes/approve')
    );
  }

  async ngOnInit(): Promise<void> {
    try {
      await this.runtimeConfig.load();

      await this.auth.processReturn();

      this.refreshSession();
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No fue posible iniciar la aplicacion.';
    } finally {
      this.initializing = false;
    }
  }

  async login(): Promise<void> {
    this.error = '';

    try {
      await this.auth.login();
    } catch (error) {
      this.error =
        error instanceof Error
          ? error.message
          : 'No fue posible iniciar sesion.';
    }
  }

  logout(): void {
    this.auth.logout();
  }

  private refreshSession(): void {
    const token =
      this.auth.getAccessToken();

    this.authenticated =
      Boolean(token)
      && Boolean(
        token
        && !this.auth.isExpired(token)
      );

    if (!this.authenticated) {
      this.groups = [];
      this.scopes = [];
      this.claims = null;
      return;
    }

    this.groups =
      this.auth.getGroups();

    this.scopes =
      this.auth.getScopes();

    this.claims =
      this.auth.getAccessTokenClaims();
  }
}