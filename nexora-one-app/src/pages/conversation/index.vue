<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="themeVars">
    <view class="app-shell list-page" :class="{ 'theme-dark': themeStore.isDark }">
      <wd-navbar
        fixed
        placeholder
        safe-area-inset-top
        left-arrow
        title="我的会话"
        :bordered="false"
        :custom-style="navbarStyle"
        @click-left="goBack"
      />

      <view class="page-content">
        <wd-search
          v-model="keyword"
          hide-cancel
          light
          placeholder-left
          placeholder="搜索会话标题或助手"
        />

        <view class="list-summary">
          <text>{{ filteredConversations.length }} 条会话</text>
          <wd-tag v-if="filterAssistantName" type="primary" plain>
            {{ filterAssistantName }}
          </wd-tag>
        </view>

        <view v-if="loading" class="loading-area">
          <wd-loading color="#04bfe5" />
          <text>正在加载会话</text>
        </view>

        <view v-else-if="filteredConversations.length" class="conversation-list">
          <wd-card
            v-for="item in filteredConversations"
            :key="item.conversationId"
            type="rectangle"
            custom-class="conversation-card"
            @click="openConversation(item)"
          >
            <view class="conversation-row">
              <assistant-avatar :assistant="item.assistantView.assistant" :size="46" />
              <view class="conversation-main">
                <text class="conversation-title">{{ item.title || '未命名会话' }}</text>
                <view class="conversation-meta">
                  <text>{{ item.assistantView.assistant.assistantName }}</text>
                  <text>{{ formatTime(item.updateTime || item.createTime) }}</text>
                </view>
              </view>
              <wd-button
                type="icon"
                icon="delete"
                aria-label="删除会话"
                custom-class="delete-button"
                @click.stop="requestDelete(item)"
              />
            </view>
          </wd-card>
        </view>

        <view v-else class="empty-state">
          {{ keyword ? '没有找到匹配的会话' : '暂无会话记录，和助手聊一次后会显示在这里' }}
        </view>
      </view>
    </view>
  </wd-config-provider>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onLoad, onShow } from '@dcloudio/uni-app';
  import dayjs from 'dayjs';
  import AssistantAvatar from '@/components/assistant-avatar/assistant-avatar.vue';
  import {
    deleteConversation,
    getAssistantConversations,
  } from '@/api/business/knowledge-api';
  import { useAssistantStore } from '@/store/modules/business/assistant';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { nexoraSentry } from '@/lib/nexora-sentry';

  const assistantStore = useAssistantStore();
  const themeStore = useThemeStore();
  const keyword = ref('');
  const loading = ref(false);
  const conversations = ref([]);
  const filterAssistantId = ref('');

  const navbarStyle = computed(
    () => `background:${themeStore.isDark ? '#09131c' : '#f6f8fb'};`,
  );
  const themeVars = computed(() => ({
    colorTheme: '#04bfe5',
    cardRadius: '8px',
    cardBg: themeStore.isDark ? '#0f1b26' : '#ffffff',
    navbarBackground: themeStore.isDark ? '#09131c' : '#f6f8fb',
    navbarColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    searchInputBg: themeStore.isDark ? '#142330' : '#ffffff',
    searchInputColor: themeStore.isDark ? '#f6f8fb' : '#101828',
  }));
  const filterAssistantName = computed(() => {
    if (!filterAssistantId.value) return '';
    return (
      assistantStore.assistants.find(
        (item) =>
          String(item.assistant?.assistantId) === String(filterAssistantId.value),
      )?.assistant?.assistantName || ''
    );
  });
  const filteredConversations = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase();
    return conversations.value.filter((item) => {
      if (
        filterAssistantId.value &&
        String(item.assistantId) !== String(filterAssistantId.value)
      ) {
        return false;
      }
      if (!normalizedKeyword) return true;
      return (
        String(item.title || '')
          .toLowerCase()
          .includes(normalizedKeyword) ||
        String(item.assistantView?.assistant?.assistantName || '')
          .toLowerCase()
          .includes(normalizedKeyword)
      );
    });
  });

  /**
   * 读取所有可用助手的真实会话并合并排序。
   */
  async function loadConversations() {
    loading.value = true;
    try {
      const assistants = await assistantStore.loadAvailableAssistants();
      const targets = filterAssistantId.value
        ? assistants.filter(
            (item) =>
              String(item.assistant?.assistantId) === String(filterAssistantId.value),
          )
        : assistants;
      const groups = await Promise.all(
        targets.map(async (assistantView) => {
          const assistantId = assistantView.assistant?.assistantId;
          const items = await getAssistantConversations(assistantId).catch(() => []);
          return items.map((conversation) => ({
            ...conversation,
            assistantView,
          }));
        }),
      );
      conversations.value = groups
        .flat()
        .sort(
          (left, right) =>
            dayjs(right.updateTime || right.createTime).valueOf() -
            dayjs(left.updateTime || left.createTime).valueOf(),
        );
    } catch (error) {
      nexoraSentry.captureError(error);
      uni.showToast({ title: '会话加载失败，请稍后重试', icon: 'none' });
    } finally {
      loading.value = false;
    }
  }

  /**
   * 打开指定历史会话。
   *
   * @param {Object} item 会话记录
   */
  function openConversation(item) {
    uni.navigateTo({
      url:
        `/pages/assistant/chat?assistantId=${item.assistantId}` +
        `&conversationId=${item.conversationId}`,
    });
  }

  /**
   * 二次确认后删除会话。
   *
   * @param {Object} item 会话记录
   */
  function requestDelete(item) {
    uni.showModal({
      title: '删除会话',
      content: `确定删除“${item.title || '未命名会话'}”吗？`,
      success(result) {
        if (result.confirm) {
          removeConversation(item);
        }
      },
    });
  }

  /**
   * 调用真实接口删除会话并更新列表。
   *
   * @param {Object} item 会话记录
   */
  async function removeConversation(item) {
    try {
      await deleteConversation(item.assistantId, item.conversationId);
      conversations.value = conversations.value.filter(
        (conversation) => conversation.conversationId !== item.conversationId,
      );
      uni.showToast({ title: '会话已删除', icon: 'none' });
    } catch (error) {
      nexoraSentry.captureError(error);
      uni.showToast({ title: '会话删除失败', icon: 'none' });
    }
  }

  /**
   * 格式化会话时间。
   *
   * @param {string} value 时间值
   * @returns {string} 展示时间
   */
  function formatTime(value) {
    if (!value || !dayjs(value).isValid()) return '';
    const time = dayjs(value);
    return time.isSame(dayjs(), 'day') ? time.format('HH:mm') : time.format('MM-DD HH:mm');
  }

  /**
   * 返回上一页或助手首页。
   */
  function goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      uni.navigateBack();
      return;
    }
    uni.reLaunch({ url: '/pages/home/index' });
  }

  onLoad((options) => {
    filterAssistantId.value = options.assistantId || '';
  });
  onShow(loadConversations);
</script>

<style lang="scss" scoped>
  @import '@/styles/assistant-theme.scss';

  .list-summary,
  .conversation-row,
  .conversation-meta {
    display: flex;
    align-items: center;
  }

  .list-summary {
    justify-content: space-between;
    min-height: 48px;
    color: var(--app-text-secondary);
    font-size: 13px;
  }

  .loading-area {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 10px;
    padding: 70px 0;
    color: var(--app-text-secondary);
    font-size: 14px;
  }

  .conversation-list {
    padding-bottom: 18px;
  }

  :deep(.conversation-card) {
    margin: 0 0 12px;
  }

  :deep(.conversation-card .wd-card__content) {
    padding: 16px;
  }

  .conversation-row {
    gap: 13px;
  }

  .conversation-main {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 8px;
  }

  .conversation-title {
    overflow: hidden;
    color: var(--app-text);
    font-size: 16px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .conversation-meta {
    justify-content: space-between;
    gap: 10px;
    color: var(--app-text-secondary);
    font-size: 12px;
  }

  .conversation-meta text:first-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.delete-button) {
    flex: none;
    color: var(--app-text-secondary);
  }
</style>
