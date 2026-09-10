<template>
  <article class="application-market-card">
    <div class="application-market-card__top">
      <div class="application-icon">
        <img v-if="application.iconUrl" :src="application.iconUrl" alt="" />
        <AppstoreOutlined v-else />
      </div>
      <div class="application-market-card__main">
        <div class="application-market-card__title">
          <strong>{{ application.marketName || application.applicationName }}</strong>
          <a-tag v-if="application.recommendTag" color="blue">{{ application.recommendTag }}</a-tag>
        </div>
        <p>{{ application.subtitle || application.summary || '暂无应用简介' }}</p>
      </div>
      <a-tooltip v-if="application.allowFavorite !== false" :title="application.favoriteFlag ? '取消收藏' : '收藏应用'">
        <a-button type="text" class="application-icon-button" @click="$emit('favorite', application)">
          <StarFilled v-if="application.favoriteFlag" class="is-favorite" />
          <StarOutlined v-else />
        </a-button>
      </a-tooltip>
    </div>
    <div class="application-market-card__tags">
      <a-tag>{{ application.category || '未分类' }}</a-tag>
      <a-tag v-for="tag in application.tags || []" :key="tag">{{ tag }}</a-tag>
    </div>
    <div class="application-market-card__footer">
      <span><EyeOutlined /> {{ application.visitCount || 0 }} 次访问</span>
      <a-button type="primary" @click="$emit('launch', application)">
        <template #icon><ExportOutlined /></template>
        进入应用
      </a-button>
    </div>
  </article>
</template>

<script setup>
  import { AppstoreOutlined, EyeOutlined, ExportOutlined, StarFilled, StarOutlined } from '@ant-design/icons-vue';

  defineProps({
    application: {
      type: Object,
      required: true,
    },
  });

  defineEmits(['favorite', 'launch']);
</script>
