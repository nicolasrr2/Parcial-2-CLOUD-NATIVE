import {
  Component,
  Input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { JwtClaims } from '../core/models';

@Component({
  selector: 'app-token-claims',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './token-claims.html',
})
export class TokenClaims {
  @Input() claims: JwtClaims | null = null;

  get groups(): string {
    const value =
      this.claims?.['cognito:groups'];

    if (Array.isArray(value)) {
      return value.join(', ') || 'Sin grupos';
    }

    return typeof value === 'string'
      ? value
      : 'Sin grupos';
  }

  get scope(): string {
    const value =
      this.claims?.scope;

    if (Array.isArray(value)) {
      return value.join(' ');
    }

    return typeof value === 'string'
      ? value
      : 'Sin scopes';
  }
}