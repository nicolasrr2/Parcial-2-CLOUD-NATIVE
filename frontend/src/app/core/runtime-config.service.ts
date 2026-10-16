import { Injectable } from '@angular/core';

export interface RuntimeConfig {
  COGNITO_DOMAIN: string;
  COGNITO_CLIENT_ID: string;
  REDIRECT_URI: string;
  API_URL: string;
}

const EMPTY_CONFIG: RuntimeConfig = {
  COGNITO_DOMAIN: '',
  COGNITO_CLIENT_ID: '',
  REDIRECT_URI: '',
  API_URL: '',
};

@Injectable({
  providedIn: 'root',
})
export class RuntimeConfigService {
  private config: RuntimeConfig = { ...EMPTY_CONFIG };
  private loaded = false;

  async load(): Promise<void> {
    if (this.loaded) {
      return;
    }

    const response = await fetch('/config.json', {
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(
        `No fue posible cargar config.json. HTTP ${response.status}.`,
      );
    }

    const data = (await response.json()) as Partial<RuntimeConfig>;

    this.config = {
      COGNITO_DOMAIN: data.COGNITO_DOMAIN?.trim() ?? '',
      COGNITO_CLIENT_ID: data.COGNITO_CLIENT_ID?.trim() ?? '',
      REDIRECT_URI: data.REDIRECT_URI?.trim() ?? '',
      API_URL: data.API_URL?.trim().replace(/\/+$/, '') ?? '',
    };

    this.loaded = true;
  }

  get value(): Readonly<RuntimeConfig> {
    return this.config;
  }

  missingAuthValues(): string[] {
    return [
      ['COGNITO_DOMAIN', this.config.COGNITO_DOMAIN],
      ['COGNITO_CLIENT_ID', this.config.COGNITO_CLIENT_ID],
      ['REDIRECT_URI', this.config.REDIRECT_URI],
    ]
      .filter(([, value]) => !value)
      .map(([name]) => name);
  }

  missingApiValues(): string[] {
    return this.config.API_URL ? [] : ['API_URL'];
  }
}