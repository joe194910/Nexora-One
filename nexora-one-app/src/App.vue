<script>
  import { useThemeStore } from '@/store/modules/system/theme';
  import { useUserStore } from '@/store/modules/system/user';

  export default {
    onLaunch() {
      useThemeStore().initializeTheme();
      useUserStore().getLoginInfo();
    },
  };
</script>

<style lang="scss">
  @import '@/uni_modules/uni-scss/index.scss';

  page {
    min-height: 100%;
    background: #f6f8fb;
  }

  body {
    margin: 0;
    color: #101828;
    background: #f6f8fb;
    font-size: 16px;
    font-family:
      Inter,
      'PingFang SC',
      'Microsoft YaHei',
      sans-serif;
  }

  view,
  text,
  input,
  textarea,
  button {
    box-sizing: border-box;
  }
</style>
