# NexoraOne API Open Platform Design QA

## Reference

- Source: user-provided API management, API editor, API market, developer guide, online debugger and publishing screenshots.
- Scope: complete API open platform workflow for administrators and application developers.
- Existing system: NexoraOne admin layout, Ant Design Vue, database-driven routes and permissions.

## Desktop Checks

- [x] API management header, summary, filters, table and actions follow the supplied information hierarchy.
- [x] Editor provides the six-step navigation shown in the reference.
- [x] Definition workflow supports basic information, request parameters, response parameters, examples and error codes.
- [x] API market, API document, application authorization, online debugging, access guide and call statistics pages are implemented.
- [x] Publishing page supports market information, scope, SLA and immediate publication.
- [x] Database menus cover visible portal entries and hidden detail/publishing routes.
- [x] Buttons, tags, badges, selects, switches and tables use the existing component library.
- [x] Panels use restrained borders and radii consistent with the current application.

## Responsive Checks

- [x] Summary cards reduce from four to two and then one column.
- [x] Query controls and editor forms collapse to one column on narrow screens.
- [x] Wide parameter and environment tables scroll horizontally.
- [x] Header actions wrap below the title on mobile.

## Functional Checks

- [x] List, summary and category data use backend APIs.
- [x] Create, edit, detail, parameter save, example save and status changes use backend APIs.
- [x] API market search, category filtering and public document lookup use backend APIs.
- [x] Authorization application, administrator review and current-user authorization lists use backend APIs.
- [x] Online debugging validates application authorization and records call logs.
- [x] Call statistics are isolated to applications owned by the current non-administrator user.
- [x] Publishing validates API definition completeness before changing the API to published status.
- [x] App ID and App Secret access-token/signature instructions link back to the existing application access flow.
- [x] Existing `openApiId` remains the stable authorization relation key.
- [x] Detail and editor routes use independent database menu entries and matching permissions.
- [x] Nested response fields preserve parent-child order and reject invalid parent references.
- [x] Gateway path uniqueness includes request method, path and version.
- [x] Frontend dependency footprint is unchanged.

## Verification

- [x] Maven compile passed for `nexora-one-admin` and dependent modules on 2026-09-10.
- [x] Vite `build:prod` passed on 2026-09-10 and emitted chunks for every new portal page.
- [x] Local Vite preview returned HTTP 200 at `http://127.0.0.1:5175/`.
- [x] `git diff --check` reported no whitespace errors.
- [ ] Authenticated browser flow and live API requests require local MySQL and Redis services plus execution of `20260910-2.sql`.
- [ ] Pixel comparison against the supplied screenshots could not be completed because the authenticated dynamic route cannot load without those backend dependencies.

final result: source and build verification passed; authenticated runtime verification pending local services and database migration.
