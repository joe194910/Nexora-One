<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">调用统计</h1>
        <div class="open-api-page__subtitle">查看 API 调用量、成功率、响应耗时与最近调用链路</div>
      </div>
      <a-button @click="loadData"><ReloadOutlined />刷新</a-button>
    </header>
    <section class="open-api-summary">
      <div v-for="item in summaryItems" :key="item.key" class="open-api-summary__item">
        <span class="open-api-summary__icon" :class="item.iconClass"><component :is="item.icon" /></span>
        <div>
          <div class="open-api-summary__label">{{ item.label }}</div>
          <div class="open-api-summary__value">{{ item.value }}</div>
        </div>
      </div>
    </section>
    <section class="open-api-panel">
      <h2 class="open-api-panel__title">最近调用日志</h2>
      <a-table :loading="loading" :columns="columns" :data-source="data.logs" row-key="callLogId" :pagination="{ pageSize: 10 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'successFlag'">
            <a-badge :status="record.successFlag ? 'success' : 'error'" :text="record.successFlag ? '成功' : '失败'" />
          </template>
          <template v-else-if="column.dataIndex === 'httpStatus'">
            <a-tag :color="record.httpStatus < 300 ? 'green' : 'red'">{{ record.httpStatus }}</a-tag>
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { ApiOutlined, CheckCircleOutlined, ClockCircleOutlined, ReloadOutlined, UnorderedListOutlined } from '@ant-design/icons-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import './open-api.less';

  const loading = ref(false);
  const data = reactive({ total: 0, recentTotal: 0, successRate: 100, averageDurationMs: 0, logs: [] });
  const summaryItems = computed(() => [
    { key: 'total', label: '累计调用', value: Number(data.total).toLocaleString('zh-CN'), icon: ApiOutlined },
    { key: 'recentTotal', label: '最近样本', value: data.recentTotal, icon: UnorderedListOutlined, iconClass: 'is-muted' },
    { key: 'successRate', label: '成功率', value: `${Number(data.successRate).toFixed(2)}%`, icon: CheckCircleOutlined, iconClass: 'is-success' },
    { key: 'averageDurationMs', label: '平均响应', value: `${data.averageDurationMs} ms`, icon: ClockCircleOutlined, iconClass: 'is-muted' },
  ]);
  const columns = [
    { title: 'Trace ID', dataIndex: 'traceId', width: 260, ellipsis: true },
    { title: 'API编码', dataIndex: 'apiCode', width: 180 },
    { title: 'App ID', dataIndex: 'appId', width: 210, ellipsis: true },
    { title: '环境', dataIndex: 'environmentCode', width: 90 },
    { title: '状态码', dataIndex: 'httpStatus', width: 100 },
    { title: '结果', dataIndex: 'successFlag', width: 100 },
    { title: '耗时(ms)', dataIndex: 'durationMs', width: 110 },
    { title: '调用时间', dataIndex: 'createTime', width: 180 },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const response = await openApiApi.statistics();
      Object.assign(data, response.data || {});
    } finally {
      loading.value = false;
    }
  }

  onMounted(loadData);
</script>
