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
};
