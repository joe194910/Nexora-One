<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="themeVars">
    <view class="app-shell settings-page" :class="{ 'theme-dark': themeStore.isDark }">
      <wd-navbar
        fixed
        placeholder
        safe-area-inset-top
        left-arrow
        :title="pageTitle"
        :bordered="false"
        :custom-style="navbarStyle"
        @click-left="goBack"
      />

      <view class="page-content">
        <wd-card v-if="pageType === 'profile'" type="rectangle" custom-class="profile-card">
          <view class="profile-heading">
            <wd-avatar
              :src="userStore.avatar"
              :text="avatarText"
              :size="64"
              bg-color="#dff7fd"
              color="#04aaca"
            />
            <view class="profile-copy">
              <text class="profile-name">{{ displayName }}</text>
              <text class="profile-subtitle">{{ userStore.departmentName || '平台用户' }}</text>
            </view>
          </view>
          <wd-cell-group border>
            <wd-cell title="登录名" :value="userStore.loginName || '未设置'" />
            <wd-cell title="手机号" :value="userStore.phone || '未设置'" />
            <wd-cell title="部门" :value="userStore.departmentName || '未设置'" />
            <wd-cell title="员工编号" :value="userStore.employeeId || '未设置'" />
          </wd-cell-group>
        </wd-card>

        <wd-card v-else-if="pageType === 'security'" type="rectangle">
          <wd-cell-group border>
            <wd-cell title="账户类型">
              <template #value>
                <wd-tag :type="userStore.administratorFlag ? 'primary' : 'default'" plain>
                  {{ userStore.administratorFlag ? '管理员' : '普通用户' }}
                </wd-tag>
              </template>
            </wd-cell>
            <wd-cell title="密码状态">
              <template #value>
                <wd-tag :type="userStore.needUpdatePwdFlag ? 'warning' : 'success'" plain>
                  {{ userStore.needUpdatePwdFlag ? '需要修改' : '正常' }}
                </wd-tag>
              </template>
            </wd-cell>
            <wd-cell title="上次登录时间" :value="userStore.lastLoginTime || '暂无记录'" />
            <wd-cell title="上次登录 IP" :value="userStore.lastLoginIp || '暂无记录'" />
            <wd-cell
              title="登录地区"
              :value="userStore.lastLoginIpRegion || '暂无记录'"
            />
            <wd-cell
              title="登录设备"
              :label="userStore.lastLoginUserAgent || '暂无记录'"
              vertical
            />
          </wd-cell-group>
        </wd-card>

        <wd-card v-else-if="pageType === 'notification'" type="rectangle">
          <wd-cell-group border>
            <wd-cell
              title="未读消息"
              label="来自平台消息中心的实时未读数量"
              :value="userStore.unreadMessageCount || 0"
            />
            <wd-cell title="消息提醒" label="在应用内展示系统通知">
              <template #value>
                <wd-switch v-model="notificationEnabled" size="22" />
              </template>
            </wd-cell>
          </wd-cell-group>
        </wd-card>

        <wd-card v-else-if="pageType === 'help'" type="rectangle">
          <view class="text-section">
            <text class="text-title">智能助手使用说明</text>
            <text class="text-paragraph">
              助手会使用平台中已发布并授权给当前账户的知识库与 AI 工具回答问题。
            </text>
            <text class="text-paragraph">
              涉及操作类工具时，页面会展示待确认卡片。只有点击“确认执行”后，系统才会调用对应工具。
            </text>
            <text class="text-paragraph">
              文档需要先在管理端知识库完成上传、解析和启用，移动端不会绕过知识库直接读取本地文件。
            </text>
          </view>
        </wd-card>

        <wd-card v-else type="rectangle">
          <view class="about-section">
            <view class="about-logo">
              <wd-icon name="view-module" size="31px" />
            </view>
            <text class="about-name">NexoraOne</text>
            <text class="about-version">移动端 3.0.0</text>
            <text class="about-description">
              面向企业知识库、智能助手与 MCP 工具调用的一体化移动入口。
            </text>
          </view>
        </wd-card>

        <wd-card type="rectangle" custom-class="appearance-card">
          <wd-cell title="深色模式" label="在浅色与深色界面之间切换">
            <template #value>
              <wd-switch v-model="darkMode" size="22" />
            </template>
          </wd-cell>
        </wd-card>
      </view>
    </view>
  </wd-config-provider>
</template>

<script setup>
  import { computed, ref, watch } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { useUserStore } from '@/store/modules/system/user';

  const NOTIFICATION_STORAGE_KEY = 'NEXORA_ONE_NOTIFICATION_ENABLED';
  const themeStore = useThemeStore();
  const userStore = useUserStore();
  const pageType = ref('profile');
  const notificationEnabled = ref(
    uni.getStorageSync(NOTIFICATION_STORAGE_KEY) !== false,
  );

  const pageTitles = {
    profile: '个人资料',
    security: '账号与安全',
    notification: '消息通知',
    help: '使用帮助',
    about: '关于 NexoraOne',
  };
  const pageTitle = computed(() => pageTitles[pageType.value] || pageTitles.profile);
  const displayName = computed(() => userStore.actualName || userStore.loginName || '用户');
  const avatarText = computed(() => displayName.value.slice(0, 1));
  const navbarStyle = computed(
    () => `background:${themeStore.isDark ? '#09131c' : '#f6f8fb'};`,
  );
  const themeVars = computed(() => ({
    colorTheme: '#04bfe5',
    cardRadius: '8px',
    cardBg: themeStore.isDark ? '#0f1b26' : '#ffffff',
    navbarBackground: themeStore.isDark ? '#09131c' : '#f6f8fb',
    navbarColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    cellTitleColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    cellLabelColor: themeStore.isDark ? '#9cabc0' : '#667085',
  }));
  const darkMode = computed({
    get: () => themeStore.isDark,
    set: (value) => themeStore.setTheme(value ? 'dark' : 'light'),
  });
  /**
   * 保存当前设备的消息提醒偏好。
   */
  watch(notificationEnabled, (value) => {
    uni.setStorageSync(NOTIFICATION_STORAGE_KEY, value);
  });

  /**
   * 返回上一页或我的页面。
   */
  function goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      uni.navigateBack();
      return;
    }
    uni.reLaunch({ url: '/pages/mine/mine' });
  }

  onLoad((options) => {
    pageType.value = pageTitles[options.type] ? options.type : 'profile';
  });
</script>

<style lang="scss" scoped>
  @import '@/styles/assistant-theme.scss';

  .page-content {
    padding-top: 20px;
  }

  .profile-heading {
    display: flex;
    align-items: center;
    gap: 15px;
    padding: 4px 0 20px;
  }

  .profile-copy {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 6px;
  }

  .profile-name {
    color: var(--app-text);
    font-size: 22px;
    font-weight: 800;
  }

  .profile-subtitle,
  .about-version,
  .about-description,
  .text-paragraph {
    color: var(--app-text-secondary);
  }

  :deep(.wd-card) {
    margin: 0 0 14px;
  }

  :deep(.wd-card .wd-cell) {
    background: var(--app-surface);
  }

  .text-section,
  .about-section {
    display: flex;
    flex-direction: column;
  }

  .text-section {
    gap: 14px;
    padding: 4px 0;
  }

  .text-title {
    color: var(--app-text);
    font-size: 18px;
    font-weight: 700;
  }

  .text-paragraph {
    font-size: 14px;
    line-height: 1.8;
  }

  .about-section {
    align-items: center;
    padding: 20px 8px;
    text-align: center;
  }

  .about-logo {
    display: flex;
    width: 60px;
    height: 60px;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    background: #04bfe5;
    border-radius: 8px;
  }

  .about-name {
    margin-top: 14px;
    color: var(--app-text);
    font-size: 22px;
    font-weight: 800;
  }

  .about-version {
    margin-top: 5px;
    font-size: 13px;
  }

  .about-description {
    max-width: 300px;
    margin-top: 18px;
    font-size: 14px;
    line-height: 1.7;
  }
</style>
