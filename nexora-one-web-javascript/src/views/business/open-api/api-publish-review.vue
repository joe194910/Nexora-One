<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">API发布审核</h1>
        <div class="open-api-page__subtitle">由平台审核API定义、路由和市场资料，审核通过后正式开放调用</div>
      </div>
      <a-select v-model:value="reviewStatus" style="width: 140px" @change="loadData">
        <a-select-option :value="undefined">全部状态</a-select-option>
        <a-select-option :value="1">待审核</a-select-option>
        <a-select-option :value="2">已通过</a-select-option>
        <a-select-option :value="3">已驳回</a-select-option>
      </a-select>
    </header>

    <section class="open-api-panel">
      <a-table :columns="columns" :data-source="records" :loading="loading" row-key="review.reviewId">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'api'">
            <div><strong>{{ record.apiName }}</strong></div>
            <div class="open-api-muted">{{ record.apiCode }}</div>
          </template>
          <template v-else-if="column.key === 'route'">
            <a-tag :color="methodColor(record.requestMethod)">{{ record.requestMethod }}</a-tag>
            <span>{{ record.requestPath }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusMeta(record.review.reviewStatus).color">
              {{ statusMeta(record.review.reviewStatus).text }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space v-if="record.review.reviewStatus === 1">
              <a-button type="link" @click="openReview(record, 2)">通过</a-button>
              <a-button type="link" danger @click="openReview(record, 3)">驳回</a-button>
            </a-space>
            <span v-else>{{ record.review.reviewerName || '-' }}</span>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="modalVisible"
      :title="reviewForm.reviewStatus === 2 ? '通过发布申请' : '驳回发布申请'"
      :confirm-loading="submitting"
      ok-text="确认提交"
      @ok="submitReview"
    >
      <a-alert
        :type="reviewForm.reviewStatus === 2 ? 'success' : 'warning'"
        show-icon
        :message="reviewForm.reviewStatus === 2 ? '审核通过后API将立即上架并允许网关调用。' : '驳回后API将回到配置状态，可修改后重新提交。'"
      />
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="审核意见">
          <a-textarea v-model:value="reviewForm.reviewRemark" :rows="4" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import './open-api.less';

  const loading = ref(false);
  const submitting = ref(false);
  const modalVisible = ref(false);
  const reviewStatus = ref(1);
  const records = ref([]);
  const reviewForm = reactive({ reviewId: undefined, reviewStatus: 2, reviewRemark: '' });
  const columns = [
    { title: 'API信息', key: 'api', width: 260 },
    { title: '版本', dataIndex: 'versionNo', width: 100 },
    { title: '请求路由', key: 'route' },
    { title: '申请人', dataIndex: ['review', 'applicantName'], width: 120 },
    { title: '提交时间', dataIndex: ['review', 'createTime'], width: 180 },
    { title: '审核状态', key: 'status', width: 110 },
    { title: '操作', key: 'action', width: 140 },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const response = await openApiApi.publishReviews(reviewStatus.value);
      records.value = response.data || [];
    } finally {
      loading.value = false;
    }
  }

  function openReview(record, status) {
    Object.assign(reviewForm, {
      reviewId: record.review.reviewId,
      reviewStatus: status,
      reviewRemark: status === 2 ? '审核通过，允许上架发布。' : '',
    });
    modalVisible.value = true;
  }

  async function submitReview() {
    if (reviewForm.reviewStatus === 3 && !reviewForm.reviewRemark.trim()) {
      message.warning('驳回时请填写审核意见');
      return;
    }
    submitting.value = true;
    try {
      await openApiApi.reviewPublish(reviewForm);
      message.success(reviewForm.reviewStatus === 2 ? '审核通过，API已上架' : '发布申请已驳回');
      modalVisible.value = false;
      await loadData();
    } finally {
      submitting.value = false;
    }
  }

  function statusMeta(status) {
    return {
      1: { text: '待审核', color: 'orange' },
      2: { text: '已通过', color: 'green' },
      3: { text: '已驳回', color: 'red' },
    }[status] || { text: '未知', color: 'default' };
  }

  function methodColor(method) {
    return method === 'GET' ? 'green' : method === 'DELETE' ? 'red' : 'blue';
  }

  onMounted(loadData);
</script>
