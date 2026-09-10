<!-- NexoraOne 首页工作台 -->
<template>
  <div class="home-workbench">
    <section class="welcome-panel">
      <div class="welcome-copy">
        <h1>{{ greeting }}，{{ displayName }}</h1>
        <p>专注构建更高效的企业数字化平台 · NexoraOne</p>
      </div>

      <a-input-search
        v-model:value="searchKeyword"
        class="global-search"
        placeholder="搜索应用、文档、API 或功能（支持全局搜索）"
        enter-button="搜索"
        size="large"
        @search="handleSearch"
      />

      <div class="date-card">
        <div>{{ dateInfo.solar }}　星期{{ dateInfo.week }}</div>
        <div>农历{{ dateInfo.lunarMonth }}月{{ dateInfo.lunarDay }}</div>
        <div class="daily-sentence">做好每一件小事，推动更大的改变。</div>
      </div>
    </section>

    <section class="workbench-section application-section">
      <WorkbenchTitle title="我的应用" icon-color="#1677ff">
        <template #icon><AppstoreOutlined /></template>
      </WorkbenchTitle>
      <button class="more-button" type="button" @click="showMore('应用')">更多</button>

      <a-spin :spinning="applicationLoading">
        <div v-if="applicationList.length" class="application-grid">
          <button
            v-for="app in applicationList"
            :key="app.applicationId"
            class="application-item"
            type="button"
            @click="openApp(app)"
          >
            <span class="application-icon" :style="{ backgroundColor: app.color }">
              <img v-if="app.iconUrl" :src="app.iconUrl" :alt="app.name" />
              <AppstoreOutlined v-else />
            </span>
            <span class="application-copy">
              <strong>{{ app.name }}</strong>
              <small>{{ app.description }}</small>
            </span>
            <RightOutlined class="application-arrow" />
          </button>
        </div>
        <a-empty v-else class="application-empty" description="暂无可使用的应用" />
      </a-spin>
    </section>

    <div class="middle-grid">
      <section class="workbench-section assistant-section">
        <WorkbenchTitle title="智能助手" icon-color="#1677ff">
          <template #icon><RobotOutlined /></template>
        </WorkbenchTitle>
        <button class="more-button" type="button" @click="showMore('智能助手')">更多</button>

        <p class="section-description">基于企业知识库的智能助手，助你更高效地工作</p>
        <div class="assistant-input">
          <a-textarea
            v-model:value="assistantQuestion"
            :maxlength="500"
            :rows="4"
            show-count
            placeholder="请输入你的问题，例如：&#10;如何接入 API？&#10;MCP 服务如何配置？&#10;..."
          />
        </div>
        <div class="assistant-actions">
          <div class="assistant-shortcuts">
            <a-button v-for="shortcut in assistantShortcuts" :key="shortcut.label" @click="applyShortcut(shortcut)">
              <component :is="shortcut.icon" />
              {{ shortcut.label }}
            </a-button>
          </div>
          <a-button type="primary" class="send-button" @click="sendQuestion">
            <SendOutlined />
            发送
          </a-button>
        </div>
      </section>

      <section class="workbench-section list-section">
        <WorkbenchTitle title="待办事项" icon-color="#1677ff">
          <template #icon><CarryOutOutlined /></template>
        </WorkbenchTitle>
        <button class="more-button" type="button" @click="showMore('待办事项')">更多</button>
        <ul class="information-list">
          <li v-for="item in todoList" :key="item.title">
            <span class="status-dot" :class="`status-${item.level}`"></span>
            <button type="button" @click="openListItem(item.title)">{{ item.title }}</button>
            <time>{{ item.date }}</time>
          </li>
        </ul>
      </section>

      <section class="workbench-section list-section knowledge-section">
        <WorkbenchTitle title="最近知识库" icon-color="#1677ff">
          <template #icon><FileTextOutlined /></template>
        </WorkbenchTitle>
        <button class="more-button" type="button" @click="showMore('知识库')">更多</button>
        <ul class="information-list">
          <li v-for="item in knowledgeList" :key="item.title">
            <FileTextOutlined class="list-icon" />
            <button type="button" @click="openListItem(item.title)">{{ item.title }}</button>
            <time>{{ item.date }}</time>
          </li>
        </ul>
      </section>
    </div>

    <div class="bottom-grid">
      <section class="workbench-section overview-section">
        <WorkbenchTitle title="开放平台概况" icon-color="#1677ff">
          <template #icon><BarChartOutlined /></template>
        </WorkbenchTitle>
        <div class="stat-grid">
          <div v-for="stat in platformStats" :key="stat.label" class="stat-item">
            <span class="stat-icon" :style="{ color: stat.color, backgroundColor: stat.background }">
              <component :is="stat.icon" />
            </span>
            <span>
              <small>{{ stat.label }}</small>
              <strong :style="{ color: stat.valueColor || '#111827' }">{{ stat.value }}</strong>
            </span>
          </div>
        </div>
      </section>

      <section class="workbench-section list-section alert-section">
        <WorkbenchTitle title="异常提醒" icon-color="#ff4d4f">
          <template #icon><WarningFilled /></template>
        </WorkbenchTitle>
        <button class="more-button" type="button" @click="showMore('异常提醒')">更多</button>
        <ul class="information-list compact-list">
          <li v-for="item in alertList" :key="item.title">
            <span class="status-dot" :class="`status-${item.level}`"></span>
            <button type="button" @click="openListItem(item.title)">{{ item.title }}</button>
            <time>{{ item.date }}</time>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
  import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { Lunar, Solar } from 'lunar-javascript';
  import {
    ApiOutlined,
    AppstoreOutlined,
    BarChartOutlined,
    ClockCircleOutlined,
    CarryOutOutlined,
    CodeOutlined,
    DatabaseOutlined,
    FileTextOutlined,
    LinkOutlined,
    RightOutlined,
    RobotOutlined,
    SendOutlined,
    WarningFilled,
  } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import { useUserStore } from '/@/store/modules/system/user';
  import { alertList, assistantShortcutData, knowledgeList, todoList } from './home-mock';

  const WorkbenchTitle = defineComponent({
    props: {
      title: { type: String, required: true },
      iconColor: { type: String, default: '#1677ff' },
    },
    setup(props, { slots }) {
      return () =>
        h('div', { class: 'workbench-title' }, [
          h('span', { class: 'title-accent' }),
          h('span', { class: 'title-icon', style: { color: props.iconColor } }, slots.icon?.()),
          h('h2', props.title),
        ]);
    },
  });

  const iconMap = {
    LinkOutlined,
    CodeOutlined,
    DatabaseOutlined,
  };

  const router = useRouter();
  const userStore = useUserStore();
  const displayName = computed(() => userStore.actualName || '管理员');
  const searchKeyword = ref('');
  const assistantQuestion = ref('');
  const applicationLoading = ref(false);
  const applicationList = ref([]);
  const overview = reactive({
    listedApplications: 0,
    publishedApis: 0,
    mcpOnlineServices: 0,
    pendingReviews: 0,
  });

  const assistantShortcuts = assistantShortcutData.map((item) => ({ ...item, icon: iconMap[item.icon] }));
  const platformStats = computed(() => [
    {
      label: '已上架应用',
      value: overview.listedApplications,
      icon: AppstoreOutlined,
      color: '#1677ff',
      background: '#eaf4ff',
    },
    {
      label: '已发布 API',
      value: overview.publishedApis,
      icon: ApiOutlined,
      color: '#20b65b',
      background: '#eaf8ef',
    },
    {
      label: 'MCP 在线服务',
      value: overview.mcpOnlineServices,
      icon: DatabaseOutlined,
      color: '#7253df',
      background: '#f0edff',
    },
    {
      label: '待审核',
      value: overview.pendingReviews,
      icon: ClockCircleOutlined,
      color: '#fa8c16',
      background: '#fff4e8',
      valueColor: '#fa8c16',
    },
  ]);

  const applicationColors = ['#1677ff', '#20b65b', '#7253df', '#fa8c16', '#1ab8aa'];

  const greeting = computed(() => {
    const hour = new Date().getHours();
    if (hour < 6) return '夜深了';
    if (hour < 12) return '早上好';
    if (hour < 14) return '中午好';
    if (hour < 19) return '下午好';
    return '晚上好';
  });

  const dateInfo = computed(() => {
    const date = new Date();
    const solar = Solar.fromDate(date);
    const lunar = Lunar.fromDate(date);
    return {
      solar: solar.toYmd(),
      week: solar.getWeekInChinese(),
      lunarMonth: lunar.getMonthInChinese(),
      lunarDay: lunar.getDayInChinese(),
    };
  });

  function handleSearch(value) {
    const keyword = value.trim();
    message.success(keyword ? `正在搜索“${keyword}”` : '请输入搜索内容');
  }

  function applyShortcut(shortcut) {
    assistantQuestion.value = shortcut.question;
  }

  function sendQuestion() {
    if (!assistantQuestion.value.trim()) {
      message.warning('请先输入要咨询的问题');
      return;
    }
    message.success('问题已发送，智能助手正在处理中');
    assistantQuestion.value = '';
  }

  async function loadHomeData() {
    applicationLoading.value = true;
    const [applicationsResult, overviewResult] = await Promise.allSettled([
      applicationApi.queryMyApplications(),
      applicationApi.queryHomeOverview(),
    ]);
    if (applicationsResult.status === 'fulfilled') {
      applicationList.value = (applicationsResult.value.data.applications || []).slice(0, 5).map((item, index) => ({
        ...item,
        name: item.marketName || item.applicationName,
        description: item.subtitle || item.summary || '暂无应用简介',
        color: applicationColors[index % applicationColors.length],
      }));
    } else {
      smartSentry.captureError(applicationsResult.reason);
    }
    if (overviewResult.status === 'fulfilled') {
      Object.assign(overview, overviewResult.value.data);
    } else {
      smartSentry.captureError(overviewResult.reason);
    }
    applicationLoading.value = false;
  }

  async function openApp(app) {
    try {
      const response = await applicationApi.launch({ applicationId: app.applicationId });
      const target = response.data.openMode === 'CURRENT' ? '_self' : '_blank';
      window.open(response.data.launchUrl, target, target === '_blank' ? 'noopener,noreferrer' : undefined);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  function openListItem(title) {
    message.info(title);
  }

  function showMore(moduleName) {
    if (moduleName === '应用') {
      router.push('/application/my');
      return;
    }
    message.info(`查看更多${moduleName}`);
  }

  onMounted(loadHomeData);
</script>

<style lang="less" scoped>
  @import './index.less';
</style>
