import { getRequest, postRequest, request } from '/@/lib/axios';

/** 用户知识库专属接口；所有权及状态由后端复核。 */
export const knowledgeApi = {
  options: () => getRequest('/knowledge/options'),
  documents: (params) => getRequest('/knowledge/documents', params),
  detail: (id) => getRequest(`/knowledge/documents/${id}`),
  chunks: (id, offset) => getRequest(`/knowledge/documents/${id}/chunks`, offset ? { offset } : {}),
  upload: (file, planId) => {
    const data = new FormData();
    data.append('file', file);
    return request({ method: 'post', url: '/knowledge/documents/upload', params: planId ? { planId } : {}, data });
  },
  retry: (id) => postRequest(`/knowledge/documents/${id}/retry`),
  cancel: (id) => postRequest(`/knowledge/documents/${id}/cancel`),
  deleteDocument: (id) => postRequest(`/knowledge/documents/${id}/delete`),
  /** 流式文件由统一鉴权请求下载，兼容 RFC 5987 的中文文件名。 */
  download: async (record) => {
    const response = await request({ method: 'get', url: `/knowledge/documents/${record.documentId}/download`, responseType: 'blob' });
    const blob = new Blob([response.data], { type: 'application/octet-stream' });
    const link = window.document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = record.fileName;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
  },
  bases: (params) => getRequest('/knowledge/bases', params),
  saveBase: (data) => postRequest('/knowledge/bases/save', data),
  deleteBase: (id) => postRequest(`/knowledge/bases/${id}/delete`),
  assistants: () => getRequest('/knowledge/assistants'),
  saveAssistant: (data) => postRequest('/knowledge/assistants/save', data),
  deleteAssistant: (id) => postRequest(`/knowledge/assistants/${id}/delete`),
  chat: (id, data) => postRequest(`/knowledge/assistants/${id}/chat`, data),
  conversations: (id) => getRequest(`/knowledge/assistants/${id}/conversations`),
  messages: (id, conversationId) => getRequest(`/knowledge/assistants/${id}/conversations/${conversationId}/messages`),
  deleteConversation: (id, conversationId) => postRequest(`/knowledge/assistants/${id}/conversations/${conversationId}/delete`),
};
