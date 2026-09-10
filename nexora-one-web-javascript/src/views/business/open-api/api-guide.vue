<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">接入指南</h1>
        <div class="open-api-page__subtitle">使用 App ID 和 App Secret 获取令牌并调用 NexoraOne 开放 API</div>
      </div>
      <a-button type="primary" @click="router.push('/application/access')">前往应用管理</a-button>
    </header>

    <a-alert type="warning" show-icon class="open-api-guide-alert" :message="guide.secretNotice" />
    <div class="open-api-guide-layout">
      <aside class="open-api-panel open-api-document-nav">
        <a-anchor :affix="false" :items="anchorItems" />
      </aside>
      <main>
        <section id="credential" class="open-api-panel">
          <h2 class="open-api-panel__title">1. 创建应用并获取凭证</h2>
          <p>在应用中心创建应用。平台会生成唯一的 App ID 和仅首次完整展示的 App Secret。</p>
        </section>
        <section id="token" class="open-api-panel">
          <h2 class="open-api-panel__title">2. 请求 Access Token</h2>
          <a-descriptions bordered :column="1" size="small">
            <a-descriptions-item label="请求方式">POST</a-descriptions-item>
            <a-descriptions-item label="接口路径">{{ guide.tokenPath }}</a-descriptions-item>
            <a-descriptions-item label="授权类型">{{ guide.grantType }}</a-descriptions-item>
            <a-descriptions-item label="有效期">{{ guide.tokenTtlSeconds }} 秒</a-descriptions-item>
          </a-descriptions>
          <div class="open-api-code-box"><pre>{{ tokenExample }}</pre></div>
        </section>
        <section id="signature" class="open-api-panel">
          <h2 class="open-api-panel__title">3. 生成请求签名</h2>
          <p>签名算法：{{ guide.signatureAlgorithm }}</p>
          <div class="open-api-code-box"><pre>{{ guide.canonicalRule }}</pre></div>
        </section>
        <section id="call" class="open-api-panel">
          <h2 class="open-api-panel__title">4. 调用业务 API</h2>
          <a-table :columns="headerColumns" :data-source="headers" row-key="name" size="small" :pagination="false" />
          <div class="open-api-code-box"><pre>{{ callExample }}</pre></div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive } from 'vue';
  import { useRouter } from 'vue-router';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import './open-api.less';

  const router = useRouter();
  const guide = reactive({ tokenPath: '/open/application/oauth/token', grantType: 'client_credentials', tokenTtlSeconds: 7200, signatureAlgorithm: 'HMAC-SHA256', canonicalRule: '', secretNotice: '' });
  const anchorItems = [
    { key: 'credential', href: '#credential', title: '获取应用凭证' },
    { key: 'token', href: '#token', title: '请求 Access Token' },
    { key: 'signature', href: '#signature', title: '生成请求签名' },
    { key: 'call', href: '#call', title: '调用业务 API' },
  ];
  const headerColumns = [
    { title: 'Header', dataIndex: 'name' },
    { title: '说明', dataIndex: 'description' },
  ];
  const headers = [
    { name: 'Authorization', description: 'Bearer {access_token}' },
    { name: 'X-App-Id', description: '应用 App ID' },
    { name: 'X-Timestamp', description: '当前毫秒时间戳' },
    { name: 'X-Nonce', description: '每次请求唯一随机串' },
    { name: 'X-Signature', description: 'HMAC-SHA256 请求签名' },
  ];
  const tokenExample = computed(() => `curl -X POST https://api.example.com${guide.tokenPath} \\
  -H "Content-Type: application/json" \\
  -d '{"app_id":"app_nxo_xxx","app_secret":"仅服务端保存","grant_type":"${guide.grantType}"}'`);
  const callExample = `curl -X GET "https://api.example.com/open-api/v1/resource" \\
  -H "Authorization: Bearer {access_token}" \\
  -H "X-App-Id: {app_id}" \\
  -H "X-Timestamp: {timestamp}" \\
  -H "X-Nonce: {nonce}" \\
  -H "X-Signature: {signature}"`;

  onMounted(async () => {
    const response = await openApiApi.guide();
    Object.assign(guide, response.data || {});
  });
</script>
