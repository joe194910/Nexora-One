<template>
  <view :class="['check-box', customClass]">
    <wd-checkbox
      v-model="agreeFlag"
      shape="square"
      checked-color="#19d2ef"
      custom-class="agreement-checkbox"
    />
    <view class="agreement-text">
      <text>我已阅读并同意</text>
      <text class="agreement-link" @click.stop="openProtocol('user_agreement')">
        《用户协议》
      </text>
      <text>与</text>
      <text class="agreement-link" @click.stop="openProtocol('privacy_terms')">
        《隐私政策》
      </text>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue';

  defineProps({
    customClass: {
      type: String,
      default: '',
    },
  });

  const agreeFlag = ref(true);

  /**
   * 打开指定协议页面。
   *
   * @param {string} protocolKey 协议类型标识
   */
  function openProtocol(protocolKey) {
    uni.navigateTo({
      url: `/pages/protocol/index?key=${protocolKey}`,
    });
  }

  defineExpose({
    agreeFlag,
  });
</script>

<style lang="scss" scoped>
  .check-box {
    display: flex;
    align-items: flex-start;
    justify-content: center;
    width: 100%;
  }

  :deep(.agreement-checkbox) {
    flex: none;
    margin-top: 2rpx;
  }

  :deep(.agreement-checkbox .wd-checkbox__shape) {
    width: 34rpx;
    height: 34rpx;
    border-color: var(--login-border, #506075);
    border-radius: 4px;
  }

  :deep(.agreement-checkbox .wd-checkbox__label) {
    display: none;
  }

  .agreement-text {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    margin-left: 14rpx;
    color: var(--login-text-secondary, #8d9aaf);
    font-size: 25rpx;
    line-height: 1.55;
  }

  .agreement-link {
    color: #19d2ef;
  }
</style>
