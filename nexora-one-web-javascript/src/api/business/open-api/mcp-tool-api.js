import { getRequest, postRequest } from '/@/lib/axios';

/** 标准 MCP Server、AI 工具与 HTTP 适配工具管理接口。权限展示由页面菜单和按钮控制。 */
export const mcpToolApi = {
  /** 分页查询当前用户可管理的标准 MCP Server。 */
  queryServers: (param) => postRequest('/mcp/servers/query', param),
  /** 查询标准 MCP Server 总数、在线数和发现工具数。 */
  serverSummary: () => getRequest('/mcp/servers/summary'),
  /** 查询标准 MCP Server 的脱敏配置和已发现工具。 */
  serverDetail: (serverId) => getRequest(`/mcp/servers/detail/${serverId}`),
  /** 新增或更新标准 MCP Server。 */
  saveServer: (param) => postRequest('/mcp/servers/save', param),
  /** 执行 MCP initialize、tools/list，导入新工具并刷新远端定义变化状态。 */
  probeServer: (serverId) => postRequest(`/mcp/servers/${serverId}/probe-sync`),
  /** 明确同步单个远端 MCP 工具的最新 Schema。 */
  syncServerToolSchema: (serverId, toolId) => postRequest(`/mcp/servers/${serverId}/tools/${toolId}/sync-schema`),
  /** 启用或停用标准 MCP Server。 */
  updateServerStatus: (param) => postRequest('/mcp/servers/status', param),
  /** 分页查询当前用户可管理的 MCP 工具。 */
  query: (param) => postRequest('/mcp/tools/query', param),
  /** 查询工具总数、来源和待审核数量。 */
  summary: () => getRequest('/mcp/tools/summary'),
  /** 查询当前用户可选择的应用列表。 */
  applications: () => getRequest('/mcp/tools/applications'),
  /** 查询单个工具的完整配置和关联信息。 */
  detail: (toolId) => getRequest(`/mcp/tools/detail/${toolId}`),
  /** 查询平台 API 与 AI 工具的版本关联关系。 */
  platformRelation: (openApiId) => getRequest(`/mcp/tools/platform/${openApiId}`),
  /** 预览平台 API 发布为工具时继承的版本和 Schema。 */
  platformPreview: (openApiId) => getRequest(`/mcp/tools/platform/${openApiId}/preview`),
  /** 保存平台 API 工具草稿或提交审核。 */
  publishPlatform: (param) => postRequest('/mcp/tools/platform/publish', param),
  /** 手动同步平台 API 的最新发布版本和 Schema。 */
  syncPlatform: (toolId) => postRequest(`/mcp/tools/${toolId}/sync`),
  /** 登记第三方 HTTP 工具。 */
  saveExternal: (param) => postRequest('/mcp/tools/external/save', param),
  /** 使用测试参数执行一次工具连通性测试。 */
  test: (param) => postRequest('/mcp/tools/test', param),
  /** 审核工具并确定最终风险和确认策略。 */
  review: (param) => postRequest('/mcp/tools/review', param),
  /** 启用或停用已经登记的工具。 */
  updateStatus: (param) => postRequest('/mcp/tools/status', param),
  /** 查询可供智能助手绑定的已审核工具。 */
  availableForAssistant: () => getRequest('/mcp/tools/assistant/available'),
  /** 查询指定智能助手已经绑定的工具。 */
  assistantTools: (assistantId) => getRequest(`/mcp/tools/assistant/${assistantId}`),
  /** 保存智能助手与工具的绑定关系和调用策略。 */
  bindAssistant: (param) => postRequest('/mcp/tools/assistant/bind', param),
  /** 确认或拒绝一次等待用户确认的工具调用。 */
  confirmCall: (param) => postRequest('/mcp/tools/calls/confirm', param),
  /** 查询指定工具调用的最新执行结果。 */
  callResult: (requestId) => getRequest(`/mcp/tools/calls/${requestId}`),
};
