<template>
  <div class="ai-page">
    <header class="ai-page__header">
      <div><h1 class="ai-page__title">调用日志</h1><div class="ai-page__subtitle">记录模型调用链路，用于审计、统计与故障排查。</div></div>
      <a-button v-privilege="'ai:call-log:export'" type="primary" @click="aiPlatformApi.exportCallLogs(params())"><DownloadOutlined />导出日志</a-button>
    </header>

    <section class="ai-summary">
      <div v-for="item in cards" :key="item.key" class="ai-summary__item">
        <span class="ai-summary__icon"><component :is="item.icon" /></span>
        <div><div class="ai-summary__label">{{ item.label }}</div><div class="ai-summary__value">{{ item.format(summary[item.key]) }}</div></div>
      </div>
    </section>

    <section class="ai-panel">
      <div class="ai-query">
        <div><label class="ai-query__label">时间范围</label><a-range-picker v-model:value="dateRange" show-time style="width: 100%" /></div>
        <div><label class="ai-query__label">用户</label><a-input v-model:value="queryForm.userName" allow-clear /></div>
        <div><label class="ai-query__label">调用类型</label><a-select v-model:value="queryForm.callType" allow-clear style="width: 100%"><a-select-option value="CHAT">对话</a-select-option><a-select-option value="EMBEDDING">向量化</a-select-option><a-select-option value="RERANK">重排序</a-select-option></a-select></div>
        <div><label class="ai-query__label">Trace ID</label><a-input v-model:value="queryForm.traceId" allow-clear /></div>
        <a-space><a-button type="primary" @click="load"><SearchOutlined />查询</a-button><a-button @click="reset"><ReloadOutlined />重置</a-button></a-space>
      </div>
    </section>

    <section class="ai-panel">
      <a-table :loading="loading" :data-source="rows" :columns="columns" row-key="callLogId" :pagination="false" :scroll="{ x: 1200 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'successFlag'"><a-tag :color="record.successFlag ? 'green' : 'red'">{{ record.successFlag ? '成功' : '失败' }}</a-tag></template>
          <template v-else-if="column.dataIndex === 'totalDurationMs'">{{ formatDuration(record.totalDurationMs) }}</template>
          <template v-else-if="column.dataIndex === 'traceId'"><a-typography-text copyable>{{ record.traceId }}</a-typography-text></template>
          <template v-else-if="column.dataIndex === 'action'"><a-button v-privilege="'ai:call-log:detail'" type="link" @click="showDetail(record)">查看</a-button></template>
        </template>
      </a-table>
      <div class="ai-pagination"><a-pagination v-model:current="queryForm.pageNum" v-model:page-size="queryForm.pageSize" show-size-changer :total="total" @change="load" /></div>
    </section>

    <a-drawer v-model:open="detail.open" title="调用详情" width="520">
      <a-descriptions v-if="detail.data" bordered size="small" :column="1">
        <a-descriptions-item label="状态"><a-tag :color="detail.data.successFlag ? 'green' : 'red'">{{ detail.data.successFlag ? '调用成功' : '调用失败' }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="Trace ID">{{ detail.data.traceId }}</a-descriptions-item>
        <a-descriptions-item label="调用时间">{{ detail.data.createTime }}</a-descriptions-item>
        <a-descriptions-item label="用户">{{ detail.data.userName }} / {{ detail.data.userIp || '-' }}</a-descriptions-item>
        <a-descriptions-item label="服务与模型">{{ detail.data.providerName }} / {{ detail.data.modelCode }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ detail.data.sourceType }} / {{ detail.data.sourceName }}</a-descriptions-item>
        <a-descriptions-item label="Token">{{ detail.data.inputTokens }} + {{ detail.data.outputTokens }} = {{ detail.data.totalTokens }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ formatDuration(detail.data.totalDurationMs) }}</a-descriptions-item>
        <a-descriptions-item label="预估费用">¥{{ Number(detail.data.estimatedCost || 0).toFixed(6) }}</a-descriptions-item>
        <a-descriptions-item label="请求摘要"><div class="ai-code-block">{{ detail.data.requestSummary }}</div></a-descriptions-item>
        <a-descriptions-item label="响应摘要"><div class="ai-code-block">{{ detail.data.responseSummary || detail.data.errorMessage }}</div></a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import dayjs from 'dayjs';
  import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, DownloadOutlined, ProfileOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue';
  import { aiPlatformApi } from '/@/api/business/ai/ai-platform-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './ai-platform.less';

  const today = () => [dayjs().startOf('day'), dayjs().endOf('day')];
  const dateRange = ref(today());
  const loading = ref(false);
  const rows = ref([]);
  const total = ref(0);
  const summary = reactive({ total: 0, success: 0, failed: 0, averageDurationMs: 0 });
  const queryForm = reactive({ pageNum: 1, pageSize: 10, userName: '', callType: undefined, traceId: '' });
  const detail = reactive({ open: false, data: null });
  const cards = [
    { key: 'total', label: '今日调用', icon: ProfileOutlined, format: formatNumber },
    { key: 'success', label: '成功', icon: CheckCircleOutlined, format: formatNumber },
    { key: 'failed', label: '失败', icon: CloseCircleOutlined, format: formatNumber },
    { key: 'averageDurationMs', label: '平均耗时', icon: ClockCircleOutlined, format: formatDuration },
  ];
  const columns = [
    { title: '调用时间', dataIndex: 'createTime', width: 170 }, { title: '用户', dataIndex: 'userName', width: 100 },
    { title: '模型', dataIndex: 'modelCode', width: 180 }, { title: '调用类型', dataIndex: 'callType', width: 100 },
    { title: '来源', dataIndex: 'sourceName', width: 150 }, { title: '输入 Token', dataIndex: 'inputTokens', width: 100 },
    { title: '输出 Token', dataIndex: 'outputTokens', width: 100 }, { title: '耗时', dataIndex: 'totalDurationMs', width: 90 },
    { title: '状态', dataIndex: 'successFlag', width: 90 }, { title: 'Trace ID', dataIndex: 'traceId', width: 230 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 80 },
  ];

  /**
   * 组装包含时间范围的日志查询参数。
   */
  function params() {
    return { ...queryForm, beginTime: dateRange.value?.[0]?.format('YYYY-MM-DD HH:mm:ss'), endTime: dateRange.value?.[1]?.format('YYYY-MM-DD HH:mm:ss') };
  }
  /**
   * 并行加载日志分页数据与概览统计。
   */
  async function load() {
    loading.value = true;
    try {
      const [listResponse, summaryResponse] = await Promise.all([aiPlatformApi.queryCallLogs(params()), aiPlatformApi.callLogSummary(params())]);
      rows.value = listResponse.data.list || []; total.value = listResponse.data.total || 0; Object.assign(summary, summaryResponse.data || {});
    } catch (error) { smartSentry.captureError(error); } finally { loading.value = false; }
  }
  /**
   * 重置日志查询条件。
   */
  function reset() { Object.assign(queryForm, { pageNum: 1, pageSize: 10, userName: '', callType: undefined, traceId: '' }); dateRange.value = today(); load(); }
  /**
   * 查询并展示单次调用完整详情。
   */
  async function showDetail(record) { const response = await aiPlatformApi.callLogDetail(record.callLogId); detail.data = response.data; detail.open = true; }
  function formatNumber(value) { return Number(value || 0).toLocaleString('zh-CN'); }
  function formatDuration(value) { return `${(Number(value || 0) / 1000).toFixed(2)}s`; }
  onMounted(load);
</script>
