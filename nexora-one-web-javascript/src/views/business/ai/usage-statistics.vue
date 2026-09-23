<template>
  <div class="ai-page">
    <header class="ai-page__header">
      <div><h1 class="ai-page__title">用量统计</h1><div class="ai-page__subtitle">分析模型调用、Token 消耗、成功率与平台资源使用情况。</div></div>
    </header>

    <section class="ai-panel">
      <a-space wrap>
        <a-radio-group v-model:value="period" button-style="solid" @change="changePeriod">
          <a-radio-button value="today">今日</a-radio-button><a-radio-button value="7">近 7 天</a-radio-button><a-radio-button value="30">近 30 天</a-radio-button>
        </a-radio-group>
        <a-range-picker v-model:value="dateRange" />
        <a-select v-model:value="queryForm.serviceId" allow-clear placeholder="服务商：全部" style="width: 180px">
          <a-select-option v-for="item in services" :key="item.serviceId" :value="item.serviceId">{{ item.serviceName }}</a-select-option>
        </a-select>
        <a-select v-model:value="queryForm.callType" allow-clear placeholder="调用类型：全部" style="width: 180px">
          <a-select-option value="CHAT">对话</a-select-option><a-select-option value="EMBEDDING">向量化</a-select-option><a-select-option value="RERANK">重排序</a-select-option>
        </a-select>
        <a-button type="primary" :loading="loading" @click="load"><SearchOutlined />查询</a-button>
      </a-space>
    </section>

    <section class="ai-summary">
      <div v-for="item in cards" :key="item.key" class="ai-summary__item">
        <span class="ai-summary__icon"><component :is="item.icon" /></span>
        <div><div class="ai-summary__label">{{ item.label }}</div><div class="ai-summary__value">{{ item.format(data.summary?.[item.key]) }}</div></div>
      </div>
    </section>

    <div class="ai-grid-2">
      <section class="ai-panel"><h2 class="ai-panel__title">调用与 Token 趋势</h2><div ref="trendEl" class="ai-chart"></div></section>
      <section class="ai-panel"><h2 class="ai-panel__title">模型调用分布</h2><div ref="pieEl" class="ai-chart"></div></section>
    </div>

    <div class="ai-grid-2">
      <section class="ai-panel"><h2 class="ai-panel__title">服务商用量</h2><a-table :data-source="data.providers" :columns="rankColumns" row-key="name" :pagination="false" size="small" /></section>
      <section class="ai-panel"><h2 class="ai-panel__title">用户用量排行</h2><a-table :data-source="data.users" :columns="rankColumns" row-key="name" :pagination="false" size="small" /></section>
    </div>
  </div>
</template>

<script setup>
  import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
  import dayjs from 'dayjs';
  import * as echarts from 'echarts';
  import { CheckCircleOutlined, DatabaseOutlined, DollarOutlined, SearchOutlined, ThunderboltOutlined } from '@ant-design/icons-vue';
  import { aiPlatformApi } from '/@/api/business/ai/ai-platform-api';
  import { nexoraSentry } from '/@/lib/nexora-sentry';
  import './ai-platform.less';

  const loading = ref(false);
  const period = ref('7');
  const dateRange = ref([dayjs().subtract(6, 'day'), dayjs()]);
  const services = ref([]);
  const trendEl = ref();
  const pieEl = ref();
  const queryForm = reactive({ serviceId: undefined, callType: undefined });
  const data = reactive({ summary: {}, trend: [], modelDistribution: {}, providers: [], users: [] });
  let trendChart;
  let pieChart;
  const cards = [
    { key: 'calls', label: '总调用次数', icon: ThunderboltOutlined, format: integer },
    { key: 'tokens', label: 'Token 总量', icon: DatabaseOutlined, format: compact },
    { key: 'successRate', label: '调用成功率', icon: CheckCircleOutlined, format: (value) => `${Number(value || 0).toFixed(2)}%` },
    { key: 'cost', label: '预计费用', icon: DollarOutlined, format: (value) => `¥${Number(value || 0).toFixed(4)}` },
  ];
  const rankColumns = [
    { title: '名称', dataIndex: 'name' }, { title: '调用次数', dataIndex: 'calls' }, { title: 'Token', dataIndex: 'tokens' },
    { title: '成功率', dataIndex: 'successRate', customRender: ({ text }) => `${Number(text || 0).toFixed(1)}%` },
  ];

  function integer(value) { return Number(value || 0).toLocaleString('zh-CN'); }
  function compact(value) { return new Intl.NumberFormat('zh-CN', { notation: 'compact', maximumFractionDigits: 2 }).format(Number(value || 0)); }
  /**
   * 根据快捷周期更新统计时间范围。
   */
  function changePeriod() {
    dateRange.value = period.value === 'today' ? [dayjs(), dayjs()] : [dayjs().subtract(Number(period.value) - 1, 'day'), dayjs()];
    load();
  }
  /**
   * 查询指定时间范围内的真实调用统计。
   */
  async function load() {
    loading.value = true;
    try {
      const response = await aiPlatformApi.statistics({
        ...queryForm,
        beginTime: dateRange.value[0].startOf('day').format('YYYY-MM-DD HH:mm:ss'),
        endTime: dateRange.value[1].endOf('day').format('YYYY-MM-DD HH:mm:ss'),
      });
      Object.assign(data, response.data || {});
      await nextTick(); renderCharts();
    } catch (error) { nexoraSentry.captureError(error); } finally { loading.value = false; }
  }
  /**
   * 渲染调用趋势和模型分布图表。
   */
  function renderCharts() {
    trendChart ||= echarts.init(trendEl.value);
    pieChart ||= echarts.init(pieEl.value);
    trendChart.setOption({
      tooltip: { trigger: 'axis' }, legend: { data: ['调用次数', 'Token 用量'] }, grid: { left: 45, right: 50, bottom: 35, top: 45 },
      xAxis: { type: 'category', data: data.trend.map((item) => item.date) },
      yAxis: [{ type: 'value', name: '调用次数' }, { type: 'value', name: 'Token' }],
      series: [
        { name: '调用次数', type: 'bar', barMaxWidth: 34, itemStyle: { color: '#3b82f6' }, data: data.trend.map((item) => item.calls) },
        { name: 'Token 用量', type: 'line', yAxisIndex: 1, smooth: true, itemStyle: { color: '#7c3aed' }, data: data.trend.map((item) => item.tokens) },
      ],
    });
    pieChart.setOption({
      tooltip: { trigger: 'item' }, legend: { orient: 'vertical', right: 10, top: 'middle' },
      series: [{ type: 'pie', radius: ['45%', '70%'], center: ['38%', '50%'], label: { show: false },
        data: Object.entries(data.modelDistribution || {}).map(([name, value]) => ({ name, value })) }],
    });
  }
  function resize() { trendChart?.resize(); pieChart?.resize(); }
  onMounted(async () => {
    try { services.value = (await aiPlatformApi.serviceOptions()).data || []; } catch (error) { nexoraSentry.captureError(error); }
    window.addEventListener('resize', resize); load();
  });
  onBeforeUnmount(() => { window.removeEventListener('resize', resize); trendChart?.dispose(); pieChart?.dispose(); });
</script>
