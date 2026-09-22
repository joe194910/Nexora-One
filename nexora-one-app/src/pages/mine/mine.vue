<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="themeVars">
    <view class="app-shell mine-page" :class="{ 'theme-dark': themeStore.isDark }">
      <wd-navbar
        fixed
        placeholder
        safe-area-inset-top
        title="我的"
        :bordered="false"
        :custom-style="navbarStyle"
      >
        <template #right>
          <view class="navbar-actions">
            <wd-button
              type="icon"
              icon="computer"
              aria-label="切换主题"
              @click.stop="themeStore.toggleTheme"
            />
            <wd-button
              type="icon"
              icon="setting"
              aria-label="设置"
              @click.stop="openSettings('profile')"
            />
          </view>
        </template>
      </wd-navbar>

      <view class="page-content">
        <view class="profile">
          <wd-avatar
            :src="userStore.avatar"
            :text="avatarText"
            :size="72"
            bg-color="#dff7fd"
            color="#04aaca"
          />
          <view class="profile-copy">
            <text class="profile-name">{{ displayName }}</text>
            <text class="profile-account">{{ userStore.loginName || '未设置登录名' }}</text>
            <text class="profile-department">
              NexoraOne · {{ userStore.departmentName || '平台用户' }}
            </text>
          </view>
          <wd-icon name="arrow-right" size="20px" class="profile-arrow" />
        </view>

        <view class="stat-grid">
          <view class="stat-item" @click="openHome">
            <text class="stat-value">{{ assistantCount }}</text>
            <text class="stat-label">我的助手</text>
          </view>
          <view class="stat-item" @click="openConversations">
            <text class="stat-value">{{ conversationCount }}</text>
            <text class="stat-label">会话记录</text>
          </view>
          <view class="stat-item" @click="openFavorites">
            <text class="stat-value">{{ favoriteCount }}</text>
            <text class="stat-label">我的收藏</text>
          </view>
        </view>

        <wd-cell-group border custom-class="mine-menu">
          <wd-cell
            title="我的会话"
            label="查看与助手的历史对话"
            icon="chat"
            is-link
            size="large"
            @click="openConversations"
          />
          <wd-cell
            title="我的收藏"
            label="查看已收藏的智能助手"
            icon="star"
            is-link
            size="large"
            @click="openFavorites"
          />
          <wd-cell
            title="个人资料"
            label="查看当前账户信息"
            icon="user"
            is-link
            size="large"
            @click="openSettings('profile')"
          />
          <wd-cell
            title="账号与安全"
            label="查看登录与安全信息"
            icon="secured"
            is-link
            size="large"
            @click="openSettings('security')"
          />
          <wd-cell
            title="消息通知"
            label="当前未读消息与通知入口"
            icon="notification"
            :value="userStore.unreadMessageCount || ''"
            is-link
            size="large"
            @click="openSettings('notification')"
          />
          <wd-cell
            title="使用帮助"
            label="了解智能助手的使用方式"
            icon="help-circle"
            is-link
            size="large"
            @click="openSettings('help')"
          />
          <wd-cell
            title="关于 NexoraOne"
            label="版本信息与产品说明"
            icon="info-circle"
            is-link
            size="large"
            @click="openSettings('about')"
          />
        </wd-cell-group>

        <wd-button block plain type="info" icon="logout" custom-class="logout-button" @click="logout">
          退出登录
        </wd-button>
      </view>

      <app-tabbar model-value="mine" />
    </view>
  </wd-config-provider>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import AppTabbar from '@/components/app-tabbar/app-tabbar.vue';
  import {
    getAssistantConversations,
  } from '@/api/business/knowledge-api';
  import { useAssistantStore } from '@/store/modules/business/assistant';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { useUserStore } from '@/store/modules/system/user';

  const assistantStore = useAssistantStore();
  const themeStore = useThemeStore();
  const userStore = useUserStore();
  const conversationCount = ref(0);

  const displayName = computed(() => userStore.actualName || userStore.loginName || '用户');
  const avatarText = computed(() => displayName.value.slice(0, 1));
  const assistantCount = computed(() => assistantStore.assistants.length);
  const favoriteCount = computed(() => assistantStore.favoriteAssistants.length);
  const navbarStyle = computed(
    () => `background:${themeStore.isDark ? '#09131c' : '#f6f8fb'};`,
  );
  const themeVars = computed(() => ({
    colorTheme: '#04bfe5',
    navbarBackground: themeStore.isDark ? '#09131c' : '#f6f8fb',
    navbarColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    cellTitleColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    cellLabelColor: themeStore.isDark ? '#9cabc0' : '#667085',
    cellArrowColor: themeStore.isDark ? '#9cabc0' : '#667085',
  }));

  /**
   * 加载我的页面真实统计数据。
   */
  async function loadSummary() {
    try {
      const assistants = await assistantStore.loadAvailableAssistants();
      await assistantStore.loadAssistantStore();
      const conversationGroups = await Promise.all(
        assistants.map((item) =>
          getAssistantConversations(item.assistant.assistantId).catch(() => []),
        ),
      );
      conversationCount.value = conversationGroups.reduce(
        (total, conversations) => total + conversations.length,
        0,
      );
    } catch (error) {
      uni.showToast({ title: '账户数据加载失败', icon: 'none' });
    }
  }

  /**
   * 回到助手首页。
   */
  function openHome() {
    uni.reLaunch({ url: '/pages/home/index' });
  }

  /**
   * 打开会话记录。
   */
  function openConversations() {
    uni.navigateTo({ url: '/pages/conversation/index' });
  }

  /**
   * 打开收藏助手。
   */
  function openFavorites() {
    uni.navigateTo({ url: '/pages/favorite/index' });
  }

  /**
   * 打开设置详情。
   *
   * @param {string} type 设置类型
   */
  function openSettings(type) {
    uni.navigateTo({ url: `/pages/settings/detail?type=${type}` });
  }

  /**
   * 退出当前账户。
   */
  function logout() {
    uni.showModal({
      title: '退出登录',
      content: '确定要退出当前账户吗？',
      success(result) {
        if (!result.confirm) return;
        userStore.logout();
        uni.reLaunch({ url: '/pages/login/login' });
      },
    });
  }

  onShow(loadSummary);
</script>

<style lang="scss" scoped>
  @import '@/styles/assistant-theme.scss';

  .navbar-actions,
  .profile,
  .stat-grid {
    display: flex;
    align-items: center;
  }

  .navbar-actions {
    gap: 4px;
  }

  .profile {
    gap: 16px;
    padding: 30px 0 26px;
  }

  .profile-copy {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 5px;
  }

  .profile-name {
    color: var(--app-text);
    font-size: 23px;
    font-weight: 800;
  }

  .profile-account,
  .profile-department {
    overflow: hidden;
    color: var(--app-text-secondary);
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .profile-arrow {
    color: var(--app-text-secondary);
  }

  .stat-grid {
    justify-content: space-around;
    padding: 10px 0 28px;
    border-bottom: 1px solid var(--app-border);
  }

  .stat-item {
    display: flex;
    width: 33.333%;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    border-right: 1px solid var(--app-border);
  }

  .stat-item:last-child {
    border-right: 0;
  }

  .stat-value {
    color: var(--app-text);
    font-size: 25px;
    font-weight: 800;
  }

  .stat-label {
    color: var(--app-text-secondary);
    font-size: 13px;
  }

  :deep(.mine-menu) {
    margin: 20px 0;
    overflow: hidden;
    border-radius: 8px;
  }

  :deep(.mine-menu .wd-cell) {
    background: var(--app-surface);
  }

  :deep(.mine-menu .wd-cell__left) {
    color: #04bfe5;
  }

  :deep(.logout-button) {
    margin: 4px 0 28px;
    border-radius: 8px;
  }
</style>
