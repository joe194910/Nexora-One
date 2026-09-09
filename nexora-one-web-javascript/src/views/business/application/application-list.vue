<template>
  <div class="application-page">
    <header class="application-page__header">
      <div>
        <h1 class="application-page__title">应用接入</h1>
        <div class="application-page__subtitle">接入企业内部或第三方应用，统一管理应用配置、权限与上架发布。</div>
      </div>
      <a-button type="primary" size="large" @click="goCreate" v-privilege="'application:create'">
        <template #icon><PlusOutlined /></template>
        创建应用
      </a-button>
    </header>

    <section class="application-panel">
      <div class="application-query">
        <div>
          <label class="application-query__label">应用名称 / ID</label>
          <a-input v-model:value="queryForm.searchWord" placeholder="请输入应用名称、编码或 App ID" @pressEnter="queryData" />
        </div>
        <div>
          <label class="application-query__label">应用类型</label>
          <a-select v-model:value="queryForm.applicationType" allowClear placeholder="请选择应用类型" style="width: 100%">
            <a-select-option :value="1">企业内部应用</a-select-option>
            <a-select-option :value="2">第三方应用</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="application-query__label">上架状态</label>
          <a-select v-model:value="queryForm.listingStatus" allowClear placeholder="请选择上架状态" style="width: 100%">
            <a-select-option v-for="item in listingOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </div>
        <a-space>
          <a-button type="primary" @click="queryData"><SearchOutlined />查询</a-button>
          <a-button @click="resetQuery"><ReloadOutlined />重置</a-button>
        </a-space>
      </div>
    </section>

    <section class="application-panel">
      <a-table
        :loading="loading"
        :data-source="tableData"
        :columns="columns"
        row-key="applicationId"
        :pagination="false"
        :scroll="{ x: 1120 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'applicationName'">
            <div class="application-table-app">
              <div class="application-icon">
                <img v-if="record.iconUrl" :src="record.iconUrl" alt="" />
                <AppstoreOutlined v-else />
              </div>
              <div>
                <div class="application-table-app__name">{{ record.applicationName }}</div>
                <div class="application-table-app__summary">{{ record.summary }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'applicationType'">
            <a-tag :color="record.applicationType === 1 ? 'blue' : 'purple'">
              {{ record.applicationType === 1 ? '企业应用' : '第三方应用' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'accessStatus'">
            <a-badge :status="accessMeta(record.accessStatus).status" :text="accessMeta(record.accessStatus).text" />
          </template>
          <template v-else-if="column.dataIndex === 'listingStatus'">
            <a-tag :color="listingMeta(record.listingStatus).color">{{ listingMeta(record.listingStatus).text }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <div class="application-actions">
              <a-button type="link" @click="goDetail(record)" v-privilege="'application:detail'">详情</a-button>
              <a-button type="link" :disabled="record.configLocked" @click="goConfigure(record)" v-privilege="'application:save'">配置</a-button>
              <a-button type="link" :disabled="record.workflowStep < 7 || record.configLocked" @click="goConfigure(record, 8)" v-privilege="'application:submit'">
                提交上架
              </a-button>
            </div>
          </template>
        </template>
      </a-table>
      <div class="smart-query-table-page">
        <a-pagination
          v-model:current="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          show-size-changer
          :total="total"
          :show-total="(value) => `共 ${value} 条`"
          @change="queryData"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { AppstoreOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './application.less';

  const router = useRouter();
  const loading = ref(false);
  const tableData = ref([]);
  const total = ref(0);
  const queryForm = reactive({ pageNum: 1, pageSize: 10, searchWord: '', applicationType: undefined, listingStatus: undefined });
  const listingOptions = [
    { value: 0, label: '未上架' },
    { value: 1, label: '审核中' },
    { value: 2, label: '已上架' },
    { value: 3, label: '已驳回' },
    { value: 4, label: '已下架' },
  ];
  const columns = [
    { title: '应用信息', dataIndex: 'applicationName', width: 330 },
    { title: 'App ID', dataIndex: 'appId', width: 230 },
    { title: '应用类型', dataIndex: 'applicationType', width: 130 },
    { title: '接入状态', dataIndex: 'accessStatus', width: 120 },
    { title: '上架状态', dataIndex: 'listingStatus', width: 120 },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 230 },
  ];

  function accessMeta(value) {
    return { 1: { status: 'processing', text: '接入中' }, 2: { status: 'success', text: '已接入' }, 3: { status: 'error', text: '接入失败' } }[value] || { status: 'default', text: '未知' };
  }

  function listingMeta(value) {
    return {
      0: { color: 'default', text: '未上架' },
      1: { color: 'processing', text: '审核中' },
      2: { color: 'success', text: '已上架' },
      3: { color: 'error', text: '已驳回' },
      4: { color: 'default', text: '已下架' },
    }[value] || { color: 'default', text: '未知' };
  }

  async function queryData() {
    loading.value = true;
    try {
      const response = await applicationApi.query(queryForm);
      tableData.value = response.data.list;
      total.value = response.data.total;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function resetQuery() {
    Object.assign(queryForm, { pageNum: 1, pageSize: 10, searchWord: '', applicationType: undefined, listingStatus: undefined });
    queryData();
  }

  function goCreate() {
    router.push({ path: '/application/onboarding' });
  }

  function goConfigure(record, step) {
    router.push({ path: '/application/onboarding', query: { applicationId: record.applicationId, step: step || Math.min(record.workflowStep + 1, 8) } });
  }

  function goDetail(record) {
    router.push({ path: '/application/detail', query: { applicationId: record.applicationId } });
  }

  onMounted(queryData);
</script>
