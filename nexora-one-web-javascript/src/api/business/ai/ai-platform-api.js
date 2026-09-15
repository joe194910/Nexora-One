import { getRequest, postDownload, postRequest } from '/@/lib/axios';

/**
 * AI 平台管理接口。
 */
export const aiPlatformApi = {
  queryServices: (param) => postRequest('/ai/model-service/query', param),
  serviceSummary: () => getRequest('/ai/model-service/summary'),
  serviceOptions: () => getRequest('/ai/model-service/options'),
  saveService: (param) => postRequest('/ai/model-service/save', param),
  testService: (id) => postRequest(`/ai/model-service/test/${id}`),
  deleteService: (id) => postRequest(`/ai/model-service/delete/${id}`),

  queryModels: (param) => postRequest('/ai/model/query', param),
  modelSummary: () => getRequest('/ai/model/summary'),
  saveModel: (param) => postRequest('/ai/model/save', param),
  debugModel: (param) => postRequest('/ai/model/debug', param),
  deleteModel: (id) => postRequest(`/ai/model/delete/${id}`),

  queryVectors: (param) => postRequest('/ai/vector-database/query', param),
  vectorSummary: () => getRequest('/ai/vector-database/summary'),
  saveVector: (param) => postRequest('/ai/vector-database/save', param),
  syncVector: (id) => postRequest(`/ai/vector-database/sync/${id}`),
  deleteVector: (id) => postRequest(`/ai/vector-database/delete/${id}`),

  parsePlans: () => getRequest('/ai/document/plans'),
  saveParsePlan: (data) => postRequest('/ai/document/plans/save', data),
  copyParsePlan: (id) => postRequest(`/ai/document/plans/${id}/copy`),
  deleteParsePlan: (id) => postRequest(`/ai/document/plans/${id}/delete`),
  testParsePlan: (id, file) => postRequest(`/ai/document/plans/${id}/test`, file),
  parseServices: () => getRequest('/ai/document/services'),
  saveParseService: (data) => postRequest('/ai/document/services/save', data),
  testParseService: (id) => postRequest(`/ai/document/services/${id}/test`),
  deleteParseService: (id) => postRequest(`/ai/document/services/${id}/delete`),
  knowledgeBases: () => getRequest('/ai/document/knowledge-bases'),
  saveKnowledgeBase: (data) => postRequest('/ai/document/knowledge-bases/save', data),
  uploadKnowledgeDocument: (id, file) => postRequest(`/ai/document/knowledge-bases/${id}/upload`, file),
  parseTasks: (data) => postRequest('/ai/document/tasks/query', data),
  parseTaskDetail: (id) => getRequest(`/ai/document/tasks/${id}`),
  cancelParseTask: (id) => postRequest(`/ai/document/tasks/${id}/cancel`),
  retryParseTask: (id) => postRequest(`/ai/document/tasks/${id}/retry`),

  queryCallLogs: (param) => postRequest('/ai/call-log/query', param),
  callLogSummary: (param) => postRequest('/ai/call-log/summary', param),
  callLogDetail: (id) => getRequest(`/ai/call-log/detail/${id}`),
  exportCallLogs: (param) => postDownload('/ai/call-log/export', param),

  statistics: (param) => postRequest('/ai/usage-statistics/query', param),
};
