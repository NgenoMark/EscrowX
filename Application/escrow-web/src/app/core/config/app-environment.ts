import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment.global';

export type AppEnvironmentMode = 'local' | 'production';

export interface AppEnvironment {
  production: boolean;
  useMockData: boolean;
  apiUrl: string;
  mpesaUrl: string;
  version: string;
}

@Injectable({
  providedIn: 'root'
})
export class AppEnvironmentService {
  readonly config: AppEnvironment = environment;
  readonly mode: AppEnvironmentMode = environment.production ? 'production' : 'local';

  get isProduction(): boolean {
    return this.config.production;
  }

  get useMockData(): boolean {
    return this.config.useMockData;
  }

  get apiUrl(): string {
    return this.config.apiUrl;
  }

  get mpesaUrl(): string {
    return this.config.mpesaUrl;
  }

  get version(): string {
    return this.config.version;
  }
}
