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

## AI 平台管理设计验收

日期：2026-09-15

### 已验证

- 六个独立页面与对应 API 均已加入项目，页面使用现有 Vue 3 / Ant Design Vue 体系。
- 前端生产构建通过；Java 17 后端模块完整编译通过。
- 本地 Vite 服务 `http://127.0.0.1:8081/` 返回 HTTP 200。
- 响应式 CSS 为窄屏提供单列布局、摘要区重排、查询区重排与标题操作区换行；宽表格保留水平滚动。

### 尚需环境验收

- 本次浏览器连接器未提供可用浏览器实例，无法截取桌面/移动视口并检查实际像素、遮挡及交互状态。
- AI 页面使用动态菜单与权限。实际路由和数据需要执行 `20260915-ai-platform.sql`，运行后端并登录系统后验收。
- 外部模型服务、Qdrant、OCR 的连接效果依赖现场服务与密钥；当前只验证代码路径，不记录虚假的连接成功。
- “解析测试”执行 Apache Tika 文本提取，图片或无文本扫描件可通过配置的 HTTP OCR 接口识别，再按已保存规则切片预览；图片提取、任务并发和重复文件策略属于后续知识库处理链路的配置，不应将此测试视为入库验收。

### 待复核视口

- 桌面：1440 x 900，检查表格固定操作列、侧边抽屉、统计图表。
- 移动：390 x 844，检查标题/操作换行、筛选单列、表格横向滚动、解析配置与日志详情。
