<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">API上架发布</h1>
        <div class="open-api-page__subtitle">完成发布前检查并配置 API 市场展示信息</div>
      </div>
      <a-button @click="router.back()">返回</a-button>
    </header>
    <a-spin :spinning="loading">
      <div class="open-api-publish-layout">
        <section class="open-api-panel">
          <h2 class="open-api-panel__title">上架前检查</h2>
          <div v-for="item in detail.checks || []" :key="item.name" class="open-api-check-item">
            <CheckCircleFilled v-if="item.passed" class="is-passed" />
            <ExclamationCircleFilled v-else class="is-failed" />
            <span>{{ item.name }}</span>
            <a-tag :color="item.passed ? 'green' : 'orange'">{{ item.passed ? '检查通过' : '待完善' }}</a-tag>
          </div>
          <a-alert v-if="!detail.ready" type="warning" show-icon message="存在未完成配置，请返回 API 编辑页补全后再发布。" />
        </section>
        <section class="open-api-panel">
          <h2 class="open-api-panel__title">API市场展示信息</h2>
          <a-form layout="vertical">
            <a-form-item label="市场标题" required><a-input v-model:value="form.marketTitle" :maxlength="100" /></a-form-item>
            <a-form-item label="市场简介" required><a-textarea v-model:value="form.marketSummary" :rows="4" :maxlength="500" show-count /></a-form-item>
            <a-form-item label="可见范围" required>
              <a-radio-group v-model:value="form.publishScope">
                <a-radio value="platform">全平台公开</a-radio>
                <a-radio value="enterprise">指定企业</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="SLA说明" required><a-textarea v-model:value="form.slaDescription" :rows="3" :maxlength="300" /></a-form-item>
            <a-alert type="warning" show-icon message="提交后当前版本将锁定并进入平台审核；审核通过后才会正式上架。" />
            <div class="open-api-publish-actions">
              <a-button @click="goEdit">返回修改</a-button>
              <a-button type="primary" :disabled="!detail.ready" :loading="publishing" @click="publish">提交平台审核</a-button>
            </div>
          </a-form>
        </section>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { CheckCircleFilled, ExclamationCircleFilled } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import './open-api.less';

  const route = useRoute();
  const router = useRouter();
  const loading = ref(false);
  const publishing = ref(false);
  const detail = reactive({ checks: [], ready: false, api: null });
  const form = reactive({ openApiId: undefined, marketTitle: '', marketSummary: '', publishScope: 'platform', slaDescription: '正常情况下响应时间不超过 500ms，服务可用性不低于 99.9%。' });

  async function loadData() {
    form.openApiId = Number(route.query.openApiId);
    if (!form.openApiId) return;
    loading.value = true;
    try {
      const response = await openApiApi.publishDetail(form.openApiId);
      Object.assign(detail, response.data || {});
      Object.assign(form, {
        marketTitle: detail.api.marketTitle || detail.api.apiName,
        marketSummary: detail.api.marketSummary || detail.api.description,
        publishScope: detail.api.publishScope || 'platform',
        slaDescription: detail.api.slaDescription || form.slaDescription,
      });
    } finally {
      loading.value = false;
    }
  }

  function goEdit() {
    router.push({ path: '/open-api/editor', query: { openApiId: form.openApiId, step: 1 } });
  }

  async function publish() {
    if (!form.marketTitle || !form.marketSummary || !form.slaDescription) {
      message.warning('请完整填写市场展示信息');
      return;
    }
    publishing.value = true;
    try {
      await openApiApi.publish(form);
      message.success('API 发布申请已提交，等待平台审核');
      router.push('/open-api/manage');
    } finally {
      publishing.value = false;
    }
  }

  onMounted(loadData);
</script>
