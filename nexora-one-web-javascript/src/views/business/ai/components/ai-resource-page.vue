<template>
  <div class="ai-page">
    <header class="ai-page__header">
      <div>
        <h1 class="ai-page__title">{{ meta.title }}</h1>
        <div class="ai-page__subtitle">{{ meta.subtitle }}</div>
      </div>
      <a-button v-privilege="permission.save" type="primary" size="large" @click="openEditor()">
        <template #icon><PlusOutlined /></template>
        {{ meta.addText }}
      </a-button>
    </header>

    <section class="ai-summary">
      <div v-for="item in summaryItems" :key="item.key" class="ai-summary__item">
        <span class="ai-summary__icon"><component :is="item.icon" /></span>
        <div>
          <div class="ai-summary__label">{{ item.label }}</div>
          <div class="ai-summary__value">{{ summary[item.key] ?? 0 }}</div>
        </div>
      </div>
    </section>

    <section class="ai-panel">
      <div class="ai-query">
        <div>
          <label class="ai-query__label">名称 / 编码</label>
          <a-input v-model:value="queryForm.keyword" allow-clear placeholder="请输入搜索内容" @pressEnter="loadData" />
        </div>
        <div>
          <label class="ai-query__label">类型</label>
          <a-select v-model:value="queryForm.type" allow-clear style="width: 100%" placeholder="请选择类型">
            <a-select-option v-for="item in meta.typeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </div>
        <div v-if="resource === 'model'">
          <label class="ai-query__label">所属服务</label>
          <a-select v-model:value="queryForm.serviceId" allow-clear style="width: 100%" placeholder="请选择模型服务">
            <a-select-option v-for="item in services" :key="item.serviceId" :value="item.serviceId">
              {{ item.serviceName }}
            </a-select-option>
          </a-select>
        </div>
        <div>
          <label class="ai-query__label">启用状态</label>
          <a-select v-model:value="queryForm.enabledFlag" allow-clear style="width: 100%" placeholder="请选择状态">
            <a-select-option :value="true">已启用</a-select-option>
            <a-select-option :value="false">已停用</a-select-option>
          </a-select>
        </div>
        <a-space>
          <a-button type="primary" @click="loadData"><SearchOutlined />查询</a-button>
          <a-button @click="resetQuery"><ReloadOutlined />重置</a-button>
        </a-space>
      </div>
    </section>

    <section class="ai-panel">
      <a-table :loading="loading" :data-source="rows" :columns="meta.columns" :row-key="meta.rowKey" :pagination="false"
               :scroll="{ x: meta.tableWidth }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'enabledFlag'">
            <span :class="['ai-status', record.enabledFlag ? 'ai-status--success' : '']">
              {{ record.enabledFlag ? '启用' : '停用' }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'connectionStatus'">
            <span :class="['ai-status', connectionClass(record.connectionStatus)]">
              {{ connectionText(record.connectionStatus) }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'defaultFlag'">
            <a-tag :color="record.defaultFlag ? 'blue' : 'default'">{{ record.defaultFlag ? '默认' : '否' }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'capabilities'">
            <a-space :size="4" wrap>
              <a-tag v-for="item in splitCapabilities(record.capabilities)" :key="item" color="blue">{{ capabilityText(item) }}</a-tag>
              <span v-if="!record.capabilities" class="ai-muted">-</span>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'serviceId'">
            {{ serviceName(record.serviceId) }}
          </template>
          <template v-else-if="column.dataIndex === 'contextLength'">
            {{ record.modelType === 'EMBEDDING' ? `${record.embeddingDimension || '-'} 维` : `${record.contextLength || '-'} Token` }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <div class="ai-actions">
              <a-button v-privilege="permission.save" type="link" @click="openEditor(record)">编辑</a-button>
              <a-button v-if="resource !== 'model'" v-privilege="permission.test" type="link"
                        :loading="testingId === record[meta.rowKey]" @click="runTest(record)">
                {{ resource === 'vector' ? '同步集合' : '测试连接' }}
              </a-button>
              <a-button v-else-if="record.modelType !== 'RERANK'" v-privilege="permission.test" type="link" @click="openDebug(record)">调试</a-button>
              <span v-else class="ai-page__subtitle">需兼容网关</span>
              <a-popconfirm title="确认删除该配置？" @confirm="remove(record)">
                <a-button v-privilege="permission.delete" type="link" danger>删除</a-button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
      <div class="ai-pagination">
        <a-pagination v-model:current="queryForm.pageNum" v-model:page-size="queryForm.pageSize"
                      show-size-changer :total="total" @change="loadData" />
      </div>
    </section>

    <a-drawer v-model:open="editor.open" :title="editorTitle" width="480" :destroy-on-close="true">
      <a-form ref="formRef" :model="editor.form" :rules="rules" layout="vertical">
        <template v-if="resource === 'service'">
          <a-form-item label="服务名称" name="serviceName"><a-input v-model:value="editor.form.serviceName" /></a-form-item>
          <a-form-item label="服务商" name="providerType">
            <a-select v-model:value="editor.form.providerType">
              <a-select-option value="DEEPSEEK">DeepSeek</a-select-option>
              <a-select-option value="OPENAI">OpenAI</a-select-option>
              <a-select-option value="QWEN">通义千问</a-select-option>
              <a-select-option value="CUSTOM">自定义</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="接口协议" name="protocolType">
            <a-select v-model:value="editor.form.protocolType"><a-select-option value="OPENAI">OpenAI 兼容</a-select-option></a-select>
          </a-form-item>
          <a-form-item label="Base URL" name="baseUrl"><a-input v-model:value="editor.form.baseUrl" /></a-form-item>
          <a-form-item label="API Key" name="apiKey"><a-input-password v-model:value="editor.form.apiKey" /></a-form-item>
          <a-form-item label="组织 ID"><a-input v-model:value="editor.form.organizationId" /></a-form-item>
          <a-form-item label="支持能力">
            <a-checkbox-group v-model:value="editor.form.capabilities" :options="capabilityOptions" />
          </a-form-item>
          <a-form-item label="请求超时（秒）"><a-input-number v-model:value="editor.form.requestTimeoutSeconds" :min="1" :max="600" style="width: 100%" /></a-form-item>
        </template>

        <template v-else-if="resource === 'model'">
          <a-form-item label="模型名称" name="modelName"><a-input v-model:value="editor.form.modelName" /></a-form-item>
          <a-form-item label="模型编码" name="modelCode"><a-input v-model:value="editor.form.modelCode" /></a-form-item>
          <a-form-item label="模型类型" name="modelType">
            <a-select v-model:value="editor.form.modelType">
              <a-select-option value="CHAT">对话模型</a-select-option>
              <a-select-option value="EMBEDDING">向量模型</a-select-option>
              <a-select-option value="RERANK">重排序模型</a-select-option>
              <a-select-option value="MULTIMODAL">多模态模型</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="所属服务" name="serviceId">
            <a-select v-model:value="editor.form.serviceId">
              <a-select-option v-for="item in services" :key="item.serviceId" :value="item.serviceId">{{ item.serviceName }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="支持能力">
            <a-checkbox-group v-model:value="editor.form.capabilities" :options="capabilityOptions" />
          </a-form-item>
          <a-form-item label="上下文长度"><a-input-number v-model:value="editor.form.contextLength" :min="1" style="width: 100%" /></a-form-item>
          <a-form-item v-if="editor.form.modelType === 'EMBEDDING'" label="向量维度"><a-input-number v-model:value="editor.form.embeddingDimension" :min="1" style="width: 100%" /></a-form-item>
          <a-form-item label="温度"><a-input-number v-model:value="editor.form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" /></a-form-item>
          <a-form-item label="最大输出 Token"><a-input-number v-model:value="editor.form.maxOutputTokens" :min="1" style="width: 100%" /></a-form-item>
          <div class="ai-grid-2">
            <a-form-item label="输入价格 / 百万 Token"><a-input-number v-model:value="editor.form.inputPrice" :min="0" style="width: 100%" /></a-form-item>
            <a-form-item label="输出价格 / 百万 Token"><a-input-number v-model:value="editor.form.outputPrice" :min="0" style="width: 100%" /></a-form-item>
          </div>
        </template>

        <template v-else>
          <a-form-item label="实例名称" name="instanceName"><a-input v-model:value="editor.form.instanceName" /></a-form-item>
          <a-form-item label="数据库类型" name="databaseType">
            <a-select v-model:value="editor.form.databaseType"><a-select-option value="QDRANT">Qdrant</a-select-option></a-select>
          </a-form-item>
          <a-form-item label="服务地址" name="serviceUrl"><a-input v-model:value="editor.form.serviceUrl" /></a-form-item>
          <a-form-item label="API Key"><a-input-password v-model:value="editor.form.apiKey" /></a-form-item>
          <a-form-item label="集合前缀" name="collectionPrefix"><a-input v-model:value="editor.form.collectionPrefix" /></a-form-item>
          <a-form-item label="请求超时（秒）"><a-input-number v-model:value="editor.form.requestTimeoutSeconds" :min="1" style="width: 100%" /></a-form-item>
          <a-form-item label="TLS 安全连接"><a-switch v-model:checked="editor.form.tlsEnabled" /></a-form-item>
        </template>
        <a-form-item label="设为默认"><a-switch v-model:checked="editor.form.defaultFlag" /></a-form-item>
        <a-form-item label="启用"><a-switch v-model:checked="editor.form.enabledFlag" /></a-form-item>
      </a-form>
      <template #footer>
        <div style="text-align: right">
          <a-space><a-button @click="editor.open = false">取消</a-button><a-button v-privilege="permission.save" type="primary" :loading="saving" @click="save">保存</a-button></a-space>
        </div>
      </template>
    </a-drawer>

    <a-modal v-model:open="debug.open" width="900px" wrap-class-name="ai-debug-modal" :footer="null"
             :mask-closable="!debug.loading" :keyboard="!debug.loading" :closable="!debug.loading">
      <template #title>
        <div class="ai-debug__title">模型调试</div>
      </template>
      <div class="ai-debug__model">
        <DeploymentUnitOutlined class="ai-debug__model-icon" />
        <div class="ai-debug__model-info">
          <strong>{{ debug.modelName }}</strong>
          <span>{{ debug.modelCode }} · {{ serviceName(debug.serviceId) }}</span>
        </div>
        <a-tag color="blue">{{ debug.modelType === 'EMBEDDING' ? '向量模型' : '对话模型' }}</a-tag>
      </div>
      <div class="ai-debug__workspace">
        <section class="ai-debug__input">
          <div class="ai-debug__section-title">请求内容</div>
          <a-form layout="vertical" @submit.prevent="submitDebug">
            <a-form-item v-if="debug.modelType !== 'EMBEDDING'" label="系统提示词（可选）">
              <a-textarea v-model:value="debug.systemPrompt" :rows="3" :maxlength="5000"
                          placeholder="为本次调用设置角色或回答要求" />
            </a-form-item>
            <a-form-item :label="debug.modelType === 'EMBEDDING' ? '向量化文本' : '测试内容'" required>
              <a-textarea v-model:value="debug.prompt" :rows="9" :maxlength="20000" show-count
                          :placeholder="debug.modelType === 'EMBEDDING' ? '输入要向量化的文本' : '输入要发送给模型的内容'" />
            </a-form-item>
          </a-form>
        </section>
        <section class="ai-debug__output" aria-live="polite">
          <div class="ai-debug__output-header">
            <div class="ai-debug__section-title">调用结果</div>
            <a-tooltip v-if="debug.result?.content" title="复制响应内容">
              <a-button type="text" size="small" aria-label="复制响应内容" @click="copyDebugText(debug.result.content)">
                <template #icon><CopyOutlined /></template>
              </a-button>
            </a-tooltip>
          </div>
          <div v-if="debug.loading" class="ai-debug__empty"><a-spin /><span>正在等待模型响应</span></div>
          <div v-else-if="debug.error" class="ai-debug__empty ai-debug__empty--error">
            <CloseCircleOutlined /><span>{{ debug.error }}</span>
          </div>
          <template v-else-if="debug.result">
            <div class="ai-debug__response">{{ debug.result.content || '模型未返回文本内容' }}</div>
            <div v-if="debug.result.vectorPreview?.length" class="ai-debug__vector">
              <span>向量预览（前 {{ debug.result.vectorPreview.length }} 维）</span>
              <code>{{ debug.result.vectorPreview.join(', ') }}</code>
            </div>
            <div class="ai-debug__metrics">
              <div><span>输入 Token</span><strong>{{ debug.result.inputTokens ?? 0 }}</strong></div>
              <div><span>输出 Token</span><strong>{{ debug.result.outputTokens ?? 0 }}</strong></div>
              <div><span>{{ debug.result.dimension ? '向量维度' : '总 Token' }}</span>
                <strong>{{ debug.result.dimension || debug.result.totalTokens || 0 }}</strong></div>
            </div>
            <div v-if="debug.result.traceId" class="ai-debug__trace">
              <span>Trace ID</span>
              <code>{{ debug.result.traceId }}</code>
              <a-tooltip title="复制 Trace ID">
                <a-button type="text" size="small" aria-label="复制 Trace ID" @click="copyDebugText(debug.result.traceId)">
                  <template #icon><CopyOutlined /></template>
                </a-button>
              </a-tooltip>
            </div>
          </template>
          <div v-else class="ai-debug__empty"><ApiOutlined /><span>暂无调用结果</span></div>
        </section>
      </div>
      <div class="ai-debug__footer">
        <a-button :disabled="debug.loading" @click="debug.open = false">关闭</a-button>
        <a-button v-privilege="permission.test" type="primary" :loading="debug.loading" @click="submitDebug">
          <template #icon><SendOutlined /></template>
          {{ debug.result || debug.error ? '重新调试' : '开始调试' }}
        </a-button>
      </div>
    </a-modal>

    <a-modal v-model:open="collections.open" title="向量集合" :footer="null" width="700px">
      <a-table :data-source="collections.rows" :columns="collectionColumns" row-key="name" :pagination="false" />
    </a-modal>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import {
    ApiOutlined, CheckCircleOutlined, CloudServerOutlined, DatabaseOutlined, DeploymentUnitOutlined,
    CloseCircleOutlined, CopyOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SendOutlined,
  } from '@ant-design/icons-vue';
  import { aiPlatformApi } from '/@/api/business/ai/ai-platform-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import '../ai-platform.less';

  const props = defineProps({ resource: { type: String, required: true } });
  const resource = computed(() => props.resource);
  const loading = ref(false);
  const saving = ref(false);
  const testingId = ref(null);
  const rows = ref([]);
  const total = ref(0);
  const services = ref([]);
  const formRef = ref();
  const queryForm = reactive({ pageNum: 1, pageSize: 10, keyword: '', type: undefined, enabledFlag: undefined, serviceId: undefined });
  const summary = reactive({});
  const editor = reactive({ open: false, form: {} });
  const debug = reactive({
    open: false, loading: false, modelId: null, modelName: '', modelCode: '',
    modelType: '', serviceId: null, systemPrompt: '', prompt: '', result: null, error: '',
  });
  const collections = reactive({ open: false, rows: [] });
  const capabilityOptions = [
    { label: '对话', value: 'CHAT' },
    { label: '工具调用', value: 'TOOL' },
    { label: '视觉', value: 'VISION' },
    { label: '向量', value: 'EMBEDDING' },
    { label: '重排序', value: 'RERANK' },
  ];

  const configs = {
    service: {
      title: '模型服务', subtitle: '统一管理模型厂商及兼容 API 服务连接。', addText: '新增模型服务',
      rowKey: 'serviceId', tableWidth: 1200,
      typeOptions: [{ label: 'OpenAI 兼容', value: 'OPENAI' }],
      columns: [
        { title: '服务名称', dataIndex: 'serviceName', width: 180 }, { title: '服务商', dataIndex: 'providerType', width: 120 },
        { title: '接口协议', dataIndex: 'protocolType', width: 130 }, { title: 'Base URL', dataIndex: 'baseUrl', width: 260 },
        { title: '支持能力', dataIndex: 'capabilities', width: 190 }, { title: '连接状态', dataIndex: 'connectionStatus', width: 120 },
        { title: '默认服务', dataIndex: 'defaultFlag', width: 100 }, { title: '启用状态', dataIndex: 'enabledFlag', width: 100 },
        { title: '操作', dataIndex: 'action', fixed: 'right', width: 200 },
      ],
    },
    model: {
      title: '模型管理', subtitle: '维护平台可调用的对话、向量、重排序及多模态模型。', addText: '新增模型',
      rowKey: 'modelId', tableWidth: 1200,
      typeOptions: [{ label: '对话', value: 'CHAT' }, { label: '向量', value: 'EMBEDDING' }, { label: '重排序', value: 'RERANK' }, { label: '多模态', value: 'MULTIMODAL' }],
      columns: [
        { title: '模型名称', dataIndex: 'modelName', width: 180 }, { title: '模型编码', dataIndex: 'modelCode', width: 190 },
        { title: '模型类型', dataIndex: 'modelType', width: 120 }, { title: '所属服务', dataIndex: 'serviceId', width: 160 },
        { title: '支持能力', dataIndex: 'capabilities', width: 180 }, { title: '上下文 / 维度', dataIndex: 'contextLength', width: 130 },
        { title: '默认模型', dataIndex: 'defaultFlag', width: 100 }, { title: '状态', dataIndex: 'enabledFlag', width: 100 },
        { title: '操作', dataIndex: 'action', fixed: 'right', width: 180 },
      ],
    },
    vector: {
      title: '向量数据库', subtitle: '配置知识库向量存储服务并同步集合运行状态。', addText: '新增向量数据库',
      rowKey: 'vectorDatabaseId', tableWidth: 1120,
      typeOptions: [{ label: 'Qdrant', value: 'QDRANT' }],
      columns: [
        { title: '实例名称', dataIndex: 'instanceName', width: 180 }, { title: '类型', dataIndex: 'databaseType', width: 110 },
        { title: '服务地址', dataIndex: 'serviceUrl', width: 250 }, { title: '集合前缀', dataIndex: 'collectionPrefix', width: 150 },
        { title: '连接状态', dataIndex: 'connectionStatus', width: 120 }, { title: '集合数', dataIndex: 'collectionCount', width: 90 },
        { title: '默认实例', dataIndex: 'defaultFlag', width: 100 }, { title: '启用状态', dataIndex: 'enabledFlag', width: 100 },
        { title: '操作', dataIndex: 'action', fixed: 'right', width: 220 },
      ],
    },
  };
  const meta = computed(() => configs[resource.value]);
  const permission = computed(() => ({
    service: { save: 'ai:model-service:save', test: 'ai:model-service:test', delete: 'ai:model-service:delete' },
    model: { save: 'ai:model:save', test: 'ai:model:debug', delete: 'ai:model:delete' },
    vector: { save: 'ai:vector:save', test: 'ai:vector:test', delete: 'ai:vector:delete' },
  })[resource.value]);
  const collectionColumns = [
    { title: '集合名称', dataIndex: 'name' },
    { title: '向量数量', dataIndex: 'vectorCount', width: 140 },
    { title: '运行状态', dataIndex: 'status', width: 140 },
  ];
  const editorTitle = computed(() => `${editor.form[meta.value.rowKey] ? '编辑' : '新增'}${meta.value.title}`);
  const summaryItems = computed(() => resource.value === 'service'
    ? [{ key: 'total', label: '服务商总数', icon: CloudServerOutlined }, { key: 'enabled', label: '已启用', icon: CheckCircleOutlined }, { key: 'connected', label: '连接正常', icon: ApiOutlined }, { key: 'defaultName', label: '默认服务', icon: DeploymentUnitOutlined }]
    : resource.value === 'model'
      ? [{ key: 'total', label: '模型总数', icon: DeploymentUnitOutlined }, { key: 'chat', label: '对话模型', icon: ApiOutlined }, { key: 'embedding', label: '向量模型', icon: DatabaseOutlined }, { key: 'enabled', label: '启用模型', icon: CheckCircleOutlined }]
      : [{ key: 'total', label: '实例总数', icon: DatabaseOutlined }, { key: 'connected', label: '连接正常', icon: CheckCircleOutlined }, { key: 'collections', label: '集合数量', icon: DeploymentUnitOutlined }, { key: 'vectors', label: '向量总数', icon: ApiOutlined }]);

  const rules = computed(() => {
    const required = { required: true, message: '此项不能为空' };
    if (resource.value === 'service') return { serviceName: [required], providerType: [required], protocolType: [required], baseUrl: [required], apiKey: [required] };
    if (resource.value === 'model') return { modelName: [required], modelCode: [required], modelType: [required], serviceId: [required] };
    return { instanceName: [required], databaseType: [required], serviceUrl: [required], collectionPrefix: [required] };
  });

  /**
   * 创建不同资源的默认表单，保证新增与编辑使用一致的数据结构。
   */
  function defaultForm() {
    if (resource.value === 'service') return { providerType: 'DEEPSEEK', protocolType: 'OPENAI', apiKey: '', requestTimeoutSeconds: 60, capabilities: ['CHAT'], defaultFlag: false, enabledFlag: true };
    if (resource.value === 'model') return { modelType: 'CHAT', capabilities: [], contextLength: 8192, temperature: 0.7, maxOutputTokens: 2048, inputPrice: 0, outputPrice: 0, defaultFlag: false, enabledFlag: true };
    return { databaseType: 'QDRANT', serviceUrl: 'http://127.0.0.1:6333', apiKey: '', collectionPrefix: 'nexora_kb_', requestTimeoutSeconds: 30, tlsEnabled: false, defaultFlag: false, enabledFlag: true };
  }

  /**
   * 查询当前资源列表，并同步页面概览数据。
   */
  async function loadData() {
    loading.value = true;
    try {
      const api = resource.value === 'service' ? aiPlatformApi.queryServices : resource.value === 'model' ? aiPlatformApi.queryModels : aiPlatformApi.queryVectors;
      const response = await api(queryForm);
      rows.value = response.data.list || [];
      total.value = response.data.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 加载模型服务选项和对应资源统计。
   */
  async function loadMeta() {
    try {
      const tasks = resource.value === 'vector' ? [] : [aiPlatformApi.serviceOptions()];
      if (resource.value === 'service') tasks.push(aiPlatformApi.serviceSummary());
      if (resource.value === 'model') tasks.push(aiPlatformApi.modelSummary());
      if (resource.value === 'vector') tasks.push(aiPlatformApi.vectorSummary());
      const results = await Promise.all(tasks);
      if (resource.value !== 'vector') services.value = results[0].data || [];
      Object.assign(summary, results[resource.value === 'vector' ? 0 : 1].data || {});
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /**
   * 重置查询条件并返回第一页。
   */
  function resetQuery() {
    Object.assign(queryForm, { pageNum: 1, pageSize: 10, keyword: '', type: undefined, enabledFlag: undefined, serviceId: undefined });
    loadData();
  }

  /**
   * 打开资源编辑抽屉，并将后端逗号分隔能力转换为表单数组。
   */
  function openEditor(record) {
    editor.form = { ...defaultForm(), ...(record || {}) };
    editor.form.capabilities = Array.isArray(editor.form.capabilities)
      ? editor.form.capabilities
      : splitCapabilities(editor.form.capabilities);
    if (record?.apiKeyCipher) editor.form.apiKey = record.apiKeyCipher;
    editor.open = true;
  }

  /**
   * 校验并保存当前资源配置。
   */
  async function save() {
    await formRef.value.validate();
    saving.value = true;
    try {
      const api = resource.value === 'service' ? aiPlatformApi.saveService : resource.value === 'model' ? aiPlatformApi.saveModel : aiPlatformApi.saveVector;
      await api(editor.form);
      message.success('保存成功');
      editor.open = false;
      await Promise.all([loadData(), loadMeta()]);
    } finally {
      saving.value = false;
    }
  }

  /**
   * 测试模型服务连接或同步向量数据库集合。
   */
  async function runTest(record) {
    const id = record[meta.value.rowKey];
    testingId.value = id;
    try {
      const response = resource.value === 'service' ? await aiPlatformApi.testService(id) : await aiPlatformApi.syncVector(id);
      message.success(resource.value === 'service' ? `连接成功，发现 ${response.data.modelCount || 0} 个模型` : '集合同步成功');
      if (resource.value === 'vector') {
        collections.rows = response.data.collections || [];
        collections.open = true;
      }
      await Promise.all([loadData(), loadMeta()]);
    } finally {
      testingId.value = null;
    }
  }

  /**
   * 删除资源配置。
   */
  async function remove(record) {
    const id = record[meta.value.rowKey];
    const api = resource.value === 'service' ? aiPlatformApi.deleteService : resource.value === 'model' ? aiPlatformApi.deleteModel : aiPlatformApi.deleteVector;
    await api(id);
    message.success('删除成功');
    await Promise.all([loadData(), loadMeta()]);
  }

  /**
   * 打开模型调试窗口。
   */
  function openDebug(record) {
    Object.assign(debug, {
      open: true, loading: false, modelId: record.modelId, modelName: record.modelName,
      modelCode: record.modelCode, modelType: record.modelType, serviceId: record.serviceId,
      systemPrompt: '', prompt: '', result: null, error: '',
    });
  }

  /**
   * 发起真实模型调试调用，并展示调用链路信息。
   */
  async function submitDebug() {
    if (!debug.prompt.trim()) return message.warning('请输入测试内容');
    if (debug.loading) return;
    debug.loading = true;
    debug.result = null;
    debug.error = '';
    try {
      const response = await aiPlatformApi.debugModel({ modelId: debug.modelId, prompt: debug.prompt, systemPrompt: debug.systemPrompt, sourceName: '模型管理' });
      debug.result = response.data;
    } catch (error) {
      debug.error = error.response?.data?.msg || error.message || '模型调用失败，请检查服务配置和调用日志';
      smartSentry.captureError(error);
    } finally {
      debug.loading = false;
    }
  }

  /**
   * 复制模型响应或调用追踪编号。
   */
  async function copyDebugText(text) {
    try {
      await navigator.clipboard.writeText(text);
      message.success('复制成功');
    } catch (error) {
      message.error('复制失败，请检查浏览器剪贴板权限');
    }
  }

  function splitCapabilities(value) { return value ? value.split(',').filter(Boolean) : []; }
  function capabilityText(value) { return { CHAT: '对话', TOOL: '工具调用', VISION: '视觉', EMBEDDING: '向量', RERANK: '重排序' }[value] || value; }
  function serviceName(id) { return services.value.find((item) => item.serviceId === id)?.serviceName || id || '-'; }
  function connectionText(value) { return { CONNECTED: '连接正常', FAILED: '连接失败', UNTESTED: '未测试' }[value] || '未知'; }
  function connectionClass(value) { return value === 'CONNECTED' ? 'ai-status--success' : value === 'FAILED' ? 'ai-status--error' : 'ai-status--warning'; }

  onMounted(() => Promise.all([loadData(), loadMeta()]));
</script>
