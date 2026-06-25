import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = 'An unexpected error occurred';
      if (error.error?.error) {
        message = error.error.error;
      } else if (error.status === 0) {
        message = 'Cannot connect to server. Please check if the backend is running.';
      } else if (error.status === 404) {
        message = 'Resource not found.';
      } else if (error.status === 500) {
        message = 'Server error. Please try again later.';
      }
      return throwError(() => new Error(message));
    })
  );
};
