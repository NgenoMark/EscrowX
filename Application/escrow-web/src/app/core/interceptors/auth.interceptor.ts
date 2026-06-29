import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('access_token');
    const user = this.authService.user(); // Get the current user

    // Start with base headers
    let headers = req.headers
      .set('Content-Type', 'application/json')
      .set('ngrok-skip-browser-warning', 'true');

    // Add token if exists
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    // Add X-Actor-User-Id for admin endpoints
    if (user?.id && this.isAdminEndpoint(req.url)) {
      headers = headers.set('X-Actor-User-Id', user.id);
    }

    const cloned = req.clone({ headers });
    return next.handle(cloned);
  }

  /**
   * Check if the request is to an admin endpoint that requires X-Actor-User-Id
   */
  private isAdminEndpoint(url: string): boolean {
    const adminEndpoints = [
      '/admin/disputes',
      '/admin/ledger-entries',
      '/admin/payouts',
      '/admin/payment-intents',
      '/audit-logs',
      '/transactions/'
    ];
    return adminEndpoints.some(endpoint => url.includes(endpoint));
  }
}