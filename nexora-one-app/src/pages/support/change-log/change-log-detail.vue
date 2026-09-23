<template>
  <view class="container">
    <view class="title">
      <uni-title type="h1" align="center" :title="detail.title"></uni-title>
      <uni-title type="h4" align="center" color="#999999" :title="detail.subTitle"></uni-title>
    </view>
    <view class="content">
      <rich-text :nodes="detail.content" />
    </view>
  </view>
</template>

<script setup>
  import { inject, reactive } from 'vue';
  import { changeLogApi } from '@/api/support/change-log-api';
  import { onLoad } from '@dcloudio/uni-app';
  import { nexoraSentry } from '@/lib/nexora-sentry';

  const nexoraEnumPlugin = inject('nexoraEnumPlugin');

  const detail = reactive({
    title: '',
    subTitle: '',
    content: '',
  });

  async function getDetail(changeLogId) {
    try {
      uni.showLoading({ title: '加载中' });
      let res = await changeLogApi.getDetail(changeLogId);
      detail.title = res.data.version + '版本' + nexoraEnumPlugin.getDescByValue('CHANGE_LOG_TYPE_ENUM', res.data.type);
      detail.content =
        '<pre style="' +
        'line-height: 18px;\n' +
        'font-size: 14px;\n' +
        'white-space: pre-wrap;\n' +
        '  white-space: -moz-pre-wrap;\n' +
        '  white-space: -pre-wrap;\n' +
        '  white-space: -o-pre-wrap;\n' +
        '  word-wrap: break-word;">' +
        res.data.content +
        '</pre>';
      let subTitleArray = [];
      if (res.data.publishAuthor) {
        subTitleArray.push(res.data.publishAuthor);
      }
      if (res.data.publicDate) {
        subTitleArray.push(res.data.publicDate);
      }
      detail.subTitle = subTitleArray.join(' | ');
    } catch (e) {
      nexoraSentry.captureError(e);
    } finally {
      uni.hideLoading();
    }
  }

  onLoad((option) => {
    uni.pageScrollTo({
      scrollTop: 0,
    });
    getDetail(option.changeLogId);
  });
</script>

<style lang="scss" scoped>
  .container {
    height: 100vh;
    padding: 20rpx;

    .content {
      border-top: #cccccc 1px solid;
      margin-top: 50rpx;
      padding: 50rpx 16rpx 50rpx 16rpx;
      line-height: 30px;
      color: #333333;
    }
  }
</style>
