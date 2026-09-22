<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="loginThemeVars">
    <scroll-view
      class="login-scroll"
      scroll-y
      :show-scrollbar="false"
      :style="loginPageStyle"
    >
      <view class="login-page" :class="{ 'theme-dark': themeStore.isDark }">
        <view class="brand">
          <image
            class="brand-logo"
            src="/static/images/login/nexora-one-logo.png"
            mode="aspectFit"
          />
          <text class="brand-name">NexoraOne</text>
        </view>

        <view class="welcome">
          <text class="welcome-title">欢迎回来</text>
          <text class="welcome-subtitle">让专业知识，成为每个人的生产力。</text>
        </view>

        <view class="login-form">
          <view class="field-shell">
            <wd-input
              v-model="loginForm.loginName"
              no-border
              clearable
              size="large"
              prefix-icon="user"
              custom-class="login-input"
              placeholder="请输入账号"
              placeholder-style="color:#77849a;font-size:16px"
              confirm-type="next"
            />
          </view>

          <view class="field-shell">
            <wd-input
              v-model="loginForm.password"
              no-border
              clearable
              show-password
              size="large"
              prefix-icon="lock-on"
              custom-class="login-input"
              placeholder="请输入密码"
              placeholder-style="color:#77849a;font-size:16px"
              confirm-type="next"
            />
          </view>

          <view v-if="emailCodeShowFlag" class="field-shell email-code-field">
            <wd-input
              v-model="loginForm.emailCode"
              no-border
              clearable
              size="large"
              prefix-icon="mail"
              custom-class="login-input"
              placeholder="请输入邮箱验证码"
              placeholder-style="color:#77849a;font-size:16px"
              confirm-type="next"
            />
            <wd-button
              type="text"
              size="small"
              :disabled="emailCodeButtonDisabled"
              custom-class="email-code-button"
              @click="sendEmailCode"
            >
              {{ emailCodeTips }}
            </wd-button>
          </view>

          <view class="captcha-row">
            <view class="field-shell captcha-field">
              <wd-input
                v-model="loginForm.captchaCode"
                no-border
                clearable
                size="large"
                prefix-icon="check-outline"
                custom-class="login-input"
                placeholder="请输入验证码"
                placeholder-style="color:#77849a;font-size:16px"
                confirm-type="done"
                @confirm="login"
              />
            </view>
            <view class="captcha-card" @click="getCaptcha">
              <image
                v-if="captchaBase64Image"
                class="captcha-image"
                :src="captchaBase64Image"
                mode="widthFix"
              />
              <view v-else class="captcha-loading">
                <wd-loading color="#19d2ef" size="20px" />
              </view>
            </view>
          </view>

          <view class="login-links">
            <text class="primary-link" @click="showFeatureTip('验证码登录')">验证码登录</text>
            <text class="secondary-link" @click="showFeatureTip('忘记密码')">忘记密码？</text>
          </view>

          <wd-button
            block
            size="large"
            :round="false"
            :loading="loginLoading"
            custom-class="login-button"
            @click="login"
          >
            登录
          </wd-button>

          <wd-button
            block
            plain
            size="large"
            :round="false"
            custom-class="register-button"
            @click="showFeatureTip('注册账号')"
          >
            注册账号
          </wd-button>
        </view>

        <LoginCheckBox ref="loginCheckBoxRef" custom-class="login-agreement" />
      </view>
    </scroll-view>
  </wd-config-provider>
</template>

<script setup>
  import { onHide, onShow, onUnload } from '@dcloudio/uni-app';
  import { computed, reactive, ref } from 'vue';
  import LoginCheckBox from './components/login-check-box.vue';
  import { loginApi } from '@/api/system/login-api';
  import { LOGIN_DEVICE_ENUM } from '@/constants/system/login-device-const';
  import { encryptData } from '@/lib/encrypt';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { useUserStore } from '@/store/modules/system/user';
  import { smartSentry } from '@/lib/smart-sentry';

  const themeStore = useThemeStore();
  const loginPageStyle = computed(
    () => `background:${themeStore.isDark ? '#08131c' : '#f8fafc'};`,
  );
  const loginThemeVars = computed(() => ({
    colorTheme: '#18cceb',
    buttonPrimaryBgColor: '#18cceb',
    buttonPrimaryColor: '#04131d',
    buttonPrimaryBorderColor: '#18cceb',
    inputColor: themeStore.isDark ? '#f5f8fc' : '#101828',
    inputIconColor: themeStore.isDark ? '#98a6ba' : '#667085',
    inputClearColor: themeStore.isDark ? '#66758a' : '#98a2b3',
  }));

  const loginForm = reactive({
    loginName: 'admin',
    password: '123456',
    captchaCode: '',
    captchaUuid: '',
    emailCode: '',
    loginDevice: LOGIN_DEVICE_ENUM.H5.value,
  });

  const loginCheckBoxRef = ref();
  const loginLoading = ref(false);
  const captchaBase64Image = ref('');
  const emailCodeShowFlag = ref(false);
  const emailCodeTips = ref('获取验证码');
  const emailCodeButtonDisabled = ref(false);
  let refreshCaptchaInterval = null;
  let countDownTimer = null;

  /**
   * 校验登录表单并调用真实登录接口。
   */
  async function login() {
    if (!loginCheckBoxRef.value?.agreeFlag) {
      uni.showToast({
        icon: 'none',
        title: '请阅读并同意《用户协议》与《隐私政策》',
      });
      return;
    }
    if (!loginForm.loginName.trim()) {
      uni.showToast({ icon: 'none', title: '请输入账号' });
      return;
    }
    if (!loginForm.password) {
      uni.showToast({ icon: 'none', title: '请输入密码' });
      return;
    }
    if (!loginForm.captchaCode.trim()) {
      uni.showToast({ icon: 'none', title: '请输入验证码' });
      return;
    }
    if (emailCodeShowFlag.value && !loginForm.emailCode.trim()) {
      uni.showToast({ icon: 'none', title: '请输入邮箱验证码' });
      return;
    }

    try {
      loginLoading.value = true;
      const encryptedForm = {
        ...loginForm,
        password: encryptData(loginForm.password),
      };
      const response = await loginApi.login(encryptedForm);
      stopRefreshCaptchaInterval();
      useUserStore().setUserLoginInfo(response.data);
      uni.showToast({ title: '登录成功' });
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/home/index' });
      }, 300);
    } catch (error) {
      loginForm.captchaCode = '';
      await getCaptcha();
      smartSentry.captureError(error);
    } finally {
      loginLoading.value = false;
    }
  }

  /**
   * 获取图形验证码并按照服务端有效期安排自动刷新。
   */
  async function getCaptcha() {
    try {
      const captchaResult = await loginApi.getCaptcha();
      captchaBase64Image.value = captchaResult.data.captchaBase64Image;
      loginForm.captchaUuid = captchaResult.data.captchaUuid;
      beginRefreshCaptchaInterval(captchaResult.data.expireSeconds);
    } catch (error) {
      smartSentry.captureError(error);
      uni.showToast({ title: '验证码加载失败', icon: 'none' });
    }
  }

  /**
   * 启动图形验证码刷新定时器。
   *
   * @param {number} expireSeconds 验证码有效秒数
   */
  function beginRefreshCaptchaInterval(expireSeconds) {
    stopRefreshCaptchaInterval();
    const refreshSeconds = Math.max(Number(expireSeconds || 60) - 5, 10);
    refreshCaptchaInterval = setInterval(getCaptcha, refreshSeconds * 1000);
  }

  /**
   * 停止图形验证码刷新定时器。
   */
  function stopRefreshCaptchaInterval() {
    if (refreshCaptchaInterval !== null) {
      clearInterval(refreshCaptchaInterval);
      refreshCaptchaInterval = null;
    }
  }

  /**
   * 获取双因子登录配置。
   */
  async function getTwoFactorLoginFlag() {
    try {
      const result = await loginApi.getTwoFactorLoginFlag();
      emailCodeShowFlag.value = Boolean(result.data);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /**
   * 发送双因子邮箱验证码。
   */
  async function sendEmailCode() {
    if (!loginForm.loginName.trim()) {
      uni.showToast({ title: '请先输入账号', icon: 'none' });
      return;
    }
    try {
      uni.showLoading({ title: '正在发送' });
      await loginApi.sendLoginEmailCode(loginForm.loginName);
      uni.showToast({
        title: '验证码已发送，请登录邮箱查看',
        icon: 'none',
      });
      runCountDown();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      uni.hideLoading();
    }
  }

  /**
   * 启动邮箱验证码六十秒倒计时。
   */
  function runCountDown() {
    stopCountDown();
    emailCodeButtonDisabled.value = true;
    let countDown = 60;
    emailCodeTips.value = `${countDown}秒`;
    countDownTimer = setInterval(() => {
      countDown -= 1;
      if (countDown > 0) {
        emailCodeTips.value = `${countDown}秒`;
        return;
      }
      stopCountDown();
    }, 1000);
  }

  /**
   * 停止邮箱验证码倒计时并恢复按钮状态。
   */
  function stopCountDown() {
    if (countDownTimer !== null) {
      clearInterval(countDownTimer);
      countDownTimer = null;
    }
    emailCodeButtonDisabled.value = false;
    emailCodeTips.value = '获取验证码';
  }

  /**
   * 提示尚未配置完成的登录能力。
   *
   * @param {string} featureName 功能名称
   */
  function showFeatureTip(featureName) {
    uni.showToast({
      title: `${featureName}暂未开放`,
      icon: 'none',
    });
  }

  onShow(() => {
    if (useUserStore().getToken) {
      uni.reLaunch({ url: '/pages/home/index' });
      return;
    }
    getCaptcha();
    getTwoFactorLoginFlag();
  });

  onHide(stopRefreshCaptchaInterval);
  onUnload(() => {
    stopRefreshCaptchaInterval();
    stopCountDown();
  });
</script>

<style lang="scss" scoped>
  .login-scroll {
    height: 100vh;
  }

  .login-page {
    --login-background: #f8fafc;
    --login-surface: #ffffff;
    --login-text: #0b1220;
    --login-text-secondary: #667085;
    --login-border: #d6dce6;
    --login-field-shadow: rgba(16, 24, 40, 0.03);

    box-sizing: border-box;
    min-height: 100vh;
    padding: calc(88rpx + env(safe-area-inset-top)) 54rpx
      calc(70rpx + env(safe-area-inset-bottom));
    color: var(--login-text);
    background: var(--login-background);
  }

  .login-page.theme-dark {
    --login-background: #08131c;
    --login-surface: #101c27;
    --login-text: #f6f8fb;
    --login-text-secondary: #98a6ba;
    --login-border: #33414f;
    --login-field-shadow: rgba(0, 0, 0, 0);
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 22rpx;
  }

  .brand-logo {
    width: 74rpx;
    height: 74rpx;
  }

  .brand-name {
    font-size: 42rpx;
    font-weight: 800;
    letter-spacing: 0;
  }

  .welcome {
    display: flex;
    flex-direction: column;
    gap: 20rpx;
    margin-top: 76rpx;
  }

  .welcome-title {
    font-size: 62rpx;
    font-weight: 800;
    line-height: 1.18;
  }

  .welcome-subtitle {
    color: var(--login-text-secondary);
    font-size: 29rpx;
    line-height: 1.5;
  }

  .login-form {
    margin-top: 62rpx;
  }

  .field-shell {
    box-sizing: border-box;
    display: flex;
    align-items: center;
    height: 112rpx;
    margin-bottom: 30rpx;
    padding: 0 28rpx;
    background: var(--login-surface);
    border: 1px solid var(--login-border);
    border-radius: 8px;
    box-shadow: 0 8rpx 24rpx var(--login-field-shadow);
  }

  :deep(.login-input) {
    width: 100%;
    padding: 0;
    color: var(--login-text);
    background: transparent;
  }

  :deep(.login-input .wd-input__body),
  :deep(.login-input .wd-input__value) {
    width: 100%;
  }

  :deep(.login-input .wd-input__inner) {
    height: 82rpx;
    color: var(--login-text);
    font-size: 31rpx;
  }

  :deep(.login-input .wd-input__prefix) {
    margin-right: 24rpx;
  }

  :deep(.login-input .wd-input__icon) {
    color: var(--login-text-secondary);
    font-size: 46rpx;
  }

  .email-code-field {
    gap: 16rpx;
  }

  :deep(.email-code-button) {
    flex: none;
    color: #19d2ef;
    font-size: 24rpx;
  }

  .captcha-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(224rpx, 42%);
    align-items: center;
    gap: 20rpx;
  }

  .captcha-field {
    min-width: 0;
    margin-bottom: 0;
  }

  .captcha-card {
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    min-height: 86rpx;
    padding: 0;
    background: rgb(230, 244, 255);
    border: 1px solid var(--login-border);
    border-radius: 8px;
    line-height: 0;
  }

  .captcha-image {
    display: block;
    width: 100%;
    height: auto;
    border-radius: 7px;
  }

  .captcha-loading {
    width: 100%;
    height: 100%;
  }

  .captcha-loading {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .login-links {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 50rpx 0 48rpx;
    font-size: 29rpx;
  }

  .primary-link {
    color: #19d2ef;
  }

  .secondary-link {
    color: var(--login-text-secondary);
  }

  :deep(.login-button),
  :deep(.register-button) {
    width: 100%;
    height: 102rpx;
    border-radius: 8px;
    font-size: 32rpx;
    font-weight: 800;
  }

  :deep(.login-button) {
    color: #04131d;
    background: #19d2ef;
    border-color: #19d2ef;
    box-shadow: 0 12rpx 30rpx rgba(25, 210, 239, 0.12);
  }

  :deep(.register-button) {
    margin-top: 30rpx;
    color: #19d2ef;
    background: transparent;
    border-color: var(--login-border);
  }

  :deep(.login-agreement) {
    margin-top: 72rpx;
  }

  @media (max-width: 350px) {
    .login-page {
      padding-right: 38rpx;
      padding-left: 38rpx;
    }

    .welcome {
      margin-top: 54rpx;
    }

    .welcome-title {
      font-size: 54rpx;
    }

    .login-form {
      margin-top: 46rpx;
    }

    .captcha-row {
      grid-template-columns: minmax(0, 1fr) minmax(210rpx, 42%);
      gap: 14rpx;
    }
  }
</style>
