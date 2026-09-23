<template>
  <wd-config-provider :theme="themeStore.mode" :theme-vars="themeVars">
    <view class="app-shell chat-page" :class="{ 'theme-dark': themeStore.isDark }">
      <wd-navbar
        fixed
        placeholder
        safe-area-inset-top
        left-arrow
        :bordered="false"
        :custom-style="navbarStyle"
        @click-left="goBack"
      >
        <template #title>
          <view class="assistant-title">
            <assistant-avatar :assistant="assistant" :size="38" />
            <view class="assistant-title-line">
              <text class="assistant-title-name">{{ assistant.assistantName || '智能助手' }}</text>
              <view class="online-dot" />
            </view>
          </view>
        </template>
        <template #right>
          <wd-button type="icon" icon="more" aria-label="更多操作" @click.stop="showActions = true" />
        </template>
      </wd-navbar>

      <view
        v-if="assistantView.baseNames?.length"
        class="knowledge-strip"
        @click="showKnowledgeSummary"
      >
        <wd-icon name="books" size="21px" />
        <text class="knowledge-strip-text">
          基于 {{ assistantView.baseNames.join('、') }} · {{ assistantView.baseNames.length }} 个知识库
        </text>
        <wd-icon name="arrow-right" size="18px" />
      </view>

      <scroll-view
        class="message-scroll"
        scroll-y
        :scroll-into-view="scrollTarget"
        :show-scrollbar="false"
      >
        <view v-if="loading" class="loading-area">
          <wd-loading color="#04bfe5" />
          <text>正在加载会话</text>
        </view>

        <view v-else class="message-list">
          <view v-if="!messages.length" class="welcome-message">
            <assistant-avatar :assistant="assistant" :size="42" />
            <view class="assistant-bubble">
              你好，我是{{ assistant.assistantName || '智能助手' }}。{{ welcomeText }}
            </view>
          </view>

          <view v-if="!messages.length" class="suggestion-list">
            <wd-tag
              v-for="suggestion in suggestions"
              :key="suggestion"
              plain
              round
              custom-class="suggestion-tag"
              @click="sendSuggestion(suggestion)"
            >
              {{ suggestion }}
            </wd-tag>
          </view>

          <view
            v-for="(entry, index) in messages"
            :id="`message-${index}`"
            :key="entry.messageId || `${entry.role}-${index}`"
            class="message-row"
            :class="entry.role === 'user' ? 'message-user' : 'message-assistant'"
          >
            <assistant-avatar
              v-if="entry.role !== 'user'"
              :assistant="assistant"
              :size="40"
              class="message-avatar"
            />

            <view class="message-body">
              <view
                v-if="entry.role === 'user'"
                class="message-content"
                :class="'user-bubble'"
                @longpress="copyAnswer(entry.content)"
              >
                {{ entry.content }}
              </view>

              <view
                v-else
                class="message-content assistant-content"
                @longpress="copyAnswer(entry.content)"
              >
                <template
                  v-for="(block, blockIndex) in messageBlocks(entry)"
                  :key="`${entry.messageId || index}-block-${blockIndex}`"
                >
                  <view
                    v-if="block.type === 'heading'"
                    class="md-heading"
                    :class="`md-heading-${block.level}`"
                  >
                    <text
                      v-for="(token, tokenIndex) in inlineTokens(block.text)"
                      :key="`heading-${blockIndex}-${tokenIndex}`"
                      :class="inlineTokenClass(token)"
                    >
                      {{ token.text }}
                    </text>
                  </view>

                  <view v-else-if="block.type === 'paragraph'" class="md-paragraph">
                    <text
                      v-for="(token, tokenIndex) in inlineTokens(block.text)"
                      :key="`paragraph-${blockIndex}-${tokenIndex}`"
                      :class="inlineTokenClass(token)"
                    >
                      {{ token.text }}
                    </text>
                  </view>

                  <view
                    v-else-if="block.type === 'ordered-list' || block.type === 'unordered-list'"
                    class="md-list"
                  >
                    <view
                      v-for="(item, itemIndex) in block.items"
                      :key="`list-${blockIndex}-${itemIndex}`"
                      class="md-list-item"
                    >
                      <text class="md-list-marker">
                        {{ block.type === 'ordered-list' ? `${itemIndex + 1}.` : '•' }}
                      </text>
                      <view class="md-list-content">
                        <text
                          v-for="(token, tokenIndex) in inlineTokens(item)"
                          :key="`list-token-${blockIndex}-${itemIndex}-${tokenIndex}`"
                          :class="inlineTokenClass(token)"
                        >
                          {{ token.text }}
                        </text>
                      </view>
                    </view>
                  </view>

                  <view
                    v-else-if="block.type === 'table' && block.compact"
                    class="md-data-table"
                  >
                    <view class="md-data-table-head">
                      <text>{{ block.headers[0] }}</text>
                      <text>{{ block.headers[1] }}</text>
                    </view>
                    <view
                      v-for="(row, rowIndex) in block.rows"
                      :key="`table-${blockIndex}-${rowIndex}`"
                      class="md-data-row"
                    >
                      <view class="md-data-label">
                        <text
                          v-for="(token, tokenIndex) in inlineTokens(row[0])"
                          :key="`label-${rowIndex}-${tokenIndex}`"
                          :class="inlineTokenClass(token)"
                        >
                          {{ token.text }}
                        </text>
                      </view>
                      <view
                        class="md-data-value"
                        @longpress.stop="copyAnswer(row[1])"
                      >
                        <text
                          v-for="(token, tokenIndex) in inlineTokens(row[1])"
                          :key="`value-${rowIndex}-${tokenIndex}`"
                          :class="inlineTokenClass(token)"
                        >
                          {{ token.text }}
                        </text>
                      </view>
                    </view>
                  </view>

                  <scroll-view
                    v-else-if="block.type === 'table'"
                    class="md-table-scroll"
                    scroll-x
                    :show-scrollbar="false"
                  >
                    <view class="md-wide-table" :style="tableGridStyle(block)">
                      <view
                        v-for="(header, headerIndex) in block.headers"
                        :key="`table-header-${blockIndex}-${headerIndex}`"
                        class="md-wide-cell md-wide-head"
                      >
                        {{ header }}
                      </view>
                      <template
                        v-for="(row, rowIndex) in block.rows"
                        :key="`wide-row-${blockIndex}-${rowIndex}`"
                      >
                        <view
                          v-for="(cell, cellIndex) in row"
                          :key="`wide-cell-${rowIndex}-${cellIndex}`"
                          class="md-wide-cell"
                          @longpress.stop="copyAnswer(cell)"
                        >
                          <text
                            v-for="(token, tokenIndex) in inlineTokens(cell)"
                            :key="`wide-token-${rowIndex}-${cellIndex}-${tokenIndex}`"
                            :class="inlineTokenClass(token)"
                          >
                            {{ token.text }}
                          </text>
                        </view>
                      </template>
                    </view>
                  </scroll-view>

                  <view
                    v-else-if="block.type === 'code'"
                    class="md-code"
                    @longpress.stop="copyAnswer(block.text)"
                  >
                    <text selectable>{{ block.text }}</text>
                  </view>
                </template>
              </view>

              <view v-if="citations(entry).length" class="citation-list">
                <view
                  v-for="(citation, citationIndex) in citations(entry)"
                  :key="`${citation.documentId || citation.fileName}-${citation.chunkIndex}-${citationIndex}`"
                  class="citation-item"
                  @click="showCitation(citation)"
                >
                  <wd-icon name="file" size="17px" />
                  <text class="citation-text">
                    来源：{{ citation.fileName || '知识库文档' }}
                    <text v-if="citation.chunkIndex !== undefined"> · 切片 {{ citation.chunkIndex }}</text>
                  </text>
                  <wd-icon name="arrow-right" size="16px" />
                </view>
              </view>

              <view v-if="toolCalls(entry).length" class="tool-call-list">
                <view
                  v-for="call in toolCalls(entry)"
                  :key="call.requestId || `${call.toolCode}-${call.traceId}`"
                  class="tool-call-item"
                >
                  <view class="tool-call-heading">
                    <view class="tool-name-line">
                      <wd-icon name="computer" size="18px" />
                      <text>{{ call.toolName || call.toolCode || 'AI 工具' }}</text>
                    </view>
                    <wd-tag :type="toolStatusMeta(call.status).type" plain>
                      {{ toolStatusMeta(call.status).text }}
                    </wd-tag>
                  </view>

                  <text v-if="call.message" class="tool-message">{{ call.message }}</text>

                  <view v-if="call.arguments" class="tool-debug">
                    <text class="tool-debug-label">调用参数</text>
                    <text
                      class="tool-debug-value"
                      @longpress.stop="copyAnswer(prettyJson(call.arguments))"
                    >
                      {{ prettyJson(call.arguments) }}
                    </text>
                  </view>

                  <view v-if="call.result" class="tool-debug">
                    <text class="tool-debug-label">执行结果</text>
                    <text
                      class="tool-debug-value"
                      @longpress.stop="copyAnswer(prettyJson(call.result))"
                    >
                      {{ prettyJson(call.result) }}
                    </text>
                  </view>

                  <view
                    v-if="call.status === 'WAITING_CONFIRMATION'"
                    class="confirmation-actions"
                  >
                    <wd-button
                      size="small"
                      type="info"
                      plain
                      :loading="confirmingRequestId === call.requestId"
                      @click="handleToolConfirmation(entry, call, false)"
                    >
                      拒绝
                    </wd-button>
                    <wd-button
                      size="small"
                      :loading="confirmingRequestId === call.requestId"
                      @click="handleToolConfirmation(entry, call, true)"
                    >
                      确认执行
                    </wd-button>
                  </view>
                </view>
              </view>

              <view v-if="entry.role !== 'user'" class="message-actions">
                <wd-button
                  type="text"
                  icon="copy"
                  size="small"
                  @click="copyAnswer(entry.content)"
                >
                  复制
                </wd-button>
                <wd-button type="text" icon="thumb-up" size="small" @click="showFeedback(true)">
                  有用
                </wd-button>
                <wd-button type="text" icon="thumb-down" size="small" @click="showFeedback(false)">
                  没用
                </wd-button>
              </view>
            </view>
          </view>

          <view v-if="sending" class="message-row message-assistant">
            <assistant-avatar :assistant="assistant" :size="40" />
            <view class="thinking">
              <wd-loading color="#04bfe5" size="20px" />
              <text>正在思考并检索知识</text>
            </view>
          </view>

          <view id="message-bottom" class="message-bottom" />
        </view>
      </scroll-view>

      <view class="composer">
        <wd-button
          type="icon"
          icon="attach"
          aria-label="添加附件"
          custom-class="composer-icon"
          @click="showAttachmentTip"
        />
        <view class="composer-input">
          <wd-input
            v-model="question"
            clearable
            confirm-type="send"
            placeholder="输入问题..."
            :disabled="sending"
            @confirm="sendQuestion"
          />
        </view>
        <wd-button
          type="primary"
          icon="arrow-up"
          :loading="sending"
          :disabled="!question.trim()"
          custom-class="send-button"
          aria-label="发送问题"
          @click="sendQuestion"
        />
      </view>

      <wd-action-sheet
        v-model="showActions"
        title="助手操作"
        cancel-text="取消"
        :actions="actionItems"
        @select="handleActionSelect"
      />
    </view>
  </wd-config-provider>
</template>

<script setup>
  import { computed, nextTick, ref } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import AssistantAvatar from '@/components/assistant-avatar/assistant-avatar.vue';
  import {
    chatWithAssistant,
    confirmToolCall,
    getAssistantDetail,
    getConversationMessages,
    getToolCallStatus,
  } from '@/api/business/knowledge-api';
  import { useAssistantStore } from '@/store/modules/business/assistant';
  import { useThemeStore } from '@/store/modules/system/theme';
  import { nexoraSentry } from '@/lib/nexora-sentry';

  const assistantStore = useAssistantStore();
  const themeStore = useThemeStore();
  const assistantId = ref('');
  const conversationId = ref('');
  const assistantView = ref({ assistant: {}, baseNames: [], tools: [] });
  const messages = ref([]);
  const question = ref('');
  const loading = ref(true);
  const sending = ref(false);
  const showActions = ref(false);
  const confirmingRequestId = ref('');
  const scrollTarget = ref('');

  const assistant = computed(() => assistantView.value.assistant || {});
  const navbarStyle = computed(
    () => `background:${themeStore.isDark ? '#08131c' : '#f7f9fc'};`,
  );
  const themeVars = computed(() => ({
    colorTheme: '#04bfe5',
    navbarBackground: themeStore.isDark ? '#08131c' : '#f7f9fc',
    navbarColor: themeStore.isDark ? '#f6f8fb' : '#0b1220',
    inputBg: themeStore.isDark ? '#142330' : '#ffffff',
    inputColor: themeStore.isDark ? '#f6f8fb' : '#101828',
    inputPlaceholderColor: themeStore.isDark ? '#8290a5' : '#8a94a6',
  }));
  const welcomeText = computed(() => {
    if (assistantView.value.baseNames?.length) {
      return `我可以基于${assistantView.value.baseNames.join('、')}回答问题。`;
    }
    if (assistantView.value.tools?.length) {
      return '我可以通过已关联的 AI 工具协助你处理业务问题。';
    }
    return '我可以协助你完成企业知识问答。';
  });
  const suggestions = computed(() => {
    const assistantName = assistant.value.assistantName || '这个助手';
    const baseName = assistantView.value.baseNames?.[0];
    return [
      `${assistantName}可以解决哪些问题`,
      baseName ? `总结${baseName}的核心内容` : '介绍可查询的知识范围',
      '请给我一个常见问题示例',
    ];
  });
  const actionItems = computed(() => [
    { name: '开始新会话', value: 'new' },
    { name: '查看会话记录', value: 'history' },
    {
      name: assistantView.value.favorited ? '取消收藏' : '收藏助手',
      value: 'favorite',
    },
  ]);

  /**
   * 加载助手详情及指定历史会话。
   *
   * @param {Object} options 页面参数
   */
  async function initializePage(options) {
    assistantId.value = options.assistantId || '';
    conversationId.value = options.conversationId || '';
    if (!assistantId.value) {
      uni.showToast({ title: '缺少助手编号', icon: 'none' });
      goBack();
      return;
    }

    loading.value = true;
    try {
      assistantView.value = await getAssistantDetail(assistantId.value);
      assistantStore.markRecent(assistantId.value);
      if (conversationId.value) {
        messages.value = await getConversationMessages(
          assistantId.value,
          conversationId.value,
        );
        await refreshPendingCalls();
      }
      scrollBottom();
    } catch (error) {
      nexoraSentry.captureError(error);
      uni.showToast({ title: '助手加载失败，请稍后重试', icon: 'none' });
    } finally {
      loading.value = false;
    }
  }

  /**
   * 返回上一页面。
   */
  function goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      uni.navigateBack();
      return;
    }
    uni.reLaunch({ url: '/pages/home/index' });
  }

  /**
   * 发送当前输入的问题。
   */
  async function sendQuestion() {
    const text = question.value.trim();
    if (!text || sending.value) return;

    const userEntry = { role: 'user', content: text };
    messages.value.push(userEntry);
    question.value = '';
    sending.value = true;
    scrollBottom();

    try {
      const result = await chatWithAssistant(assistantId.value, {
        conversationId: conversationId.value || null,
        question: text,
      });
      conversationId.value = result.conversationId;
      messages.value.push({
        role: 'assistant',
        content: result.content,
        citationsJson: JSON.stringify(result.citations || []),
        toolCallsJson: JSON.stringify(result.toolCalls || []),
      });
    } catch (error) {
      messages.value = messages.value.filter((item) => item !== userEntry);
      question.value = text;
      nexoraSentry.captureError(error);
      uni.showToast({ title: '发送失败，请稍后重试', icon: 'none' });
    } finally {
      sending.value = false;
      scrollBottom();
    }
  }

  /**
   * 使用建议问题发起对话。
   *
   * @param {string} suggestion 建议问题
   */
  function sendSuggestion(suggestion) {
    question.value = suggestion;
    sendQuestion();
  }

  /**
   * 查询历史消息中仍在等待确认的工具调用。
   */
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
          const result = await getToolCallStatus(call.requestId);
          updateToolCall(entry, call.requestId, result);
        } catch (error) {
          nexoraSentry.captureError(error);
        }
      }),
    );
  }

  /**
   * 确认或拒绝一次操作类工具调用。
   *
   * @param {Object} entry 消息记录
   * @param {Object} call 工具调用记录
   * @param {boolean} approved 是否批准执行
   */
  async function handleToolConfirmation(entry, call, approved) {
    if (!call.requestId || confirmingRequestId.value) return;
    confirmingRequestId.value = call.requestId;
    try {
      const result = await confirmToolCall(call.requestId, approved);
      updateToolCall(entry, call.requestId, result);
      uni.showToast({
        title: approved ? '工具已执行' : '已拒绝本次工具调用',
        icon: 'none',
      });
    } catch (error) {
      nexoraSentry.captureError(error);
      uni.showToast({ title: '工具确认失败', icon: 'none' });
    } finally {
      confirmingRequestId.value = '';
      scrollBottom();
    }
  }

  /**
   * 合并工具调用的最新状态。
   *
   * @param {Object} entry 消息记录
   * @param {string} requestId 工具调用请求编号
   * @param {Object} result 最新调用结果
   */
  function updateToolCall(entry, requestId, result) {
    const calls = toolCalls(entry).map((call) =>
      call.requestId === requestId
        ? {
            ...call,
            ...result,
            confirmationRequired: result?.confirmationRequired ?? false,
          }
        : call,
    );
    entry.toolCallsJson = JSON.stringify(calls);
  }

  /**
   * 解析消息引用。
   *
   * @param {Object} entry 消息记录
   * @returns {Array} 引用列表
   */
  function citations(entry) {
    return parseJsonArray(entry.citationsJson);
  }

  /**
   * 解析工具调用轨迹。
   *
   * @param {Object} entry 消息记录
   * @returns {Array} 工具调用列表
   */
  function toolCalls(entry) {
    return parseJsonArray(entry.toolCallsJson);
  }

  /**
   * 安全解析 JSON 数组。
   *
   * @param {string|Array} value JSON 字符串或数组
   * @returns {Array} 解析后的数组
   */
  function parseJsonArray(value) {
    if (Array.isArray(value)) return value;
    try {
      const parsed = JSON.parse(value || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return [];
    }
  }

  /**
   * 根据工具状态修正历史提示语。
   *
   * @param {Object} entry 消息记录
   * @returns {string} 页面显示正文
   */
  function assistantContent(entry) {
    const calls = toolCalls(entry);
    const content = String(entry.content || '');
    if (!calls.length || !content.includes('请确认后继续')) return content;
    if (calls.some((call) => call.status === 'SUCCESS')) {
      return '工具调用已执行完成，结果见下方调用记录。';
    }
    if (calls.some((call) => call.status === 'USER_REJECTED')) {
      return '本次工具调用已取消。';
    }
    return content;
  }

  /**
   * 将助手返回的 Markdown 文本解析为移动端可读的内容块。
   *
   * @param {Object} entry 消息记录
   * @returns {Array} 标题、段落、列表、表格和代码块
   */
  function messageBlocks(entry) {
    return parseMarkdownBlocks(assistantContent(entry));
  }

  /**
   * 解析常用 Markdown 块，避免在页面直接展示井号和表格分隔符。
   *
   * @param {string} content Markdown 文本
   * @returns {Array} 结构化内容块
   */
  function parseMarkdownBlocks(content) {
    const lines = String(content || '')
      .replace(/\r\n?/g, '\n')
      .split('\n');
    const blocks = [];
    let lineIndex = 0;

    while (lineIndex < lines.length) {
      const currentLine = lines[lineIndex].trim();
      if (!currentLine) {
        lineIndex += 1;
        continue;
      }

      if (currentLine.startsWith('```')) {
        const codeLines = [];
        lineIndex += 1;
        while (lineIndex < lines.length && !lines[lineIndex].trim().startsWith('```')) {
          codeLines.push(lines[lineIndex]);
          lineIndex += 1;
        }
        if (lineIndex < lines.length) {
          lineIndex += 1;
        }
        blocks.push({ type: 'code', text: codeLines.join('\n') });
        continue;
      }

      const headingMatch = currentLine.match(/^(#{1,3})\s+(.+)$/);
      if (headingMatch) {
        blocks.push({
          type: 'heading',
          level: headingMatch[1].length,
          text: headingMatch[2].trim(),
        });
        lineIndex += 1;
        continue;
      }

      if (
        currentLine.includes('|') &&
        lineIndex + 1 < lines.length &&
        isTableSeparator(lines[lineIndex + 1])
      ) {
        const headers = splitMarkdownRow(currentLine);
        const rows = [];
        lineIndex += 2;
        while (lineIndex < lines.length) {
          const rowLine = lines[lineIndex].trim();
          if (!rowLine || !rowLine.includes('|')) break;
          const row = splitMarkdownRow(rowLine);
          if (row.length) {
            rows.push(headers.map((header, index) => row[index] || ''));
          }
          lineIndex += 1;
        }
        blocks.push({
          type: 'table',
          headers,
          rows,
          compact: headers.length === 2,
        });
        continue;
      }

      const orderedMatch = currentLine.match(/^\d+\.\s+(.+)$/);
      if (orderedMatch) {
        const items = [];
        while (lineIndex < lines.length) {
          const itemMatch = lines[lineIndex].trim().match(/^\d+\.\s+(.+)$/);
          if (!itemMatch) break;
          items.push(itemMatch[1].trim());
          lineIndex += 1;
        }
        blocks.push({ type: 'ordered-list', items });
        continue;
      }

      const unorderedMatch = currentLine.match(/^[-*]\s+(.+)$/);
      if (unorderedMatch) {
        const items = [];
        while (lineIndex < lines.length) {
          const itemMatch = lines[lineIndex].trim().match(/^[-*]\s+(.+)$/);
          if (!itemMatch) break;
          items.push(itemMatch[1].trim());
          lineIndex += 1;
        }
        blocks.push({ type: 'unordered-list', items });
        continue;
      }

      const paragraphLines = [currentLine];
      lineIndex += 1;
      while (lineIndex < lines.length) {
        const nextLine = lines[lineIndex].trim();
        if (!nextLine || startsMarkdownBlock(lines, lineIndex)) break;
        paragraphLines.push(nextLine);
        lineIndex += 1;
      }
      blocks.push({
        type: 'paragraph',
        text: paragraphLines.join(' '),
      });
    }

    return blocks.length ? blocks : [{ type: 'paragraph', text: '' }];
  }

  /**
   * 判断指定行是否开始新的 Markdown 内容块。
   *
   * @param {Array<string>} lines 全部文本行
   * @param {number} index 当前行索引
   * @returns {boolean} 是否为新块
   */
  function startsMarkdownBlock(lines, index) {
    const line = String(lines[index] || '').trim();
    if (/^(#{1,3})\s+/.test(line)) return true;
    if (/^\d+\.\s+/.test(line) || /^[-*]\s+/.test(line)) return true;
    if (line.startsWith('```')) return true;
    return (
      line.includes('|') &&
      index + 1 < lines.length &&
      isTableSeparator(lines[index + 1])
    );
  }

  /**
   * 判断一行是否为 Markdown 表格分隔行。
   *
   * @param {string} line 待判断文本行
   * @returns {boolean} 是否为表格分隔行
   */
  function isTableSeparator(line) {
    const cells = splitMarkdownRow(line);
    return (
      cells.length > 1 &&
      cells.every((cell) => /^:?-{3,}:?$/.test(cell.replace(/\s+/g, '')))
    );
  }

  /**
   * 拆分 Markdown 表格行并清理两侧竖线。
   *
   * @param {string} line 表格文本行
   * @returns {Array<string>} 单元格列表
   */
  function splitMarkdownRow(line) {
    return String(line || '')
      .trim()
      .replace(/^\|/, '')
      .replace(/\|$/, '')
      .split('|')
      .map((cell) => cell.trim());
  }

  /**
   * 解析粗体和行内代码，供普通文本组件安全渲染。
   *
   * @param {string} text 行内 Markdown 文本
   * @returns {Array<Object>} 文本片段
   */
  function inlineTokens(text) {
    const source = String(text || '').replace(/\[([^\]]+)]\([^)]+\)/g, '$1');
    const tokens = [];
    const expression = /(\*\*.+?\*\*|`[^`]+`)/g;
    let lastIndex = 0;
    let match;

    while ((match = expression.exec(source)) !== null) {
      if (match.index > lastIndex) {
        tokens.push({ type: 'text', text: source.slice(lastIndex, match.index) });
      }
      const value = match[0];
      if (value.startsWith('**')) {
        tokens.push({ type: 'strong', text: value.slice(2, -2) });
      } else {
        tokens.push({ type: 'code', text: value.slice(1, -1) });
      }
      lastIndex = expression.lastIndex;
    }

    if (lastIndex < source.length) {
      tokens.push({ type: 'text', text: source.slice(lastIndex) });
    }
    return tokens.length ? tokens : [{ type: 'text', text: source }];
  }

  /**
   * 返回行内文本片段的样式类。
   *
   * @param {Object} token 文本片段
   * @returns {Object} 动态样式类
   */
  function inlineTokenClass(token) {
    return {
      'inline-strong': token.type === 'strong',
      'inline-code': token.type === 'code',
    };
  }

  /**
   * 生成多列表格的网格宽度。
   *
   * @param {Object} block 表格内容块
   * @returns {string} 行内网格样式
   */
  function tableGridStyle(block) {
    const columnCount = Math.max(block.headers?.length || 1, 1);
    return `grid-template-columns:repeat(${columnCount},132px);width:${columnCount * 132}px;`;
  }

  /**
   * 返回工具状态展示信息。
   *
   * @param {string} status 工具状态
   * @returns {Object} 标签类型和中文文案
   */
  function toolStatusMeta(status) {
    return (
      {
        WAITING_CONFIRMATION: { type: 'warning', text: '待确认' },
        CREATED: { type: 'primary', text: '已创建' },
        EXECUTING: { type: 'primary', text: '执行中' },
        SUCCESS: { type: 'success', text: '成功' },
        USER_REJECTED: { type: 'default', text: '已拒绝' },
        TIMEOUT: { type: 'danger', text: '超时' },
        SCHEMA_ERROR: { type: 'danger', text: 'Schema 错误' },
        FAILED: { type: 'danger', text: '失败' },
        CONFIRMATION_EXPIRED: { type: 'warning', text: '确认已过期' },
      }[status] || { type: 'default', text: status || '未知' }
    );
  }

  /**
   * 格式化工具参数和结果。
   *
   * @param {*} value 待格式化内容
   * @returns {string} 格式化文本
   */
  function prettyJson(value) {
    if (typeof value === 'string') return value;
    try {
      return JSON.stringify(value, null, 2);
    } catch (error) {
      return String(value);
    }
  }

  /**
   * 滚动到最新消息。
   */
  async function scrollBottom() {
    scrollTarget.value = '';
    await nextTick();
    scrollTarget.value = 'message-bottom';
  }

  /**
   * 复制助手回答。
   *
   * @param {string} content 回答正文
   */
  function copyAnswer(content) {
    const copyContent = String(content || '').trim();
    if (!copyContent) return;
    uni.setClipboardData({
      data: copyContent,
      success() {
        uni.showToast({ title: '已复制', icon: 'none' });
      },
    });
  }

  /**
   * 展示轻量反馈结果。
   *
   * @param {boolean} useful 是否有用
   */
  function showFeedback(useful) {
    uni.showToast({
      title: useful ? '感谢反馈' : '已记录，我们会继续优化',
      icon: 'none',
    });
  }

  /**
   * 展示引用片段内容。
   *
   * @param {Object} citation 引用信息
   */
  function showCitation(citation) {
    uni.showModal({
      title: citation.fileName || '知识库引用',
      content: String(citation.text || '该引用暂无可展示的片段正文。'),
      showCancel: false,
    });
  }

  /**
   * 展示当前助手知识库范围。
   */
  function showKnowledgeSummary() {
    uni.showModal({
      title: '关联知识库',
      content: assistantView.value.baseNames.join('、'),
      showCancel: false,
    });
  }

  /**
   * 说明附件应先进入平台知识库。
   */
  function showAttachmentTip() {
    uni.showToast({
      title: '请先在管理端知识库上传并完成解析',
      icon: 'none',
    });
  }

  /**
   * 处理助手操作菜单。
   *
   * @param {Object} event Wot Design Uni 菜单事件
   */
  async function handleActionSelect(event) {
    const action = event.item?.value;
    if (action === 'new') {
      conversationId.value = '';
      messages.value = [];
      question.value = '';
      return;
    }
    if (action === 'history') {
      uni.navigateTo({
        url: `/pages/conversation/index?assistantId=${assistantId.value}`,
      });
      return;
    }
    if (action === 'favorite') {
      try {
        await assistantStore.toggleFavorite(assistantView.value);
        uni.showToast({
          title: assistantView.value.favorited ? '已收藏' : '已取消收藏',
          icon: 'none',
        });
      } catch (error) {
        nexoraSentry.captureError(error);
        uni.showToast({ title: '收藏状态更新失败', icon: 'none' });
      }
    }
  }

  onLoad(initializePage);
</script>

<style lang="scss" scoped>
  @import '@/styles/assistant-theme.scss';

  .chat-page {
    height: 100vh;
    overflow: hidden;
  }

  .assistant-title,
  .assistant-title-line,
  .knowledge-strip,
  .welcome-message,
  .message-row,
  .tool-call-heading,
  .tool-name-line,
  .confirmation-actions,
  .message-actions,
  .thinking,
  .composer {
    display: flex;
    align-items: center;
  }

  .assistant-title {
    max-width: 238px;
    gap: 10px;
    text-align: left;
  }

  .assistant-title-line {
    min-width: 0;
    gap: 8px;
  }

  .assistant-title-name {
    overflow: hidden;
    color: var(--app-text);
    font-size: 18px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .online-dot {
    width: 8px;
    height: 8px;
    flex: none;
    background: var(--app-success);
    border-radius: 50%;
  }

  :deep(.wd-navbar__content) {
    height: 58px;
  }

  :deep(.wd-navbar__left) {
    padding-left: 18px;
  }

  :deep(.wd-navbar__right) {
    padding-right: 12px;
  }

  .knowledge-strip {
    height: 56px;
    margin: 14px 20px 0;
    padding: 0 16px;
    gap: 10px;
    color: var(--app-text-secondary);
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    box-shadow: var(--app-shadow);
  }

  .knowledge-strip-text {
    overflow: hidden;
    flex: 1;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .message-scroll {
    height: calc(100vh - 64px - 82px - env(safe-area-inset-bottom));
  }

  .knowledge-strip + .message-scroll {
    height: calc(100vh - 132px - 82px - env(safe-area-inset-bottom));
  }

  .message-list {
    padding: 26px 20px 34px;
  }

  .loading-area {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 10px;
    padding-top: 88px;
    color: var(--app-text-secondary);
    font-size: 14px;
  }

  .welcome-message {
    align-items: flex-start;
    gap: 12px;
  }

  .assistant-bubble {
    max-width: calc(100% - 62px);
    padding: 15px 17px;
    color: var(--app-text);
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    font-size: 15px;
    line-height: 1.65;
    box-shadow: var(--app-shadow);
  }

  .suggestion-list {
    display: flex;
    flex-wrap: wrap;
    gap: 9px;
    margin: 14px 0 28px 54px;
  }

  :deep(.suggestion-tag) {
    padding: 7px 12px;
    color: var(--app-text-secondary);
    background: transparent;
    border-color: var(--app-border);
    font-size: 13px;
  }

  .message-row {
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 30px;
  }

  .message-user {
    justify-content: flex-end;
  }

  .message-body {
    min-width: 0;
    max-width: calc(100% - 52px);
    flex: 1;
  }

  .message-user .message-body {
    flex: none;
    max-width: 86%;
  }

  .message-content {
    white-space: pre-wrap;
    word-break: break-word;
  }

  .user-bubble {
    padding: 13px 17px;
    color: #063844;
    background: #43d8ef;
    border-radius: 8px;
    font-size: 15px;
    line-height: 1.55;
    box-shadow: 0 8px 18px rgba(4, 191, 229, 0.14);
  }

  .assistant-content {
    color: var(--app-text);
    font-size: 15px;
    line-height: 1.75;
  }

  .md-heading {
    color: var(--app-text);
    font-weight: 800;
    line-height: 1.42;
  }

  .md-heading-1 {
    margin: 0 0 15px;
    font-size: 21px;
  }

  .md-heading-2 {
    margin: 2px 0 13px;
    font-size: 19px;
  }

  .md-heading-3 {
    margin: 2px 0 11px;
    font-size: 17px;
  }

  .md-paragraph {
    margin-bottom: 15px;
    color: var(--app-text-secondary);
    font-size: 15px;
    line-height: 1.8;
  }

  .md-paragraph:last-child {
    margin-bottom: 0;
  }

  .inline-strong {
    color: var(--app-text);
    font-weight: 800;
  }

  .inline-code {
    padding: 1px 5px;
    color: #0a7890;
    background: var(--app-primary-soft);
    border-radius: 4px;
    font-family: monospace;
    font-size: 13px;
  }

  .md-list {
    margin: 4px 0 16px;
  }

  .md-list-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 10px;
    color: var(--app-text-secondary);
    line-height: 1.75;
  }

  .md-list-marker {
    width: 24px;
    flex: none;
    color: var(--app-text-secondary);
    text-align: right;
  }

  .md-list-content {
    min-width: 0;
    flex: 1;
  }

  .md-data-table {
    overflow: hidden;
    margin: 6px 0 18px;
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    box-shadow: var(--app-shadow);
  }

  .md-data-table-head,
  .md-data-row {
    display: grid;
    grid-template-columns: minmax(82px, 34%) minmax(0, 1fr);
  }

  .md-data-table-head {
    color: var(--app-text-secondary);
    background: var(--app-surface-soft);
    font-size: 12px;
    font-weight: 700;
  }

  .md-data-table-head text,
  .md-data-label,
  .md-data-value {
    padding: 10px 12px;
  }

  .md-data-table-head text + text,
  .md-data-value {
    border-left: 1px solid var(--app-border);
  }

  .md-data-row {
    border-top: 1px solid var(--app-border);
  }

  .md-data-label {
    color: var(--app-text-secondary);
    background: var(--app-surface-soft);
    font-size: 12px;
    line-height: 1.55;
  }

  .md-data-value {
    min-width: 0;
    color: var(--app-text);
    font-size: 13px;
    line-height: 1.6;
    word-break: break-word;
  }

  .md-table-scroll {
    width: 100%;
    margin: 6px 0 18px;
    border: 1px solid var(--app-border);
    border-radius: 8px;
  }

  .md-wide-table {
    display: grid;
    overflow: hidden;
    background: var(--app-surface);
  }

  .md-wide-cell {
    box-sizing: border-box;
    min-height: 42px;
    padding: 10px 11px;
    color: var(--app-text);
    border-right: 1px solid var(--app-border);
    border-bottom: 1px solid var(--app-border);
    font-size: 12px;
    line-height: 1.55;
    word-break: break-word;
  }

  .md-wide-head {
    color: var(--app-text-secondary);
    background: var(--app-surface-soft);
    font-weight: 700;
  }

  .md-code {
    overflow: auto;
    margin: 6px 0 18px;
    padding: 13px;
    color: var(--app-text);
    background: var(--app-surface-soft);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    font-family: monospace;
    font-size: 12px;
    line-height: 1.6;
    white-space: pre;
  }

  .citation-list,
  .tool-call-list {
    margin-top: 14px;
  }

  .citation-item {
    display: flex;
    align-items: center;
    gap: 9px;
    min-height: 46px;
    padding: 10px 12px;
    color: var(--app-text-secondary);
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
  }

  .citation-item + .citation-item {
    margin-top: 8px;
  }

  .citation-text {
    min-width: 0;
    flex: 1;
    overflow: hidden;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tool-call-item {
    padding: 13px;
    background: var(--app-surface-soft);
    border: 1px solid var(--app-border);
    border-radius: 8px;
  }

  .tool-call-item + .tool-call-item {
    margin-top: 9px;
  }

  .tool-call-heading {
    justify-content: space-between;
    gap: 10px;
  }

  .tool-name-line {
    min-width: 0;
    gap: 7px;
    color: var(--app-text);
    font-size: 13px;
    font-weight: 700;
  }

  .tool-message {
    display: block;
    margin-top: 9px;
    color: var(--app-text-secondary);
    font-size: 12px;
    line-height: 1.5;
  }

  .tool-debug {
    margin-top: 9px;
  }

  .tool-debug-label {
    display: block;
    margin-bottom: 4px;
    color: var(--app-text-secondary);
    font-size: 11px;
  }

  .tool-debug-value {
    display: block;
    max-height: 150px;
    overflow: auto;
    padding: 9px;
    color: var(--app-text);
    background: var(--app-surface);
    border-radius: 6px;
    font-size: 11px;
    line-height: 1.45;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .confirmation-actions {
    justify-content: flex-end;
    gap: 9px;
    margin-top: 12px;
  }

  .message-actions {
    gap: 2px;
    margin-top: 10px;
    padding-top: 4px;
  }

  :deep(.message-actions .wd-button) {
    color: var(--app-text-secondary);
  }

  .thinking {
    gap: 9px;
    padding: 12px 14px;
    color: var(--app-text-secondary);
    background: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 8px;
    font-size: 13px;
  }

  .message-bottom {
    height: 1px;
  }

  .composer {
    position: fixed;
    z-index: 20;
    right: 0;
    bottom: 0;
    left: 0;
    min-height: 76px;
    padding: 10px 18px calc(10px + env(safe-area-inset-bottom));
    gap: 10px;
    background: var(--app-surface);
    border-top: 1px solid var(--app-border);
  }

  .composer-input {
    min-width: 0;
    flex: 1;
    padding: 0 14px;
    background: var(--app-surface-soft);
    border: 1px solid var(--app-border);
    border-radius: 24px;
  }

  :deep(.composer-input .wd-input) {
    min-height: 46px;
    padding: 0;
    background: transparent;
  }

  :deep(.composer-icon) {
    width: 46px;
    min-width: 46px;
    height: 46px;
    flex: none;
    color: var(--app-text-secondary);
    background: var(--app-surface-soft);
    border: 1px solid var(--app-border);
    border-radius: 50%;
  }

  :deep(.send-button) {
    width: 46px;
    min-width: 46px;
    height: 46px;
    padding: 0;
    border-radius: 50%;
  }

  @media (max-width: 350px) {
    .message-list {
      padding-right: 15px;
      padding-left: 15px;
    }

    .knowledge-strip {
      margin-right: 15px;
      margin-left: 15px;
    }
  }
</style>
