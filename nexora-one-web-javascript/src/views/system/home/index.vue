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

      <div class="application-grid">
        <button v-for="app in applicationList" :key="app.name" class="application-item" type="button" @click="openApp(app)">
          <span class="application-icon" :style="{ backgroundColor: app.color }">
            <component :is="app.icon" />
          </span>
          <span class="application-copy">
            <strong>{{ app.name }}</strong>
            <small>{{ app.description }}</small>
          </span>
          <RightOutlined class="application-arrow" />
        </button>
      </div>
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
  import { computed, defineComponent, h, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { Lunar, Solar } from 'lunar-javascript';
  import {
    ApartmentOutlined,
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
    SettingOutlined,
    ShopOutlined,
    WarningFilled,
  } from '@ant-design/icons-vue';
  import { useUserStore } from '/@/store/modules/system/user';
  import { alertList, applicationData, assistantShortcutData, knowledgeList, platformStatData, todoList } from './home-mock';

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
    ApartmentOutlined,
    ShopOutlined,
    FileTextOutlined,
    BarChartOutlined,
    SettingOutlined,
    LinkOutlined,
    CodeOutlined,
    DatabaseOutlined,
    AppstoreOutlined,
    ApiOutlined,
    ClockCircleOutlined,
  };

  const userStore = useUserStore();
  const displayName = computed(() => userStore.actualName || '管理员');
  const searchKeyword = ref('');
  const assistantQuestion = ref('');

  const applicationList = applicationData.map((item) => ({ ...item, icon: iconMap[item.icon] }));
  const assistantShortcuts = assistantShortcutData.map((item) => ({ ...item, icon: iconMap[item.icon] }));
  const platformStats = platformStatData.map((item) => ({ ...item, icon: iconMap[item.icon] }));

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

  function openApp(app) {
    message.info(`正在打开${app.name}`);
  }

  function openListItem(title) {
    message.info(title);
  }

  function showMore(moduleName) {
    message.info(`查看更多${moduleName}`);
  }
</script>

<style lang="less" scoped>
  @import './index.less';
</style>
