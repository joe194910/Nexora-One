<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="themeVars">
    <view class="app-shell home-page" :class="{ 'theme-dark': themeStore.isDark }">
      <wd-navbar
        fixed
        placeholder
        safe-area-inset-top
        :bordered="false"
        :custom-style="navbarStyle"
      >
        <template #left>
          <view class="brand">
            <image
              class="brand-logo"
              src="/static/images/login/nexora-one-logo.png"
              mode="aspectFit"
            />
            <text>NexoraOne</text>
          </view>
        </template>
        <template #right>
          <view class="navbar-actions">
            <wd-button
              type="icon"
              icon="computer"
              custom-class="navbar-icon-button"
              aria-label="切换主题"
              @click.stop="themeStore.toggleTheme"
            />
            <wd-button
              type="icon"
              icon="notification"
              custom-class="navbar-icon-button"
              aria-label="消息通知"
              @click.stop="openMessageSettings"
            />
          </view>
        </template>
      </wd-navbar>

      <view class="page-content">
        <view class="welcome">
          <text class="welcome-title">{{ greeting }}，{{ displayName }}</text>
          <text class="welcome-subtitle">让专业知识，成为每个人的生产力。</text>
        </view>

        <view class="search-panel">
          <wd-search
            v-model="keyword"
            hide-cancel
            light
            placeholder-left
            custom-class="assistant-search"
            placeholder="搜索智能助手"
            @search="applySearch"
            @clear="applySearch"
          />
          <view class="search-divider" />
          <wd-button
            type="icon"
            icon="filter"
            custom-class="filter-button"
            aria-label="筛选助手"
            @click="showFilterActions = true"
          />
        </view>

        <view v-if="recentDisplay.length" class="section-block">
          <view class="section-heading">
            <text class="section-title">{{ recentHeading }}</text>
            <view class="section-link" @click="showAllAssistants">
              <text>查看全部</text>
              <wd-icon name="arrow-right" size="17px" />
            </view>
          </view>

          <view class="recent-grid">
            <wd-card
              v-for="item in recentDisplay"
              :key="item.assistant.assistantId"
              type="rectangle"
              custom-class="recent-card"
              @click="openAssistant(item)"
            >
              <view class="recent-card-content">
                <assistant-avatar :assistant="item.assistant" :size="42" />
                <view class="recent-copy">
                  <text class="recent-name">{{ item.assistant.assistantName }}</text>
                  <text class="recent-time">
                    {{ recentAssistants.length ? '最近使用' : '推荐助手' }}
                  </text>
                </view>
                <wd-icon name="arrow-right" size="18px" class="row-arrow" />
              </view>
            </wd-card>
          </view>
        </view>

        <view id="assistant-directory" class="section-heading assistant-heading">
          <text class="section-title">所有助手</text>
        </view>

        <wd-segmented
          v-model:value="filterType"
          :options="filterOptions"
          size="small"
          custom-class="assistant-segmented"
        >
          <template #label="{ option }">
            {{ option.payload.label }}
          </template>
        </wd-segmented>

        <view v-if="assistantStore.loading" class="loading-area">
          <wd-loading color="#04bfe5" />
          <text>正在加载助手</text>
        </view>

        <view v-else-if="filteredAssistants.length" class="assistant-list">
          <view
            v-for="item in filteredAssistants"
            :key="item.assistant.assistantId"
            class="assistant-row"
            @click="openAssistant(item)"
          >
            <assistant-avatar :assistant="item.assistant" :size="52" />
            <view class="assistant-main">
              <view class="assistant-name-line">
                <text class="assistant-name">{{ item.assistant.assistantName }}</text>
              </view>
              <text class="assistant-description">{{ assistantDescription(item) }}</text>
              <view class="assistant-tags">
                <wd-tag type="primary">知识库</wd-tag>
                <wd-tag v-if="item.tools?.length" type="warning">工具</wd-tag>
                <wd-tag v-else-if="item.baseNames?.[0]" type="default" plain>
                  {{ compactBaseName(item.baseNames[0]) }}
                </wd-tag>
              </view>
            </view>
            <wd-icon name="arrow-right" size="20px" class="row-arrow" />
          </view>
        </view>

        <view v-else class="empty-state">
          {{ keyword ? '没有找到匹配的助手' : '暂无可用助手，请先在知识库中发布或收藏助手' }}
        </view>
      </view>

      <app-tabbar model-value="assistant" />

      <wd-action-sheet
        v-model="showFilterActions"
        title="筛选助手"
        cancel-text="取消"
        :actions="filterActions"
        @select="handleFilterSelect"
      />
    </view>
  </wd-config-provider>
</template>

<script setup>
  import { computed, nextTick, ref } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import AppTabbar from '@/components/app-tabbar/app-tabbar.vue';
  import AssistantAvatar from '@/components/assistant-avatar/assistant-avatar.vue';
  import { useAssistantStore } from '@/store/modules/business/assistant';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { useUserStore } from '@/store/modules/system/user';

  const assistantStore = useAssistantStore();
  const themeStore = useThemeStore();
  const userStore = useUserStore();
  const keyword = ref('');
  const filterType = ref('all');
  const showFilterActions = ref(false);
  const filterOptions = [
    { value: 'all', payload: { label: '全部' } },
    { value: 'knowledge', payload: { label: '知识库' } },
    { value: 'tools', payload: { label: '工具' } },
  ];
  const filterActions = [
    { name: '全部助手', value: 'all' },
    { name: '知识库助手', value: 'knowledge' },
    { name: '工具助手', value: 'tools' },
  ];

  const displayName = computed(() => userStore.actualName || userStore.loginName || '用户');
  const greeting = computed(() => {
    const hour = new Date().getHours();
    if (hour < 6) return '夜深了';
    if (hour < 12) return '上午好';
    if (hour < 18) return '下午好';
    return '晚上好';
  });
  const navbarStyle = computed(
    () => `background:${themeStore.isDark ? '#08131c' : '#f7f9fc'};`,
  );
  const themeVars = computed(() => ({
    colorTheme: '#04bfe5',
    cardRadius: '8px',
    cardBg: themeStore.isDark ? '#0f1b26' : '#ffffff',
    cardTitleColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    cardContentColor: themeStore.isDark ? '#9cabc0' : '#667085',
    navbarBackground: themeStore.isDark ? '#08131c' : '#f7f9fc',
    navbarColor: themeStore.isDark ? '#f6f8fb' : '#0b1220',
    searchInputBg: themeStore.isDark ? '#142330' : '#ffffff',
    searchInputColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    searchPlaceholderColor: themeStore.isDark ? '#8290a5' : '#8a94a6',
    segmentedItemBgColor: themeStore.isDark ? '#142330' : '#eef3f8',
    segmentedItemColor: themeStore.isDark ? '#a8b5c6' : '#596579',
    segmentedItemAcitveBg: themeStore.isDark ? '#234253' : '#ffffff',
  }));

  const recentAssistants = computed(() => assistantStore.recentAssistants.slice(0, 2));
  const recentDisplay = computed(() => {
    if (recentAssistants.value.length) {
      return recentAssistants.value;
    }
    return assistantStore.assistants.slice(0, 2);
  });
  const recentHeading = computed(() =>
    recentAssistants.value.length ? '最近使用' : '推荐助手',
  );
  const filteredAssistants = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase();
    return assistantStore.assistants.filter((item) => {
      const assistant = item.assistant || {};
      const matchesKeyword =
        !normalizedKeyword ||
        String(assistant.assistantName || '')
          .toLowerCase()
          .includes(normalizedKeyword) ||
        String(assistant.systemPrompt || '')
          .toLowerCase()
          .includes(normalizedKeyword) ||
        (item.baseNames || []).some((name) =>
          String(name).toLowerCase().includes(normalizedKeyword),
        );
      const matchesType =
        filterType.value === 'all' ||
        (filterType.value === 'knowledge' && (item.baseNames || []).length > 0) ||
        (filterType.value === 'tools' && (item.tools || []).length > 0);
      return matchesKeyword && matchesType;
    });
  });

  /**
   * 读取可用助手并恢复最近使用记录。
   */
  async function loadAssistants() {
    assistantStore.initializeRecent();
    try {
      await assistantStore.loadAvailableAssistants();
    } catch (error) {
      uni.showToast({ title: '助手加载失败，请稍后重试', icon: 'none' });
    }
  }

  /**
   * 生成助手列表描述。
   *
   * @param {Object} item 助手视图
   * @returns {string} 助手描述
   */
  function assistantDescription(item) {
    const prompt = String(item.assistant?.systemPrompt || '').replace(/\s+/g, ' ').trim();
    if (prompt) {
      return prompt.length > 42 ? `${prompt.slice(0, 42)}...` : prompt;
    }
    if (item.baseNames?.length) {
      return `基于 ${item.baseNames.join('、')} 提供专业问答`;
    }
    return '使用企业知识与工具提供智能问答';
  }

  /**
   * 压缩知识库名称，避免标签挤压助手主信息。
   *
   * @param {string} baseName 知识库名称
   * @returns {string} 适合标签展示的名称
   */
  function compactBaseName(baseName) {
    const value = String(baseName || '');
    return value.length > 8 ? `${value.slice(0, 8)}...` : value;
  }

  /**
   * 执行本地助手搜索。
   */
  function applySearch() {
    keyword.value = keyword.value.trim();
  }

  /**
   * 进入助手会话页面。
   *
   * @param {Object} item 助手视图
   */
  function openAssistant(item) {
    const assistantId = item.assistant?.assistantId;
    if (!assistantId) return;
    assistantStore.markRecent(assistantId);
    uni.navigateTo({
      url: `/pages/assistant/chat?assistantId=${assistantId}`,
    });
  }

  /**
   * 打开消息设置入口。
   */
  function openMessageSettings() {
    uni.navigateTo({ url: '/pages/settings/detail?type=notification' });
  }

  /**
   * 通过底部选择器切换助手分类。
   *
   * @param {Object} event Wot Design Uni 选择事件
   */
  function handleFilterSelect(event) {
    filterType.value = event.item?.value || 'all';
    showFilterActions.value = false;
    scrollToAssistantDirectory();
  }

  /**
   * 滚动到助手目录区域，不改变用户当前选择的筛选条件。
   */
  async function scrollToAssistantDirectory() {
    await nextTick();
    uni.pageScrollTo({
      selector: '#assistant-directory',
      duration: 240,
    });
  }

  /**
   * 回到完整助手目录并滚动到列表区域。
   */
  async function showAllAssistants() {
    filterType.value = 'all';
    await scrollToAssistantDirectory();
  }

  onShow(loadAssistants);
</script>

<style lang="scss" scoped>
  @import '@/styles/assistant-theme.scss';

  .home-page {
    padding-bottom: 6px;
  }

  .brand,
  .navbar-actions,
  .recent-card-content,
  .assistant-name-line,
  .assistant-tags,
  .section-heading {
    display: flex;
    align-items: center;
  }

  .brand {
    gap: 10px;
    color: var(--app-text);
    font-size: 22px;
    font-weight: 800;
  }

  .brand-logo {
    width: 37px;
    height: 37px;
  }

  .navbar-actions {
    gap: 7px;
  }

  :deep(.navbar-icon-button) {
    width: 40px;
    min-width: 40px;
    height: 40px;
    color: var(--app-text);
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    box-shadow: var(--app-shadow);
  }

  :deep(.wd-navbar__left) {
    max-width: 210px;
    padding-left: 20px;
  }

  :deep(.wd-navbar__right) {
    padding-right: 16px;
  }

  .welcome {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 34px 2px 24px;
  }

  .welcome-title {
    color: var(--app-text);
    font-size: 31px;
    font-weight: 800;
    line-height: 1.22;
  }

  .welcome-subtitle {
    color: var(--app-text-secondary);
    font-size: 16px;
    line-height: 1.5;
  }

  .search-panel {
    display: flex;
    align-items: center;
    height: 58px;
    padding: 0 9px 0 0;
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    box-shadow: var(--app-shadow);
  }

  :deep(.assistant-search) {
    min-width: 0;
    flex: 1;
    padding: 0;
    background: transparent;
  }

  :deep(.assistant-search .wd-search__block) {
    height: 56px;
    background: transparent;
    border-radius: 8px;
  }

  .search-divider {
    width: 1px;
    height: 26px;
    background: var(--app-border);
  }

  :deep(.filter-button) {
    width: 43px;
    min-width: 43px;
    height: 43px;
    color: var(--app-text-secondary);
  }

  .section-block {
    margin-top: 9px;
  }

  .section-heading {
    justify-content: space-between;
  }

  .section-title {
    margin-bottom: 12px;
  }

  .section-link {
    display: flex;
    align-items: center;
    gap: 2px;
    color: var(--app-text-secondary);
    font-size: 14px;
  }

  .recent-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  :deep(.recent-card) {
    margin: 0;
  }

  :deep(.recent-card .wd-card__content) {
    padding: 15px 13px;
  }

  .recent-card-content {
    min-width: 0;
    gap: 10px;
  }

  .recent-copy {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 5px;
  }

  .recent-name {
    overflow: hidden;
    color: var(--app-text);
    font-size: 15px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .recent-time,
  .assistant-description {
    color: var(--app-text-secondary);
    font-size: 13px;
  }

  .assistant-heading {
    margin-top: 7px;
  }

  :deep(.assistant-segmented) {
    width: 270px;
    margin-bottom: 9px;
  }

  .assistant-list {
    margin-top: 2px;
  }

  .assistant-row {
    display: flex;
    align-items: center;
    gap: 14px;
    min-height: 104px;
    padding: 16px 0;
    border-bottom: 1px solid var(--app-border);
  }

  .assistant-main {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 7px;
  }

  .assistant-name-line,
  .assistant-tags {
    gap: 7px;
  }

  .assistant-name {
    overflow: hidden;
    color: var(--app-text);
    font-size: 18px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .assistant-description {
    overflow: hidden;
    line-height: 1.45;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .row-arrow {
    flex: none;
    color: var(--app-text-secondary);
  }

  .loading-area {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 56px 0;
    color: var(--app-text-secondary);
    font-size: 14px;
  }

  @media (max-width: 350px) {
    .recent-grid {
      grid-template-columns: 1fr;
    }

    .welcome-title {
      font-size: 28px;
    }
  }
</style>
