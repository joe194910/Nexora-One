<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="themeVars">
    <view class="app-shell favorite-page" :class="{ 'theme-dark': themeStore.isDark }">
      <wd-navbar
        fixed
        placeholder
        safe-area-inset-top
        left-arrow
        title="我的收藏"
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
          placeholder="搜索已收藏助手"
        />

        <view class="section-heading">
          <text class="section-title">收藏助手</text>
          <wd-tag v-if="filteredFavorites.length" type="primary" plain>
            {{ filteredFavorites.length }}
          </wd-tag>
        </view>

        <view v-if="loading" class="loading-area">
          <wd-loading color="#04bfe5" />
          <text>正在加载收藏</text>
        </view>

        <view v-else-if="filteredFavorites.length" class="favorite-list">
          <wd-card
            v-for="item in filteredFavorites"
            :key="item.assistant.assistantId"
            type="rectangle"
            custom-class="favorite-card"
            @click="openAssistant(item)"
          >
            <view class="favorite-row">
              <assistant-avatar :assistant="item.assistant" :size="50" />
              <view class="favorite-main">
                <view class="favorite-name-line">
                  <text class="favorite-name">{{ item.assistant.assistantName }}</text>
                  <wd-tag v-if="item.tools?.length" type="warning" plain>工具</wd-tag>
                </view>
                <text class="favorite-description">{{ assistantDescription(item) }}</text>
                <text class="favorite-bases">
                  {{ item.baseNames?.length ? item.baseNames.join('、') : '未关联知识库' }}
                </text>
              </view>
              <wd-button
                type="icon"
                icon="star"
                aria-label="取消收藏"
                custom-class="favorite-button"
                @click.stop="removeFavorite(item)"
              />
            </view>
          </wd-card>
        </view>

        <view v-else class="empty-state">
          {{ keyword ? '没有找到匹配的收藏助手' : '还没有收藏助手，可以从助手首页进入并收藏' }}
        </view>
      </view>
    </view>
  </wd-config-provider>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import AssistantAvatar from '@/components/assistant-avatar/assistant-avatar.vue';
  import { useAssistantStore } from '@/store/modules/business/assistant';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { smartSentry } from '@/lib/smart-sentry';

  const assistantStore = useAssistantStore();
  const themeStore = useThemeStore();
  const keyword = ref('');
  const loading = ref(false);

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
  const filteredFavorites = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase();
    return assistantStore.favoriteAssistants.filter((item) => {
      if (!normalizedKeyword) return true;
      return (
        String(item.assistant?.assistantName || '')
          .toLowerCase()
          .includes(normalizedKeyword) ||
        String(item.assistant?.systemPrompt || '')
          .toLowerCase()
          .includes(normalizedKeyword) ||
        (item.baseNames || []).some((name) =>
          String(name).toLowerCase().includes(normalizedKeyword),
        )
      );
    });
  });

  /**
   * 从助手市场加载当前用户收藏状态。
   */
  async function loadFavorites() {
    loading.value = true;
    try {
      await assistantStore.loadAssistantStore();
    } catch (error) {
      smartSentry.captureError(error);
      uni.showToast({ title: '收藏加载失败，请稍后重试', icon: 'none' });
    } finally {
      loading.value = false;
    }
  }

  /**
   * 进入收藏助手的聊天页面。
   *
   * @param {Object} item 助手视图
   */
  function openAssistant(item) {
    const assistantId = item.assistant?.assistantId;
    if (!assistantId) return;
    assistantStore.markRecent(assistantId);
    uni.navigateTo({ url: `/pages/assistant/chat?assistantId=${assistantId}` });
  }

  /**
   * 取消收藏并立即刷新列表状态。
   *
   * @param {Object} item 助手视图
   */
  async function removeFavorite(item) {
    try {
      await assistantStore.toggleFavorite(item);
      uni.showToast({ title: '已取消收藏', icon: 'none' });
    } catch (error) {
      smartSentry.captureError(error);
      uni.showToast({ title: '取消收藏失败', icon: 'none' });
    }
  }

  /**
   * 生成助手摘要。
   *
   * @param {Object} item 助手视图
   * @returns {string} 助手摘要
   */
  function assistantDescription(item) {
    const prompt = String(item.assistant?.systemPrompt || '').replace(/\s+/g, ' ').trim();
    if (prompt) return prompt.length > 46 ? `${prompt.slice(0, 46)}...` : prompt;
    return item.baseNames?.length
      ? `基于 ${item.baseNames.join('、')} 提供专业问答`
      : '使用企业知识与工具提供智能问答';
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

  onShow(loadFavorites);
</script>

<style lang="scss" scoped>
  @import '@/styles/assistant-theme.scss';

  .section-heading,
  .favorite-row,
  .favorite-name-line {
    display: flex;
    align-items: center;
  }

  .section-heading {
    justify-content: space-between;
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

  :deep(.favorite-card) {
    margin: 0 0 12px;
  }

  :deep(.favorite-card .wd-card__content) {
    padding: 16px;
  }

  .favorite-row {
    gap: 13px;
  }

  .favorite-main {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 7px;
  }

  .favorite-name-line {
    gap: 7px;
  }

  .favorite-name {
    overflow: hidden;
    color: var(--app-text);
    font-size: 17px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .favorite-description,
  .favorite-bases {
    overflow: hidden;
    color: var(--app-text-secondary);
    font-size: 12px;
    line-height: 1.45;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.favorite-button) {
    flex: none;
    color: #f5a623;
  }
</style>
