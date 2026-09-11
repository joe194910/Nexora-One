<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">在线调试</h1>
        <div class="open-api-page__subtitle">使用已授权应用验证 API 定义、鉴权配置和响应结构</div>
      </div>
      <a-tag color="green">测试环境</a-tag>
    </header>

    <div class="open-api-debug-layout">
      <section class="open-api-panel">
        <h2 class="open-api-panel__title">请求配置</h2>
        <a-form layout="vertical">
          <div class="open-api-form-grid">
            <a-form-item label="调用应用" required>
              <a-select v-model:value="form.applicationId" placeholder="请选择应用">
                <a-select-option v-for="item in applications" :key="item.applicationId" :value="item.applicationId">
                  {{ item.applicationName }}（{{ item.appId || '无有效凭证' }}）
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="API" required>
              <a-select v-model:value="form.openApiId" show-search placeholder="请选择已上架 API" @change="selectApi">
                <a-select-option v-for="item in apis" :key="item.openApiId" :value="item.openApiId">
                  {{ item.apiName }}（{{ item.requestMethod }}）
                </a-select-option>
              </a-select>
            </a-form-item>
          </div>
          <a-form-item label="请求地址">
            <a-input :value="selectedApi ? `${selectedApi.requestMethod} ${selectedApi.requestPath}` : ''" disabled />
          </a-form-item>
          <a-form-item label="请求参数（JSON）">
            <a-textarea v-model:value="paramsText" :rows="4" class="open-api-code-input" placeholder='{"employeeId": 10001}' />
          </a-form-item>
          <a-form-item label="请求体（JSON）">
            <a-textarea v-model:value="form.body" :rows="7" class="open-api-code-input" placeholder="GET 请求可留空" />
          </a-form-item>
          <div class="open-api-editor__footer">
            <a-button @click="clearResult"><ReloadOutlined />清空</a-button>
            <a-button type="primary" :loading="sending" @click="send"><SendOutlined />发送请求</a-button>
          </div>
        </a-form>
      </section>

      <section class="open-api-panel">
        <div class="open-api-table-toolbar">
          <h2 class="open-api-panel__title">响应结果</h2>
          <a-space v-if="result">
            <a-tag :color="result.httpStatus < 300 ? 'green' : 'red'">{{ result.httpStatus }}</a-tag>
            <span>{{ result.durationMs }} ms</span>
            <span>{{ result.responseBytes }} B</span>
          </a-space>
        </div>
        <a-empty v-if="!result" description="发送请求后在此查看结果" />
        <template v-else>
          <a-descriptions bordered :column="1" size="small">
            <a-descriptions-item label="Trace ID">{{ result.traceId }}</a-descriptions-item>
            <a-descriptions-item label="请求 URL">{{ result.requestUrl }}</a-descriptions-item>
            <a-descriptions-item label="App ID">{{ result.appId }}</a-descriptions-item>
            <a-descriptions-item label="时间戳">{{ result.timestamp }}</a-descriptions-item>
            <a-descriptions-item label="Nonce">{{ result.nonce }}</a-descriptions-item>
          </a-descriptions>
          <div class="open-api-code-box open-api-debug-response"><pre>{{ formatJson(result.responseBody) }}</pre></div>
          <a-alert v-if="result.dispatchMode === 'real-forward'" type="success" show-icon message="请求已由平台网关真实转发到配置的测试环境。" />
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { ReloadOutlined, SendOutlined } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const applications = ref([]);
  const apis = ref([]);
  const selectedApi = ref(null);
  const paramsText = ref('{}');
  const result = ref(null);
  const sending = ref(false);
  const form = reactive({ applicationId: undefined, openApiId: undefined, environmentCode: 'test', params: {}, headers: {}, body: '' });

  async function loadMeta() {
    const [applicationResponse, apiResponse] = await Promise.all([
      openApiApi.applications(),
      openApiApi.queryMarket({ pageNum: 1, pageSize: 200, orderType: 'recommend' }),
    ]);
    applications.value = applicationResponse.data || [];
    apis.value = apiResponse.data.list || [];
  }

  function selectApi(openApiId) {
    selectedApi.value = apis.value.find((item) => item.openApiId === openApiId);
  }

  async function send() {
    if (!form.applicationId || !form.openApiId) {
      message.warning('请选择调用应用和 API');
      return;
    }
    try {
      form.params = JSON.parse(paramsText.value || '{}');
    } catch {
      message.warning('请求参数不是有效 JSON');
      return;
    }
    sending.value = true;
    try {
      const response = await openApiApi.debug(form);
      result.value = response.data;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      sending.value = false;
    }
  }

  function clearResult() {
    result.value = null;
    paramsText.value = '{}';
    form.body = '';
  }

  function formatJson(content) {
    try {
      return JSON.stringify(JSON.parse(content), null, 2);
    } catch {
      return content;
    }
  }

  onMounted(loadMeta);
</script>
