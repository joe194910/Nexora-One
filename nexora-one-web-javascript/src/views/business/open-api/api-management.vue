<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">API管理</h1>
        <div class="open-api-page__subtitle">创建、维护和管理 NexoraOne 开放接口。</div>
      </div>
      <a-button type="primary" size="large" @click="goCreate" v-privilege="'open-api:add'">
        <template #icon><PlusOutlined /></template>
        创建API
      </a-button>
    </header>

    <section class="open-api-summary">
      <div v-for="item in summaryItems" :key="item.key" class="open-api-summary__item">
        <span class="open-api-summary__icon" :class="item.iconClass">
          <component :is="item.icon" />
        </span>
        <div>
          <div class="open-api-summary__label">{{ item.label }}</div>
          <div class="open-api-summary__value">{{ summary[item.key] || 0 }}</div>
        </div>
      </div>
    </section>

    <section class="open-api-panel">
      <div class="open-api-query">
        <div>
          <label class="open-api-query__label">API名称 / 编码</label>
          <a-input v-model:value="queryForm.searchWord" placeholder="请输入 API 名称或编码" @pressEnter="queryData" />
        </div>
        <div>
          <label class="open-api-query__label">所属分类</label>
          <a-select v-model:value="queryForm.categoryName" allow-clear placeholder="请选择所属分类" style="width: 100%">
            <a-select-option v-for="item in categories" :key="item" :value="item">{{ item }}</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="open-api-query__label">请求方式</label>
          <a-select v-model:value="queryForm.requestMethod" allow-clear placeholder="请选择请求方式" style="width: 100%">
            <a-select-option v-for="item in methodOptions" :key="item" :value="item">{{ item }}</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="open-api-query__label">发布状态</label>
          <a-select v-model:value="queryForm.status" allow-clear placeholder="请选择发布状态" style="width: 100%">
            <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </div>
        <a-space>
          <a-button type="primary" @click="queryData"><SearchOutlined />查询</a-button>
          <a-button @click="resetQuery"><ReloadOutlined />重置</a-button>
        </a-space>
      </div>
    </section>

    <section class="open-api-panel">
      <h2 class="open-api-panel__title">API列表</h2>
      <a-table
        :loading="loading"
        :data-source="tableData"
        :columns="columns"
        row-key="openApiId"
        :pagination="false"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'apiName'">
            <div class="open-api-api-cell__name">{{ record.apiName }}</div>
            <div class="open-api-api-cell__description">{{ record.description }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'requestMethod'">
            <a-tag class="open-api-method" :color="methodColor(record.requestMethod)">{{ record.requestMethod }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'permissionLevel'">
            <a-tag :color="permissionMeta(record.permissionLevel).color">{{ permissionMeta(record.permissionLevel).text }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-badge :status="statusMeta(record.status).badge" :text="statusMeta(record.status).text" />
          </template>
          <template v-else-if="column.dataIndex === 'todayCallCount'">
            {{ formatNumber(record.todayCallCount) }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <div class="open-api-actions">
              <a-button type="link" @click="goDetail(record)" v-privilege="'open-api:detail'">详情</a-button>
              <a-button type="link" :disabled="record.status === 4" @click="goEdit(record)" v-privilege="'open-api:save'">编辑</a-button>
              <a-button v-if="record.status !== 4" type="link" @click="goPublish(record)" v-privilege="'open-api:publish'">
                上架
              </a-button>
              <a-popconfirm
                v-else
                title="确认停用当前 API？已授权应用将无法继续调用。"
                ok-text="停用"
                cancel-text="取消"
                @confirm="changeStatus(record, 5)"
              >
                <a-button type="link" danger v-privilege="'open-api:status'">停用</a-button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
      <div class="open-api-pagination">
        <a-pagination
          v-model:current="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          show-size-changer
          :total="total"
          :show-total="(value) => `共 ${value} 条`"
          @change="queryData"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    ApiOutlined,
    CheckCircleOutlined,
    FileTextOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    StopOutlined,
  } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const router = useRouter();
  const loading = ref(false);
  const tableData = ref([]);
  const total = ref(0);
  const categories = ref([]);
  const summary = reactive({ total: 0, published: 0, draft: 0, disabled: 0 });
  const queryForm = reactive({
    pageNum: 1,
    pageSize: 10,
    searchWord: '',
    categoryName: undefined,
    requestMethod: undefined,
    status: undefined,
  });

  const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
  const statusOptions = [
    { value: 1, label: '草稿' },
    { value: 2, label: '配置中' },
    { value: 3, label: '待发布' },
    { value: 4, label: '已上架' },
    { value: 5, label: '已停用' },
    { value: 6, label: '已下线' },
  ];
  const summaryItems = computed(() => [
    { key: 'total', label: 'API总数', icon: ApiOutlined, iconClass: '' },
    { key: 'published', label: '已上架', icon: CheckCircleOutlined, iconClass: 'is-success' },
    { key: 'draft', label: '草稿', icon: FileTextOutlined, iconClass: 'is-muted' },
    { key: 'disabled', label: '已停用', icon: StopOutlined, iconClass: 'is-danger' },
  ]);
  const columns = [
    { title: 'API信息', dataIndex: 'apiName', width: 300 },
    { title: 'API编码', dataIndex: 'apiCode', width: 180 },
    { title: '所属分类', dataIndex: 'categoryName', width: 130 },
    { title: '请求方式', dataIndex: 'requestMethod', width: 100 },
    { title: '版本', dataIndex: 'apiVersion', width: 90 },
    { title: '权限级别', dataIndex: 'permissionLevel', width: 110 },
    { title: '发布状态', dataIndex: 'status', width: 120 },
    { title: '今日调用量', dataIndex: 'todayCallCount', width: 120 },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 210 },
  ];

  function methodColor(method) {
    return { GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red', PATCH: 'purple' }[method] || 'default';
  }

  function permissionMeta(value) {
    return {
      1: { color: 'green', text: '公开' },
      2: { color: 'blue', text: '需申请' },
      3: { color: 'orange', text: '敏感' },
    }[value] || { color: 'default', text: '未设置' };
  }

  function statusMeta(value) {
    return {
      1: { badge: 'default', text: '草稿' },
      2: { badge: 'processing', text: '配置中' },
      3: { badge: 'warning', text: '待发布' },
      4: { badge: 'success', text: '已上架' },
      5: { badge: 'error', text: '已停用' },
      6: { badge: 'default', text: '已下线' },
    }[value] || { badge: 'default', text: '未知' };
  }

  function formatNumber(value) {
    return Number(value || 0).toLocaleString('zh-CN');
  }

  async function queryData() {
    loading.value = true;
    try {
      const response = await openApiApi.query(queryForm);
      tableData.value = response.data.list || [];
      total.value = response.data.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  async function loadMeta() {
    try {
      const [summaryResponse, categoryResponse] = await Promise.all([openApiApi.summary(), openApiApi.categories()]);
      Object.assign(summary, summaryResponse.data || {});
      categories.value = categoryResponse.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  function resetQuery() {
    Object.assign(queryForm, {
      pageNum: 1,
      pageSize: 10,
      searchWord: '',
      categoryName: undefined,
      requestMethod: undefined,
      status: undefined,
    });
    queryData();
  }

  function goCreate() {
    router.push({ path: '/open-api/editor' });
  }

  function goEdit(record) {
    router.push({ path: '/open-api/editor', query: { openApiId: record.openApiId, step: Math.min(record.workflowStep || 1, 4) } });
  }

  function goDetail(record) {
    router.push({ path: '/open-api/detail', query: { openApiId: record.openApiId, mode: 'detail' } });
  }

  function goPublish(record) {
    router.push({ path: '/open-api/publish', query: { openApiId: record.openApiId } });
  }

  async function changeStatus(record, status) {
    try {
      await openApiApi.updateStatus({ openApiId: record.openApiId, status });
      message.success(status === 4 ? 'API已启用' : 'API已停用');
      await Promise.all([queryData(), loadMeta()]);
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  onMounted(() => {
    queryData();
    loadMeta();
  });
</script>
