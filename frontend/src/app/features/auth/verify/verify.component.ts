import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SessionService } from '../../../core/services/session.service';
import { CommonModule } from '@angular/common';
import { AuthUser } from '../../../core/models/auth.model';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify.component.html',
  styleUrls: ['./verify.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class VerifyComponent implements OnInit {
  verifyForm: FormGroup;
  email: string = '';
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private sessionService: SessionService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.verifyForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });
  }

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    if (!this.email) {
      this.errorMessage = 'Email is required for verification.';
    }
  }

  onSubmit(): void {
    if (this.verifyForm.invalid) return;

    const code = this.verifyForm.value.code;
    this.authService.verifyEmail(this.email, code).subscribe({
      next: (response: AuthUser) => {
        this.router.navigate(['/review']);
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Verification failed. Please check the code.';
      }
    });
  }
}
