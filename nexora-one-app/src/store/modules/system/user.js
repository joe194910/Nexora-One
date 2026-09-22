/*
 * 登录用户
 *
 * @Author:    NexoraOne-主任：卓大
 * @Date:      2022-09-06 20:55:09
 * @Wechat:    NexoraOne
 * @Email:     NexoraOne
 * @Copyright  NexoraOne （ # ），Since 2012
 */
import { defineStore } from 'pinia';
import { USER_TOKEN } from '@/constants/local-storage-key-const';
import { loginApi } from '@/api/system/login-api';
import { smartSentry } from '@/lib/smart-sentry';
import { messageApi } from '@/api/support/message-api';

const defaultUserInfo = {
  token: '',
  // 员工 ID
  employeeId: '',
  // 头像
  avatar: '',
  // 登录名
  loginName: '',
  // 姓名
  actualName: '',
  // 手机号
  phone: '',
  // 部门 ID
  departmentId: '',
  // 部门名称
  departmentName: '',
  // 是否需要修改密码
  needUpdatePwdFlag: false,
  // 是否为超级管理员
  administratorFlag: true,
  // 上次登录 IP
  lastLoginIp: '',
  // 上次登录 IP 地区
  lastLoginIpRegion: '',
  // 上次登录设备
  lastLoginUserAgent: '',
  // 上次登录时间
  lastLoginTime: '',
  // 未读消息数量
  unreadMessageCount: 0,
};

export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    ...defaultUserInfo,
  }),
  getters: {
    getToken(state) {
      return uni.getStorageSync(USER_TOKEN);
    },
  },

  actions: {
    /**
     * 退出登录并清理本地用户状态。
     */
    logout() {
      Object.assign(this, defaultUserInfo);
      uni.removeStorageSync(USER_TOKEN);
    },

    /**
     * 清理失效登录态。
     */
    clearUserLoginInfo() {
      Object.assign(this, defaultUserInfo);
      uni.removeStorageSync(USER_TOKEN);
    },

    /**
     * 根据本地令牌刷新当前用户资料。
     */
    async getLoginInfo() {
      const token = uni.getStorageSync(USER_TOKEN);
      if (!token) {
        return;
      }
      try {
        const res = await loginApi.getLoginInfo();
        this.setUserLoginInfo({ ...res.data, token });
      } catch (error) {
        smartSentry.captureError(error);
      }
    },

    /**
     * 查询当前用户未读消息数量。
     */
    async queryUnreadMessageCount() {
      try {
        const result = await messageApi.queryUnreadCount();
        this.unreadMessageCount = result.data;
      } catch (error) {
        smartSentry.captureError(error);
      }
    },

    /**
     * 保存登录信息，并保留接口未返回的本地令牌。
     *
     * @param {Object} data 登录接口或用户信息接口返回的数据
     */
    setUserLoginInfo(data) {
      const token = data?.token || uni.getStorageSync(USER_TOKEN) || '';
      Object.keys(defaultUserInfo).forEach((key) => {
        this[key] = data?.[key] ?? defaultUserInfo[key];
      });
      this.token = token;

      if (token) {
        uni.setStorageSync(USER_TOKEN, token);
        this.queryUnreadMessageCount();
      }
    },
  },
});
