/*
 * 所有路由入口
 *
 * @Author:    NexoraOne-主任：卓大
 * @Date:      2022-09-06 20:52:26
 * @Wechat:    NexoraOne
 * @Email:     NexoraOne
 * @Copyright  NexoraOne （ # ），Since 2012
 */
import { homeRouters } from './system/home';
import { loginRouters } from './system/login';
import { helpDocRouters } from './support/help-doc';
import NotFound from '/@/views/system/40X/404.vue';
import NoPrivilege from '/@/views/system/40X/403.vue';

export const routerArray = [
    ...loginRouters,
    ...homeRouters,
    ...helpDocRouters,
    {
      path: '/share/assistant/:code',
      name: 'assistant-share-public',
      component: () => import('/@/views/business/knowledge/assistant-share.vue'),
      meta: { publicPage: true, title: '智能助手分享' },
    },
    { path: '/:pathMatch(.*)*', name: '404', component: NotFound },
    { path: '/403', name: '403', component: NoPrivilege }
];
