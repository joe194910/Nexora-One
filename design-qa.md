# NexoraOne Application Center Design QA

## Scope

- Application access list
- Application creation and credential generation
- Login/SSO configuration
- Interface security configuration
- API permission application
- Marketplace listing information
- Publication scope
- Submission preview and review
- Application detail and release progress

## Verification Evidence

- Backend clean compile: passed with Java 17 and Maven.
- Frontend production build: passed with Vite.
- Static branding scan: no `SmartAdmin`, `smart-admin`, `smartAdmin`, or `amsrtAdmin` matches outside generated dependencies/build output.
- Patch formatting check: `git diff --check` passed.
- Reference requirement source: conversation screenshots and `pasted-text.txt`.

## Visual Comparison

Browser-rendered comparison evidence could not be collected in this session:

- The Browser runtime tool required for local interactive inspection is not available after tool discovery.
- The reference screenshots are present only in conversation context; the attachment directory contains `pasted-text.txt` but no readable source image files.
- A same-viewport side-by-side reference/prototype comparison therefore cannot be produced without inventing visual evidence.

## Residual Checks

- Apply `数据库SQL脚本/mysql/sql-update-log/20260909.sql` to the target database before testing dynamic menus and application data.
- Restart the backend service after deployment so the new `/application/**` endpoints are loaded.
- After database migration and restart, test the complete wizard with an authenticated administrator account and inspect the browser console and network requests.
- Compare each implemented page against its matching reference screenshot at the same desktop viewport.

final result: blocked
