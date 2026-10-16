import { AuthService } from './auth.service';
import { RuntimeConfigService } from './runtime-config.service';

describe('AuthService', () => {
  let service: AuthService;

  const configMock = {
    value: {
      COGNITO_DOMAIN: 'https://cognito.example',
      COGNITO_CLIENT_ID: 'client-test',
      REDIRECT_URI: 'http://localhost:4200/',
      API_URL: 'https://api.example',
    },
    missingAuthValues: () => [],
  } as unknown as RuntimeConfigService;

  function encode(value: unknown): string {
    return btoa(
      JSON.stringify(value),
    )
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  function token(
    payload: Record<string, unknown>,
  ): string {
    return [
      encode({
        alg: 'none',
        typ: 'JWT',
      }),
      encode(payload),
      'signature',
    ].join('.');
  }

  beforeEach(() => {
    sessionStorage.clear();

    service =
      new AuthService(configMock);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should decode access token claims', () => {
    const jwt = token({
      sub: 'usuario-123',
      token_use: 'access',
      exp:
        Math.floor(Date.now() / 1000)
        + 3600,
    });

    const claims =
      service.decodeJwt(jwt);

    expect(claims?.sub)
      .toBe('usuario-123');

    expect(claims?.token_use)
      .toBe('access');
  });

  it('should read groups and scopes from the access token', () => {
    const jwt = token({
      exp:
        Math.floor(Date.now() / 1000)
        + 3600,
      'cognito:groups': [
        'solicitantes',
      ],
      scope:
        'openid solicitudes/read solicitudes/write',
    });

    sessionStorage.setItem(
      'DSY1107_tokens',
      JSON.stringify({
        access_token: jwt,
      }),
    );

    expect(service.getGroups())
      .toEqual([
        'solicitantes',
      ]);

    expect(service.getScopes())
      .toContain(
        'solicitudes/read',
      );

    expect(service.getScopes())
      .toContain(
        'solicitudes/write',
      );
  });

  it('should detect expired access tokens', () => {
    const jwt = token({
      exp:
        Math.floor(Date.now() / 1000)
        - 60,
    });

    expect(
      service.isExpired(jwt),
    ).toBeTrue();
  });
});