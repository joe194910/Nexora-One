import { defineStore } from 'pinia';
import {
  favoriteAssistant,
  getAssistantStore,
  getAvailableAssistants,
  unfavoriteAssistant,
} from '@/api/business/knowledge-api';

const RECENT_ASSISTANT_KEY = 'NEXORA_ONE_RECENT_ASSISTANTS';

/**
 * 智能助手目录状态。
 */
export const useAssistantStore = defineStore('assistant', {
  state: () => ({
    assistants: [],
    marketAssistants: [],
    recentAssistantIds: [],
    loading: false,
  }),
  getters: {
    recentAssistants(state) {
      return state.recentAssistantIds
        .map((assistantId) =>
          state.assistants.find(
            (item) => String(item.assistant?.assistantId) === String(assistantId),
          ),
        )
        .filter(Boolean);
    },
    favoriteAssistants(state) {
      return state.marketAssistants.filter((item) => item.favorited);
    },
  },
  actions: {
    /**
     * 初始化本地最近使用记录。
     */
    initializeRecent() {
      const recentAssistantIds = uni.getStorageSync(RECENT_ASSISTANT_KEY);
      this.recentAssistantIds = Array.isArray(recentAssistantIds) ? recentAssistantIds : [];
    },

    /**
     * 加载当前用户可用助手。
     */
    async loadAvailableAssistants() {
      this.loading = true;
      try {
        this.assistants = (await getAvailableAssistants()) || [];
        return this.assistants;
      } finally {
        this.loading = false;
      }
    },

    /**
     * 加载助手市场，用于收藏与发现助手。
     *
     * @param {string} keyword 搜索关键词
     */
    async loadAssistantStore(keyword = '') {
      this.marketAssistants = (await getAssistantStore(keyword)) || [];
      return this.marketAssistants;
    },

    /**
     * 记录最近使用的助手。
     *
     * @param {string|number} assistantId 助手编号
     */
    markRecent(assistantId) {
      const normalizedId = String(assistantId);
      this.recentAssistantIds = [
        normalizedId,
        ...this.recentAssistantIds.filter((item) => String(item) !== normalizedId),
      ].slice(0, 6);
      uni.setStorageSync(RECENT_ASSISTANT_KEY, this.recentAssistantIds);
    },

    /**
     * 切换助手收藏状态。
     *
     * @param {Object} assistantView 助手视图
     */
    async toggleFavorite(assistantView) {
      const assistantId = assistantView.assistant?.assistantId;
      if (!assistantId) {
        return;
      }
      if (assistantView.favorited) {
        await unfavoriteAssistant(assistantId);
      } else {
        await favoriteAssistant(assistantId);
      }
      assistantView.favorited = !assistantView.favorited;
    },
  },
});
