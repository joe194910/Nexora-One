<template>
  <div class="application-page">
    <header class="application-page__header">
      <div>
        <h1 class="application-page__title">应用管理</h1>
        <div class="application-page__subtitle">统一管理应用配置、发布状态和运行数据。</div>
      </div>
      <a-button type="primary" @click="router.push('/application/onboarding')" v-privilege="'application:create'">
        <PlusOutlined />创建应用
      </a-button>
    </header>

    <section class="application-summary-grid">
      <div v-for="item in summaryItems" :key="item.key" class="application-summary-item">
        <component :is="item.icon" />
        <div><span>{{ item.label }}</span><strong>{{ summary[item.key] || 0 }}</strong></div>
      </div>
    </section>

    <section class="application-panel">
      <div class="application-query">
        <div>
          <label class="application-query__label">应用名称 / 编码</label>
          <a-input v-model:value="queryForm.searchWord" allow-clear placeholder="请输入关键词" @pressEnter="search" />
        </div>
        <div>
          <label class="application-query__label">应用类型</label>
          <a-select v-model:value="queryForm.applicationType" allow-clear placeholder="全部类型">
            <a-select-option :value="1">企业内部应用</a-select-option>
            <a-select-option :value="2">第三方应用</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="application-query__label">上架状态</label>
          <a-select v-model:value="queryForm.listingStatus" allow-clear placeholder="全部状态">
            <a-select-option v-for="item in listingOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </div>
        <a-space><a-button type="primary" @click="search"><SearchOutlined />查询</a-button><a-button @click="reset"><ReloadOutlined />重置</a-button></a-space>
      </div>
    </section>

    <section class="application-panel">
      <a-table :loading="loading" :data-source="records" :columns="columns" row-key="applicationId" :pagination="false" :scroll="{ x: 1080 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'applicationName'">
            <div class="application-table-app">
              <div class="application-icon"><img v-if="record.iconUrl" :src="record.iconUrl" alt="" /><AppstoreOutlined v-else /></div>
              <div><div class="application-table-app__name">{{ record.applicationName }}</div><div class="application-table-app__summary">{{ record.summary }}</div></div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'applicationType'">
            <a-tag :color="record.applicationType === 1 ? 'blue' : 'purple'">{{ record.applicationType === 1 ? '企业应用' : '第三方应用' }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'listingStatus'">
            <a-tag :color="listingMeta(record.listingStatus).color">{{ listingMeta(record.listingStatus).text }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <div class="application-actions">
              <a-button type="link" @click="detail(record)">详情</a-button>
              <a-button type="link" :disabled="record.configLocked" @click="configure(record)">配置</a-button>
              <a-popconfirm v-if="record.listingStatus === 2" title="下架后用户将无法进入该应用，确认下架吗？" @confirm="changeStatus(record, 4)">
                <a-button type="link" danger v-privilege="'application:status'">下架</a-button>
              </a-popconfirm>
              <a-button v-else-if="record.listingStatus === 4" type="link" @click="changeStatus(record, 2)" v-privilege="'application:status'">重新上架</a-button>
            </div>
          </template>
        </template>
      </a-table>
      <div class="smart-query-table-page">
        <a-pagination v-model:current="queryForm.pageNum" v-model:page-size="queryForm.pageSize" :total="total" show-size-changer @change="queryData" />
      </div>
    </section>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { AppstoreOutlined, AuditOutlined, CheckCircleOutlined, HistoryOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './application.less';

  const router = useRouter();
  const loading = ref(false);
  const records = ref([]);
  const total = ref(0);
  const summary = reactive({});
  const queryForm = reactive({ pageNum: 1, pageSize: 10, searchWord: '', applicationType: undefined, listingStatus: undefined });
  const summaryItems = [
    { key: 'total', label: '应用总数', icon: AppstoreOutlined },
    { key: 'listed', label: '已上架', icon: CheckCircleOutlined },
    { key: 'reviewing', label: '审核中', icon: AuditOutlined },
    { key: 'unlisted', label: '未上架 / 已下架', icon: StopOutlined },
    { key: 'visits', label: '累计访问', icon: HistoryOutlined },
  ];
  const listingOptions = [
    { value: 0, label: '未上架' }, { value: 1, label: '审核中' }, { value: 2, label: '已上架' },
    { value: 3, label: '已驳回' }, { value: 4, label: '已下架' },
  ];
  const columns = [
    { title: '应用信息', dataIndex: 'applicationName', width: 320 },
    { title: 'App ID', dataIndex: 'appId', width: 220 },
    { title: '类型', dataIndex: 'applicationType', width: 120 },
    { title: '上架状态', dataIndex: 'listingStatus', width: 120 },
    { title: '负责人', dataIndex: 'ownerName', width: 120 },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 230 },
  ];

  function listingMeta(value) {
    return {
      0: { color: 'default', text: '未上架' }, 1: { color: 'processing', text: '审核中' },
      2: { color: 'success', text: '已上架' }, 3: { color: 'error', text: '已驳回' }, 4: { color: 'default', text: '已下架' },
    }[value] || { color: 'default', text: '未知' };
  }

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

  async function loadSummary() {
    const response = await applicationApi.summary();
    Object.assign(summary, response.data);
  }

  function search() {
    queryForm.pageNum = 1;
    queryData();
  }

  function reset() {
    Object.assign(queryForm, { pageNum: 1, pageSize: 10, searchWord: '', applicationType: undefined, listingStatus: undefined });
    queryData();
  }

  function detail(record) {
    router.push({ path: '/application/detail', query: { applicationId: record.applicationId } });
  }

  function configure(record) {
    router.push({ path: '/application/onboarding', query: { applicationId: record.applicationId, step: Math.min((record.workflowStep || 1) + 1, 8) } });
  }

  async function changeStatus(record, listingStatus) {
    try {
      await applicationApi.updateStatus({ applicationId: record.applicationId, listingStatus });
      message.success(listingStatus === 2 ? '应用已重新上架' : '应用已下架');
      await Promise.all([queryData(), loadSummary()]);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  onMounted(() => Promise.all([queryData(), loadSummary()]));
</script>
