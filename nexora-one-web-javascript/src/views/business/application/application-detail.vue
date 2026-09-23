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
          <a-button
            v-if="canCreateVersion && ($privilege('application:save') || $privilege('application:review'))"
            @click="openVersionModal"
          >
            <PlusOutlined />创建新版本
          </a-button>
          <a-button v-if="$privilege('application:secret:reset') || $privilege('application:review')" @click="credentialModalVisible = true">
            <KeyOutlined />凭证管理
          </a-button>
          <a-button @click="goConfigure">
            <EyeOutlined v-if="detail.editable === false" />
            <SettingOutlined v-else />
            {{ configurationActionText }}
          </a-button>
          <a-button type="primary" v-if="hasPublishedVersion" @click="openApplication"><ExportOutlined />进入应用</a-button>
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
            <a-descriptions-item label="编辑版本">{{ currentVersionNo }}</a-descriptions-item>
            <a-descriptions-item label="线上版本">{{ publishedVersionNo }}</a-descriptions-item>
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
              <a-tag v-if="record.versionId === detail.publishedVersionId" color="green" class="application-version-online">
                当前线上
              </a-tag>
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

    <a-modal v-model:open="credentialModalVisible" title="应用凭证管理" :footer="null" @after-close="clearNewSecret">
      <a-alert
        type="warning"
        show-icon
        message="重置后旧 App Secret 和使用旧密钥签发的 Access Token 会立即失效。"
      />
      <a-descriptions class="credential-management" :column="1" bordered size="small">
        <a-descriptions-item label="App ID">
          <div class="credential-management__value">
            <code>{{ newCredential.appId || detail.credential?.appId || '-' }}</code>
            <a-button type="link" @click="copyText(newCredential.appId || detail.credential?.appId)">
              <CopyOutlined />复制
            </a-button>
          </div>
        </a-descriptions-item>
        <a-descriptions-item label="App Secret">
          <div class="credential-management__value">
            <code>{{ newCredential.appSecret || detail.credential?.maskedSecret || '-' }}</code>
            <a-button v-if="newCredential.appSecret" type="link" @click="copyText(newCredential.appSecret)">
              <CopyOutlined />复制
            </a-button>
          </div>
        </a-descriptions-item>
      </a-descriptions>
      <a-alert
        v-if="newCredential.appSecret"
        class="credential-management__notice"
        type="success"
        show-icon
        message="新 App Secret 仅在本次弹窗中完整展示，请立即保存到服务端安全配置。"
      />
      <div class="application-review-actions">
        <a-button @click="credentialModalVisible = false">关闭</a-button>
        <a-popconfirm
          title="确认重置 App Secret 吗？旧密钥和旧令牌会立即失效。"
          ok-text="确认重置"
          cancel-text="取消"
          :disabled="detail.secretResettable === false"
          @confirm="resetSecret"
        >
          <a-button danger :disabled="detail.secretResettable === false" :loading="resettingSecret">
            <KeyOutlined />重置 App Secret
          </a-button>
        </a-popconfirm>
      </div>
    </a-modal>

    <a-modal
      v-model:open="versionModalVisible"
      title="创建应用新版本"
      ok-text="创建并配置"
      cancel-text="取消"
      :confirm-loading="creatingVersion"
      @ok="createVersion"
    >
      <a-alert
        type="info"
        show-icon
        message="新版本将复制当前线上配置。编辑、预发布和审核期间，线上版本仍会继续提供服务。"
      />
      <a-form layout="vertical" class="credential-management">
        <a-form-item label="新版本号" required>
          <a-input v-model:value="newVersionNo" placeholder="例如：v1.0.1" :maxlength="20" />
        </a-form-item>
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
    CopyOutlined,
    ExportOutlined,
    EyeOutlined,
    KeyOutlined,
    PlusOutlined,
    SettingOutlined,
  } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { nexoraSentry } from '/@/lib/nexora-sentry';
  import './application.less';

  const route = useRoute();
  const router = useRouter();
  const applicationId = Number(route.query.applicationId);
  const loading = ref(false);
  const reviewing = ref(false);
  const resettingSecret = ref(false);
  const creatingVersion = ref(false);
  const reviewModalVisible = ref(false);
  const credentialModalVisible = ref(false);
  const versionModalVisible = ref(false);
  const reviewRemark = ref('');
  const newVersionNo = ref('');
  const detail = reactive({});
  const newCredential = reactive({});

  const versionColumns = [
    { title: '版本号', dataIndex: 'versionNo' },
    { title: '状态', dataIndex: 'versionStatus' },
    { title: '提交时间', dataIndex: 'submitTime' },
    { title: '发布时间', dataIndex: 'publishTime' },
  ];
  const hasPublishedVersion = computed(() => detail.onlineStatus === 2
    && (Boolean(detail.publishedVersionId)
      || (detail.listingStatus === 2 && Boolean(detail.homeUrl))));
  const canCreateVersion = computed(() => detail.editable !== false
    && [2, 4].includes(detail.listingStatus)
    && ((Boolean(detail.publishedVersionId) && detail.currentVersionId === detail.publishedVersionId)
      || (!detail.publishedVersionId && !detail.currentVersionId)));
  const currentVersionNo = computed(() => detail.currentVersion?.versionNo
    || detail.listingConfig?.versionNo || '-');
  const publishedVersionNo = computed(() => detail.publishedVersion?.versionNo
    || (detail.onlineStatus === 2 ? detail.listingConfig?.versionNo : null) || '-');
  const listingMeta = computed(() => {
    if (detail.publishedVersionId && detail.currentVersionId !== detail.publishedVersionId) {
      const onlineText = detail.onlineStatus === 2 ? '线上版运行中' : '线上版已下架';
      return ({
        0: { color: 'default', text: `新版本草稿，${onlineText}` },
        1: { color: 'processing', text: `新版本审核中，${onlineText}` },
        3: { color: 'error', text: `新版本已驳回，${onlineText}` },
        5: { color: 'cyan', text: `新版本已预发布，${onlineText}` },
      }[detail.listingStatus] || {
        color: detail.onlineStatus === 2 ? 'success' : 'default',
        text: onlineText,
      });
    }
    return ({
      0: { color: 'default', text: '未上架' },
      1: { color: 'processing', text: '审核中' },
      2: { color: 'success', text: '已上架' },
      3: { color: 'error', text: '已驳回' },
      4: { color: 'default', text: '已下架' },
      5: { color: 'cyan', text: '已预发布' },
    }[detail.listingStatus] || { color: 'default', text: '未知' });
  });
  const scopeText = computed(() => ({
    ENTERPRISE: '仅本企业',
    SELECTED: '指定企业或组织',
    PUBLIC: '全平台公开',
  }[detail.publishConfig?.scopeType] || '-'));
  const publishOrganizations = computed(() => detail.publishConfig?.organizationNames?.join('、') || '-');
  const healthItems = computed(() => [
    { label: '应用凭证', ready: !!detail.credential?.appId },
    { label: '平台接入', ready: detail.accessStatus === 2 },
    { label: '登录回调', ready: !!Object.keys(detail.loginConfig || {}).length },
    { label: '接口安全', ready: !!Object.keys(detail.securityConfig || {}).length },
    { label: 'API 授权', ready: !!detail.apiPermissions?.length },
  ]);
  const suggestions = computed(() => {
    if (detail.listingStatus === 2) {
      return [
        { title: '管理应用凭证', description: '需要轮换密钥时可在本页重置 App Secret，旧令牌会立即失效。' },
        { title: '查看调用数据', description: '结合监控服务观察 API 调用量和异常情况。' },
        { title: '配置告警', description: '为接口错误率、超时和健康状态配置告警。' },
      ];
    }
    if (detail.listingStatus === 5 && detail.accessStatus !== 2) {
      return [
        { title: '完成接入验证', description: '由外部应用服务端使用 App ID 和 App Secret 成功换取 Access Token。' },
        { title: '检查服务端配置', description: '确认 App Secret 仅保存在服务端，并按照平台规则生成请求签名。' },
      ];
    }
    if (detail.listingStatus === 5) {
      return [
        { title: '提交上架审核', description: '接入验证已完成，可以提交平台进行正式上架审核。' },
        { title: '核对发布范围', description: '提交前再次确认可见企业、组织和角色符合预期。' },
      ];
    }
    return [
        { title: '完善应用配置', description: '补齐待配置项目后先提交预发布，再进行平台接入验证。' },
        { title: '核对发布范围', description: '确认可见企业、组织和角色符合预期。' },
      ];
  });
  const configurationActionText = computed(() => {
    if (detail.listingStatus === 5 && detail.accessStatus !== 2) return '查看接入状态';
    if (detail.listingStatus === 5) return '提交上架审核';
    if (detail.editable === false) return '查看配置';
    return '接入配置';
  });

  function versionMeta(value) {
    return {
      0: { status: 'default', text: '草稿' },
      1: { status: 'processing', text: '审核中' },
      2: { status: 'success', text: '已发布' },
      3: { status: 'error', text: '已驳回' },
      4: { status: 'default', text: '已下架' },
      5: { status: 'processing', text: '已预发布' },
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
      nexoraSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function goConfigure() {
    let step = Math.min((detail.workflowStep || 1) + 1, 8);
    if (detail.listingStatus === 5) {
      step = 8;
    }
    router.push({ path: '/application/onboarding', query: { applicationId, step } });
  }

  function openVersionModal() {
    const source = publishedVersionNo.value;
    const matched = /^v?(\d+)\.(\d+)\.(\d+)$/.exec(source);
    newVersionNo.value = matched
      ? `v${matched[1]}.${matched[2]}.${Number(matched[3]) + 1}`
      : '';
    versionModalVisible.value = true;
  }

  async function createVersion() {
    const versionNo = newVersionNo.value.trim();
    if (!/^v\d+\.\d+\.\d+$/.test(versionNo)) {
      message.warning('版本号需使用 v1.0.1 格式');
      return;
    }
    creatingVersion.value = true;
    try {
      await applicationApi.createVersion({ applicationId, versionNo });
      message.success('新版本已创建，线上版本将继续运行');
      versionModalVisible.value = false;
      router.push({ path: '/application/onboarding', query: { applicationId, step: 1 } });
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      creatingVersion.value = false;
    }
  }

  async function openApplication() {
    try {
      const response = await applicationApi.launch({ applicationId });
      const target = response.data.openMode === 'CURRENT' ? '_self' : '_blank';
      window.open(response.data.launchUrl, target, target === '_blank' ? 'noopener,noreferrer' : undefined);
    } catch (error) {
      nexoraSentry.captureError(error);
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
      nexoraSentry.captureError(error);
    } finally {
      reviewing.value = false;
    }
  }

  async function resetSecret() {
    resettingSecret.value = true;
    try {
      const response = await applicationApi.resetSecret(applicationId);
      Object.assign(newCredential, response.data || {});
      detail.credential = {
        ...(detail.credential || {}),
        appId: response.data?.appId,
        maskedSecret: response.data?.maskedSecret,
        versionNo: response.data?.versionNo,
      };
      if (![2, 4].includes(detail.listingStatus)) {
        detail.accessStatus = 1;
      }
      message.success('App Secret 已重置，请立即保存新密钥');
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      resettingSecret.value = false;
    }
  }

  async function copyText(value) {
    if (!value) {
      message.warning('当前没有可复制的内容');
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      message.success('已复制');
    } catch (error) {
      nexoraSentry.captureError(error);
      message.error('复制失败，请手动选择复制');
    }
  }

  function clearNewSecret() {
    Object.keys(newCredential).forEach((key) => delete newCredential[key]);
  }

  onMounted(loadDetail);
</script>
