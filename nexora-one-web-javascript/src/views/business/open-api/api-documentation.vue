<template>
  <div class="open-api-page">
    <a-spin :spinning="loading">
      <header v-if="api" class="open-api-page__header">
        <div class="open-api-editor__intro">
          <span class="open-api-editor__icon"><ApiOutlined /></span>
          <div>
            <h1 class="open-api-page__title">{{ api.marketTitle || api.apiName }}</h1>
            <div class="open-api-page__subtitle">{{ api.marketSummary || api.description }}</div>
          </div>
        </div>
        <a-space>
          <a-tag :color="methodColor(version?.requestMethod)" class="open-api-document__method">{{ version?.requestMethod }}</a-tag>
          <a-tag>{{ version?.versionNo }}</a-tag>
          <a-button type="primary" @click="goDebug"><CodeOutlined />在线调试</a-button>
        </a-space>
      </header>

      <div v-if="api" class="open-api-document-layout">
        <aside class="open-api-panel open-api-document-nav">
          <a-anchor :items="anchorItems" :affix="false" />
        </aside>
        <main>
          <section id="overview" class="open-api-panel">
            <h2 class="open-api-panel__title">接口概述</h2>
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="接口编码">{{ api.apiCode }}</a-descriptions-item>
              <a-descriptions-item label="请求地址">
                <a-tag :color="methodColor(version?.requestMethod)">{{ version?.requestMethod }}</a-tag>
                {{ version?.gatewayPath }}
              </a-descriptions-item>
              <a-descriptions-item label="Content-Type">{{ version?.contentType }}</a-descriptions-item>
              <a-descriptions-item label="权限说明">{{ permissionText(version?.permissionLevel) }}</a-descriptions-item>
              <a-descriptions-item label="SLA">{{ api.slaDescription || '正常情况下服务可用性不低于 99.9%' }}</a-descriptions-item>
            </a-descriptions>
          </section>

          <section id="request" class="open-api-panel">
            <h2 class="open-api-panel__title">请求参数</h2>
            <a-table :columns="parameterColumns" :data-source="requestParameters" row-key="parameterId" size="small" :pagination="false" />
          </section>

          <section id="response" class="open-api-panel">
            <h2 class="open-api-panel__title">返回参数</h2>
            <a-table :columns="parameterColumns" :data-source="responseParameters" row-key="parameterId" size="small" :pagination="false" />
          </section>

          <section id="examples" class="open-api-panel">
            <h2 class="open-api-panel__title">请求与返回示例</h2>
            <div class="open-api-json-grid">
              <div v-for="item in examples" :key="item.exampleId" class="open-api-code-box">
                <strong>{{ item.exampleName || item.exampleType }}</strong>
                <pre>{{ formatJson(item.content) }}</pre>
              </div>
            </div>
          </section>

          <section id="errors" class="open-api-panel">
            <h2 class="open-api-panel__title">错误码</h2>
            <a-table :columns="errorColumns" :data-source="errorCodes" row-key="errorCodeId" size="small" :pagination="false" />
          </section>
        </main>
      </div>
      <a-empty v-else-if="!loading" description="请选择要查看的 API 文档" />
    </a-spin>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { ApiOutlined, CodeOutlined } from '@ant-design/icons-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const route = useRoute();
  const router = useRouter();
  const loading = ref(false);
  const api = ref(null);
  const version = ref(null);
  const requestParameters = ref([]);
  const responseParameters = ref([]);
  const examples = ref([]);
  const errorCodes = ref([]);
  const anchorItems = [
    { key: 'overview', href: '#overview', title: '接口概述' },
    { key: 'request', href: '#request', title: '请求参数' },
    { key: 'response', href: '#response', title: '返回参数' },
    { key: 'examples', href: '#examples', title: '请求示例' },
    { key: 'errors', href: '#errors', title: '错误码' },
  ];
  const parameterColumns = [
    { title: '参数名', dataIndex: 'parameterName' },
    { title: '位置', dataIndex: 'location', width: 100 },
    { title: '类型', dataIndex: 'dataType', width: 120 },
    { title: '必填', dataIndex: 'requiredFlag', width: 80, customRender: ({ text }) => (text ? '是' : '否') },
    { title: '说明', dataIndex: 'description' },
  ];
  const errorColumns = [
    { title: 'HTTP状态码', dataIndex: 'httpStatus', width: 110 },
    { title: '业务错误码', dataIndex: 'businessCode', width: 160 },
    { title: '错误信息', dataIndex: 'errorMessage' },
    { title: '处理建议', dataIndex: 'handlingAdvice' },
  ];

  function methodColor(method) {
    return { GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red' }[method] || 'default';
  }

  function permissionText(level) {
    return { 1: '公开调用', 2: '应用申请授权后调用', 3: '敏感接口，管理员审核后调用' }[level] || '未配置';
  }

  function formatJson(content) {
    try {
      return JSON.stringify(JSON.parse(content), null, 2);
    } catch {
      return content;
    }
  }

  async function loadData() {
    if (!route.query.openApiId) return;
    loading.value = true;
    try {
      const response = await openApiApi.document(route.query.openApiId);
      api.value = response.data.api;
      version.value = response.data.version;
      requestParameters.value = response.data.requestParameters || [];
      responseParameters.value = response.data.responseParameters || [];
      examples.value = response.data.examples || [];
      errorCodes.value = response.data.errorCodes || [];
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function goDebug() {
    router.push({ path: '/open-api/debugger', query: { openApiId: api.value.openApiId } });
  }

  onMounted(loadData);
</script>
