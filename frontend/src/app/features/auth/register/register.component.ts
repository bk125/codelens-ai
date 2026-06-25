import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  name = '';
  email = '';
  password = '';
  showPassword = false;
  isLoading = signal(false);
  error = signal<string | null>(null);

  constructor(
    private authService: AuthService,
    private router: Router,
    public themeService: ThemeService
  ) {}

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  validatePassword(password: string): boolean {
    const regex = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;
    return regex.test(password);
  }

  submit() {
    if (!this.name || !this.email || !this.password) return;
    if (!this.validatePassword(this.password)) {
      this.error.set('Password must be at least 8 characters and include at least one letter, one number, and one special character');
      return;
    }
    this.isLoading.set(true);
    this.error.set(null);
    this.authService.register({ name: this.name, email: this.email, password: this.password }).subscribe({
      next: (res) => this.router.navigate(['/verify'], { queryParams: { email: res.email } }),
      error: (err: Error) => { this.error.set(err.message); this.isLoading.set(false); }
    });
  }
}
