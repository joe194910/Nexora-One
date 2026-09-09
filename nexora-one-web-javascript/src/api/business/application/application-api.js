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
  submit: (param) => postRequest('/application/submit', param),
  review: (param) => postRequest('/application/review', param),
};
