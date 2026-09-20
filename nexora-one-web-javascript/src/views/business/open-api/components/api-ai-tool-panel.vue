<template>
  <a-spin :spinning="loading">
    <section class="open-api-panel">
      <div class="open-api-table-toolbar">
        <h2 class="open-api-panel__title" style="margin-bottom: 0">AI工具</h2>
        <a-space v-if="relation.tool">
          <a-button @click="goToolDetail" v-privilege="'mcp:tool:query'">
            <ExportOutlined />进入MCP工具详情
          </a-button>
          <a-button
            v-if="relation.tool.syncAvailable"
            type="primary"
            :loading="syncing"
            @click="syncVersion"
            v-privilege="'mcp:tool:save'"
          >
            <SyncOutlined />同步新版本
          </a-button>
          <a-popconfirm
            v-if="relation.tool.enabledStatus === 'ENABLED'"
            title="停用后，已关联助手将不能继续调用该工具。"
            ok-text="停用"
            cancel-text="取消"
            @confirm="updateStatus('DISABLED')"
          >
            <a-button danger v-privilege="'mcp:tool:save'"><StopOutlined />停用AI工具</a-button>
          </a-popconfirm>
          <a-button
            v-else-if="relation.tool.auditStatus === 'APPROVED'"
            type="primary"
            @click="updateStatus('ENABLED')"
            v-privilege="'mcp:tool:save'"
          >
            <PlayCircleOutlined />启用AI工具
          </a-button>
        </a-space>
        <a-button
          v-else-if="relation.publishable"
          type="primary"
          @click="publishOpen = true"
          v-privilege="'open-api:ai-tool:publish'"
        >
          <RobotOutlined />发布为AI工具
        </a-button>
      </div>

      <a-alert
        v-if="relation.tool?.syncAvailable"
        type="info"
        show-icon
        closable
        message="API 已发布新版本，当前工具仍使用原 Schema。同步需要手动确认，不会改变助手关联关系。"
        style="margin-bottom: 16px"
      />

      <a-empty
        v-if="!relation.tool"
        :description="relation.publishable ? '该 API 尚未发布为 AI 工具' : 'API 稳定上架后才能发布为 AI 工具'"
      />

      <template v-else>
        <div class="open-api-tool-overview">
          <div>
            <span>当前状态</span>
            <a-tag :color="auditMeta.color">{{ auditMeta.text }}</a-tag>
          </div>
          <div><span>关联工具</span><strong>{{ relation.tool.toolName }}</strong><code>{{ relation.tool.toolCode }}</code></div>
          <div><span>工具类型</span><strong>{{ relation.tool.toolType === 'ACTION' ? '操作工具' : '查询工具' }}</strong></div>
          <div>
            <span>风险等级</span>
            <a-tag :color="riskMeta.color">{{ riskMeta.text }}</a-tag>
          </div>
          <div>
            <span>执行确认</span>
            <strong>{{ relation.tool.confirmationPolicy === 'REQUIRED' ? '调用前确认' : '自动执行' }}</strong>
          </div>
          <div><span>关联助手</span><strong>{{ relation.assistants?.length || 0 }} 个</strong></div>
          <div><span>来源版本</span><strong>{{ relation.tool.sourceVersion || '-' }}</strong></div>
          <div><span>最近更新</span><strong>{{ relation.tool.updateTime || '-' }}</strong></div>
        </div>
      </template>
    </section>

    <template v-if="relation.tool">
      <section class="open-api-panel">
        <h2 class="open-api-panel__title">版本关联关系</h2>
        <a-table :data-source="relation.versions || []" :columns="versionColumns" row-key="versionId" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'associated'">
              <a-tag :color="record.associated ? 'green' : 'default'">
                {{ record.associated ? '已关联' : '未关联' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-button
                v-if="!record.associated && record.versionNo === relation.tool.latestVersion"
                type="link"
                :loading="syncing"
                @click="syncVersion"
                v-privilege="'mcp:tool:save'"
              >
                同步此版本
              </a-button>
              <span v-else class="open-api-muted">-</span>
            </template>
          </template>
        </a-table>
      </section>

      <section class="open-api-panel">
        <h2 class="open-api-panel__title">关联智能助手</h2>
        <a-table
          :data-source="relation.assistants || []"
          :columns="assistantColumns"
          row-key="assistantId"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'enabledFlag'">
              <a-badge :status="record.enabledFlag ? 'success' : 'default'" :text="record.enabledFlag ? '已启用' : '已停用'" />
            </template>
          </template>
        </a-table>
      </section>

      <section class="open-api-panel">
        <h2 class="open-api-panel__title">Schema同步历史</h2>
        <a-table
          :data-source="relation.syncHistory || []"
          :columns="syncColumns"
          row-key="syncId"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'syncType'">
              {{ record.syncType === 'CREATE' ? '创建关联' : '手动同步' }}
            </template>
          </template>
        </a-table>
      </section>
    </template>

    <PublishAiToolDrawer v-model:open="publishOpen" :api="api" @published="loadRelation" />
  </a-spin>
</template>

<script setup>
  import { computed, onMounted, reactive, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    ExportOutlined,
    PlayCircleOutlined,
    RobotOutlined,
    StopOutlined,
    SyncOutlined,
  } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { mcpToolApi } from '/@/api/business/open-api/mcp-tool-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import PublishAiToolDrawer from './publish-ai-tool-drawer.vue';

  const props = defineProps({
    openApiId: { type: Number, required: true },
    api: { type: Object, default: () => ({}) },
  });

  const router = useRouter();
  const loading = ref(false);
  const syncing = ref(false);
  const publishOpen = ref(false);
  const relation = reactive({
    publishable: false,
    tool: null,
    versions: [],
    assistants: [],
    syncHistory: [],
  });

  const auditMeta = computed(() => ({
    DRAFT: { color: 'default', text: '草稿' },
    PENDING: { color: 'orange', text: '待审核' },
    APPROVED: {
      color: relation.tool?.enabledStatus === 'ENABLED' ? 'green' : 'default',
      text: relation.tool?.enabledStatus === 'ENABLED' ? '已发布' : '已停用',
    },
    REJECTED: { color: 'red', text: '已驳回' },
  }[relation.tool?.auditStatus] || { color: 'default', text: '未知' }));

  const riskMeta = computed(() => ({
    LOW: { color: 'green', text: '低风险' },
    MEDIUM: { color: 'orange', text: '中风险' },
    HIGH: { color: 'red', text: '高风险' },
  }[relation.tool?.riskLevel] || { color: 'default', text: '未设置' }));

  const versionColumns = [
    { title: 'API版本', dataIndex: 'versionNo' },
    { title: '关联状态', dataIndex: 'associated', width: 140 },
    { title: '发布时间', dataIndex: 'publishTime', width: 190 },
    { title: '操作', dataIndex: 'action', width: 130 },
  ];
  const assistantColumns = [
    { title: '助手名称', dataIndex: 'assistantName' },
    { title: '状态', dataIndex: 'enabledFlag', width: 140 },
  ];
  const syncColumns = [
    { title: '同步时间', dataIndex: 'createTime', width: 200 },
    { title: 'API版本', dataIndex: 'apiVersionNo', width: 140 },
    { title: '操作类型', dataIndex: 'syncType', width: 140 },
    { title: '操作人ID', dataIndex: 'operatorId', width: 140 },
  ];

  /** 加载当前 API 的工具关系、版本状态、同步历史和关联助手。 */
  async function loadRelation() {
    if (!props.openApiId) return;
    loading.value = true;
    try {
      const response = await mcpToolApi.platformRelation(props.openApiId);
      Object.assign(relation, {
        publishable: false,
        tool: null,
        versions: [],
        assistants: [],
        syncHistory: [],
        ...(response.data || {}),
      });
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  /** 手动同步到 API 最新发布版本，避免自动影响正在使用的助手。 */
  async function syncVersion() {
    syncing.value = true;
    try {
      await mcpToolApi.syncPlatform(relation.tool.toolId);
      message.success('AI 工具 Schema 已同步到最新发布版本，助手关联保持不变');
      await loadRelation();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      syncing.value = false;
    }
  }

  /** 启用或停用当前 AI 工具，保留版本和助手关联。 */
  async function updateStatus(enabledStatus) {
    try {
      await mcpToolApi.updateStatus({ toolId: relation.tool.toolId, enabledStatus });
      message.success(enabledStatus === 'ENABLED' ? 'AI 工具已启用' : 'AI 工具已停用');
      await loadRelation();
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 跳转到统一 MCP 工具管理页并定位当前工具。 */
  function goToolDetail() {
    router.push({ path: '/open-api/mcp-tools', query: { toolId: relation.tool.toolId } });
  }

  watch(() => props.openApiId, loadRelation);
  onMounted(loadRelation);
</script>
