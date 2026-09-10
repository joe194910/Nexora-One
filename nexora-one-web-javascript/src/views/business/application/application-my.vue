<template>
  <div class="application-page">
    <header class="application-page__header">
      <div>
        <h1 class="application-page__title">我的应用</h1>
        <div class="application-page__subtitle">集中查看可使用、已收藏和最近访问的应用。</div>
      </div>
      <a-button type="primary" @click="router.push('/application/market')"><ShopOutlined />浏览应用市场</a-button>
    </header>

    <section class="application-panel application-my-tabs">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="applications" :tab="`全部应用 ${data.applications.length}`" />
        <a-tab-pane key="favorites" :tab="`我的收藏 ${data.favorites.length}`" />
        <a-tab-pane key="recent" :tab="`最近访问 ${data.recent.length}`" />
      </a-tabs>
    </section>

    <a-spin :spinning="loading">
      <section v-if="currentList.length" class="application-market-grid">
        <application-portal-card
          v-for="item in currentList"
          :key="item.applicationId"
          :application="item"
          @favorite="toggleFavorite"
          @launch="launch"
        />
      </section>
      <section v-else class="application-panel"><a-empty description="当前分组暂无应用" /></section>
    </a-spin>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { ShopOutlined } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import ApplicationPortalCard from './components/application-portal-card.vue';
  import './application.less';

  const router = useRouter();
  const activeTab = ref('applications');
  const loading = ref(false);
  const data = reactive({ applications: [], favorites: [], recent: [] });
  const currentList = computed(() => data[activeTab.value] || []);

  async function loadData() {
    loading.value = true;
    try {
      const response = await applicationApi.queryMyApplications();
      Object.assign(data, response.data);
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  async function toggleFavorite(application) {
    try {
      await applicationApi.updateFavorite({
        applicationId: application.applicationId,
        favoriteFlag: !application.favoriteFlag,
      });
      await loadData();
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  async function launch(application) {
    try {
      const response = await applicationApi.launch({ applicationId: application.applicationId });
      await loadData();
      const target = response.data.openMode === 'CURRENT' ? '_self' : '_blank';
      window.open(response.data.launchUrl, target, target === '_blank' ? 'noopener,noreferrer' : undefined);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  onMounted(loadData);
</script>
