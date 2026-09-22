import { getRequest, postRequest } from '@/lib/smart-request';

/**
 * 查询助手市场。
 *
 * @param {string} keyword 关键词
 * @returns {Promise<Array>} 助手列表
 */
export const getAssistantStore = (keyword = '') =>
  getRequest(`/knowledge/assistant-store?keyword=${encodeURIComponent(keyword)}`).then(
    (response) => response.data || [],
  );

/**
 * 收藏助手。
 *
 * @param {string|number} assistantId 助手编号
 * @returns {Promise<Object>} 操作结果
 */
export const favoriteAssistant = (assistantId) =>
  postRequest(`/knowledge/assistant-store/${assistantId}/favorite`).then(
    (response) => response.data,
  );

/**
 * 取消收藏助手。
 *
 * @param {string|number} assistantId 助手编号
 * @returns {Promise<Object>} 操作结果
 */
export const unfavoriteAssistant = (assistantId) =>
  postRequest(`/knowledge/assistant-store/${assistantId}/unfavorite`).then(
    (response) => response.data,
  );

/**
 * 查询当前用户可使用的助手。
 *
 * @returns {Promise<Array>} 可用助手列表
 */
export const getAvailableAssistants = () =>
  getRequest('/knowledge/assistants/available').then((response) => response.data || []);

/**
 * 查询助手详情。
 *
 * @param {string|number} assistantId 助手编号
 * @returns {Promise<Object>} 助手详情
 */
export const getAssistantDetail = (assistantId) =>
  getRequest(`/knowledge/assistants/${assistantId}`).then((response) => response.data);

/**
 * 向助手发送问题。
 *
 * @param {string|number} assistantId 助手编号
 * @param {Object} body 对话请求
 * @returns {Promise<Object>} 助手回复
 */
export const chatWithAssistant = (assistantId, body) =>
  postRequest(`/knowledge/assistants/${assistantId}/chat`, body).then(
    (response) => response.data,
  );

/**
 * 查询助手会话列表。
 *
 * @param {string|number} assistantId 助手编号
 * @returns {Promise<Array>} 会话列表
 */
export const getAssistantConversations = (assistantId) =>
  getRequest(`/knowledge/assistants/${assistantId}/conversations`).then(
    (response) => response.data || [],
  );

/**
 * 查询会话消息。
 *
 * @param {string|number} assistantId 助手编号
 * @param {string|number} conversationId 会话编号
 * @returns {Promise<Array>} 消息列表
 */
export const getConversationMessages = (assistantId, conversationId) =>
  getRequest(`/knowledge/assistants/${assistantId}/conversations/${conversationId}/messages`).then(
    (response) => response.data || [],
  );

/**
 * 删除会话。
 *
 * @param {string|number} assistantId 助手编号
 * @param {string|number} conversationId 会话编号
 * @returns {Promise<Object>} 删除结果
 */
export const deleteConversation = (assistantId, conversationId) =>
  postRequest(`/knowledge/assistants/${assistantId}/conversations/${conversationId}/delete`).then(
    (response) => response.data,
  );

/**
 * 查询工具调用状态。
 *
 * @param {string} requestId 工具调用请求编号
 * @returns {Promise<Object>} 工具调用状态
 */
export const getToolCallStatus = (requestId) =>
  getRequest(`/mcp/tools/calls/${requestId}`).then((response) => response.data);

/**
 * 确认或拒绝工具调用。
 *
 * @param {string} requestId 工具调用请求编号
 * @param {boolean} approved 是否批准
 * @returns {Promise<Object>} 工具调用结果
 */
export const confirmToolCall = (requestId, approved) =>
  postRequest('/mcp/tools/calls/confirm', { requestId, approved }).then(
    (response) => response.data,
  );
