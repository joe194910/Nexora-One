<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">MCP工具管理</h1>
        <div class="open-api-page__subtitle">统一管理平台 API 转换工具与第三方应用登记的 HTTP 工具。</div>
      </div>
      <a-space>
        <a-button @click="showAccessGuide"><FileTextOutlined />接入文档</a-button>
        <a-button type="primary" @click="openExternalDrawer" v-privilege="'mcp:tool:save'">
          <PlusOutlined />手工添加
        </a-button>
      </a-space>
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
      <div class="mcp-tool-query">
        <div>
          <label class="open-api-query__label">工具名称 / 编码</label>
          <a-input v-model:value="queryForm.searchWord" allow-clear placeholder="请输入工具名称或编码" @pressEnter="queryData" />
        </div>
        <div>
          <label class="open-api-query__label">工具来源</label>
          <a-select v-model:value="queryForm.sourceType" allow-clear placeholder="请选择工具来源">
            <a-select-option value="PLATFORM_API">平台API</a-select-option>
            <a-select-option value="EXTERNAL_HTTP">第三方HTTP</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="open-api-query__label">所属应用 / 服务</label>
          <a-select
            v-model:value="queryForm.applicationId"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="请选择所属应用"
            :options="applicationOptions"
          />
        </div>
        <div>
          <label class="open-api-query__label">风险等级</label>
          <a-select v-model:value="queryForm.riskLevel" allow-clear placeholder="请选择风险等级">
            <a-select-option value="LOW">低</a-select-option>
            <a-select-option value="MEDIUM">中</a-select-option>
            <a-select-option value="HIGH">高</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="open-api-query__label">审核状态</label>
          <a-select v-model:value="queryForm.auditStatus" allow-clear placeholder="请选择审核状态">
            <a-select-option value="DRAFT">草稿</a-select-option>
            <a-select-option value="PENDING">待审核</a-select-option>
            <a-select-option value="APPROVED">已通过</a-select-option>
            <a-select-option value="REJECTED">已驳回</a-select-option>
          </a-select>
        </div>
        <div>
          <label class="open-api-query__label">启用状态</label>
          <a-select v-model:value="queryForm.enabledStatus" allow-clear placeholder="请选择启用状态">
            <a-select-option value="ENABLED">已启用</a-select-option>
            <a-select-option value="DISABLED">已停用</a-select-option>
          </a-select>
        </div>
        <a-space>
          <a-button type="primary" @click="queryData"><SearchOutlined />查询</a-button>
          <a-button @click="resetQuery"><ReloadOutlined />重置</a-button>
        </a-space>
      </div>
    </section>

    <section class="open-api-panel">
      <h2 class="open-api-panel__title">工具列表</h2>
      <a-table
        :loading="loading"
        :data-source="rows"
        :columns="columns"
        row-key="toolId"
        :pagination="false"
        :scroll="{ x: 1450 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'toolName'">
            <strong>{{ record.toolName }}</strong>
            <div class="open-api-muted">{{ record.toolCode }}</div>
            <div class="open-api-api-cell__description">{{ record.description }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'sourceType'">
            <a-tag :color="record.sourceType === 'PLATFORM_API' ? 'blue' : 'purple'">
              {{ record.sourceType === 'PLATFORM_API' ? '平台API' : '第三方HTTP' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'sourceName'">
            <strong>{{ record.sourceName || '-' }}</strong>
            <div class="open-api-muted">{{ record.sourceCode || '-' }}</div>
            <div v-if="record.sourceVersion" class="open-api-muted">
              {{ record.sourceVersion }}
              <a-tag v-if="record.syncAvailable" color="orange">可同步 {{ record.latestVersion }}</a-tag>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'toolType'">
            {{ record.toolType === 'ACTION' ? '数据操作' : '数据查询' }}
          </template>
          <template v-else-if="column.dataIndex === 'riskLevel'">
            <a-tag :color="riskMeta(record.riskLevel).color">{{ riskMeta(record.riskLevel).text }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'auditStatus'">
            <a-badge :status="auditMeta(record.auditStatus).badge" :text="auditMeta(record.auditStatus).text" />
            <div v-if="record.lastTestStatus" class="open-api-muted">
              测试：{{ record.lastTestStatus === 'SUCCESS' ? '成功' : '失败' }}
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'enabledStatus'">
            <a-badge
              :status="record.enabledStatus === 'ENABLED' ? 'success' : 'default'"
              :text="record.enabledStatus === 'ENABLED' ? '已启用' : '已停用'"
            />
            <div class="open-api-muted">在线：{{ onlineText(record.onlineStatus) }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'assistantCount'">
            {{ record.assistantCount || 0 }} 个
          </template>
          <template v-else-if="column.dataIndex === 'lastCallTime'">
            <div>{{ record.lastCallTime || '-' }}</div>
            <div class="open-api-muted">{{ formatNumber(record.totalCallCount) }} 次</div>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <div class="open-api-actions">
              <a-button type="link" @click="openDetail(record)">详情</a-button>
              <a-button type="link" @click="openTest(record)" v-privilege="'mcp:tool:test'">测试</a-button>
              <a-button
                type="link"
                @click="openReview(record)"
                v-privilege="'mcp:tool:review'"
              >
                审核
              </a-button>
              <a-button
                v-if="record.sourceType === 'PLATFORM_API' && record.syncAvailable"
                type="link"
                @click="syncTool(record)"
                v-privilege="'mcp:tool:save'"
              >
                同步版本
              </a-button>
              <a-popconfirm
                :title="record.enabledStatus === 'ENABLED' ? '确认停用该工具？' : '确认启用该工具？'"
                @confirm="toggleStatus(record)"
              >
                <a-button
                  type="link"
                  :danger="record.enabledStatus === 'ENABLED'"
                  v-privilege="'mcp:tool:save'"
                >
                  {{ record.enabledStatus === 'ENABLED' ? '停用' : '启用' }}
                </a-button>
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

    <a-drawer v-model:open="externalDrawer.open" title="登记第三方HTTP工具" width="min(680px, 100vw)">
      <a-alert
        type="info"
        show-icon
        message="该工具归属所选应用，使用现有 App ID、App Secret 和 Access Token，不创建第二套 MCP 凭证。"
        style="margin-bottom: 18px"
      />
      <a-form layout="vertical">
        <a-form-item label="所属应用" required>
          <a-select
            v-model:value="externalForm.applicationId"
            show-search
            option-filter-prop="label"
            placeholder="请选择当前可管理的应用"
            :options="applicationOptions"
          />
        </a-form-item>
        <div class="mcp-form-grid">
          <a-form-item label="工具名称" required>
            <a-input v-model:value="externalForm.toolName" :maxlength="100" />
          </a-form-item>
          <a-form-item label="工具编码" required extra="以字母开头，仅支持字母、数字和下划线">
            <a-input v-model:value="externalForm.toolCode" :maxlength="100" />
          </a-form-item>
          <a-form-item label="工具类型" required>
            <a-radio-group v-model:value="externalForm.toolType" @change="syncExternalSafety">
              <a-radio value="QUERY">查询工具</a-radio>
              <a-radio value="ACTION">操作工具</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="风险等级" required>
            <a-select v-model:value="externalForm.riskLevel">
              <a-select-option value="LOW">低风险</a-select-option>
              <a-select-option value="MEDIUM">中风险</a-select-option>
              <a-select-option value="HIGH">高风险</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="HTTP方法" required>
            <a-select v-model:value="externalForm.httpMethod">
              <a-select-option v-for="method in methods" :key="method" :value="method">{{ method }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="超时时间" required>
            <a-input-number v-model:value="externalForm.timeoutSeconds" :min="1" :max="60" addon-after="秒" style="width: 100%" />
          </a-form-item>
        </div>
        <a-form-item label="回调地址" required>
          <a-input v-model:value="externalForm.callbackUrl" placeholder="https://service.example.com/open-api/nexora/tool" />
        </a-form-item>
        <a-form-item label="工具说明" required>
          <a-textarea v-model:value="externalForm.description" :rows="4" :maxlength="2000" show-count />
        </a-form-item>
        <a-form-item label="调用确认">
          <a-switch v-model:checked="externalForm.requireConfirmation" :disabled="externalForm.toolType === 'ACTION'" />
          <span class="mcp-inline-help">操作类工具固定需要用户确认</span>
        </a-form-item>
        <a-form-item label="输入Schema" required>
          <a-textarea v-model:value="externalForm.inputSchema" class="mcp-json-input" :rows="10" />
        </a-form-item>
        <a-form-item label="输出Schema">
          <a-textarea v-model:value="externalForm.outputSchema" class="mcp-json-input" :rows="8" />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="publish-tool-footer">
          <a-button @click="externalDrawer.open = false">取消</a-button>
          <a-button type="primary" :loading="externalDrawer.saving" @click="saveExternal">登记并提交审核</a-button>
        </div>
      </template>
    </a-drawer>

    <a-drawer v-model:open="detailDrawer.open" title="MCP工具详情" width="min(760px, 100vw)">
      <a-spin :spinning="detailDrawer.loading">
        <template v-if="detailDrawer.record">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="工具名称">{{ detailDrawer.record.toolName }}</a-descriptions-item>
            <a-descriptions-item label="工具编码">{{ detailDrawer.record.toolCode }}</a-descriptions-item>
            <a-descriptions-item label="工具来源">
              {{ detailDrawer.record.sourceType === 'PLATFORM_API' ? '平台API' : '第三方HTTP' }}
            </a-descriptions-item>
            <a-descriptions-item label="来源对象">{{ detailDrawer.record.sourceName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="风险等级">{{ riskMeta(detailDrawer.record.riskLevel).text }}</a-descriptions-item>
            <a-descriptions-item label="确认策略">
              {{ detailDrawer.record.confirmationPolicy === 'REQUIRED' ? '调用前确认' : '自动执行' }}
            </a-descriptions-item>
            <a-descriptions-item label="HTTP方法">{{ detailDrawer.record.httpMethod }}</a-descriptions-item>
            <a-descriptions-item label="超时时间">{{ detailDrawer.record.timeoutSeconds }} 秒</a-descriptions-item>
            <a-descriptions-item label="回调地址" :span="2">
              {{ detailDrawer.record.callbackUrl || '由平台 API 版本配置继承' }}
            </a-descriptions-item>
            <a-descriptions-item label="工具说明" :span="2">{{ detailDrawer.record.description }}</a-descriptions-item>
          </a-descriptions>
          <h3 class="mcp-schema-title">输入Schema</h3>
          <pre class="mcp-schema">{{ prettyJson(detailDrawer.record.inputSchema) }}</pre>
          <h3 class="mcp-schema-title">输出Schema</h3>
          <pre class="mcp-schema">{{ prettyJson(detailDrawer.record.outputSchema) }}</pre>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="testModal.open"
      :title="`测试工具：${testModal.record?.toolName || ''}`"
      ok-text="执行测试"
      cancel-text="取消"
      :confirm-loading="testModal.loading"
      width="720px"
      @ok="submitTest"
    >
      <a-alert
        v-if="testModal.record?.toolType === 'ACTION'"
        type="warning"
        show-icon
        message="这是操作类工具。点击执行测试即视为管理员明确确认，请使用测试数据。"
        style="margin-bottom: 16px"
      />
      <a-textarea v-model:value="testModal.argumentsJson" class="mcp-json-input" :rows="12" />
      <div v-if="testModal.result" class="mcp-test-result">
        <a-tag :color="testModal.result.status === 'SUCCESS' ? 'green' : 'red'">{{ testModal.result.status }}</a-tag>
        <span>{{ testModal.result.message }}</span>
        <pre class="mcp-schema">{{ prettyJson(testModal.result.result) }}</pre>
      </div>
    </a-modal>

    <a-modal
      v-model:open="reviewModal.open"
      :title="`审核工具：${reviewModal.record?.toolName || ''}`"
      ok-text="提交审核结果"
      cancel-text="取消"
      :confirm-loading="reviewModal.loading"
      @ok="submitReview"
    >
      <a-form layout="vertical">
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="reviewModal.auditStatus">
            <a-radio value="APPROVED">通过</a-radio>
            <a-radio value="REJECTED">驳回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="最终风险等级" required>
          <a-radio-group v-model:value="reviewModal.riskLevel">
            <a-radio value="LOW">低</a-radio>
            <a-radio value="MEDIUM">中</a-radio>
            <a-radio value="HIGH">高</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="最终确认策略" required>
          <a-radio-group
            v-model:value="reviewModal.confirmationPolicy"
            :disabled="reviewModal.record?.toolType === 'ACTION'"
          >
            <a-radio value="AUTO">自动执行</a-radio>
            <a-radio value="REQUIRED">调用前确认</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见">
          <a-textarea v-model:value="reviewModal.remark" :rows="4" :maxlength="1000" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="guideOpen" title="第三方HTTP工具接入" :footer="null" width="760px">
      <a-steps
        direction="vertical"
        :items="[
          { title: '获取 Access Token', description: '使用现有 App ID 与 App Secret 调用 /open-api/oauth/token。' },
          { title: '实现业务回调', description: '接收 toolCode、arguments 和服务端生成的安全上下文。' },
          { title: '登记工具', description: '携带 Bearer Token 调用 /open-api/v1/ai-tools，应用归属由 Token 确定。' },
          { title: '测试并审核', description: '在本页执行真实连接测试，审核通过后才能供助手选择。' },
        ]"
      />
    </a-modal>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import {
    ApiOutlined,
    ClockCircleOutlined,
    FileTextOutlined,
    GlobalOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    ToolOutlined,
  } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { mcpToolApi } from '/@/api/business/open-api/mcp-tool-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const route = useRoute();
  const router = useRouter();
  const loading = ref(false);
  const rows = ref([]);
  const total = ref(0);
  const applications = ref([]);
  const guideOpen = ref(false);
  const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
  const summary = reactive({ total: 0, platformApi: 0, externalHttp: 0, pending: 0 });
  const queryForm = reactive({
    pageNum: 1,
    pageSize: 10,
    searchWord: '',
    sourceType: undefined,
    applicationId: undefined,
    riskLevel: undefined,
    auditStatus: undefined,
    enabledStatus: undefined,
  });
  const externalDrawer = reactive({ open: false, saving: false });
  const externalForm = reactive(defaultExternalForm());
  const detailDrawer = reactive({ open: false, loading: false, record: null });
  const testModal = reactive({ open: false, loading: false, record: null, argumentsJson: '{}', result: null });
  const reviewModal = reactive({
    open: false,
    loading: false,
    record: null,
    auditStatus: 'APPROVED',
    riskLevel: 'LOW',
    confirmationPolicy: 'AUTO',
    remark: '',
  });

  const summaryItems = computed(() => [
    { key: 'total', label: '工具总数', icon: ToolOutlined, iconClass: '' },
    { key: 'platformApi', label: '平台API工具', icon: ApiOutlined, iconClass: 'is-success' },
    { key: 'externalHttp', label: '第三方HTTP工具', icon: GlobalOutlined, iconClass: 'is-purple' },
    { key: 'pending', label: '待审核', icon: ClockCircleOutlined, iconClass: 'is-danger' },
  ]);
  const applicationOptions = computed(() => applications.value.map((item) => ({
    value: item.applicationId,
    label: `${item.applicationName}（${item.applicationCode}）`,
  })));
  const columns = [
    { title: '工具名称 / 编码', dataIndex: 'toolName', width: 260 },
    { title: '工具来源', dataIndex: 'sourceType', width: 130 },
    { title: '来源对象', dataIndex: 'sourceName', width: 210 },
    { title: '类型', dataIndex: 'toolType', width: 110 },
    { title: '风险等级', dataIndex: 'riskLevel', width: 110 },
    { title: '审核状态', dataIndex: 'auditStatus', width: 135 },
    { title: '工具状态', dataIndex: 'enabledStatus', width: 130 },
    { title: '关联助手', dataIndex: 'assistantCount', width: 110 },
    { title: '最近调用', dataIndex: 'lastCallTime', width: 190 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 330 },
  ];

  /** 返回登记第三方 HTTP 工具时使用的默认表单。 */
  function defaultExternalForm() {
    return {
      applicationId: undefined,
      toolName: '',
      toolCode: '',
      description: '',
      toolType: 'QUERY',
      callbackUrl: '',
      httpMethod: 'POST',
      contentType: 'application/json',
      timeoutSeconds: 30,
      riskLevel: 'LOW',
      requireConfirmation: false,
      inputSchema: JSON.stringify({
        type: 'object',
        properties: {
          keyword: { type: 'string', description: '查询关键字' },
        },
        required: ['keyword'],
        additionalProperties: false,
      }, null, 2),
      outputSchema: JSON.stringify({ type: 'object', properties: {} }, null, 2),
    };
  }

  /** 返回风险等级对应的中文文案和标签颜色。 */
  function riskMeta(value) {
    return {
      LOW: { color: 'green', text: '低' },
      MEDIUM: { color: 'orange', text: '中' },
      HIGH: { color: 'red', text: '高' },
    }[value] || { color: 'default', text: '未知' };
  }

  /** 返回审核状态对应的中文文案和徽标状态。 */
  function auditMeta(value) {
    return {
      DRAFT: { badge: 'default', text: '草稿' },
      PENDING: { badge: 'warning', text: '待审核' },
      APPROVED: { badge: 'success', text: '已通过' },
      REJECTED: { badge: 'error', text: '已驳回' },
    }[value] || { badge: 'default', text: '未知' };
  }

  /** 将第三方工具上报的在线状态转换为中文。 */
  function onlineText(value) {
    return { ONLINE: '在线', OFFLINE: '离线', ABNORMAL: '异常', UNKNOWN: '未知' }[value] || '未知';
  }

  /** 按中文数字格式展示调用次数。 */
  function formatNumber(value) {
    return Number(value || 0).toLocaleString('zh-CN');
  }

  /** 将 Schema、参数或结果格式化为便于查看的 JSON 文本。 */
  function prettyJson(value) {
    if (!value) return '-';
    try {
      return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2);
    } catch {
      return String(value);
    }
  }

  /** 按筛选条件分页加载工具列表。 */
  async function queryData() {
    loading.value = true;
    try {
      const response = await mcpToolApi.query(queryForm);
      rows.value = response.data?.list || [];
      total.value = response.data?.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  /** 加载统计卡片和当前用户可管理的应用选项。 */
  async function loadMeta() {
    try {
      const [summaryResponse, applicationsResponse] = await Promise.all([
        mcpToolApi.summary(),
        mcpToolApi.applications(),
      ]);
      Object.assign(summary, summaryResponse.data || {});
      applications.value = applicationsResponse.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 重置全部工具筛选条件并重新查询。 */
  function resetQuery() {
    Object.assign(queryForm, {
      pageNum: 1,
      pageSize: 10,
      searchWord: '',
      sourceType: undefined,
      applicationId: undefined,
      riskLevel: undefined,
      auditStatus: undefined,
      enabledStatus: undefined,
    });
    queryData();
  }

  /** 打开第三方 HTTP 工具登记抽屉并重置表单。 */
  function openExternalDrawer() {
    Object.assign(externalForm, defaultExternalForm());
    externalDrawer.open = true;
  }

  /** 操作类工具强制开启确认，并至少按中风险处理。 */
  function syncExternalSafety() {
    if (externalForm.toolType === 'ACTION') {
      externalForm.requireConfirmation = true;
      if (externalForm.riskLevel === 'LOW') externalForm.riskLevel = 'MEDIUM';
    }
  }

  /** 校验 JSON 文本并返回解析结果。 */
  function validateJson(value, label) {
    try {
      return JSON.parse(value || '{}');
    } catch {
      message.warning(`${label}必须是有效 JSON`);
      return null;
    }
  }

  /** 保存第三方 HTTP 工具并进入统一审核流程。 */
  async function saveExternal() {
    if (!externalForm.applicationId || !externalForm.toolName.trim() || !externalForm.description.trim()) {
      message.warning('请完整填写所属应用、工具名称和工具说明');
      return;
    }
    if (!/^[A-Za-z][A-Za-z0-9_]{2,99}$/.test(externalForm.toolCode)) {
      message.warning('工具编码需以字母开头，仅支持字母、数字和下划线');
      return;
    }
    if (!validateJson(externalForm.inputSchema, '输入Schema') || !validateJson(externalForm.outputSchema, '输出Schema')) {
      return;
    }
    externalDrawer.saving = true;
    try {
      await mcpToolApi.saveExternal({ ...externalForm });
      message.success('第三方 HTTP 工具已登记并进入待审核状态');
      externalDrawer.open = false;
      await Promise.all([queryData(), loadMeta()]);
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      externalDrawer.saving = false;
    }
  }

  /** 打开工具详情并把工具编号写入路由，支持刷新后恢复。 */
  async function openDetail(record) {
    detailDrawer.open = true;
    detailDrawer.loading = true;
    try {
      const response = await mcpToolApi.detail(record.toolId);
      detailDrawer.record = response.data;
      await router.replace({ path: route.path, query: { ...route.query, toolId: record.toolId } });
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      detailDrawer.loading = false;
    }
  }

  /** 根据输入 Schema 生成可直接编辑的测试参数样例。 */
  function exampleArguments(record) {
    try {
      const schema = JSON.parse(record.inputSchema || '{}');
      return Object.fromEntries(Object.entries(schema.properties || {}).map(([name, property]) => {
        const value = property.example ?? property.default;
        if (value !== undefined) return [name, value];
        if (property.type === 'number' || property.type === 'integer') return [name, 0];
        if (property.type === 'boolean') return [name, false];
        if (property.type === 'array') return [name, []];
        if (property.type === 'object') return [name, {}];
        return [name, ''];
      }));
    } catch {
      return {};
    }
  }

  /** 打开工具测试窗口并填充示例参数。 */
  function openTest(record) {
    Object.assign(testModal, {
      open: true,
      loading: false,
      record,
      argumentsJson: JSON.stringify(exampleArguments(record), null, 2),
      result: null,
    });
  }

  /** 执行真实工具测试并展示完整调用结果。 */
  async function submitTest() {
    const argumentsValue = validateJson(testModal.argumentsJson, '测试参数');
    if (!argumentsValue) return;
    testModal.loading = true;
    try {
      const response = await mcpToolApi.test({ toolId: testModal.record.toolId, arguments: argumentsValue });
      testModal.result = response.data;
      message.success('连接测试完成');
      await queryData();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      testModal.loading = false;
    }
  }

  /** 打开审核窗口并回填当前风险与确认策略。 */
  function openReview(record) {
    Object.assign(reviewModal, {
      open: true,
      loading: false,
      record,
      auditStatus: record.auditStatus === 'REJECTED' ? 'REJECTED' : 'APPROVED',
      riskLevel: record.riskLevel || 'LOW',
      confirmationPolicy: record.toolType === 'ACTION' ? 'REQUIRED' : record.confirmationPolicy || 'AUTO',
      remark: record.auditRemark || '',
    });
  }

  /** 提交审核结论，服务端会再次执行安全规则校验。 */
  async function submitReview() {
    reviewModal.loading = true;
    try {
      await mcpToolApi.review({
        toolId: reviewModal.record.toolId,
        auditStatus: reviewModal.auditStatus,
        riskLevel: reviewModal.riskLevel,
        confirmationPolicy: reviewModal.record.toolType === 'ACTION'
          ? 'REQUIRED'
          : reviewModal.confirmationPolicy,
        remark: reviewModal.remark,
      });
      message.success(reviewModal.auditStatus === 'APPROVED' ? '工具审核已通过并启用' : '工具已驳回');
      reviewModal.open = false;
      await Promise.all([queryData(), loadMeta()]);
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      reviewModal.loading = false;
    }
  }

  /** 启用或停用工具，不改变助手绑定关系。 */
  async function toggleStatus(record) {
    try {
      const enabledStatus = record.enabledStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED';
      await mcpToolApi.updateStatus({ toolId: record.toolId, enabledStatus });
      message.success(enabledStatus === 'ENABLED' ? '工具已启用' : '工具已停用');
      await queryData();
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 将平台 API 工具同步到最新发布版本，助手关系保持不变。 */
  async function syncTool(record) {
    try {
      await mcpToolApi.syncPlatform(record.toolId);
      message.success('Schema 已同步到 API 最新发布版本，助手关联保持不变');
      await queryData();
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 展示 HTTP 工具接入和鉴权说明。 */
  function showAccessGuide() {
    guideOpen.value = true;
  }

  /** 初始化列表、统计信息，并恢复路由中指定的工具详情。 */
  async function initialize() {
    await Promise.all([queryData(), loadMeta()]);
    const toolId = Number(route.query.toolId);
    if (toolId) {
      const record = rows.value.find((item) => item.toolId === toolId) || { toolId };
      await openDetail(record);
    }
  }

  onMounted(initialize);
</script>

<style scoped>
  .mcp-tool-query {
    display: grid;
    grid-template-columns: 1.2fr repeat(5, minmax(150px, 0.8fr)) auto;
    gap: 14px;
    align-items: end;
  }

  .mcp-tool-query .ant-select {
    width: 100%;
  }

  .mcp-form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 18px;
  }

  .mcp-json-input textarea,
  .mcp-schema {
    font-family: Consolas, Monaco, monospace;
  }

  .mcp-inline-help {
    margin-left: 10px;
    color: #7a8599;
  }

  .mcp-schema-title {
    margin: 20px 0 8px;
    font-size: 15px;
  }

  .mcp-schema {
    max-height: 320px;
    margin: 0;
    padding: 14px;
    overflow: auto;
    color: #d8e5f5;
    background: #172033;
    border-radius: 6px;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .mcp-test-result {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #e8edf3;
  }

  .mcp-test-result .mcp-schema {
    margin-top: 12px;
  }

  .publish-tool-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }

  @media (max-width: 1400px) {
    .mcp-tool-query {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .mcp-tool-query,
    .mcp-form-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
