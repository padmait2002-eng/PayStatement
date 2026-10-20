# Paystatement

Spring Boot REST services and a React web application for weekly pay statements, employee adjustment requests, specialist approvals, configurable pay rules, and Kafka integration.

## Build and run locally

Requirements: Java 21 and Maven 3.9+. Node.js/npm are installed by the Maven frontend plugin.

From the repository root, build the project once:

```text
mvn clean package
```

Run the backend and frontend in separate terminals:

```text
java -jar App/target/paystatement-app-1.0.0.jar
```

```text
cd "Front end"
npm install
npm run dev
```

Open the React home page at `http://localhost:5173`. The React app calls the Spring Boot API directly at `http://localhost:8080`.

The separate Angular frontend is in `Angular front end`; run it in its own terminal:

```text
cd "Angular front end"
npm install
npm start
```

Open the Angular home page at `http://localhost:4200`. Both browser apps call the same Spring Boot API at `http://localhost:8080`; the backend allows these development origins for CORS. The local profile uses an in-memory H2 database, so records are cleared on restart. Local requests use `X-Employee-Id` and `X-User-Role` headers, exposed as the identity controls in each UI. This is for local development only; do not enable local profile in a shared environment.

Use the Admin setup tab to add an employee and their hourly rate, enter their hours for a week ending date, and configure federal/regional tax rules and deductions. Regional tax rules apply to employees assigned to that region. Pay generation requires a configured employee rate and weekly-hours record.

## Payroll behavior

The first 40 weekly work hours are paid at the configured regular hourly rate; additional hours are paid at 1.5×. Incentives are excluded. Approved employee requests require an active admin-configured pay-code rule. Rules determine whether requested hours count toward the overtime threshold and the multiplier for separate paid entitlements (such as sick time). Approval regenerates the statement when that week's hours and pay-code rule have been configured; otherwise configure those inputs before approving.

Tax and deduction rules are persisted in H2. Supported rules are percentage of gross pay or a flat amount per weekly statement. Tax calculation is intentionally rule-driven; no federal or region-specific rates, tax brackets, caps, pre-tax treatments, or legal withholding advice are bundled. Configure values with your payroll specialist before using generated net pay operationally.

## API and Kafka

- `GET /api/statements` — authenticated employee's statements.
- `GET /api/statements/employee/{employeeId}/status?weekEnding=YYYY-MM-DD` — statement status; employees may read only their own, while privileged roles may read others.
- `POST /api/requests` — submit hours/pay-code update for approval.
- `GET /api/specialist/requests` and `POST /api/specialist/requests/{id}/decision?approve=true|false` — specialist queue and decisions.
- `POST /api/statements/generate` — generate a weekly statement (ADMIN, PAYMENT_SPECIALIST, or SYSTEM role).
- Admin configuration endpoints (including employee pay, weekly hours, tax/deduction, and pay-code rules) are under `/api/admin`.

Kafka generation requests are consumed from `paystatement.generate` as JSON with `employeeId` and `weekEnding` fields. Generated-statement notifications are published to `paystatement.generated`, keyed by employee ID, with `statementId`, `employeeId`, `weekEnding`, `status`, and `generatedAt`. Kafka is disabled by default locally; set `PAYSTATEMENT_KAFKA_ENABLED=true` and `KAFKA_BOOTSTRAP_SERVERS` to enable it.

Set `SPRING_PROFILES_ACTIVE=dev`, `uat`, or `prod` outside local. Those profiles require `ADFS_ISSUER_URI` and validate JWTs; employee identity is read from `employeeId`, then `preferred_username`, then JWT subject. JWT `roles` claims must include `ADMIN`, `PAYMENT_SPECIALIST`, or `SYSTEM` as needed. Paste the ADFS access token into the UI's token field to authorize browser requests; the UI does not implement the ADFS sign-in/token issuance flow. Each profile defaults to an in-memory H2 database; set `DATABASE_URL`, `DATABASE_DRIVER`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` to override it for a deployment.
