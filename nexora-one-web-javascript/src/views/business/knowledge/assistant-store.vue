<template>
  <div class="knowledge-page">
    <header class="knowledge-header">
      <div>
        <h1>智能助手商店</h1>
        <p>发现团队已上架的智能助手，可直接问答或收藏到我的助手</p>
      </div>
      <a-button @click="router.push('/knowledge/assistants')">
        <RobotOutlined />管理我的助手
      </a-button>
    </header>

    <section class="knowledge-panel">
      <div class="knowledge-filters">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="搜索智能助手"
          style="width: 320px"
          @search="load"
        />
        <a-segmented v-model:value="scope" :options="scopeOptions" />
        <a-button @click="load"><ReloadOutlined />刷新</a-button>
      </div>
    </section>

    <a-spin :spinning="loading">
      <div v-if="filteredRows.length" class="knowledge-store-grid">
        <article
          v-for="row in filteredRows"
          :key="row.assistant.assistantId"
          class="knowledge-store-card"
        >
          <div class="knowledge-store-card__head">
            <span class="knowledge-store-card__icon"><RobotOutlined /></span>
            <div>
              <h2>{{ row.assistant.assistantName }}</h2>
              <p>{{ row.owned ? '我创建的智能助手' : `由 ${row.ownerName} 创建` }}</p>
            </div>
            <a-tag v-if="row.owned" color="blue">我的</a-tag>
            <a-tag v-else-if="row.favorited" color="gold">已收藏</a-tag>
          </div>

          <p class="knowledge-store-card__description">
            {{ row.assistant.systemPrompt || '该助手暂未填写介绍，可进入问答了解其知识范围。' }}
          </p>

          <div class="knowledge-store-card__meta">
            <span><ReadOutlined />{{ row.baseNames?.length || 0 }} 个知识库</span>
            <span><CloudOutlined />{{ row.modelName }}</span>
            <span><StarOutlined />{{ row.favoriteCount }} 次收藏</span>
          </div>

          <div class="knowledge-store-card__assistants">
            <span>知识范围</span>
            <a-tag v-for="name in row.baseNames" :key="name">{{ name }}</a-tag>
            <span v-if="!row.baseNames?.length" class="knowledge-muted">暂无可用知识库</span>
          </div>

          <div class="knowledge-store-card__actions">
            <a-button
              v-if="!row.owned"
              :type="row.favorited ? 'default' : 'primary'"
              ghost
              @click="toggleFavorite(row)"
            >
              <StarFilled v-if="row.favorited" />
              <StarOutlined v-else />
              {{ row.favorited ? '取消收藏' : '收藏' }}
            </a-button>
            <a-button type="primary" @click="useAssistant(row)">
              <MessageOutlined />立即使用
            </a-button>
          </div>
        </article>
      </div>
      <a-empty v-else description="暂无符合条件的已上架智能助手" />
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  MessageOutlined,
  ReadOutlined,
  ReloadOutlined,
  RobotOutlined,
  CloudOutlined,
  StarFilled,
  StarOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
import './knowledge.less';

const router = useRouter();
const rows = ref([]);
const keyword = ref('');
const scope = ref('all');
const loading = ref(false);
const scopeOptions = [
  { label: '全部', value: 'all' },
  { label: '已收藏', value: 'favorite' },
  { label: '我的', value: 'mine' },
];
const filteredRows = computed(() =>
  rows.value.filter(
    (row) =>
      scope.value === 'all' ||
      (scope.value === 'favorite' && row.favorited) ||
      (scope.value === 'mine' && row.owned),
  ),
);

async function load() {
  loading.value = true;
  try {
    rows.value = (await api.assistantStore({ keyword: keyword.value })).data || [];
  } finally {
    loading.value = false;
  }
}

async function toggleFavorite(row) {
  if (row.favorited) {
    await api.unfavoriteAssistant(row.assistant.assistantId);
    message.success('已取消收藏');
  } else {
    await api.favoriteAssistant(row.assistant.assistantId);
    message.success('已收藏，助手已加入首页和全部助手');
  }
  await load();
}

function useAssistant(row) {
  router.push({
    path: '/knowledge/assistants',
    query: { assistantId: String(row.assistant.assistantId) },
  });
}

onMounted(load);
</script>
