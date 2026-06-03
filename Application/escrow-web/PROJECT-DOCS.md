# EscrowX Frontend Documentation

This project is an Angular standalone-component admin console for managing escrow users, transactions, disputes, analytics, and audit logs.

## Runtime Flow

1. `src/main.ts` bootstraps `AppComponent` with the providers from `src/app/app.config.ts`.
2. `AppComponent` renders `LayoutComponent` through `src/app/app.html`.
3. `LayoutComponent` owns the persistent application shell: sidebar, top header, and the router outlet.
4. `src/app/app.routes.ts` maps URL paths to feature pages.
5. Feature pages read and mutate local state through `DataService`.
6. `ApiService` is a backend-ready HTTP service, but the current screens mostly use `DataService` mock data.

## Root Files

### `src/main.ts`
Starts Angular by calling `bootstrapApplication(AppComponent, appConfig)`. This is the application entry point in the browser.

### `src/index.html`
The host HTML document. Angular mounts into the root element declared there.

### `src/styles.css`
Global styling. It imports Font Awesome and Tailwind, then defines the project-wide visual system: body background, typography, card styling, table styling, form focus states, and color overrides.

### `src/app/app.ts`
Defines the root standalone component. It imports `LayoutComponent` and renders it.

### `src/app/app.html`
Contains `<app-layout></app-layout>`, so every page is shown inside the shared admin layout.

### `src/app/app.config.ts`
Registers Angular providers:

- `provideRouter(routes)` connects Angular routing to `app.routes.ts`.
- `provideHttpClient()` enables HTTP services.
- `provideClientHydration(withEventReplay())` supports hydrated browser rendering.
- `provideBrowserGlobalErrorListeners()` wires global error handling.

### `src/app/app.routes.ts`
Maps each route to a page component:

- `/dashboard` -> `DashboardComponent`
- `/users` -> `UsersComponent`
- `/transactions` -> `TransactionsComponent`
- `/disputes` -> `DisputesComponent`
- `/analytics` -> `AnalyticsComponent`
- `/audit` -> `AuditComponent`
- empty and unknown paths redirect to `/dashboard`

## Layout Components

### `src/app/shared/layout/layout.ts`
Controls the shell state. It listens to router navigation events and updates `pageTitle` based on the active route. It also owns sidebar behavior:

- `sidebarCollapsed` collapses the desktop sidebar to icons.
- `mobileSidebarOpen` opens or closes the mobile drawer.
- `toggleSidebar()` chooses desktop collapse or mobile drawer behavior based on viewport width.
- `closeMobileSidebar()` closes the mobile drawer after navigation or backdrop click.

### `src/app/shared/layout/layout.html`
Composes the page:

- `<app-sidebar>` receives collapse/open inputs and emits close events.
- A backdrop appears only when the mobile sidebar is open.
- `<app-header>` displays the page title and emits sidebar toggle events.
- `<router-outlet>` renders the active route page.

### `src/app/shared/layout/layout.css`
Defines the responsive shell spacing, sidebar overlay backdrop, and main content offset. The main area changes padding when the sidebar is collapsed.

### `src/app/shared/layout/sidebar/sidebar.ts`
Defines the navigation items used by the sidebar template. Inputs control collapsed and mobile-open states. The `closeMobile` output lets the layout close the drawer.

### `src/app/shared/layout/sidebar/sidebar.html`
Renders the brand, route links, dispute count, and admin footer. In collapsed mode, labels are hidden and link titles remain available for hover.

### `src/app/shared/layout/sidebar/sidebar.css`
Styles the dark sidebar, active navigation state, compact icon-only state, and mobile slide-in behavior.

### `src/app/shared/layout/header/header.ts`
Receives the current `pageTitle` from the layout and emits `toggleSidebar` when the menu button is clicked.

### `src/app/shared/layout/header/header.html`
Renders the sticky top bar, page title, search input, notification button, and menu button.

### `src/app/shared/layout/header/header.css`
Styles the glass-like top bar, icon buttons, and search field.

## Feature Structure

Each feature folder follows the same pattern:

- `.ts` file: component state, computed values, event handlers, and calls into `DataService`.
- `.html` file: page layout and Angular template bindings.
- `.css` file: page-specific styling when needed.

## Shared UI Components

### `src/app/shared/ui/stat-card/*`
Reusable metric card component used by the dashboard. It accepts:

- `label` for the metric name
- `value` for the highlighted number or text
- `note` for supporting context
- `icon` for a Font Awesome icon class
- `tone` for default or danger styling

## Routed Feature Pages

### `src/app/features/dashboard/*`
Shows high-level metrics, recent transactions, and operational insights. It calls:

- `getTotalVolume()`
- `getPlatformFees()`
- `getActiveEscrows()`
- `getOpenDisputes()`
- `getRecentTransactions()`

### `src/app/features/users/*`
Shows user metrics, search, verification state, KYC state, and admin actions. It uses `DataService` methods such as:

- `getFilteredUsers()`
- `getUserStats()`
- `suspendUser()`
- `activateUser()`
- `approveKYC()`
- `rejectKYC()`
- `blacklistUser()`
- `removeBlacklist()`

### `src/app/features/transactions/*`
Shows transaction stats, filtering, searching, admin override actions, and a transaction detail modal. It uses:

- `getFilteredTransactions()`
- `getTransactionStats()`
- `forceReleaseFunds()`
- `forceRefund()`
- `cancelTransaction()`

### `src/app/features/disputes/*`
Shows a dispute queue and mediation workspace. It reads selected dispute details and the associated transaction, then lets an admin update status, add notes, or resolve the case. It uses:

- `getFilteredDisputes()`
- `getDisputeStats()`
- `getTransactionById()`
- `updateDisputeStatus()`
- `addAdminNote()`
- `resolveDisputeRefundBuyer()`
- `resolveDisputeReleaseSeller()`
- `resolveDisputePartial()`

### `src/app/features/analytics/*`
Displays charts and reporting tables. It uses Chart.js and data helpers from `DataService`, including:

- `getTransactionVolumeByDay()`
- `getDisputeTrendByWeek()`
- `getTransactionStatusDistribution()`
- `getUserGrowth()`
- `getTopSellers()`

### `src/app/features/audit/*`
Shows admin action history, filters, pagination, export, and clear-log behavior. It uses:

- `getAuditLogs()`
- `getFilteredAuditLogs()`
- `getAuditLogStats()`
- `clearAuditLogs()`

## Data And Models

### `src/app/core/services/data.ts`
This is the current in-memory state layer. It uses Angular `signal()` for reactive arrays:

- `transactionsSignal`
- `usersSignal`
- `disputesSignal`
- `auditLogsSignal`

The service initializes mock records in `loadMockData()` and sample audit history in `initializeSampleAuditLogs()`. Components call public methods to read filtered data, calculate stats, and perform admin actions. Mutating methods update the relevant signal and add an audit log entry through `addAuditLog()`.

### `src/app/core/services/api.service.ts`
Backend integration service. It defines HTTP methods for auth, users, transactions, disputes, analytics, and audit logs. It uses `environment.apiUrl` as the base URL. This service is prepared for real API calls but is not the primary data source for the current mock-driven pages.

### `src/app/core/services/search.ts`
Shared search state for the application. The topbar search input writes into `SearchService.query`, and routed pages read that same signal to filter their visible rows. This keeps the topbar search and page-level search fields synchronized.

### `src/app/core/models/user.ts`
Defines the UI user shape:

- identity and contact fields
- buyer/seller role
- active/suspended status
- phone/email verification flags
- KYC status
- blacklist and transaction summary fields

### `src/app/core/models/transaction.ts`
Defines the UI transaction shape:

- ID, buyer, seller, amount
- escrow status
- created/completed dates
- optional auto-release and dispute metadata

### `src/app/core/models/dispute.ts`
Defines the UI dispute shape:

- dispute and transaction IDs
- parties and role
- reason, description, evidence
- status, amount, resolution, notes

## Environments And HTTP

### `src/environments/environment.ts`
Development environment values, including the API base URL.

### `src/environments/environment.prod.ts`
Production environment values.

### `src/app/core/interceptors/auth.interceptor.ts`
Intended to attach authentication data to outgoing HTTP requests. The current `app.config.ts` provides `HttpClient`, but the interceptor is not registered in the provider chain yet.

## Styling System

The app uses Tailwind utilities in templates plus project-level CSS in `src/styles.css`. The redesign keeps the existing templates mostly intact while improving the visible UI through:

- a restrained teal/navy operations palette
- darker closeable sidebar
- consistent card and panel shadows
- cleaner table headers and hover states
- consistent form focus styling
- reduced rounded-card look

Page-specific CSS is used where a page needs custom structure, such as the dashboard metric cards.

## How Files Interconnect

```text
main.ts
  -> app.config.ts
      -> AppComponent
      -> LayoutComponent
          -> SidebarComponent
          -> HeaderComponent
          -> RouterOutlet
              -> Dashboard / Users / Transactions / Disputes / Analytics / Audit
                  -> DataService
                      -> User / Transaction / Dispute models
                  -> shared UI components such as StatCardComponent
                  -> ApiService is available for future backend wiring
```

## Development Commands

Run from `Application/escrow-web`:

```bash
npm start
npm run build
npm test
```

`npm start` runs the Angular development server. `npm run build` verifies production compilation.
