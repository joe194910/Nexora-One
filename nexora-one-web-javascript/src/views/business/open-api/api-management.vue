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
        <div>
          <label class="open-api-query__label">AI工具状态</label>
          <a-select v-model:value="queryForm.aiToolStatus" allow-clear placeholder="请选择AI工具状态" style="width: 100%">
            <a-select-option v-for="item in aiToolStatusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
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
          <template v-else-if="column.dataIndex === 'apiVersion'">
            <strong>{{ record.currentVersionNo || record.apiVersion }}</strong>
            <div v-if="record.currentVersionNo && record.currentVersionNo !== record.apiVersion" class="open-api-muted">
              线上 {{ record.apiVersion }}
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'permissionLevel'">
            <a-tag :color="permissionMeta(record.permissionLevel).color">{{ permissionMeta(record.permissionLevel).text }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-badge :status="statusMeta(record.status).badge" :text="statusMeta(record.status).text" />
          </template>
          <template v-else-if="column.dataIndex === 'aiToolStatus'">
            <a-space size="small">
              <a-tag :color="aiToolStatusMeta(record.aiToolStatus).color">
                {{ aiToolStatusMeta(record.aiToolStatus).text }}
              </a-tag>
              <a-tooltip v-if="record.aiToolSyncAvailable" title="API 已发布新版本，可在 AI 工具页手动同步">
                <SyncOutlined class="open-api-sync-indicator" />
              </a-tooltip>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'todayCallCount'">
            {{ formatNumber(record.todayCallCount) }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <div class="open-api-actions">
              <a-button type="link" @click="goDetail(record)" v-privilege="'open-api:detail'">详情</a-button>
              <a-button type="link" @click="showVersionHistory(record)" v-privilege="'open-api:detail'">版本记录</a-button>
              <a-button
                v-if="canEditCurrentVersion(record)"
                type="link"
                @click="goEdit(record)"
                v-privilege="'open-api:save'"
              >
                {{ hasSeparateVersion(record) ? '编辑新版' : '编辑' }}
              </a-button>
              <a-button
                v-if="canCreateVersion(record)"
                type="link"
                @click="showCreateVersion(record)"
                v-privilege="'open-api:add'"
              >
                创建新版本
              </a-button>
              <a-button
                v-if="canSubmitPublish(record)"
                type="link"
                @click="goPublish(record)"
                v-privilege="'open-api:publish'"
              >
                {{ hasSeparateVersion(record) ? '提交新版' : record.status === 5 ? '重新上架' : '上架' }}
              </a-button>
              <a-button
                v-if="record.status === 4 && record.aiToolStatus === 'UNPUBLISHED'"
                type="link"
                @click="showPublishAiTool(record)"
                v-privilege="'open-api:ai-tool:publish'"
              >
                发布为AI工具
              </a-button>
              <a-button
                v-else-if="record.aiToolId"
                type="link"
                @click="goAiTool(record)"
                v-privilege="'open-api:detail'"
              >
                查看AI工具
              </a-button>
              <a-button v-if="isPendingReview(record)" type="link" disabled>审核中</a-button>
              <a-popconfirm
                v-if="record.status === 4"
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

    <a-modal
      v-model:open="versionModal.open"
      title="创建API新版本"
      ok-text="创建并编辑"
      cancel-text="取消"
      :confirm-loading="versionModal.loading"
      @ok="submitCreateVersion"
    >
      <a-form layout="vertical">
        <a-form-item label="新版本号" required>
          <a-input
            v-model:value="versionModal.versionNo"
            placeholder="例如 v1.0.1"
            :maxlength="20"
            @pressEnter="submitCreateVersion"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="historyModal.open" title="API版本记录" :footer="null" width="920px">
      <a-table
        :loading="historyModal.loading"
        :data-source="historyModal.list"
        :columns="versionColumns"
        row-key="versionId"
        :pagination="false"
        size="middle"
        :scroll="{ x: 820 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'versionNo'">
            <a-space>
              <strong>{{ record.versionNo }}</strong>
              <a-tag v-if="record.currentFlag" color="blue">当前编辑</a-tag>
              <a-tag v-if="record.publishedFlag" color="green">线上版本</a-tag>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'requestMethod'">
            <a-tag :color="methodColor(record.requestMethod)">{{ record.requestMethod }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-badge :status="versionStatusMeta(record.status).badge" :text="versionStatusMeta(record.status).text" />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button type="link" @click="viewHistoryVersion(record)">查看配置</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>

    <PublishAiToolDrawer
      v-model:open="publishDrawer.open"
      :api="publishDrawer.api"
      @published="handleAiToolPublished"
    />
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
    SyncOutlined,
  } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import PublishAiToolDrawer from './components/publish-ai-tool-drawer.vue';
  import './open-api.less';

  const router = useRouter();
  const loading = ref(false);
  const tableData = ref([]);
  const total = ref(0);
  const categories = ref([]);
  const summary = reactive({ total: 0, published: 0, draft: 0, disabled: 0 });
  const versionModal = reactive({
    open: false,
    loading: false,
    openApiId: undefined,
    versionNo: '',
  });
  const historyModal = reactive({
    open: false,
    loading: false,
    openApiId: undefined,
    list: [],
  });
  const publishDrawer = reactive({
    open: false,
    api: {},
  });
  const queryForm = reactive({
    pageNum: 1,
    pageSize: 10,
    searchWord: '',
    categoryName: undefined,
    requestMethod: undefined,
    status: undefined,
    aiToolStatus: undefined,
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
  const aiToolStatusOptions = [
    { value: 'UNPUBLISHED', label: '未发布' },
    { value: 'DRAFT', label: '草稿' },
    { value: 'PENDING', label: '待审核' },
    { value: 'APPROVED', label: '已发布' },
    { value: 'REJECTED', label: '已驳回' },
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
    { title: 'AI工具', dataIndex: 'aiToolStatus', width: 130 },
    { title: '今日调用量', dataIndex: 'todayCallCount', width: 120 },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 460 },
  ];
  const versionColumns = [
    { title: '版本', dataIndex: 'versionNo', width: 220 },
    { title: '请求方式', dataIndex: 'requestMethod', width: 100 },
    { title: '网关路径', dataIndex: 'gatewayPath', ellipsis: true, width: 250 },
    { title: '状态', dataIndex: 'status', width: 110 },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 100 },
  ];

  /** 返回 HTTP 请求方式对应的标签颜色。 */
  function methodColor(method) {
    return { GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red', PATCH: 'purple' }[method] || 'default';
  }

  /** 返回 API 权限级别对应的中文展示信息。 */
  function permissionMeta(value) {
    return {
      1: { color: 'green', text: '公开' },
      2: { color: 'blue', text: '需申请' },
      3: { color: 'orange', text: '敏感' },
    }[value] || { color: 'default', text: '未设置' };
  }

  /** 返回 API 主状态对应的中文展示信息。 */
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

  /** 返回 API 版本状态对应的中文展示信息。 */
  function versionStatusMeta(value) {
    return {
      1: { badge: 'default', text: '草稿' },
      2: { badge: 'processing', text: '待审核' },
      3: { badge: 'success', text: '已发布' },
      4: { badge: 'error', text: '已停用' },
      5: { badge: 'default', text: '已下线' },
    }[value] || { badge: 'default', text: '未知' };
  }

  /** 返回 AI 工具发布状态对应的中文展示信息。 */
  function aiToolStatusMeta(value) {
    return {
      UNPUBLISHED: { color: 'default', text: '未发布' },
      DRAFT: { color: 'default', text: '草稿' },
      PENDING: { color: 'orange', text: '待审核' },
      APPROVED: { color: 'green', text: '已发布' },
      REJECTED: { color: 'red', text: '已驳回' },
    }[value] || { color: 'default', text: '未发布' };
  }

  /** 按中文数字格式展示调用次数。 */
  function formatNumber(value) {
    return Number(value || 0).toLocaleString('zh-CN');
  }

  /** 按筛选条件分页加载 API，并附带 AI 工具状态。 */
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

  /** 加载 API 汇总数据和分类筛选项。 */
  async function loadMeta() {
    try {
      const [summaryResponse, categoryResponse] = await Promise.all([openApiApi.summary(), openApiApi.categories()]);
      Object.assign(summary, summaryResponse.data || {});
      categories.value = categoryResponse.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 清空全部筛选条件并重新加载 API 列表。 */
  function resetQuery() {
    Object.assign(queryForm, {
      pageNum: 1,
      pageSize: 10,
      searchWord: '',
      categoryName: undefined,
      requestMethod: undefined,
      status: undefined,
      aiToolStatus: undefined,
    });
    queryData();
  }

  /** 进入原有六步 API 创建流程。 */
  function goCreate() {
    router.push({ path: '/open-api/editor' });
  }

  /** 编辑当前仍允许修改的 API 版本。 */
  function goEdit(record) {
    router.push({ path: '/open-api/editor', query: { openApiId: record.openApiId, step: Math.min(record.workflowStep || 1, 4) } });
  }

  /** 判断当前 API 是否已经创建了未发布的新版本。 */
  function hasSeparateVersion(record) {
    return Boolean(record.publishedVersionId) && record.currentVersionId !== record.publishedVersionId;
  }

  /** 判断当前版本是否处于发布审核中。 */
  function isPendingReview(record) {
    return record.currentVersionStatus === 2 || record.status === 3;
  }

  /** 判断当前版本是否允许继续编辑。 */
  function canEditCurrentVersion(record) {
    return record.currentVersionStatus === 1 && !isPendingReview(record);
  }

  /** 判断已上架或已停用 API 是否允许创建新版本。 */
  function canCreateVersion(record) {
    return [4, 5].includes(record.status) && !hasSeparateVersion(record) && !isPendingReview(record);
  }

  /** 判断当前 API 是否允许提交发布审核。 */
  function canSubmitPublish(record) {
    if (isPendingReview(record)) {
      return false;
    }
    if ([1, 2].includes(record.status)) {
      return true;
    }
    if (hasSeparateVersion(record)) {
      return record.currentVersionStatus === 1;
    }
    return record.status === 5;
  }

  /** 根据当前版本号生成下一个补丁版本号。 */
  function nextVersion(versionNo) {
    const match = String(versionNo || '').match(/^(v?)(\d+)\.(\d+)\.(\d+)$/);
    if (!match) {
      return 'v1.0.1';
    }
    return `${match[1] || 'v'}${match[2]}.${match[3]}.${Number(match[4]) + 1}`;
  }

  /** 打开创建新版本窗口并给出默认版本号。 */
  function showCreateVersion(record) {
    Object.assign(versionModal, {
      open: true,
      loading: false,
      openApiId: record.openApiId,
      versionNo: nextVersion(record.apiVersion),
    });
  }

  /** 创建 API 新版本并进入原有配置流程。 */
  async function submitCreateVersion() {
    const versionNo = versionModal.versionNo.trim();
    if (!/^v?\d+\.\d+\.\d+$/.test(versionNo)) {
      message.warning('版本号需使用 v1.0.1 格式');
      return;
    }
    versionModal.loading = true;
    try {
      const response = await openApiApi.createVersion({
        openApiId: versionModal.openApiId,
        versionNo,
      });
      versionModal.open = false;
      message.success('新版本已创建，请完善配置后提交审核');
      router.push({
        path: '/open-api/editor',
        query: { openApiId: response.data.openApiId, step: 1 },
      });
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      versionModal.loading = false;
    }
  }

  /** 查询并展示 API 的全部历史版本。 */
  async function showVersionHistory(record) {
    Object.assign(historyModal, {
      open: true,
      loading: true,
      openApiId: record.openApiId,
      list: [],
    });
    try {
      const response = await openApiApi.versionList(record.openApiId);
      historyModal.list = response.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      historyModal.loading = false;
    }
  }

  /** 以只读方式查看指定历史版本。 */
  function viewHistoryVersion(record) {
    router.push({
      path: '/open-api/detail',
      query: {
        openApiId: historyModal.openApiId,
        versionId: record.versionId,
        mode: 'detail',
      },
    });
  }

  /** 进入 API 详情页。 */
  function goDetail(record) {
    router.push({ path: '/open-api/detail', query: { openApiId: record.openApiId, mode: 'detail' } });
  }

  /** 打开“发布为 AI 工具”抽屉。 */
  function showPublishAiTool(record) {
    publishDrawer.api = record;
    publishDrawer.open = true;
  }

  /** 进入 API 详情中的 AI 工具页签。 */
  function goAiTool(record) {
    router.push({
      path: '/open-api/detail',
      query: { openApiId: record.openApiId, mode: 'detail', tab: 'ai-tool' },
    });
  }

  /** 工具发布完成后刷新列表中的 AI 工具状态。 */
  async function handleAiToolPublished() {
    await queryData();
  }

  /** 进入原有 API 发布审核页面。 */
  function goPublish(record) {
    router.push({ path: '/open-api/publish', query: { openApiId: record.openApiId } });
  }

  /** 启用或停用 API 本身，不直接修改 AI 工具状态。 */
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
