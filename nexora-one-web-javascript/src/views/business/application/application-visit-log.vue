<template>
  <div class="application-page">
    <header class="application-page__header">
      <div><h1 class="application-page__title">应用访问日志</h1><div class="application-page__subtitle">追踪应用进入结果、访问人员和失败原因。</div></div>
      <a-button @click="queryData"><ReloadOutlined />刷新日志</a-button>
    </header>

    <section class="application-panel">
      <div class="application-query application-query--logs">
        <div>
          <label class="application-query__label">应用 / 用户 / 部门</label>
          <a-input v-model:value="queryForm.searchWord" allow-clear placeholder="请输入查询关键词" @pressEnter="search" />
        </div>
        <div>
          <label class="application-query__label">访问结果</label>
          <a-select v-model:value="queryForm.successFlag" allow-clear placeholder="全部结果">
            <a-select-option :value="true">成功</a-select-option>
            <a-select-option :value="false">失败</a-select-option>
          </a-select>
        </div>
        <a-space><a-button type="primary" @click="search"><SearchOutlined />查询</a-button><a-button @click="reset">重置</a-button></a-space>
      </div>
    </section>

    <section class="application-panel">
      <a-table :loading="loading" :data-source="records" :columns="columns" row-key="visitLogId" :pagination="false" :scroll="{ x: 1180 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'successFlag'">
            <a-badge :status="record.successFlag ? 'success' : 'error'" :text="record.successFlag ? '成功' : '失败'" />
          </template>
          <template v-else-if="column.dataIndex === 'failureReason'">{{ record.failureReason || '-' }}</template>
          <template v-else-if="column.dataIndex === 'launchUrl'">
            <a-tooltip :title="record.launchUrl"><span class="application-log-url">{{ record.launchUrl || '-' }}</span></a-tooltip>
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
  import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './application.less';

  const loading = ref(false);
  const records = ref([]);
  const total = ref(0);
  const queryForm = reactive({ pageNum: 1, pageSize: 10, searchWord: '', successFlag: undefined });
  const columns = [
    { title: '应用名称', dataIndex: 'applicationName', width: 180 },
    { title: '访问用户', dataIndex: 'employeeName', width: 130 },
    { title: '所属部门', dataIndex: 'departmentName', width: 180 },
    { title: '访问结果', dataIndex: 'successFlag', width: 110 },
    { title: '跳转地址', dataIndex: 'launchUrl', width: 260 },
    { title: '失败原因', dataIndex: 'failureReason', width: 220 },
    { title: '访问 IP', dataIndex: 'ipAddress', width: 150 },
    { title: '访问时间', dataIndex: 'visitTime', width: 180 },
  ];

  async function queryData() {
    loading.value = true;
    try {
      const response = await applicationApi.queryVisitLogs(queryForm);
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

  function reset() {
    Object.assign(queryForm, { pageNum: 1, pageSize: 10, searchWord: '', successFlag: undefined });
    queryData();
  }

  onMounted(queryData);
</script>
