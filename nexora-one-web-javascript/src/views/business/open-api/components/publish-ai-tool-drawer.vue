<template>
  <a-drawer
    :open="open"
    title="发布为AI工具"
    width="min(640px, 100vw)"
    :destroy-on-close="true"
    @update:open="emit('update:open', $event)"
  >
    <a-alert
      type="info"
      show-icon
      message="入参、出参和版本将从当前线上 API 自动生成；后续新版本只提示手动同步，不会改变正在使用的助手。"
      style="margin-bottom: 18px"
    />
    <div class="publish-tool-source">
      <div>
        <span>来源API</span>
        <strong>{{ api?.apiName }}</strong>
        <a-tag color="green">已上架</a-tag>
      </div>
      <div>
        <span>API编码</span>
        <strong>{{ api?.apiCode }}</strong>
        <span>版本 {{ schemaPreview.versionNo || api?.apiVersion || '-' }}</span>
      </div>
      <p>{{ api?.description }}</p>
    </div>

    <a-form layout="vertical">
      <a-form-item label="工具名称" required>
        <a-input v-model:value="form.toolName" :maxlength="100" show-count />
      </a-form-item>
      <a-form-item label="工具编码" required extra="仅支持字母、数字和下划线，创建后应保持稳定">
        <a-input v-model:value="form.toolCode" :maxlength="100" show-count />
      </a-form-item>
      <a-form-item
        label="工具说明"
        required
        extra="请清晰描述适用场景、返回内容和关键限制，供模型选择工具。"
      >
        <a-textarea v-model:value="form.description" :rows="5" :maxlength="2000" show-count />
      </a-form-item>
      <a-form-item label="工具类型" required>
        <a-radio-group v-model:value="form.toolType" @change="syncSafetyDefaults">
          <a-radio value="QUERY">查询工具</a-radio>
          <a-radio value="ACTION">操作工具</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="风险等级" required>
        <a-radio-group v-model:value="form.riskLevel">
          <a-radio value="LOW">低</a-radio>
          <a-radio value="MEDIUM">中</a-radio>
          <a-radio value="HIGH">高</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="调用确认" required>
        <a-radio-group v-model:value="form.confirmationPolicy" :disabled="form.toolType === 'ACTION'">
          <a-radio value="AUTO">自动执行</a-radio>
          <a-radio value="REQUIRED">调用前确认</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>

    <a-spin :spinning="previewLoading">
      <a-collapse v-model:active-key="schemaPanels" class="publish-tool-schema">
        <a-collapse-panel key="input" header="输入Schema（自动生成）">
          <pre>{{ prettyJson(schemaPreview.inputSchema) }}</pre>
        </a-collapse-panel>
        <a-collapse-panel key="output" header="输出Schema（自动生成）">
          <pre>{{ prettyJson(schemaPreview.outputSchema) }}</pre>
        </a-collapse-panel>
      </a-collapse>
    </a-spin>

    <template #footer>
      <div class="publish-tool-footer">
        <a-button @click="emit('update:open', false)">取消</a-button>
        <a-button :loading="submitting" @click="submit(false)">保存草稿</a-button>
        <a-button type="primary" :loading="submitting" @click="submit(true)">提交审核</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
  import { reactive, ref, watch } from 'vue';
  import { message } from 'ant-design-vue';
  import { mcpToolApi } from '/@/api/business/open-api/mcp-tool-api';
  import { nexoraSentry } from '/@/lib/nexora-sentry';

  const props = defineProps({
    open: Boolean,
    api: { type: Object, default: () => ({}) },
  });
  const emit = defineEmits(['update:open', 'published']);
  const submitting = ref(false);
  const previewLoading = ref(false);
  const schemaPanels = ref([]);
  const schemaPreview = reactive({
    versionId: null,
    versionNo: '',
    inputSchema: '',
    outputSchema: '',
  });
  const form = reactive({
    toolName: '',
    toolCode: '',
    description: '',
    toolType: 'QUERY',
    riskLevel: 'LOW',
    confirmationPolicy: 'AUTO',
  });

  /** 将 API 编码转换为模型工具允许使用的稳定编码。 */
  function normalizeCode(value) {
    let code = String(value || '').replace(/[^A-Za-z0-9_]/g, '_');
    if (!/^[A-Za-z]/.test(code)) code = `tool_${code}`;
    return code.slice(0, 100);
  }

  /** 根据 API 方法填充初始工具类型和默认安全策略。 */
  function resetForm() {
    const queryTool = String(props.api?.requestMethod || '').toUpperCase() === 'GET';
    Object.assign(form, {
      toolName: props.api?.apiName || '',
      toolCode: normalizeCode(props.api?.apiCode),
      description: props.api?.description || '',
      toolType: queryTool ? 'QUERY' : 'ACTION',
      riskLevel: queryTool ? 'LOW' : 'MEDIUM',
      confirmationPolicy: queryTool ? 'AUTO' : 'REQUIRED',
    });
    Object.assign(schemaPreview, {
      versionId: null,
      versionNo: '',
      inputSchema: '',
      outputSchema: '',
    });
    schemaPanels.value = [];
  }

  /** 操作类工具强制使用确认策略，并至少按中风险处理。 */
  function syncSafetyDefaults() {
    if (form.toolType === 'ACTION') {
      form.confirmationPolicy = 'REQUIRED';
      if (form.riskLevel === 'LOW') form.riskLevel = 'MEDIUM';
    }
  }

  /** 从后端读取真正将被继承的线上版本和 JSON Schema。 */
  async function loadSchemaPreview() {
    if (!props.api?.openApiId) return;
    previewLoading.value = true;
    try {
      const response = await mcpToolApi.platformPreview(props.api.openApiId);
      Object.assign(schemaPreview, response.data || {});
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      previewLoading.value = false;
    }
  }

  /** 将 Schema 字符串格式化为便于审核的 JSON 文本。 */
  function prettyJson(value) {
    if (!value) return '暂无 Schema';
    try {
      return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2);
    } catch {
      return String(value);
    }
  }

  /** 保存平台 API 工具草稿，或直接提交到统一工具审核流程。 */
  async function submit(submitReview) {
    if (!form.toolName.trim() || !form.description.trim()) {
      message.warning('请完整填写工具名称和工具说明');
      return;
    }
    if (!/^[A-Za-z][A-Za-z0-9_]{2,99}$/.test(form.toolCode)) {
      message.warning('工具编码需以字母开头，仅支持字母、数字和下划线');
      return;
    }
    submitting.value = true;
    try {
      const response = await mcpToolApi.publishPlatform({
        openApiId: props.api.openApiId,
        ...form,
        submitReview,
      });
      message.success(submitReview ? '已提交AI工具审核' : 'AI工具草稿已保存');
      emit('update:open', false);
      emit('published', response.data);
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      submitting.value = false;
    }
  }

  watch(
    () => props.open,
    (value) => {
      if (value) {
        resetForm();
        loadSchemaPreview();
      }
    },
  );
</script>

<style scoped>
  .publish-tool-source {
    margin-bottom: 20px;
    padding: 14px 16px;
    background: #f7faff;
    border: 1px solid #cfe1ff;
    border-radius: 6px;
  }

  .publish-tool-source > div {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
  }

  .publish-tool-source span {
    color: #667085;
  }

  .publish-tool-source p {
    margin: 4px 0 0;
    color: #667085;
    line-height: 1.65;
  }

  .publish-tool-schema {
    margin-bottom: 8px;
  }

  .publish-tool-schema pre {
    max-height: 300px;
    margin: 0;
    padding: 14px;
    overflow: auto;
    color: #d8e5f5;
    font: 13px/1.6 Consolas, Monaco, monospace;
    background: #172033;
    border-radius: 6px;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .publish-tool-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
</style>
