<template>
  <div class="application-page">
    <header class="application-page__header">
      <div><h1 class="application-page__title">应用审核</h1><div class="application-page__subtitle">处理应用上架申请，审核通过后立即发布。</div></div>
      <a-badge :count="total" :overflow-count="999"><a-button><AuditOutlined />待审核队列</a-button></a-badge>
    </header>

    <section class="application-panel">
      <div class="application-review-toolbar">
        <a-input-search v-model:value="queryForm.searchWord" allow-clear placeholder="搜索应用名称或编码" @search="search" />
        <a-button @click="queryData"><ReloadOutlined />刷新</a-button>
      </div>
      <a-table :loading="loading" :data-source="records" :columns="columns" row-key="applicationId" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'applicationName'">
            <div class="application-table-app">
              <div class="application-icon"><img v-if="record.iconUrl" :src="record.iconUrl" alt="" /><AppstoreOutlined v-else /></div>
              <div><div class="application-table-app__name">{{ record.applicationName }}</div><div class="application-table-app__summary">{{ record.summary }}</div></div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'applicationType'">{{ record.applicationType === 1 ? '企业内部应用' : '第三方应用' }}</template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" @click="goDetail(record)">查看详情</a-button>
              <a-button type="primary" @click="openReview(record)">处理审核</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <a-empty v-if="!loading && !records.length" description="当前没有待审核应用" />
      <div v-if="total" class="smart-query-table-page">
        <a-pagination v-model:current="queryForm.pageNum" v-model:page-size="queryForm.pageSize" :total="total" @change="queryData" />
      </div>
    </section>

    <a-modal v-model:open="modalVisible" title="处理应用审核" :footer="null">
      <a-alert type="info" show-icon :message="`正在审核：${selected?.applicationName || ''}`" />
      <a-form layout="vertical" class="application-review-form">
        <a-form-item label="审核说明" required>
          <a-textarea v-model:value="reviewRemark" :rows="5" :maxlength="500" show-count placeholder="请填写审核结论、风险或修改建议" />
        </a-form-item>
      </a-form>
      <div class="application-review-actions">
        <a-button @click="modalVisible = false">取消</a-button>
        <a-button danger :loading="reviewing" @click="submit(3)">驳回</a-button>
        <a-button type="primary" :loading="reviewing" @click="submit(2)">通过并发布</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { AppstoreOutlined, AuditOutlined, ReloadOutlined } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './application.less';

  const router = useRouter();
  const loading = ref(false);
  const reviewing = ref(false);
  const modalVisible = ref(false);
  const reviewRemark = ref('');
  const selected = ref(null);
  const records = ref([]);
  const total = ref(0);
  const queryForm = reactive({ pageNum: 1, pageSize: 10, searchWord: '', listingStatus: 1 });
  const columns = [
    { title: '应用信息', dataIndex: 'applicationName', width: 360 },
    { title: 'App ID', dataIndex: 'appId', width: 220 },
    { title: '应用类型', dataIndex: 'applicationType', width: 140 },
    { title: '负责人', dataIndex: 'ownerName', width: 130 },
    { title: '提交时间', dataIndex: 'updateTime', width: 180 },
    { title: '操作', dataIndex: 'action', width: 200 },
  ];

  async function queryData() {
    loading.value = true;
    try {
      const response = await applicationApi.query(queryForm);
      records.value = response.data.list || [];
      total.value = response.data.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function search() {
    queryForm.pageNum = 1;
    queryData();
  }

  function goDetail(record) {
    router.push({ path: '/application/detail', query: { applicationId: record.applicationId } });
  }

  function openReview(record) {
    selected.value = record;
    reviewRemark.value = '';
    modalVisible.value = true;
  }

  async function submit(reviewStatus) {
    if (!reviewRemark.value.trim()) {
      message.warning('请填写审核说明');
      return;
    }
    reviewing.value = true;
    try {
      await applicationApi.review({
        applicationId: selected.value.applicationId,
        reviewStatus,
        reviewRemark: reviewRemark.value.trim(),
      });
      message.success(reviewStatus === 2 ? '审核通过，应用已发布' : '应用已驳回');
      modalVisible.value = false;
      await queryData();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      reviewing.value = false;
    }
  }

  onMounted(queryData);
</script>
