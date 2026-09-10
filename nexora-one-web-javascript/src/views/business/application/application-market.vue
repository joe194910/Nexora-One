<template>
  <div class="application-page">
    <header class="application-page__header">
      <div>
        <h1 class="application-page__title">应用市场</h1>
        <div class="application-page__subtitle">发现并进入已获得访问权限的企业应用。</div>
      </div>
      <a-button @click="router.push('/application/my')"><AppstoreAddOutlined />我的应用</a-button>
    </header>

    <section class="application-panel application-market-toolbar">
      <a-input-search
        v-model:value="queryForm.searchWord"
        size="large"
        allow-clear
        placeholder="搜索应用名称、编码或能力"
        @search="search"
      />
      <a-select v-model:value="queryForm.categoryName" allow-clear placeholder="全部分类" @change="search">
        <a-select-option v-for="item in categories" :key="item" :value="item">{{ item }}</a-select-option>
      </a-select>
      <a-checkbox v-model:checked="queryForm.recommendOnly" @change="search">只看推荐</a-checkbox>
    </section>

    <a-spin :spinning="loading">
      <section v-if="records.length" class="application-market-grid">
        <application-portal-card
          v-for="item in records"
          :key="item.applicationId"
          :application="item"
          @favorite="toggleFavorite"
          @launch="launch"
        />
      </section>
      <section v-else class="application-panel"><a-empty description="暂无符合条件的应用" /></section>
    </a-spin>

    <div v-if="total" class="smart-query-table-page">
      <a-pagination
        v-model:current="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        show-size-changer
        @change="queryData"
      />
    </div>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { AppstoreAddOutlined } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import ApplicationPortalCard from './components/application-portal-card.vue';
  import './application.less';

  const router = useRouter();
  const loading = ref(false);
  const records = ref([]);
  const categories = ref([]);
  const total = ref(0);
  const queryForm = reactive({ pageNum: 1, pageSize: 12, searchWord: '', categoryName: undefined, recommendOnly: false });

  async function queryData() {
    loading.value = true;
    try {
      const response = await applicationApi.queryMarket(queryForm);
      records.value = response.data.list || [];
      total.value = response.data.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  function search() {
    queryForm.pageNum = 1;
    queryData();
  }

  async function toggleFavorite(application) {
    try {
      await applicationApi.updateFavorite({
        applicationId: application.applicationId,
        favoriteFlag: !application.favoriteFlag,
      });
      application.favoriteFlag = !application.favoriteFlag;
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  async function launch(application) {
    try {
      const response = await applicationApi.launch({ applicationId: application.applicationId });
      const target = response.data.openMode === 'CURRENT' ? '_self' : '_blank';
      window.open(response.data.launchUrl, target, target === '_blank' ? 'noopener,noreferrer' : undefined);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  onMounted(async () => {
    const response = await applicationApi.queryCategories();
    categories.value = response.data || [];
    await queryData();
  });
</script>
