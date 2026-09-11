import { getRequest, postRequest } from '/@/lib/axios';

/**
 * API开放平台管理接口。
 */
export const openApiApi = {
  query: (param) => postRequest('/open-api/manage/query', param),
  summary: () => getRequest('/open-api/manage/summary'),
  categories: () => getRequest('/open-api/manage/categories'),
  checkCode: (apiCode, openApiId) => getRequest('/open-api/manage/code/check', { apiCode, openApiId }),
  create: (param) => postRequest('/open-api/manage/create', param),
  updateBasic: (param) => postRequest('/open-api/manage/basic/update', param),
  saveParameters: (param) => postRequest('/open-api/manage/parameters/save', param),
  saveExamples: (param) => postRequest('/open-api/manage/examples/save', param),
  detail: (openApiId) => getRequest(`/open-api/manage/detail/${openApiId}`),
  updateStatus: (param) => postRequest('/open-api/manage/status/update', param),
  queryMarket: (param) => postRequest('/open-api/portal/market/query', param),
  document: (openApiId) => getRequest(`/open-api/portal/document/${openApiId}`),
  applications: () => getRequest('/open-api/portal/applications'),
  applyPermission: (param) => postRequest('/open-api/portal/permission/apply', param),
  permissions: (applyStatus) => getRequest('/open-api/portal/permission/list', { applyStatus }),
  reviewPermission: (param) => postRequest('/open-api/portal/permission/review', param),
  publishDetail: (openApiId) => getRequest(`/open-api/portal/publish/${openApiId}`),
  publish: (param) => postRequest('/open-api/portal/publish', param),
  publishReviews: (reviewStatus) => getRequest('/open-api/portal/publish/review/list', { reviewStatus }),
  reviewPublish: (param) => postRequest('/open-api/portal/publish/review', param),
  debug: (param) => postRequest('/open-api/portal/debug', param),
  statistics: () => getRequest('/open-api/portal/statistics'),
  guide: () => getRequest('/open-api/portal/guide'),
};
