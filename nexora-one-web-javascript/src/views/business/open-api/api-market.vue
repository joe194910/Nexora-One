<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">API市场</h1>
        <div class="open-api-page__subtitle">发现并接入 NexoraOne 开放能力</div>
      </div>
      <a-input-search v-model:value="query.searchWord" placeholder="搜索 API 名称、编码或能力" class="open-api-market__search" @search="loadData" />
    </header>

    <section class="open-api-panel">
      <div class="open-api-filter-row">
        <a-select v-model:value="query.requestMethod" allow-clear placeholder="请求方式" @change="loadData">
          <a-select-option value="GET">GET</a-select-option>
          <a-select-option value="POST">POST</a-select-option>
          <a-select-option value="PUT">PUT</a-select-option>
          <a-select-option value="DELETE">DELETE</a-select-option>
        </a-select>
        <a-select v-model:value="query.permissionLevel" allow-clear placeholder="权限级别" @change="loadData">
          <a-select-option :value="1">公开</a-select-option>
          <a-select-option :value="2">需申请</a-select-option>
          <a-select-option :value="3">敏感</a-select-option>
        </a-select>
        <a-select v-model:value="query.orderType" @change="loadData">
          <a-select-option value="recommend">推荐排序</a-select-option>
          <a-select-option value="calls">调用量排序</a-select-option>
        </a-select>
        <a-button @click="reset"><ReloadOutlined />重置</a-button>
      </div>
    </section>

    <a-spin :spinning="loading">
      <div class="open-api-market-grid">
        <article v-for="item in list" :key="item.openApiId" class="open-api-market-card">
          <div class="open-api-market-card__top">
            <span class="open-api-market-card__icon"><ApiOutlined /></span>
            <div class="open-api-market-card__content">
              <h2>{{ item.marketTitle || item.apiName }}</h2>
              <p>{{ item.marketSummary || item.description }}</p>
            </div>
            <a-tag :color="methodColor(item.requestMethod)">{{ item.requestMethod }}</a-tag>
          </div>
          <div class="open-api-market-card__meta">
            <a-tag color="blue">{{ item.categoryName || '未分类' }}</a-tag>
            <a-tag :color="permissionMeta(item.permissionLevel).color">{{ permissionMeta(item.permissionLevel).text }}</a-tag>
            <span>{{ formatNumber(item.totalCallCount) }} 次调用</span>
          </div>
          <div class="open-api-market-card__footer">
            <span class="open-api-available"><CheckCircleFilled /> 可用</span>
            <a-space>
              <a-button @click="openDocument(item)">查看文档</a-button>
              <a-button v-if="item.permissionLevel !== 1" type="primary" @click="openApply(item)">申请调用</a-button>
            </a-space>
          </div>
        </article>
      </div>
      <a-empty v-if="!loading && !list.length" description="暂无符合条件的 API" />
      <div class="open-api-pagination">
        <a-pagination v-model:current="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />
      </div>
    </a-spin>

    <a-modal v-model:open="applyVisible" title="申请 API 调用权限" ok-text="提交申请" @ok="submitApply">
      <a-form layout="vertical">
        <a-form-item label="目标 API"><a-input :value="selectedApi?.apiName" disabled /></a-form-item>
        <a-form-item label="调用应用" required>
          <a-select v-model:value="applyForm.applicationId" placeholder="请选择已创建并取得凭证的应用">
            <a-select-option v-for="app in applications" :key="app.applicationId" :value="app.applicationId">
              {{ app.applicationName }}（{{ app.appId || '尚未生成 App ID' }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="使用场景" required><a-input v-model:value="applyForm.useScene" /></a-form-item>
        <a-form-item label="申请环境" required>
          <a-radio-group v-model:value="applyForm.applyEnvironment">
            <a-radio value="test">测试环境</a-radio>
            <a-radio value="prod">生产环境</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="申请原因" required><a-textarea v-model:value="applyForm.applyReason" :rows="4" :maxlength="500" show-count /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { ApiOutlined, CheckCircleFilled, ReloadOutlined } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const router = useRouter();
  const loading = ref(false);
  const list = ref([]);
  const total = ref(0);
  const applications = ref([]);
  const applyVisible = ref(false);
  const selectedApi = ref(null);
  const query = reactive({ pageNum: 1, pageSize: 12, searchWord: '', requestMethod: undefined, permissionLevel: undefined, orderType: 'recommend' });
  const applyForm = reactive({ applicationId: undefined, openApiId: undefined, useScene: '', applyEnvironment: 'test', applyReason: '' });

  function methodColor(method) {
    return { GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red' }[method] || 'default';
  }

  function permissionMeta(level) {
    return { 1: { color: 'green', text: '公开' }, 2: { color: 'orange', text: '需申请' }, 3: { color: 'red', text: '敏感' } }[level] || { color: 'default', text: '未配置' };
  }

  function formatNumber(value) {
    return Number(value || 0).toLocaleString('zh-CN');
  }

  async function loadData() {
    loading.value = true;
    try {
      const response = await openApiApi.queryMarket(query);
      list.value = response.data.list || [];
      total.value = response.data.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    Object.assign(query, { pageNum: 1, searchWord: '', requestMethod: undefined, permissionLevel: undefined, orderType: 'recommend' });
    loadData();
  }

  function openDocument(item) {
    router.push({ path: '/open-api/document', query: { openApiId: item.openApiId } });
  }

  async function openApply(item) {
    selectedApi.value = item;
    applyForm.openApiId = item.openApiId;
    applyVisible.value = true;
    if (!applications.value.length) {
      const response = await openApiApi.applications();
      applications.value = response.data || [];
    }
  }

  async function submitApply() {
    if (!applyForm.applicationId || !applyForm.useScene || !applyForm.applyReason) {
      message.warning('请完整填写申请信息');
      return;
    }
    await openApiApi.applyPermission(applyForm);
    message.success('权限申请已提交');
    applyVisible.value = false;
  }

  onMounted(loadData);
</script>
