import { createSSRApp } from 'vue';
import App from './App.vue';
import { store } from './store/index';

/*每个页面公共css */
import './theme/index.scss';

// 枚举管理
import nexoraEnumPlugin from '@/plugins/nexora-enums-plugin';
import constantsInfo from '@/constants/index';
import lodash from 'lodash';

export function createApp() {
  const app = createSSRApp(App);
  app.use(store);
  app.use(nexoraEnumPlugin, constantsInfo);
  app.config.globalProperties.$lodash = lodash;
  return {
    app,
    store,
  };
}
