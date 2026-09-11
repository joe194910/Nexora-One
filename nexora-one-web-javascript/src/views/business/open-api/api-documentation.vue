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
          <button
            v-for="item in navigationItems"
            :key="item.key"
            type="button"
            class="open-api-document-nav__item"
            :class="{ 'open-api-document-nav__item--active': activeSection === item.key }"
            @click="scrollToSection(item.key)"
          >
            {{ item.title }}
          </button>
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
            <a-table
              :columns="parameterColumns"
              :data-source="requestParameters"
              row-key="parameterId"
              size="small"
              :pagination="false"
              :locale="{ emptyText: '该接口暂无请求参数' }"
            />
          </section>

          <section id="response" class="open-api-panel">
            <h2 class="open-api-panel__title">返回参数</h2>
            <a-table
              :columns="parameterColumns"
              :data-source="responseParameters"
              row-key="parameterId"
              size="small"
              :pagination="false"
              :locale="{ emptyText: '该接口暂无返回参数定义' }"
            />
          </section>

          <section id="examples" class="open-api-panel">
            <div class="open-api-table-toolbar">
              <h2 class="open-api-panel__title" style="margin-bottom: 0">请求示例</h2>
              <a-button size="small" @click="copyText(activeCodeExample)"><CopyOutlined />复制代码</a-button>
            </div>
            <a-tabs v-model:activeKey="activeCodeType">
              <a-tab-pane key="curl" tab="cURL">
                <div class="open-api-code-box"><pre>{{ curlExample }}</pre></div>
              </a-tab-pane>
              <a-tab-pane key="java" tab="Java（Hutool）">
                <div class="open-api-code-box"><pre>{{ javaExample }}</pre></div>
              </a-tab-pane>
              <a-tab-pane key="javascript" tab="JavaScript（Node.js）">
                <div class="open-api-code-box"><pre>{{ javascriptExample }}</pre></div>
              </a-tab-pane>
            </a-tabs>
            <div class="open-api-example-response">
              <h3>业务报文示例</h3>
              <div v-if="examples.length" class="open-api-json-grid">
                <div v-for="item in examples" :key="item.exampleId" class="open-api-code-box">
                  <strong>{{ item.exampleName || item.exampleType }}</strong>
                  <pre>{{ formatJson(item.content) }}</pre>
                </div>
              </div>
              <a-empty v-else :image="simpleImage" description="该接口暂未配置业务报文示例" />
            </div>
          </section>

          <section id="errors" class="open-api-panel">
            <h2 class="open-api-panel__title">错误码</h2>
            <a-table
              :columns="errorColumns"
              :data-source="errorCodes"
              row-key="errorCodeId"
              size="small"
              :pagination="false"
              :locale="{ emptyText: '该接口暂无专属错误码，请参考平台通用错误响应' }"
            />
          </section>
        </main>
      </div>
      <a-empty v-else-if="!loading" description="请选择要查看的 API 文档" />
    </a-spin>
  </div>
</template>

<script setup>
  import { computed, onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { ApiOutlined, CodeOutlined, CopyOutlined } from '@ant-design/icons-vue';
  import { Empty, message } from 'ant-design-vue';
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
  const activeSection = ref('overview');
  const activeCodeType = ref('curl');
  const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
  const navigationItems = [
    { key: 'overview', title: '接口概述' },
    { key: 'request', title: '请求参数' },
    { key: 'response', title: '返回参数' },
    { key: 'examples', title: '请求示例' },
    { key: 'errors', title: '错误码' },
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

  const configuredApiUrl = import.meta.env.VITE_APP_API_URL || window.location.origin;
  const apiBaseUrl = new URL(configuredApiUrl, window.location.origin).href.replace(/\/$/, '');
  const requestBody = computed(() => {
    const requestExample = examples.value.find((item) => String(item.exampleType).toLowerCase() === 'request');
    return requestExample?.content ? formatJson(requestExample.content) : '';
  });
  const requestMethod = computed(() => (version.value?.requestMethod || 'GET').toUpperCase());
  const requestTarget = computed(() => {
    let target = version.value?.gatewayPath || '/open-api/v1/resource';
    const query = [];
    requestParameters.value.forEach((parameter) => {
      const location = String(parameter.location || '').toLowerCase();
      const value = parameter.exampleValue || parameter.defaultValue || `{${parameter.parameterName}}`;
      if (location === 'path') {
        target = target.replace(`{${parameter.parameterName}}`, encodeURIComponent(value));
      } else if (location === 'query') {
        query.push(`${encodeURIComponent(parameter.parameterName)}=${encodeURIComponent(value)}`);
      }
    });
    if (query.length) {
      target += `${target.includes('?') ? '&' : '?'}${query.join('&')}`;
    }
    return target;
  });
  const requestUrl = computed(() => `${apiBaseUrl}${requestTarget.value}`);
  const isTokenEndpoint = computed(
    () => api.value?.apiCode === 'application:oauth:token'
      || /\/oauth\/token(?:\?|$)/.test(requestTarget.value),
  );
  const curlExample = computed(() => {
    if (isTokenEndpoint.value) {
      return `curl -X POST "${requestUrl.value}" \\
  -H "Content-Type: application/json" \\
  --data '{
    "appId": "替换为 App ID",
    "appSecret": "替换为创建或重置时获得的完整 App Secret",
    "grantType": "client_credentials"
  }'`;
    }
    const body = requestBody.value && !['GET', 'DELETE'].includes(requestMethod.value)
      ? ` \\\n  -H "Content-Type: ${version.value?.contentType || 'application/json'}" \\\n  --data '${requestBody.value.replace(/\r?\n/g, '')}'`
      : '';
    return `curl -X ${requestMethod.value} "${requestUrl.value}" \\
  -H "Authorization: Bearer {access_token}" \\
  -H "X-App-Id: {app_id}" \\
  -H "X-Timestamp: {timestamp}" \\
  -H "X-Nonce: {nonce}" \\
  -H "X-Signature: {signature}"${body}`;
  });
  const javaExample = computed(() => {
    if (isTokenEndpoint.value) {
      return `// 依赖：cn.hutool:hutool-all:5.8+
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.http.HttpRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class NexoraOneTokenExample {
    private static final String APP_ID = "替换为 App ID";
    private static final String APP_SECRET = "替换为创建或重置时获得的完整 App Secret";
    private static final String TOKEN_URL = "${requestUrl.value}";

    public static void main(String[] args) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("appId", APP_ID);
        requestBody.put("appSecret", APP_SECRET);
        requestBody.put("grantType", "client_credentials");

        String responseBody = HttpRequest.post(TOKEN_URL)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(${(version.value?.timeoutSeconds || 10) * 1000})
                .execute()
                .body();

        JSONObject response = JSONUtil.parseObj(responseBody);
        if (!response.getBool("ok", false)) {
            throw new IllegalStateException("获取 Access Token 失败：" + responseBody);
        }

        JSONObject data = response.getJSONObject("data");
        String accessToken = data.getStr("access_token");
        long expiresIn = data.getLong("expires_in", 0L);
        System.out.println("Access Token：" + accessToken);
        System.out.println("有效期（秒）：" + expiresIn);
    }
}`;
    }
    return `// 依赖：cn.hutool:hutool-all:5.8+
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class NexoraOneApiExample {
    private static final String APP_ID = "替换为 App ID";
    private static final String APP_SECRET = "仅从服务端安全配置读取";
    private static final String ACCESS_TOKEN = "替换为 Access Token";
    private static final String REQUEST_TARGET = "${requestTarget.value}";

    public static void main(String[] args) {
        String method = "${requestMethod.value}";
        String body = ${JSON.stringify(requestBody.value)};
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodyHash = SecureUtil.sha256(body);
        String canonical = String.join("\\n", method, REQUEST_TARGET, timestamp, nonce, bodyHash);
        String signingKey = SecureUtil.sha256(APP_SECRET);
        HMac hmac = SecureUtil.hmac(
                HmacAlgorithm.HmacSHA256, signingKey.getBytes(StandardCharsets.UTF_8));
        String signature = hmac.digestHex(canonical, CharsetUtil.CHARSET_UTF_8);

        HttpRequest request = HttpRequest.${requestMethod.value.toLowerCase()}("${requestUrl.value}")
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .timeout(${(version.value?.timeoutSeconds || 10) * 1000});
        ${requestBody.value && !['GET', 'DELETE'].includes(requestMethod.value) ? 'request.body(body);' : ''}
        System.out.println(request.execute().body());
    }
}`;
  });
  const javascriptExample = computed(() => {
    if (isTokenEndpoint.value) {
      return `const appId = process.env.NEXORA_APP_ID;
const appSecret = process.env.NEXORA_APP_SECRET;

if (!appId || !appSecret) {
  throw new Error('请先配置 NEXORA_APP_ID 和 NEXORA_APP_SECRET 环境变量');
}

const response = await fetch('${requestUrl.value}', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    appId,
    appSecret,
    grantType: 'client_credentials',
  }),
});

const result = await response.json();
if (!response.ok || !result.ok) {
  throw new Error(\`获取 Access Token 失败：\${JSON.stringify(result)}\`);
}

const accessToken = result.data.access_token;
console.log('Access Token：', accessToken);
console.log('有效期（秒）：', result.data.expires_in);`;
    }
    return `import crypto from 'node:crypto';

const appId = process.env.NEXORA_APP_ID;
const appSecret = process.env.NEXORA_APP_SECRET;
const accessToken = process.env.NEXORA_ACCESS_TOKEN;
const method = '${requestMethod.value}';
const requestTarget = '${requestTarget.value}';
const body = ${JSON.stringify(requestBody.value)};
const timestamp = Date.now().toString();
const nonce = crypto.randomUUID().replaceAll('-', '');
const bodyHash = crypto.createHash('sha256').update(body, 'utf8').digest('hex');
const canonical = [method, requestTarget, timestamp, nonce, bodyHash].join('\\n');
const signingKey = crypto.createHash('sha256').update(appSecret, 'utf8').digest('hex');
const signature = crypto.createHmac('sha256', signingKey).update(canonical, 'utf8').digest('hex');

const response = await fetch('${requestUrl.value}', {
  method,
  headers: {
    Authorization: \`Bearer \${accessToken}\`,
    'X-App-Id': appId,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature,
    'Content-Type': '${version.value?.contentType || 'application/json'}',
  },${requestBody.value && !['GET', 'DELETE'].includes(requestMethod.value) ? '\n  body,' : ''}
});

if (!response.ok) {
  throw new Error(\`调用失败：\${response.status} \${await response.text()}\`);
}
console.log(await response.json());`;
  });
  const activeCodeExample = computed(() => ({
    curl: curlExample.value,
    java: javaExample.value,
    javascript: javascriptExample.value,
  })[activeCodeType.value]);

  function scrollToSection(sectionId) {
    activeSection.value = sectionId;
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function copyText(text) {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
      message.success('代码已复制');
    } catch (error) {
      smartSentry.captureError(error);
      message.error('复制失败，请手动选择代码复制');
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
