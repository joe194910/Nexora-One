<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">我的授权</h1>
        <div class="open-api-page__subtitle">查看应用的 API 申请、审核与有效授权记录</div>
      </div>
      <a-select v-model:value="status" allow-clear placeholder="全部状态" style="width: 180px" @change="loadData">
        <a-select-option :value="1">待审核</a-select-option>
        <a-select-option :value="2">已授权</a-select-option>
        <a-select-option :value="3">已拒绝</a-select-option>
        <a-select-option :value="4">已撤销</a-select-option>
      </a-select>
    </header>

    <section class="open-api-panel">
      <a-table :loading="loading" :columns="columns" :data-source="list" :row-key="record => record.permission.permissionId" :pagination="{ pageSize: 10 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'api'">
            <strong>{{ record.apiName }}</strong>
            <div class="open-api-muted">{{ record.apiCode }} · {{ record.apiVersion }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'application'">
            {{ record.applicationName }}
            <div class="open-api-muted">{{ record.permission.applyEnvironment || 'test' }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'applyStatus'">
            <a-tag :color="statusMeta(record.permission.applyStatus).color">{{ statusMeta(record.permission.applyStatus).text }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button v-if="record.permission.applyStatus === 1" type="link" @click="openReview(record)" v-privilege="'open-api:grant:review'">
              审核
            </a-button>
            <span v-else class="open-api-muted">{{ record.permission.reviewRemark || '-' }}</span>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal v-model:open="reviewVisible" title="审核 API 权限申请" ok-text="提交审核" @ok="submitReview">
      <a-descriptions v-if="selected" bordered :column="1" size="small">
        <a-descriptions-item label="应用">{{ selected.applicationName }}</a-descriptions-item>
        <a-descriptions-item label="API">{{ selected.apiName }}</a-descriptions-item>
        <a-descriptions-item label="使用场景">{{ selected.permission.useScene }}</a-descriptions-item>
        <a-descriptions-item label="申请原因">{{ selected.permission.applyReason }}</a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical" class="open-api-modal-form">
        <a-form-item label="审核结果">
          <a-radio-group v-model:value="reviewForm.applyStatus">
            <a-radio :value="2">通过</a-radio>
            <a-radio :value="3">拒绝</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="reviewForm.applyStatus === 2" label="每日调用额度">
          <a-input-number v-model:value="reviewForm.dailyQuota" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="reviewForm.applyStatus === 2" label="有效天数">
          <a-input-number v-model:value="reviewForm.effectiveDays" :min="1" style="width: 100%" placeholder="留空表示长期有效" />
        </a-form-item>
        <a-form-item label="审核说明"><a-textarea v-model:value="reviewForm.reviewRemark" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const loading = ref(false);
  const list = ref([]);
  const status = ref(undefined);
  const reviewVisible = ref(false);
  const selected = ref(null);
  const reviewForm = reactive({ permissionId: undefined, applyStatus: 2, reviewRemark: '', dailyQuota: 100000, effectiveDays: undefined });
  const columns = [
    { title: 'API', dataIndex: 'api', width: 240 },
    { title: '应用 / 环境', dataIndex: 'application', width: 220 },
    { title: '申请人', dataIndex: ['permission', 'applicantName'], width: 120 },
    { title: '使用场景', dataIndex: ['permission', 'useScene'] },
    { title: '申请时间', dataIndex: ['permission', 'createTime'], width: 180 },
    { title: '状态', dataIndex: 'applyStatus', width: 110 },
    { title: '操作', dataIndex: 'action', width: 180 },
  ];

  function statusMeta(value) {
    return { 1: { color: 'orange', text: '待审核' }, 2: { color: 'green', text: '已授权' }, 3: { color: 'red', text: '已拒绝' }, 4: { color: 'default', text: '已撤销' } }[value] || { color: 'default', text: '未知' };
  }

  async function loadData() {
    loading.value = true;
    try {
      const response = await openApiApi.permissions(status.value);
      list.value = response.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function openReview(record) {
    selected.value = record;
    Object.assign(reviewForm, { permissionId: record.permission.permissionId, applyStatus: 2, reviewRemark: '', dailyQuota: 100000, effectiveDays: undefined });
    reviewVisible.value = true;
  }

  async function submitReview() {
    await openApiApi.reviewPermission(reviewForm);
    message.success('审核结果已提交');
    reviewVisible.value = false;
    loadData();
  }

  onMounted(loadData);
</script>
