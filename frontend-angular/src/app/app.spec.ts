import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { AuthService } from './core/auth.service';
import { RuntimeConfigService } from './core/runtime-config.service';

describe('App', () => {
  let authMock: jasmine.SpyObj<AuthService>;
  let configMock: jasmine.SpyObj<RuntimeConfigService>;

  beforeEach(async () => {
    authMock = jasmine.createSpyObj<AuthService>(
      'AuthService',
      [
        'processReturn',
        'getAccessToken',
        'isExpired',
        'getGroups',
        'getScopes',
        'getAccessTokenClaims',
        'login',
        'logout',
      ],
    );

    configMock = jasmine.createSpyObj<RuntimeConfigService>(
      'RuntimeConfigService',
      ['load'],
    );

    configMock.load.and.returnValue(
      Promise.resolve(),
    );

    authMock.processReturn.and.returnValue(
      Promise.resolve(null),
    );

    authMock.getAccessToken.and.returnValue(null);
    authMock.getGroups.and.returnValue([]);
    authMock.getScopes.and.returnValue([]);
    authMock.getAccessTokenClaims.and.returnValue(null);

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: AuthService,
          useValue: authMock,
        },
        {
          provide: RuntimeConfigService,
          useValue: configMock,
        },
      ],
    }).compileComponents();
  });

  it('should create the Pedidos360 app', () => {
    const fixture =
      TestBed.createComponent(App);

    expect(
      fixture.componentInstance,
    ).toBeTruthy();
  });

  it('should render Pedidos360 title', async () => {
    const fixture =
      TestBed.createComponent(App);

    fixture.detectChanges();

    await fixture.whenStable();

    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    expect(
      compiled.querySelector('h1')?.textContent,
    ).toContain('Pedidos360');
  });

  it('should offer Cognito login when there is no active session', async () => {
    const fixture =
      TestBed.createComponent(App);

    fixture.detectChanges();

    await fixture.whenStable();

    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    expect(
      compiled.textContent,
    ).toContain('Iniciar sesión con Cognito');
  });
});