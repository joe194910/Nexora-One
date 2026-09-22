import { defineStore } from 'pinia';

const THEME_STORAGE_KEY = 'NEXORA_ONE_APP_THEME';

/**
 * 移动端主题状态。
 */
export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: 'light',
  }),
  getters: {
    isDark: (state) => state.mode === 'dark',
  },
  actions: {
    /**
     * 从本地缓存恢复主题。
     */
    initializeTheme() {
      const cachedTheme = uni.getStorageSync(THEME_STORAGE_KEY);
      this.mode = cachedTheme === 'dark' ? 'dark' : 'light';
    },

    /**
     * 设置主题并持久化。
     *
     * @param {string} mode 主题模式
     */
    setTheme(mode) {
      this.mode = mode === 'dark' ? 'dark' : 'light';
      uni.setStorageSync(THEME_STORAGE_KEY, this.mode);
    },

    /**
     * 切换浅色与深色主题。
     */
    toggleTheme() {
      this.setTheme(this.isDark ? 'light' : 'dark');
    },
  },
});
