import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { RuntimeConfigService } from './runtime-config.service';

describe('ApiService', () => {
  let service: ApiService;

  let authMock:
    jasmine.SpyObj<AuthService>;

  const configMock = {
    value: {
      COGNITO_DOMAIN: '',
      COGNITO_CLIENT_ID: '',
      REDIRECT_URI: '',
      API_URL: 'https://api.example',
    },
    missingApiValues: () => [],
  } as unknown as RuntimeConfigService;

  beforeEach(() => {
    authMock =
      jasmine.createSpyObj<AuthService>(
        'AuthService',
        [
          'getAccessToken',
          'isExpired',
        ],
      );

    authMock
      .getAccessToken
      .and.returnValue(
        'access-token-test',
      );

    authMock
      .isExpired
      .and.returnValue(false);

    service =
      new ApiService(
        authMock,
        configMock,
      );
  });

  afterEach(() => {
    jasmine.getEnv()
      .allowRespy(true);
  });

  it('should send the access token as Bearer when listing requests', async () => {
    const fetchSpy =
      spyOn(window, 'fetch')
        .and.returnValue(
          Promise.resolve(
            new Response(
              JSON.stringify([]),
              {
                status: 200,
                headers: {
                  'Content-Type':
                    'application/json',
                },
              },
            ),
          ),
        );

    await service.listMyRequests();

    expect(fetchSpy)
      .toHaveBeenCalled();

    const [
      url,
      options,
    ] =
      fetchSpy.calls
        .mostRecent()
        .args;

    expect(url)
      .toBe(
        'https://api.example/solicitudes/mias',
      );

    const headers =
      (
        options as RequestInit
      ).headers as Record<string, string>;

    expect(
      headers['Authorization'],
    ).toBe(
      'Bearer access-token-test',
    );
  });

  it('should propagate a clear API error message', async () => {
    spyOn(window, 'fetch')
      .and.returnValue(
        Promise.resolve(
          new Response(
            JSON.stringify({
              message:
                'Operacion no autorizada',
            }),
            {
              status: 403,
              headers: {
                'Content-Type':
                  'application/json',
              },
            },
          ),
        ),
      );

    let message = '';

    try {
      await service.listPendingRequests();
    } catch (error) {
      message =
        error instanceof Error
          ? error.message
          : '';
    }

    expect(message)
      .toBe(
        'Operacion no autorizada',
      );
  });
});