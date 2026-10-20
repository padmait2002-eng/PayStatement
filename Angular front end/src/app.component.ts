import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

type Region = 'NORTH' | 'SOUTH' | 'WEST' | 'EAST' | 'CENTRAL';
type PayStatement = {
  id: number;
  employeeId: string;
  weekEnding: string;
  regularHours: number;
  overtimeHours: number;
  otherPaidHours: number;
  grossPay: number;
  taxes: number;
  deductions: number;
  netPay: number;
  status: string;
};
type ChangeRequest = {
  id: number;
  employeeId: string;
  weekEnding: string;
  hours: number;
  payCode: string;
  explanation: string;
  status: string;
};
type Employee = { employeeId: string; name: string; region: Region };
type PayRule = { id: number; name: string; scope?: string; region?: Region; kind: string; value: number };
type PayCode = {
  payCode: string;
  description: string;
  multiplier: number;
  countsTowardOvertime: boolean;
};
type UiRole = 'EMPLOYEE' | 'PAYMENT_SPECIALIST' | 'ADMIN';
type RuleForm = { name: string; scope: 'FEDERAL' | 'REGION'; region: Region; kind: 'PERCENTAGE' | 'FLAT'; value: number };
type PayCodeForm = { payCode: string; description: string; multiplier: number; countsTowardOvertime: boolean };

const API_BASE = 'http://localhost:8080/api';
const REGIONS: Region[] = ['NORTH', 'SOUTH', 'WEST', 'EAST', 'CENTRAL'];

@Component({
  selector: 'paystatement-app',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="topbar">
      <a class="brand" href="/" aria-label="Paystatement home"><span class="mark">P</span><span><b>Paystatement</b><small>Angular employee pay center</small></span></a>
      <div class="identity">
        <input aria-label="Employee ID" [(ngModel)]="employeeId" (change)="saveIdentity()" placeholder="Employee ID">
        <select aria-label="Local user role" [(ngModel)]="role" (change)="saveIdentity()">
          <option value="EMPLOYEE">EMPLOYEE</option>
          <option value="PAYMENT_SPECIALIST">PAYMENT_SPECIALIST</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <input aria-label="ADFS bearer token" class="token" type="password" autocomplete="off" [(ngModel)]="token"
          (change)="saveIdentity()" placeholder="ADFS token (dev/UAT/prod)">
        <button class="refresh" type="button" (click)="load()">Refresh</button>
      </div>
    </header>

    <main>
      <section class="hero">
        <div><span class="eyebrow">WEEKLY PAY · ANGULAR</span><h1>Your pay, clearly stated.</h1>
          <p>Search statements, request a change, or review payroll activity.</p></div>
        <span class="hero-icon" aria-hidden="true">▤</span>
      </section>

      <div *ngIf="message" class="notice" role="status">{{ message }}<button type="button" (click)="message = ''">×</button></div>
      <div *ngIf="error" class="error" role="alert">{{ error }}<button type="button" (click)="error = ''">×</button></div>

      <nav class="tabs" aria-label="Paystatement sections">
        <button [class.selected]="tab === 'statements'" (click)="tab = 'statements'">Pay statements</button>
        <button [class.selected]="tab === 'modify'" (click)="tab = 'modify'">Request a change</button>
        <button *ngIf="privileged" [class.selected]="tab === 'approvals'" (click)="tab = 'approvals'">Approvals</button>
        <button *ngIf="privileged" [class.selected]="tab === 'admin'" (click)="tab = 'admin'">Admin setup</button>
      </nav>

      <section *ngIf="tab === 'statements'" class="panel">
        <div class="section-heading"><div><h2>Pay statements</h2><p>Weekly earnings, taxes, deductions, and net pay.</p></div>
          <label class="search">⌕ <input aria-label="Search pay statements" [(ngModel)]="query"
            placeholder="Search by week ending or statement ID"></label></div>
        <form *ngIf="privileged" class="inline-form" (ngSubmit)="generateStatement()">
          <label>Employee<select name="statementEmployee" [(ngModel)]="generateEmployeeId" required>
            <option *ngFor="let person of employees" [value]="person.employeeId">{{ person.name }} ({{ person.employeeId }})</option>
          </select></label>
          <label>Week ending<input name="statementWeek" type="date" [(ngModel)]="generateWeekEnding" required></label>
          <button class="primary" type="submit">Generate statement</button>
        </form>
        <div class="table-wrap"><table><thead><tr><th>Week ending</th><th>Hours</th><th>Gross pay</th><th>Taxes</th><th>Deductions</th><th>Net pay</th><th>Status</th></tr></thead>
          <tbody>
            <tr *ngFor="let statement of visibleStatements">
              <td>{{ statement.weekEnding }}</td>
              <td>{{ hours(statement) }} <small>({{ statement.overtimeHours }} OT · {{ statement.otherPaidHours || 0 }} entitlement)</small></td>
              <td>{{ money(statement.grossPay) }}</td><td>{{ money(statement.taxes) }}</td><td>{{ money(statement.deductions) }}</td>
              <td class="net">{{ money(statement.netPay) }}</td><td><span class="badge">{{ statement.status }}</span></td>
            </tr>
            <tr *ngIf="!visibleStatements.length"><td colspan="7" class="empty">No matching statements yet.</td></tr>
          </tbody>
        </table></div>
      </section>

      <section *ngIf="tab === 'modify'" class="panel narrow">
        <h2>Request a pay change</h2><p>Request additional hours or an entitlement/pay-code correction for specialist approval.</p>
        <form class="form-grid" (ngSubmit)="submitChange()">
          <label>Week ending<input name="requestWeek" type="date" [(ngModel)]="requestForm.weekEnding" required></label>
          <label>Hours to add<input name="requestHours" type="number" min="0.01" max="168" step="0.01" [(ngModel)]="requestForm.hours" required></label>
          <label>Pay code<input name="requestCode" [(ngModel)]="requestForm.payCode" placeholder="REGULAR, OVERTIME, SICK" required></label>
          <label class="wide">Explanation<textarea name="requestExplanation" rows="3" [(ngModel)]="requestForm.explanation" required></textarea></label>
          <button class="primary" type="submit">Submit for approval</button>
        </form>
        <h3>Your requests</h3><ng-container *ngTemplateOutlet="requestList; context: { items: requests, canReview: false }"></ng-container>
      </section>

      <section *ngIf="tab === 'approvals' && privileged" class="panel">
        <h2>Specialist review queue</h2><p>Approved changes regenerate an existing statement when hours and pay-code rules are configured.</p>
        <ng-container *ngTemplateOutlet="requestList; context: { items: requests, canReview: true }"></ng-container>
      </section>

      <section *ngIf="tab === 'admin' && privileged" class="panel">
        <h2>Payroll configuration</h2><p>Configure employee pay rates, hours, withholding, deductions, and entitlement rules.</p>
        <div class="admin-grid">
          <form class="admin-card" (ngSubmit)="saveEmployee()"><h3>Employee and hourly rate</h3>
            <label>Employee ID<input name="employeeId" [(ngModel)]="employeeForm.employeeId" required></label>
            <label>Full name<input name="employeeName" [(ngModel)]="employeeForm.name" required></label>
            <label>Region<select name="employeeRegion" [(ngModel)]="employeeForm.region"><option *ngFor="let region of regions" [value]="region">{{ region }}</option></select></label>
            <label>Incentives (excluded from gross pay)<input name="employeeIncentives" type="number" min="0" step="0.01" [(ngModel)]="employeeForm.incentives"></label>
            <label>Regular hourly rate ($)<input name="hourlyRate" type="number" min="0.01" step="0.01" [(ngModel)]="employeeForm.rate" required></label>
            <button class="primary" type="submit">Save employee</button>
          </form>
          <form class="admin-card" (ngSubmit)="saveHours()"><h3>Weekly hours</h3>
            <label>Employee<select name="hoursEmployee" [(ngModel)]="hoursForm.employeeId" required><option *ngFor="let person of employees" [value]="person.employeeId">{{ person.name }} ({{ person.employeeId }})</option></select></label>
            <label>Week ending<input name="hoursWeek" type="date" [(ngModel)]="hoursForm.weekEnding" required></label>
            <label>Hours worked<input name="workedHours" type="number" min="0" max="168" step="0.01" [(ngModel)]="hoursForm.hoursWorked" required></label>
            <button class="primary" type="submit">Save weekly hours</button>
          </form>
          <form class="admin-card" (ngSubmit)="saveTax()"><h3>Federal / regional tax</h3>
            <label>Rule name<input name="taxName" [(ngModel)]="taxForm.name" required></label>
            <label>Scope<select name="taxScope" [(ngModel)]="taxForm.scope"><option value="FEDERAL">FEDERAL</option><option value="REGION">REGION</option></select></label>
            <label *ngIf="taxForm.scope === 'REGION'">Region<select name="taxRegion" [(ngModel)]="taxForm.region"><option *ngFor="let region of regions" [value]="region">{{ region }}</option></select></label>
            <ng-container *ngTemplateOutlet="ruleFields; context: { form: taxForm, prefix: 'tax' }"></ng-container>
            <button class="primary" type="submit">Save tax rule</button>
          </form>
          <form class="admin-card" (ngSubmit)="saveDeduction()"><h3>Deduction rule</h3>
            <label>Rule name<input name="deductionName" [(ngModel)]="deductionForm.name" required></label>
            <ng-container *ngTemplateOutlet="ruleFields; context: { form: deductionForm, prefix: 'deduction' }"></ng-container>
            <button class="primary" type="submit">Save deduction</button>
          </form>
          <form class="admin-card" (ngSubmit)="savePayCode()"><h3>Entitlement / pay code</h3>
            <label>Pay code<input name="code" [(ngModel)]="payCodeForm.payCode" placeholder="SICK" required></label>
            <label>Description<input name="codeDescription" [(ngModel)]="payCodeForm.description" required></label>
            <label>Pay multiplier<input name="codeMultiplier" type="number" min="0" step="0.01" [(ngModel)]="payCodeForm.multiplier" required></label>
            <label class="check"><input name="countsOvertime" type="checkbox" [(ngModel)]="payCodeForm.countsTowardOvertime"> Counts toward overtime threshold</label>
            <button class="primary" type="submit">Save pay code</button>
          </form>
        </div>
        <div class="rules">
          <div><h3>Tax rules</h3><span *ngFor="let rule of taxes" class="chip">{{ rule.name }} · {{ rule.scope }}{{ rule.region ? ' / ' + rule.region : '' }} · {{ ruleValue(rule) }}</span></div>
          <div><h3>Deductions</h3><span *ngFor="let rule of deductions" class="chip">{{ rule.name }} · {{ ruleValue(rule) }}</span></div>
          <div><h3>Pay codes</h3><span *ngFor="let code of payCodes" class="chip">{{ code.payCode }} · {{ code.multiplier }}×</span></div>
        </div>
        <p class="fine-print">Tax and deduction rules are configurable examples; they do not implement jurisdiction-specific tax brackets or tax advice.</p>
      </section>
      <footer>Overtime starts after 40 work hours per week at 1.5× the regular rate. Incentives are excluded from gross pay.</footer>
    </main>

    <ng-template #requestList let-items="items" let-canReview="canReview">
      <div *ngIf="!items.length" class="empty-box">No requests yet.</div>
      <article *ngFor="let request of items" class="request">
        <div><strong>{{ request.employeeId }}</strong><span>{{ request.weekEnding }} · {{ request.payCode }} · {{ request.hours }} hours</span><p>{{ request.explanation }}</p></div>
        <div class="actions"><span class="badge" [class.pending]="request.status === 'PENDING'" [class.rejected]="request.status === 'REJECTED'">{{ request.status }}</span>
          <button *ngIf="canReview && request.status === 'PENDING'" class="approve" type="button" (click)="decide(request.id, true)">Approve</button>
          <button *ngIf="canReview && request.status === 'PENDING'" class="reject" type="button" (click)="decide(request.id, false)">Reject</button>
        </div>
      </article>
    </ng-template>

    <ng-template #ruleFields let-form="form" let-prefix="prefix">
      <label>Calculation<select [name]="prefix + 'Kind'" [(ngModel)]="form.kind"><option value="PERCENTAGE">Percentage of gross</option><option value="FLAT">Flat amount per statement</option></select></label>
      <label>Value<input [name]="prefix + 'Value'" type="number" min="0" step="0.01" [(ngModel)]="form.value" required></label>
    </ng-template>
  `
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly regions = REGIONS;
  role: UiRole = this.readRole();
  employeeId = localStorage.getItem('employeeId') || 'local-employee';
  token = '';
  tab: 'statements' | 'modify' | 'approvals' | 'admin' = 'statements';
  query = '';
  message = '';
  error = '';
  statements: PayStatement[] = [];
  requests: ChangeRequest[] = [];
  employees: Employee[] = [];
  taxes: PayRule[] = [];
  deductions: PayRule[] = [];
  payCodes: PayCode[] = [];
  generateEmployeeId = '';
  generateWeekEnding = '';
  requestForm = { weekEnding: '', hours: 1, payCode: '', explanation: '' };
  employeeForm = { employeeId: '', name: '', region: 'NORTH' as Region, incentives: 0, rate: 0 };
  hoursForm = { employeeId: '', weekEnding: '', hoursWorked: 0 };
  taxForm: RuleForm = { name: '', scope: 'FEDERAL', region: 'NORTH', kind: 'PERCENTAGE', value: 0 };
  deductionForm: RuleForm = { name: '', scope: 'FEDERAL', region: 'NORTH', kind: 'PERCENTAGE', value: 0 };
  payCodeForm: PayCodeForm = { payCode: '', description: '', multiplier: 1, countsTowardOvertime: false };

  get privileged(): boolean {
    return this.role === 'ADMIN' || this.role === 'PAYMENT_SPECIALIST';
  }

  get visibleStatements(): PayStatement[] {
    const term = this.query.trim().toLowerCase();
    if (!term) return this.statements;
    return this.statements.filter(statement =>
      statement.weekEnding.includes(term) || String(statement.id).includes(term));
  }

  ngOnInit(): void {
    void this.load();
  }

  saveIdentity(): void {
    localStorage.setItem('employeeId', this.employeeId);
    localStorage.setItem('role', this.role);
    void this.load();
  }

  async load(): Promise<void> {
    try {
      this.error = '';
      if (this.privileged) {
        const [employees, taxes, deductions, payCodes, requests] = await Promise.all([
          this.get<Employee[]>('/admin/employees'),
          this.get<PayRule[]>('/admin/rules/taxes'),
          this.get<PayRule[]>('/admin/rules/deductions'),
          this.get<PayCode[]>('/admin/rules/pay-codes'),
          this.get<ChangeRequest[]>('/specialist/requests')
        ]);
        this.employees = employees;
        this.taxes = taxes;
        this.deductions = deductions;
        this.payCodes = payCodes;
        this.requests = requests;
        if (employees.some(person => person.employeeId === this.employeeId)) {
          this.statements = await this.get<PayStatement[]>(`/admin/statements?employeeId=${encodeURIComponent(this.employeeId)}`);
        } else {
          this.statements = [];
        }
      } else {
        const [statements, requests] = await Promise.all([
          this.get<PayStatement[]>('/statements'),
          this.get<ChangeRequest[]>('/requests/mine')
        ]);
        this.statements = statements;
        this.requests = requests;
      }
    } catch (error: unknown) {
      this.showError(error);
    }
  }

  async submitChange(): Promise<void> {
    await this.action(async () => {
      await this.post('/requests', {
        weekEnding: this.requestForm.weekEnding,
        hours: Number(this.requestForm.hours),
        payCode: this.requestForm.payCode,
        explanation: this.requestForm.explanation
      });
      this.requestForm = { weekEnding: '', hours: 1, payCode: '', explanation: '' };
      this.message = 'Change request submitted for payment specialist approval.';
      await this.load();
    });
  }

  async saveEmployee(): Promise<void> {
    await this.action(async () => {
      await this.post('/admin/employees', {
        employeeId: this.employeeForm.employeeId,
        name: this.employeeForm.name,
        region: this.employeeForm.region,
        incentives: Number(this.employeeForm.incentives),
        active: true
      });
      await this.put(`/admin/employees/${encodeURIComponent(this.employeeForm.employeeId)}/compensation`, {
        regularHourlyRate: Number(this.employeeForm.rate)
      });
      this.employeeId = this.employeeForm.employeeId;
      this.saveEmployeeId();
      this.message = 'Employee and hourly rate saved.';
      await this.load();
    });
  }

  async saveHours(): Promise<void> {
    await this.action(async () => {
      await this.post('/admin/weekly-hours', {
        employeeId: this.hoursForm.employeeId,
        weekEnding: this.hoursForm.weekEnding,
        hoursWorked: Number(this.hoursForm.hoursWorked)
      });
      this.message = 'Weekly hours saved.';
    });
  }

  async saveTax(): Promise<void> {
    await this.saveRule('taxes', this.taxForm);
  }

  async saveDeduction(): Promise<void> {
    await this.saveRule('deductions', this.deductionForm);
  }

  async savePayCode(): Promise<void> {
    await this.action(async () => {
      await this.post('/admin/rules/pay-codes', {
        ...this.payCodeForm,
        payCode: this.payCodeForm.payCode.trim().toUpperCase(),
        multiplier: Number(this.payCodeForm.multiplier),
        enabled: true
      });
      this.payCodeForm = { payCode: '', description: '', multiplier: 1, countsTowardOvertime: false };
      this.message = 'Pay-code rule saved.';
      await this.load();
    });
  }

  async generateStatement(): Promise<void> {
    await this.action(async () => {
      await this.post('/statements/generate', {
        employeeId: this.generateEmployeeId,
        weekEnding: this.generateWeekEnding
      });
      this.employeeId = this.generateEmployeeId;
      this.saveEmployeeId();
      this.message = 'Statement generated.';
      await this.load();
    });
  }

  async decide(id: number, approve: boolean): Promise<void> {
    await this.action(async () => {
      await this.post(`/specialist/requests/${id}/decision?approve=${approve}`, null);
      this.message = approve ? 'Request approved.' : 'Request rejected.';
      await this.load();
    });
  }

  hours(statement: PayStatement): number {
    return Number(statement.regularHours) + Number(statement.overtimeHours) + Number(statement.otherPaidHours || 0);
  }

  money(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(amount || 0));
  }

  ruleValue(rule: PayRule): string {
    return rule.kind === 'PERCENTAGE' ? `${rule.value}%` : this.money(rule.value);
  }

  private async saveRule(path: 'taxes' | 'deductions', form: RuleForm): Promise<void> {
    await this.action(async () => {
      const rule = {
        name: form.name,
        kind: form.kind,
        value: Number(form.value),
        enabled: true,
        ...(path === 'taxes' ? {
          scope: form.scope,
          region: form.scope === 'REGION' ? form.region : null
        } : {})
      };
      await this.post(`/admin/rules/${path}`, rule);
      this.message = path === 'taxes' ? 'Tax rule saved.' : 'Deduction rule saved.';
      await this.load();
    });
  }

  private async get<T>(path: string): Promise<T> {
    return firstValueFrom(this.http.get<T>(`${API_BASE}${path}`, { headers: this.headers() }));
  }

  private async post<T>(path: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.post<T>(`${API_BASE}${path}`, body, { headers: this.headers() }));
  }

  private async put<T>(path: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.put<T>(`${API_BASE}${path}`, body, { headers: this.headers() }));
  }

  private headers(): HttpHeaders {
    let headers = new HttpHeaders({
      'X-Employee-Id': this.employeeId || 'local-employee',
      'X-User-Role': this.role
    });
    if (this.token.trim()) headers = headers.set('Authorization', `Bearer ${this.token.trim()}`);
    return headers;
  }

  private async action(operation: () => Promise<void>): Promise<void> {
    this.error = '';
    this.message = '';
    try {
      await operation();
    } catch (error: unknown) {
      this.showError(error);
    }
  }

  private showError(error: unknown): void {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const response = error.error;
      if (typeof response === 'object' && response !== null && 'detail' in response
          && typeof response.detail === 'string') {
        this.error = response.detail;
        return;
      }
      if (typeof response === 'string' && response) {
        this.error = response;
        return;
      }
    }
    this.error = error instanceof Error ? error.message : 'The request could not be completed.';
  }

  private saveEmployeeId(): void {
    localStorage.setItem('employeeId', this.employeeId);
  }

  private readRole(): UiRole {
    const saved = localStorage.getItem('role');
    if (saved === 'ADMIN' || saved === 'PAYMENT_SPECIALIST') return saved;
    return 'EMPLOYEE';
  }
}
