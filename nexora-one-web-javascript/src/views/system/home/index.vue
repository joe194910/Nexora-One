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
        <button class="more-button assistant-more-button" type="button" @click="showMore('助手')">
          全部助手
          <RightOutlined />
        </button>

        <a-spin :spinning="knowledgeLoading">
          <div v-if="assistantList.length" class="assistant-workspace">
            <nav class="assistant-selector" aria-label="智能助手列表">
              <button
                v-for="item in assistantList"
                :key="item.assistantId"
                type="button"
                :class="{ 'assistant-selector__item--active': selectedAssistantId === item.assistantId }"
                :disabled="!item.enabledFlag"
                @click="selectAssistant(item)"
              >
                <span class="assistant-selector__icon" :class="`assistant-selector__icon--${item.tone}`">
                  <component :is="item.icon" />
                </span>
                <span>{{ item.assistantName }}</span>
              </button>
            </nav>

            <div v-if="selectedAssistant" class="assistant-console">
              <div class="assistant-intro">
                <span class="assistant-avatar"><RobotOutlined /></span>
                <div>
                  <h3>{{ greeting }}，我是{{ selectedAssistant.assistantName }}</h3>
                  <p>
                    我可以基于{{ selectedAssistant.baseNames.length ? selectedAssistant.baseNames.join('、') : '关联知识库' }}回答你的问题
                  </p>
                </div>
              </div>

              <div class="assistant-shortcuts">
                <button
                  v-for="shortcut in assistantShortcuts"
                  :key="shortcut.label"
                  type="button"
                  @click="applyShortcut(shortcut)"
                >
                  {{ shortcut.label }}
                </button>
              </div>

              <div class="assistant-composer">
                <a-input
                  v-model:value="assistantQuestion"
                  :maxlength="500"
                  placeholder="请输入问题..."
                  @keydown="handleAssistantKeydown"
                />
                <a-button type="primary" class="send-button" :disabled="!assistantQuestion.trim()" @click="sendQuestion">
                  <SendOutlined />
                  发送
                </a-button>
              </div>
            </div>
          </div>
          <a-empty v-else class="assistant-empty" description="暂无可用智能助手" />
        </a-spin>
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
        <WorkbenchTitle title="我的知识库" icon-color="#1677ff">
          <template #icon><FileTextOutlined /></template>
        </WorkbenchTitle>
        <button class="more-button" type="button" @click="showMore('知识库')">更多</button>
        <a-spin :spinning="knowledgeLoading">
          <ul v-if="knowledgeBasePreviewList.length" class="information-list knowledge-base-list">
            <li v-for="item in knowledgeBasePreviewList" :key="item.baseId">
              <FileTextOutlined class="list-icon" />
              <button type="button" :disabled="!item.assistantId" @click="openKnowledgeBase(item)">
                {{ item.baseName }}
              </button>
              <span class="knowledge-base-meta">{{ item.documentCount }} 个文档</span>
              <RightOutlined v-if="item.assistantId" class="knowledge-base-arrow" />
              <a-tooltip v-else title="请先在智能助手列表中关联并启用一个助手">
                <span class="knowledge-base-unavailable">未关联助手</span>
              </a-tooltip>
            </li>
          </ul>
          <a-empty v-else class="knowledge-empty" description="暂无知识库" />
        </a-spin>
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
    CustomerServiceOutlined,
    DatabaseOutlined,
    FileTextOutlined,
    ReadOutlined,
    RightOutlined,
    RobotOutlined,
    SendOutlined,
    WarningFilled,
  } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { knowledgeApi } from '/@/api/business/knowledge/knowledge-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import { useUserStore } from '/@/store/modules/system/user';
  import { alertList, todoList } from './home-mock';

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

  const router = useRouter();
  const userStore = useUserStore();
  const displayName = computed(() => userStore.actualName || '管理员');
  const searchKeyword = ref('');
  const assistantQuestion = ref('');
  const selectedAssistantId = ref();
  const applicationLoading = ref(false);
  const knowledgeLoading = ref(false);
  const applicationList = ref([]);
  const knowledgeBases = ref([]);
  const knowledgeAssistants = ref([]);
  const overview = reactive({
    listedApplications: 0,
    publishedApis: 0,
    mcpOnlineServices: 0,
    pendingReviews: 0,
  });

  const assistantShortcuts = [
    { label: '介绍知识范围', question: '请介绍一下你可以基于知识库回答哪些问题？' },
    { label: '总结核心内容', question: '请总结关联知识库中的核心内容。' },
    { label: '列出常见问题', question: '请列出知识库中最常见的问题和答案。' },
  ];
  const assistantIcons = [RobotOutlined, ReadOutlined, CustomerServiceOutlined];
  const assistantTones = ['blue', 'green', 'orange'];
  const assistantList = computed(() =>
    knowledgeAssistants.value
      .filter((row) => row.assistant.enabledFlag)
      .map((row, index) => ({
        ...row.assistant,
        baseIds: row.baseIds || [],
        baseNames:
          row.baseNames ||
          (row.baseIds || [])
            .map((id) => knowledgeBases.value.find((baseRow) => baseRow.base.baseId === id)?.base.baseName)
            .filter(Boolean),
        icon: assistantIcons[index % assistantIcons.length],
        tone: assistantTones[index % assistantTones.length],
      }))
      .slice(0, 3)
  );
  const selectedAssistant = computed(
    () => assistantList.value.find((item) => item.assistantId === selectedAssistantId.value) || null
  );
  const knowledgeBaseList = computed(() =>
    knowledgeBases.value
      .filter((row) => row.base.enabledFlag)
      .map((row) => {
        const assistant = row.assistants?.[0];
        const assistantRow = assistant
          ? null
          : knowledgeAssistants.value.find(
              (item) => item.assistant.enabledFlag && (item.baseIds || []).includes(row.base.baseId)
            );
        return {
          baseId: row.base.baseId,
          baseName: row.base.baseName,
          documentCount: row.documentIds?.length || 0,
          assistantId: assistant?.assistantId || assistantRow?.assistant.assistantId,
          assistantName: assistant?.assistantName || assistantRow?.assistant.assistantName,
        };
      })
  );
  const knowledgeBasePreviewList = computed(() => knowledgeBaseList.value.slice(0, 7));
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

  function selectAssistant(item) {
    if (item.enabledFlag) selectedAssistantId.value = item.assistantId;
  }

  function handleAssistantKeydown(event) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return;
    event.preventDefault();
    sendQuestion();
  }

  async function sendQuestion() {
    if (!assistantQuestion.value.trim()) {
      message.warning('请先输入要咨询的问题');
      return;
    }
    if (!selectedAssistant.value?.assistantId) {
      message.warning('请选择可用的智能助手');
      return;
    }
    const question = assistantQuestion.value.trim();
    await openAssistant(selectedAssistant.value.assistantId, question);
    assistantQuestion.value = '';
  }

  async function loadHomeData() {
    applicationLoading.value = true;
    knowledgeLoading.value = true;
    const [applicationsResult, overviewResult, knowledgeResult, assistantsResult] = await Promise.allSettled([
      applicationApi.queryMyApplications(),
      applicationApi.queryHomeOverview(),
      knowledgeApi.availableBases(),
      knowledgeApi.availableAssistants(),
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
    if (knowledgeResult.status === 'fulfilled') {
      knowledgeBases.value = knowledgeResult.value.data || [];
    } else {
      smartSentry.captureError(knowledgeResult.reason);
    }
    if (assistantsResult.status === 'fulfilled') {
      knowledgeAssistants.value = assistantsResult.value.data || [];
    } else {
      smartSentry.captureError(assistantsResult.reason);
    }
    selectedAssistantId.value =
      assistantList.value.find((item) => item.enabledFlag)?.assistantId || assistantList.value[0]?.assistantId;
    applicationLoading.value = false;
    knowledgeLoading.value = false;
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

  function openKnowledgeBase(item, question) {
    if (!item.assistantId) {
      message.warning('该知识库尚未关联可用的智能助手');
      return Promise.resolve();
    }
    return router.push({
      path: '/knowledge/assistants',
      query: {
        assistantId: String(item.assistantId),
        ...(question ? { question } : {}),
      },
    });
  }

  function openAssistant(assistantId, question) {
    return router.push({
      path: '/knowledge/assistants',
      query: {
        assistantId: String(assistantId),
        ...(question ? { question } : {}),
      },
    });
  }

  function showMore(moduleName) {
    if (moduleName === '应用') {
      router.push('/application/my');
      return;
    }
    if (moduleName === '知识库') {
      router.push('/knowledge/bases');
      return;
    }
    if (moduleName === '助手') {
      router.push('/knowledge/assistants');
      return;
    }
    message.info(`查看更多${moduleName}`);
  }

  onMounted(loadHomeData);
</script>

<style lang="less" scoped>
  @import './index.less';
</style>
