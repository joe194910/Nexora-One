import { getRequest, postRequest } from '/@/lib/axios';

/**
 * 应用中心接口。
 */
export const applicationApi = {
  query: (param) => postRequest('/application/query', param),
  create: (param) => postRequest('/application/create', param),
  updateBase: (param) => postRequest('/application/base/update', param),
  detail: (applicationId) => getRequest(`/application/detail/${applicationId}`),
  saveStep: (param) => postRequest('/application/step/save', param),
  saveApiPermissions: (param) => postRequest('/application/api-permission/save', param),
  queryOpenApiCatalog: () => getRequest('/application/open-api/catalog'),
  resetSecret: (applicationId) => postRequest(`/application/secret/reset/${applicationId}`),
  testConnection: (param) => postRequest('/application/connect/test', param),
  submit: (param) => postRequest('/application/submit', param),
  review: (param) => postRequest('/application/review', param),
  queryMarket: (param) => postRequest('/application/portal/market/query', param),
  queryCategories: () => getRequest('/application/portal/category/list'),
  queryMyApplications: () => getRequest('/application/portal/my'),
  queryHomeOverview: () => getRequest('/application/portal/home/overview'),
  updateFavorite: (param) => postRequest('/application/portal/favorite', param),
  queryRecent: () => getRequest('/application/portal/recent'),
  launch: (param) => postRequest('/application/portal/launch', param),
  summary: () => getRequest('/application/manage/summary'),
  updateStatus: (param) => postRequest('/application/manage/status/update', param),
  queryVisitLogs: (param) => postRequest('/application/manage/visit-log/query', param),
};
