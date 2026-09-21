# MCP 工具接入设计验证记录

验证日期：2026-09-21

## 验证范围

- 参考原型：本次会话提供的 API 管理列表、发布 AI 工具抽屉、API 详情 AI 工具页签、MCP 工具管理、MCP Server、接入指南和调用日志页面。
- 实现页面：`/open-api/mcp-tools`、`/knowledge/assistants`。
- 目标状态：工具列表、Server 列表、注册或编辑 Server、探活并发现工具、工具详情、Schema 待同步、审核与启停、关联助手、调用确认和调用日志。
- 参考图已在会话中提供，当前工作区没有可引用的原始图片文件路径。

## 静态设计检查

- 平台 API、第三方 HTTP 和标准 MCP 使用独立来源标识，避免把普通 HTTP 接口伪装成标准 MCP。
- MCP Server 注册使用 Streamable HTTP endpoint，并提供无鉴权、Bearer Token、API Key 请求头三种静态鉴权配置。
- 标准 MCP 工具沿用统一工具列表、测试、审核、启停、风险等级、确认策略和助手关联能力。
- Schema 漂移只显示待同步并停用工具，不静默覆盖正在使用的工具定义；显式同步后必须重新测试、审核和启用。
- 智能助手编辑页使用真实工具多选绑定，聊天页支持调用前确认和确认后的真实执行结果。
- 页面按钮权限仅使用 `web_perms`；MCP 管理接口未增加接口权限注解。

## 浏览器验证

- 本轮未获得有效浏览器截图、视口尺寸、交互状态或控制台日志。
- 内置浏览器启动失败：`node_repl kernel exited unexpectedly`。
- 直接阻塞原因：`failed to parse model_catalog_json path C:\Users\admin\.codex\cc-switch-model-catalog.json as JSON: missing field base_instructions at line 74 column 5`。
- 因浏览器运行时不可用，本轮不能确认桌面端和移动端像素级布局、抽屉交互、溢出、响应式行为及运行时控制台状态。

## 比较记录

- 迭代 1，2026-09-20：曾使用 Microsoft Edge Headless CDP 检查旧版 MCP 工具页；该方式不作为本轮内置浏览器设计验收证据。
- 迭代 2，2026-09-21：完成标准 MCP Server、工具发现、Schema 同步、助手绑定和调用链路的静态审阅；浏览器在截图前被本机模型目录配置阻塞。

## 待复验项

- 修复 `C:\Users\admin\.codex\cc-switch-model-catalog.json` 后，用内置浏览器分别验证桌面和移动视口。
- 验证 Server 注册、静态鉴权、探活同步、Schema 待同步提示、审核启用和助手绑定的完整交互。
- 检查页面水平溢出、抽屉宽度、表格操作区、加载态、空态、错误提示和浏览器控制台。

## 最终结果

`blocked`
