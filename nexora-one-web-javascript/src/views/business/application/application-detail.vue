<template>
  <div class="application-page">
    <a-spin :spinning="loading">
      <section class="application-panel application-detail-header">
        <div class="application-hero">
          <div class="application-icon application-detail-header__icon">
            <img v-if="detail.iconUrl" :src="detail.iconUrl" alt="" />
            <AppstoreOutlined v-else />
          </div>
          <div>
            <div class="application-detail-header__title">
              <h1>{{ detail.applicationName || '应用详情' }}</h1>
              <a-tag :color="listingMeta.color">{{ listingMeta.text }}</a-tag>
            </div>
            <div class="application-detail-header__meta">
              <span>App ID：{{ detail.credential?.appId || '-' }}</span>
              <a-tag :color="detail.applicationType === 1 ? 'blue' : 'purple'">
                {{ detail.applicationType === 1 ? '企业内部应用' : '第三方应用' }}
              </a-tag>
              <span>{{ detail.listingConfig?.versionNo || 'v1.0.0' }}</span>
            </div>
            <p>{{ detail.summary }}</p>
          </div>
        </div>
        <a-space>
          <a-button :disabled="detail.configLocked" @click="goConfigure"><SettingOutlined />接入配置</a-button>
          <a-button type="primary" v-if="detail.homeUrl" @click="openApplication"><ExportOutlined />进入应用</a-button>
        </a-space>
      </section>

      <div class="detail-grid">
        <section class="application-panel">
          <h2 class="application-panel__title">审核与发布进度</h2>
          <a-empty v-if="!detail.reviews?.length" description="尚未提交审核" />
          <a-timeline v-else>
            <a-timeline-item v-for="review in detail.reviews" :key="review.reviewId" :color="reviewColor(review.reviewStatus)">
              <div class="review-line">
                <strong>{{ review.reviewStage }}</strong>
                <span>{{ formatTime(review.createTime) }}</span>
              </div>
              <div class="application-page__subtitle">{{ review.reviewRemark || '无补充说明' }}</div>
              <div class="application-page__subtitle">操作人：{{ review.operatorName || '系统' }}</div>
            </a-timeline-item>
          </a-timeline>
        </section>

        <section class="application-panel">
          <h2 class="application-panel__title">发布信息</h2>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="上架范围">{{ scopeText }}</a-descriptions-item>
            <a-descriptions-item label="可见组织">{{ publishOrganizations }}</a-descriptions-item>
            <a-descriptions-item label="当前版本">{{ detail.listingConfig?.versionNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="负责人">{{ detail.ownerName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</a-descriptions-item>
          </a-descriptions>
          <a-button
            v-if="detail.listingStatus === 1"
            class="application-review-button"
            type="primary"
            block
            v-privilege="'application:review'"
            @click="reviewModalVisible = true"
          >
            <AuditOutlined />处理审核
          </a-button>
        </section>
      </div>

      <div class="detail-grid">
        <section class="application-panel">
          <h2 class="application-panel__title">接入状态</h2>
          <div class="health-list">
            <div v-for="item in healthItems" :key="item.label" class="health-item">
              <span><CheckCircleFilled :style="{ color: item.ready ? '#22a447' : '#faad14' }" /> {{ item.label }}</span>
              <a-tag :color="item.ready ? 'success' : 'warning'">{{ item.ready ? '正常' : '待配置' }}</a-tag>
            </div>
          </div>
        </section>
        <section class="application-panel">
          <h2 class="application-panel__title">下一步建议</h2>
          <a-list :data-source="suggestions" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.title" :description="item.description">
                  <template #avatar><BulbOutlined class="suggestion-icon" /></template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </section>
      </div>

      <section class="application-panel">
        <h2 class="application-panel__title">版本记录</h2>
        <a-table :data-source="detail.versions || []" :columns="versionColumns" row-key="versionId" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'versionStatus'">
              <a-badge :status="versionMeta(record.versionStatus).status" :text="versionMeta(record.versionStatus).text" />
            </template>
            <template v-else-if="column.dataIndex === 'submitTime'">{{ formatTime(record.submitTime) }}</template>
            <template v-else-if="column.dataIndex === 'publishTime'">{{ formatTime(record.publishTime) }}</template>
          </template>
        </a-table>
      </section>
    </a-spin>

    <div class="application-footer-actions">
      <a-button @click="router.push('/application/access')">返回应用列表</a-button>
    </div>

    <a-modal v-model:open="reviewModalVisible" title="处理应用审核" :confirm-loading="reviewing" :footer="null">
      <a-form layout="vertical">
        <a-form-item label="审核说明" required>
          <a-textarea v-model:value="reviewRemark" :rows="4" :maxlength="500" show-count placeholder="请填写审核结论和说明" />
        </a-form-item>
        <div class="application-review-actions">
          <a-button @click="reviewModalVisible = false">取消</a-button>
          <a-button danger :loading="reviewing" @click="submitReview(3)">驳回</a-button>
          <a-button type="primary" :loading="reviewing" @click="submitReview(2)">通过并发布</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import {
    AppstoreOutlined,
    AuditOutlined,
    BulbOutlined,
    CheckCircleFilled,
    ExportOutlined,
    SettingOutlined,
  } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './application.less';

  const route = useRoute();
  const router = useRouter();
  const applicationId = Number(route.query.applicationId);
  const loading = ref(false);
  const reviewing = ref(false);
  const reviewModalVisible = ref(false);
  const reviewRemark = ref('');
  const detail = reactive({});

  const versionColumns = [
    { title: '版本号', dataIndex: 'versionNo' },
    { title: '状态', dataIndex: 'versionStatus' },
    { title: '提交时间', dataIndex: 'submitTime' },
    { title: '发布时间', dataIndex: 'publishTime' },
  ];
  const listingMeta = computed(() => ({
    0: { color: 'default', text: '未上架' },
    1: { color: 'processing', text: '审核中' },
    2: { color: 'success', text: '已上架' },
    3: { color: 'error', text: '已驳回' },
    4: { color: 'default', text: '已下架' },
  }[detail.listingStatus] || { color: 'default', text: '未知' }));
  const scopeText = computed(() => ({
    ENTERPRISE: '仅本企业',
    SELECTED: '指定企业或组织',
    PUBLIC: '全平台公开',
  }[detail.publishConfig?.scopeType] || '-'));
  const publishOrganizations = computed(() => detail.publishConfig?.organizationNames?.join('、') || '-');
  const healthItems = computed(() => [
    { label: '应用凭证', ready: !!detail.credential?.appId },
    { label: '登录回调', ready: !!Object.keys(detail.loginConfig || {}).length },
    { label: '接口安全', ready: !!Object.keys(detail.securityConfig || {}).length },
    { label: 'API 授权', ready: !!detail.apiPermissions?.length },
  ]);
  const suggestions = computed(() => detail.listingStatus === 2
    ? [
        { title: '创建新版本', description: '应用功能更新后，可调整配置并提交新版本。' },
        { title: '查看调用数据', description: '结合监控服务观察 API 调用量和异常情况。' },
        { title: '配置告警', description: '为接口错误率、超时和健康状态配置告警。' },
      ]
    : [
        { title: '完善接入配置', description: '补齐待配置项目后提交应用上架审核。' },
        { title: '核对发布范围', description: '确认可见企业、组织和角色符合预期。' },
      ]);

  function versionMeta(value) {
    return {
      1: { status: 'processing', text: '审核中' },
      2: { status: 'success', text: '已发布' },
      3: { status: 'error', text: '已驳回' },
      4: { status: 'default', text: '已下架' },
    }[value] || { status: 'default', text: '未知' };
  }

  function reviewColor(value) {
    return value === 2 ? 'green' : value === 3 ? 'red' : 'blue';
  }

  function formatTime(value) {
    return value ? String(value).replace('T', ' ') : '-';
  }

  async function loadDetail() {
    if (!applicationId) {
      message.warning('缺少应用主键');
      return;
    }
    loading.value = true;
    try {
      const response = await applicationApi.detail(applicationId);
      Object.assign(detail, response.data);
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function goConfigure() {
    router.push({ path: '/application/onboarding', query: { applicationId, step: Math.min((detail.workflowStep || 1) + 1, 8) } });
  }

  async function openApplication() {
    try {
      const response = await applicationApi.launch({ applicationId });
      const target = response.data.openMode === 'CURRENT' ? '_self' : '_blank';
      window.open(response.data.launchUrl, target, target === '_blank' ? 'noopener,noreferrer' : undefined);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  async function submitReview(reviewStatus) {
    if (!reviewRemark.value.trim()) {
      message.warning('请填写审核说明');
      return;
    }
    reviewing.value = true;
    try {
      await applicationApi.review({ applicationId, reviewStatus, reviewRemark: reviewRemark.value.trim() });
      message.success(reviewStatus === 2 ? '审核通过，应用已发布' : '应用已驳回');
      reviewModalVisible.value = false;
      reviewRemark.value = '';
      await loadDetail();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      reviewing.value = false;
    }
  }

  onMounted(loadDetail);
</script>
