<template>
  <div class="knowledge-page" :class="{ 'knowledge-page--chat': active }">
    <header v-if="!active" class="knowledge-header">
      <div>
        <h1>智能助手</h1>
        <p>关联知识库和 AI 工具，完成有来源、可审计的业务问答</p>
      </div>
      <a-button type="primary" @click="edit()"><PlusOutlined />新建助手</a-button>
    </header>

    <section v-if="!active" class="knowledge-panel">
      <a-table
        :data-source="rows"
        :loading="loading"
        :row-key="(row) => row.assistant.assistantId"
        :scroll="{ x: 980 }"
        :pagination="{ pageSize: 10 }"
      >
        <a-table-column title="助手" :width="190">
          <template #default="{ record }"
            ><strong>{{ record.assistant.assistantName }}</strong></template
          >
        </a-table-column>
        <a-table-column title="对话模型" :width="160">
          <template #default="{ record }">
            {{ models.find((model) => model.modelId === record.assistant.modelId)?.modelName || record.assistant.modelId }}
          </template>
        </a-table-column>
        <a-table-column title="关联知识库" :width="230">
          <template #default="{ record }">{{ baseText(record) }}</template>
        </a-table-column>
        <a-table-column title="AI工具" :width="180">
          <template #default="{ record }">
            <a-space size="small" wrap>
              <a-tag v-for="tool in (record.tools || []).slice(0, 2)" :key="tool.toolId" color="blue">
                {{ tool.toolName }}
              </a-tag>
              <span v-if="!record.tools?.length" class="knowledge-muted">未关联</span>
              <span v-else-if="record.tools.length > 2" class="knowledge-muted">+{{ record.tools.length - 2 }}</span>
            </a-space>
          </template>
        </a-table-column>
        <a-table-column title="TopK / 阈值" :width="110">
          <template #default="{ record }"> {{ record.assistant.topK }} / {{ record.assistant.scoreThreshold }} </template>
        </a-table-column>
        <a-table-column title="来源" :width="90">
          <template #default="{ record }">
            <a-tag :color="record.owned ? 'blue' : 'gold'">{{ record.owned ? '我创建的' : '已收藏' }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="状态" :width="85">
          <template #default="{ record }">
            <a-tag :color="record.assistant.enabledFlag ? 'green' : 'default'">
              {{ record.assistant.enabledFlag ? '已启用' : '已停用' }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="商店" :width="85">
          <template #default="{ record }">
            <a-tag v-if="record.owned" :color="record.assistant.publishedFlag ? 'blue' : 'default'">
              {{ record.assistant.publishedFlag ? '已上架' : '未上架' }}
            </a-tag>
            <span v-else class="knowledge-muted">已收藏</span>
          </template>
        </a-table-column>
        <a-table-column title="操作" :width="245" fixed="right">
          <template #default="{ record }">
            <a-button type="link" :disabled="!record.assistant.enabledFlag" @click="openChat(record)">问答</a-button>
            <a-button v-if="record.owned" type="link" @click="togglePublish(record)">
              {{ record.assistant.publishedFlag ? '下架' : '上架' }}
            </a-button>
            <a-button v-if="record.owned" type="link" @click="edit(record)">编辑</a-button>
            <a-popconfirm v-if="record.owned" title="删除助手及全部会话？知识库和工具不会被删除。" @confirm="remove(record)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </a-table-column>
      </a-table>
    </section>

    <template v-else>
      <div class="knowledge-header">
        <a-space>
          <a-button aria-label="返回助手列表" @click="active = null"><ArrowLeftOutlined /></a-button>
          <strong>{{ active.assistant.assistantName }}</strong>
          <span class="knowledge-muted">{{ baseText(active) }}</span>
          <a-tag v-if="active.tools?.length" color="blue">{{ active.tools.length }} 个工具</a-tag>
        </a-space>
        <a-button @click="newConversation"><PlusOutlined />新会话</a-button>
      </div>
      <div class="knowledge-chat">
        <aside class="knowledge-chat__side">
          <a-list :data-source="conversations" size="small">
            <template #renderItem="{ item }">
              <a-list-item
                class="knowledge-chat__conversation"
                :class="{ 'knowledge-chat__conversation--active': conversationId === item.conversationId }"
                @click="selectConversation(item)"
              >
                <a-space
                  ><MessageOutlined /><span>{{ item.title }}</span></a-space
                >
                <a-popconfirm title="删除此会话？" @confirm.stop="deleteConversation(item)">
                  <a-button type="text" size="small" aria-label="删除会话" @click.stop><DeleteOutlined /></a-button>
                </a-popconfirm>
              </a-list-item>
            </template>
          </a-list>
        </aside>
        <main class="knowledge-chat__main">
          <div ref="messageArea" class="knowledge-chat__messages">
            <a-empty v-if="!messages.length" description="开始提问" />
            <div
              v-for="(entry, index) in messages"
              :key="entry.messageId || index"
              class="knowledge-chat__bubble"
              :class="{ 'knowledge-chat__bubble--user': entry.role === 'user' }"
            >
              <MarkdownContent v-if="entry.role === 'assistant'" :content="assistantContent(entry)" />
              <div v-else class="knowledge-chat__plain">{{ entry.content }}</div>

              <div v-if="entry.role === 'assistant' && toolCalls(entry).length" class="knowledge-tool-trace">
                <div v-for="call in toolCalls(entry)" :key="call.requestId || call.toolCode" class="knowledge-tool-call">
                  <div class="knowledge-tool-call__head">
                    <span><ToolOutlined /> {{ call.toolName || call.toolCode }}</span>
                    <a-tag :color="toolStatusMeta(call.status).color">{{ toolStatusMeta(call.status).text }}</a-tag>
                  </div>
                  <div class="knowledge-tool-call__meta">
                    <span>{{ call.toolType === 'ACTION' ? '操作工具' : '查询工具' }}</span>
                    <span v-if="call.durationMs">{{ call.durationMs }} ms</span>
                    <span v-if="call.traceId">Trace: {{ call.traceId }}</span>
                  </div>
                  <a-alert
                    v-if="call.status === 'WAITING_CONFIRMATION'"
                    type="warning"
                    show-icon
                    message="该操作需要你的明确确认"
                    :description="call.message || '确认后平台才会调用目标业务接口。'"
                  >
                    <template #action>
                      <a-space>
                        <a-button size="small" :loading="confirmingRequestId === call.requestId" @click="confirmToolCall(entry, call, false)">
                          拒绝
                        </a-button>
                        <a-button
                          type="primary"
                          size="small"
                          :loading="confirmingRequestId === call.requestId"
                          @click="confirmToolCall(entry, call, true)"
                        >
                          确认执行
                        </a-button>
                      </a-space>
                    </template>
                  </a-alert>
                  <details v-if="call.arguments || call.result" class="knowledge-tool-call__details">
                    <summary>查看调用数据</summary>
                    <div v-if="call.arguments">
                      <strong>参数</strong>
                      <pre>{{ prettyJson(call.arguments) }}</pre>
                    </div>
                    <div v-if="call.result">
                      <strong>结果</strong>
                      <pre>{{ prettyJson(call.result) }}</pre>
                    </div>
                  </details>
                </div>
              </div>

              <div v-if="entry.role === 'assistant' && citations(entry).length" class="knowledge-chat__citation">
                <div v-for="hit in citations(entry)" :key="`${hit.documentId}-${hit.chunkIndex}`">
                  <FileTextOutlined />
                  {{ hit.fileName }} · 切片 {{ hit.chunkIndex }} · 相似度 {{ Number(hit.score).toFixed(3) }}
                </div>
              </div>
            </div>
            <div v-if="sending" class="knowledge-chat__bubble">正在生成回答…</div>
          </div>
          <div class="knowledge-chat__composer">
            <div class="knowledge-chat__composer-box">
              <a-textarea
                v-model:value="question"
                class="knowledge-chat__input"
                :auto-size="{ minRows: 2, maxRows: 5 }"
                :maxlength="4000"
                placeholder="发送消息"
                @keydown="handleQuestionKeydown"
              />
              <a-tooltip title="发送">
                <a-button
                  class="knowledge-chat__send"
                  type="primary"
                  shape="circle"
                  :loading="sending"
                  :disabled="!question.trim()"
                  aria-label="发送"
                  @click="send"
                >
                  <ArrowUpOutlined />
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </main>
      </div>
    </template>

    <a-drawer v-model:open="drawer" :title="form.assistantId ? '编辑智能助手' : '新建智能助手'" width="min(640px, 100vw)">
      <a-form layout="vertical">
        <a-form-item label="助手名称" required>
          <a-input v-model:value="form.assistantName" :maxlength="100" />
        </a-form-item>
        <a-form-item label="对话模型" required>
          <a-select v-model:value="form.modelId" style="width: 100%" placeholder="选择已启用模型">
            <a-select-option v-for="model in models" :key="model.modelId" :value="model.modelId">
              {{ model.modelName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="系统提示词">
          <a-textarea v-model:value="form.systemPrompt" :rows="5" :maxlength="8000" show-count />
        </a-form-item>
        <a-form-item label="关联知识库">
          <a-select v-model:value="form.baseIds" mode="multiple" style="width: 100%" placeholder="可选择多个知识库">
            <a-select-option v-for="row in bases.filter((item) => item.base.enabledFlag)" :key="row.base.baseId" :value="row.base.baseId">
              {{ row.base.baseName }} · {{ row.documentIds.length }} 个文档
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关联AI工具" extra="仅展示已审核、已启用且来源在线的工具；工具版本升级后需在 MCP 工具管理中手动同步。">
          <a-select
            v-model:value="form.toolIds"
            mode="multiple"
            style="width: 100%"
            placeholder="按业务需要选择工具"
            option-filter-prop="label"
            :options="toolOptions"
            @change="handleToolSelection"
          />
          <div v-if="selectedTools.length" class="knowledge-tool-selector">
            <div v-for="tool in selectedTools" :key="tool.toolId" class="knowledge-tool-selector__item">
              <div>
                <strong>{{ tool.toolName }}</strong>
                <span>{{ tool.toolCode }}</span>
              </div>
              <a-space>
                <a-tag :color="toolSourceMeta(tool.sourceType).color">
                  {{ toolSourceMeta(tool.sourceType).text }}
                </a-tag>
                <a-tag :color="tool.toolType === 'ACTION' ? 'orange' : 'green'">
                  {{ tool.toolType === 'ACTION' ? '操作' : '查询' }}
                </a-tag>
                <a-tag :color="riskMeta(tool.riskLevel).color">{{ riskMeta(tool.riskLevel).text }}风险</a-tag>
              </a-space>
            </div>
          </div>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检索TopK">
              <a-input-number v-model:value="form.topK" :min="1" :max="20" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="相似度阈值">
              <a-input-number v-model:value="form.scoreThreshold" :min="0" :max="1" :step="0.05" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单轮最多工具调用">
              <a-input-number v-model:value="form.maxToolCalls" :min="1" :max="5" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="允许操作类工具">
          <a-switch v-model:checked="form.allowActionToolFlag" :disabled="selectedActionTools.length > 0" />
          <span v-if="selectedActionTools.length" class="knowledge-form-help"> 已选择操作类工具，必须开启；实际执行前仍会要求用户确认。 </span>
        </a-form-item>
        <a-form-item label="显示工具调试信息">
          <a-switch v-model:checked="form.toolDebugFlag" />
          <span class="knowledge-form-help">开启后在问答中展示工具参数、结果和 Trace ID。</span>
        </a-form-item>
        <a-form-item label="展示引用来源"><a-switch v-model:checked="form.showCitations" /></a-form-item>
        <a-form-item label="启用助手"><a-switch v-model:checked="form.enabledFlag" /></a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="drawer = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="save">保存助手</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
  import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import {
    ArrowLeftOutlined,
    ArrowUpOutlined,
    DeleteOutlined,
    FileTextOutlined,
    MessageOutlined,
    PlusOutlined,
    ToolOutlined,
  } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
  import { mcpToolApi } from '/@/api/business/open-api/mcp-tool-api';
  import { nexoraSentry } from '/@/lib/nexora-sentry';
  import MarkdownContent from './components/markdown-content.vue';
  import './knowledge.less';

  const route = useRoute();
  const router = useRouter();
  const rows = ref([]);
  const bases = ref([]);
  const models = ref([]);
  const availableTools = ref([]);
  const loading = ref(false);
  const drawer = ref(false);
  const saving = ref(false);
  const active = ref(null);
  const conversations = ref([]);
  const conversationId = ref(null);
  const messages = ref([]);
  const question = ref('');
  const sending = ref(false);
  const messageArea = ref(null);
  const confirmingRequestId = ref('');
  const initialized = ref(false);
  const form = reactive(defaultForm());

  const toolOptions = computed(() =>
    availableTools.value.map((tool) => ({
      value: tool.toolId,
      label: `${tool.toolName}（${tool.toolCode}）`,
    }))
  );
  const selectedTools = computed(() => availableTools.value.filter((tool) => form.toolIds.includes(tool.toolId)));
  const selectedActionTools = computed(() => selectedTools.value.filter((tool) => tool.toolType === 'ACTION'));

  /** 返回新建助手时使用的完整默认配置。 */
  function defaultForm() {
    return {
      assistantId: null,
      assistantName: '',
      modelId: undefined,
      systemPrompt: '',
      baseIds: [],
      toolIds: [],
      topK: 5,
      scoreThreshold: 0.3,
      maxToolCalls: 3,
      toolDebugFlag: false,
      allowActionToolFlag: false,
      showCitations: true,
      enabledFlag: true,
    };
  }

  /** 同时加载助手、知识库、模型和当前用户可绑定的真实工具。 */
  async function load() {
    loading.value = true;
    try {
      const [assistants, baseRows, options, tools] = await Promise.all([
        api.availableAssistants(),
        api.bases({}),
        api.options(),
        mcpToolApi.availableForAssistant(),
      ]);
      rows.value = assistants.data || [];
      bases.value = baseRows.data || [];
      models.value = options.data.chatModels || [];
      availableTools.value = tools.data || [];
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  /** 打开助手配置，并回填知识库、工具和工具调用安全策略。 */
  function edit(row) {
    const assistant = row?.assistant || {};
    Object.assign(form, defaultForm(), {
      assistantId: assistant.assistantId || null,
      assistantName: assistant.assistantName || '',
      modelId: assistant.modelId || undefined,
      systemPrompt: assistant.systemPrompt || '',
      baseIds: [...(row?.baseIds || [])],
      toolIds: [...(row?.toolIds || [])],
      topK: assistant.topK || 5,
      scoreThreshold: Number(assistant.scoreThreshold ?? 0.3),
      maxToolCalls: assistant.maxToolCalls || 3,
      toolDebugFlag: assistant.toolDebugFlag ?? false,
      allowActionToolFlag: assistant.allowActionToolFlag ?? false,
      showCitations: assistant.showCitations ?? true,
      enabledFlag: assistant.enabledFlag ?? true,
    });
    handleToolSelection();
    drawer.value = true;
  }

  /** 选择操作工具时自动开启操作权限，执行阶段仍强制用户逐次确认。 */
  function handleToolSelection() {
    if (selectedActionTools.value.length) {
      form.allowActionToolFlag = true;
    }
  }

  /** 保存助手及其工具关系，服务端会再次校验工具状态和数据归属。 */
  async function save() {
    if (!form.assistantName.trim() || !form.modelId) {
      message.warning('请填写助手名称并选择对话模型');
      return;
    }
    if (selectedActionTools.value.length && !form.allowActionToolFlag) {
      message.warning('关联操作类工具时必须允许操作类工具');
      return;
    }
    saving.value = true;
    try {
      await api.saveAssistant({ ...form });
      drawer.value = false;
      message.success('助手已保存');
      await load();
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      saving.value = false;
    }
  }

  /** 上下架本人助手，工具关联关系保持不变。 */
  async function togglePublish(row) {
    const published = !row.assistant.publishedFlag;
    await api.publishAssistant(row.assistant.assistantId, published);
    message.success(published ? '智能助手已上架' : '智能助手已下架');
    await load();
  }

  /** 删除助手及会话，知识库和工具本身不会被删除。 */
  async function remove(row) {
    await api.deleteAssistant(row.assistant.assistantId);
    message.success('助手已删除');
    await load();
  }

  /** 进入助手工作台并加载当前用户的历史会话。 */
  async function openChat(row) {
    active.value = row;
    newConversation();
    await reloadConversations();
  }

  /** 清空当前消息区，准备开始一段新会话。 */
  function newConversation() {
    conversationId.value = null;
    messages.value = [];
    question.value = '';
  }

  /** 加载当前助手下属于登录用户的会话列表。 */
  async function reloadConversations() {
    if (active.value) {
      conversations.value = (await api.conversations(active.value.assistant.assistantId)).data || [];
    }
  }

  /** 切换会话后同步刷新仍处于确认态的工具调用状态。 */
  async function selectConversation(item) {
    conversationId.value = item.conversationId;
    messages.value = (await api.messages(active.value.assistant.assistantId, item.conversationId)).data || [];
    await refreshPendingCalls();
    scrollBottom();
  }

  /** 查询调用日志，避免历史消息一直显示已经处理过的确认按钮。 */
  async function refreshPendingCalls() {
    const requests = [];
    messages.value.forEach((entry) => {
      toolCalls(entry)
        .filter((call) => call.status === 'WAITING_CONFIRMATION' && call.requestId)
        .forEach((call) => requests.push({ entry, call }));
    });
    await Promise.all(
      requests.map(async ({ entry, call }) => {
        try {
          const response = await mcpToolApi.callResult(call.requestId);
          updateToolCall(entry, call.requestId, response.data);
        } catch (error) {
          nexoraSentry.captureError(error);
        }
      })
    );
  }

  /** 删除本人会话，并在必要时退出当前会话。 */
  async function deleteConversation(item) {
    await api.deleteConversation(active.value.assistant.assistantId, item.conversationId);
    if (conversationId.value === item.conversationId) newConversation();
    await reloadConversations();
  }

  /** 回车发送，Shift+Enter 换行，并避开输入法候选确认事件。 */
  function handleQuestionKeydown(event) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return;
    event.preventDefault();
    send();
  }

  /** 调用真实检索、模型与工具循环，并把完整工具轨迹追加到消息区。 */
  async function send() {
    if (!question.value.trim() || sending.value) return;
    const text = question.value.trim();
    question.value = '';
    messages.value.push({ role: 'user', content: text });
    sending.value = true;
    scrollBottom();
    try {
      const result = (
        await api.chat(active.value.assistant.assistantId, {
          conversationId: conversationId.value,
          question: text,
        })
      ).data;
      conversationId.value = result.conversationId;
      messages.value.push({
        role: 'assistant',
        content: result.content,
        citationsJson: JSON.stringify(result.citations || []),
        toolCallsJson: JSON.stringify(result.toolCalls || []),
      });
      await reloadConversations();
      scrollBottom();
    } catch (error) {
      messages.value.pop();
      question.value = text;
      nexoraSentry.captureError(error);
    } finally {
      sending.value = false;
    }
  }

  /** 确认或拒绝一次操作类工具调用，并用审计日志结果更新当前消息。 */
  async function confirmToolCall(entry, call, approved) {
    if (!call.requestId || confirmingRequestId.value) return;
    confirmingRequestId.value = call.requestId;
    try {
      const response = await mcpToolApi.confirmCall({ requestId: call.requestId, approved });
      updateToolCall(entry, call.requestId, response.data);
      message.success(approved ? '工具已执行' : '已拒绝本次工具调用');
      scrollBottom();
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      confirmingRequestId.value = '';
    }
  }

  /** 合并最新调用状态，同时保留模型生成的工具名称和参数。 */
  function updateToolCall(entry, requestId, result) {
    const calls = toolCalls(entry).map((call) =>
      call.requestId === requestId ? { ...call, ...result, confirmationRequired: result?.confirmationRequired ?? false } : call
    );
    entry.toolCallsJson = JSON.stringify(calls);
  }

  /** 从持久化消息中解析本次实际命中的知识库引用。 */
  function citations(entry) {
    try {
      return JSON.parse(entry.citationsJson || '[]');
    } catch {
      return [];
    }
  }

  /** 从持久化消息中解析工具选择、确认和执行结果。 */
  function toolCalls(entry) {
    try {
      return JSON.parse(entry.toolCallsJson || '[]');
    } catch {
      return [];
    }
  }

  /** 工具确认完成后替换旧提示语，使历史会话状态和调用日志一致。 */
  function assistantContent(entry) {
    const calls = toolCalls(entry);
    if (!calls.length || !String(entry.content || '').includes('请确认后继续')) return entry.content;
    if (calls.some((call) => call.status === 'SUCCESS')) return '工具调用已执行完成，结果见下方调用记录。';
    if (calls.some((call) => call.status === 'USER_REJECTED')) return '本次工具调用已取消。';
    return entry.content;
  }

  /** 返回工具调用状态在页面上的中文文案和颜色。 */
  function toolStatusMeta(status) {
    return (
      {
        WAITING_CONFIRMATION: { color: 'orange', text: '待确认' },
        CREATED: { color: 'processing', text: '已创建' },
        EXECUTING: { color: 'processing', text: '执行中' },
        SUCCESS: { color: 'green', text: '成功' },
        USER_REJECTED: { color: 'default', text: '已拒绝' },
        TIMEOUT: { color: 'red', text: '超时' },
        SCHEMA_ERROR: { color: 'red', text: 'Schema错误' },
        FAILED: { color: 'red', text: '失败' },
      }[status] || { color: 'default', text: status || '未知' }
    );
  }

  /** 返回风险等级展示信息。 */
  function riskMeta(value) {
    return (
      {
        LOW: { color: 'green', text: '低' },
        MEDIUM: { color: 'orange', text: '中' },
        HIGH: { color: 'red', text: '高' },
      }[value] || { color: 'default', text: '未知' }
    );
  }

  /** 返回智能助手工具选择器中使用的来源文案和颜色。 */
  function toolSourceMeta(value) {
    return (
      {
        PLATFORM_API: { color: 'blue', text: '平台API' },
        STANDARD_MCP: { color: 'cyan', text: '标准MCP' },
        EXTERNAL_HTTP: { color: 'purple', text: 'HTTP适配' },
      }[value] || { color: 'default', text: '未知来源' }
    );
  }

  /** 格式化调试参数和工具结果。 */
  function prettyJson(value) {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }

  /** 生成列表和聊天标题中使用的知识库摘要。 */
  function baseText(row) {
    return (
      row.baseNames?.join('、') ||
      row.baseIds?.map((id) => bases.value.find((item) => item.base.baseId === id)?.base.baseName || id).join('、') ||
      '未关联'
    );
  }

  /** 新消息出现后滚动到消息区底部。 */
  async function scrollBottom() {
    await nextTick();
    if (messageArea.value) messageArea.value.scrollTop = messageArea.value.scrollHeight;
  }

  /** 处理首页知识库入口，可直接打开指定助手并自动发送首个问题。 */
  async function handleKnowledgeEntry() {
    if (!initialized.value) return;
    const assistantId = Number(route.query.assistantId);
    if (!assistantId) return;
    let row = rows.value.find((item) => item.assistant.assistantId === assistantId && item.assistant.enabledFlag);
    if (!row) {
      try {
        const result = await api.assistant(assistantId);
        row = result.data;
        if (row?.assistant?.enabledFlag) rows.value = [row, ...rows.value];
      } catch {
        return;
      }
    }
    if (!row) {
      message.warning('未找到可用的知识库问答助手');
      return;
    }
    const initialQuestion = Array.isArray(route.query.question) ? route.query.question[0] : route.query.question;
    await openChat(row);
    await router.replace({ path: route.path });
    if (initialQuestion?.trim()) {
      question.value = initialQuestion.trim();
      await send();
    }
  }

  /** 完成页面首次数据加载后再处理跨页面问答入口。 */
  async function initialize() {
    await load();
    initialized.value = true;
    await handleKnowledgeEntry();
  }

  watch(() => [route.query.assistantId, route.query.question], handleKnowledgeEntry);
  onMounted(initialize);
</script>
