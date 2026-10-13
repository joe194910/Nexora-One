<template>
  <div class="public-chat" :class="{ 'public-chat--dark': darkMode }">
    <header class="public-chat__header">
      <div class="brand">
        <span class="brand__mark">N</span>
        <span>NexoraOne</span>
      </div>
      <div class="header-actions">
        <a-tooltip :title="darkMode ? '切换浅色' : '切换深色'">
          <a-button shape="circle" size="large" @click="toggleTheme">
            <BulbOutlined />
          </a-button>
        </a-tooltip>
      </div>
    </header>

    <a-spin :spinning="loading" class="public-chat__loading">
      <a-result
        v-if="loadError"
        status="warning"
        title="该分享链接当前不可用"
        :sub-title="loadError"
      >
        <template #extra>
          <a-button @click="loadShare">重新检查</a-button>
        </template>
      </a-result>

      <main v-else-if="share" class="public-chat__workspace">
        <section class="assistant-heading">
          <span class="assistant-heading__avatar"><RobotOutlined /></span>
          <div>
            <div class="assistant-heading__title">
              <h1>{{ share.assistantName }}</h1>
              <span class="online-dot" />
            </div>
            <p>由 {{ share.ownerName || 'NexoraOne' }} 提供 · 链接有效期至 {{ formatTime(share.expireTime) }}</p>
          </div>
        </section>

        <section class="knowledge-strip">
          <ReadOutlined />
          <span>基于 {{ share.baseNames?.join('、') || '已授权知识库' }} 提供回答</span>
          <a-tag color="cyan">免登录访问</a-tag>
        </section>

        <section ref="messageArea" class="public-chat__messages">
          <div v-if="!messages.length" class="welcome-message">
            <span class="message-avatar"><RobotOutlined /></span>
            <div class="assistant-bubble">
              你好，我是{{ share.assistantName }}。你可以直接提出问题，公开会话不会执行 MCP 或业务操作工具。
            </div>
          </div>

          <div
            v-for="(entry, index) in messages"
            :key="`${entry.role}-${index}`"
            class="message-row"
            :class="`message-row--${entry.role}`"
          >
            <span v-if="entry.role === 'assistant'" class="message-avatar"><RobotOutlined /></span>
            <div class="message-content">
              <div v-if="entry.role === 'user'" class="user-bubble">{{ entry.content }}</div>
              <div v-else class="assistant-bubble">
                <MarkdownContent :content="entry.content" />
              </div>
              <div v-if="entry.role === 'assistant' && entry.citations?.length" class="citations">
                <div v-for="citation in entry.citations" :key="`${citation.documentId}-${citation.chunkIndex}`">
                  <FileTextOutlined />
                  {{ citation.fileName }} · 切片 {{ citation.chunkIndex }}
                </div>
              </div>
              <a-button
                v-if="entry.role === 'assistant'"
                class="copy-answer"
                type="text"
                size="small"
                @click="copyAnswer(entry.content)"
              >
                <CopyOutlined />复制
              </a-button>
            </div>
          </div>

          <div v-if="sending" class="message-row message-row--assistant">
            <span class="message-avatar"><RobotOutlined /></span>
            <div class="assistant-bubble thinking"><a-spin size="small" /> 正在检索并生成回答…</div>
          </div>
        </section>

        <footer class="composer">
          <div class="composer__box">
            <a-textarea
              v-model:value="question"
              :auto-size="{ minRows: 1, maxRows: 5 }"
              :maxlength="4000"
              :disabled="sending"
              placeholder="输入问题..."
              @keydown="handleKeydown"
            />
            <a-button
              type="primary"
              shape="circle"
              size="large"
              :loading="sending"
              :disabled="!question.trim()"
              aria-label="发送问题"
              @click="send"
            >
              <SendOutlined />
            </a-button>
          </div>
          <p>分享会话仅保存在当前浏览器。请勿输入密码、令牌等敏感信息。</p>
        </footer>
      </main>
    </a-spin>
  </div>
</template>

<script setup>
  import dayjs from 'dayjs';
  import { message } from 'ant-design-vue';
  import { nextTick, onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import {
    BulbOutlined,
    CopyOutlined,
    FileTextOutlined,
    ReadOutlined,
    RobotOutlined,
    SendOutlined,
  } from '@ant-design/icons-vue';
  import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
  import MarkdownContent from './components/markdown-content.vue';

  const route = useRoute();
  const shareCode = String(route.params.code || '');
  const share = ref(null);
  const messages = ref([]);
  const question = ref('');
  const loading = ref(true);
  const sending = ref(false);
  const loadError = ref('');
  const messageArea = ref(null);
  const darkMode = ref(localStorage.getItem('nexora-share-theme') === 'dark');

  /** 返回当前分享链接独立使用的浏览器缓存键。 */
  function cacheKey() {
    return `nexora:assistant-share:${shareCode}`;
  }

  /** 从浏览器恢复该公开链接最近的问答内容。 */
  function restoreMessages() {
    try {
      const cached = JSON.parse(localStorage.getItem(cacheKey()) || '[]');
      messages.value = Array.isArray(cached) ? cached.slice(-40) : [];
    } catch {
      messages.value = [];
    }
  }

  /** 将公开会话保存到当前浏览器，避免刷新页面后丢失。 */
  function persistMessages() {
    localStorage.setItem(cacheKey(), JSON.stringify(messages.value.slice(-40)));
  }

  /** 查询公开分享详情，服务端会同步校验分享人和助手可用状态。 */
  async function loadShare() {
    loading.value = true;
    loadError.value = '';
    try {
      const response = await api.publicAssistantShare(shareCode);
      share.value = response.data;
      restoreMessages();
      scrollBottom();
    } catch (error) {
      share.value = null;
      loadError.value = error?.response?.data?.msg || error?.message || '链接可能已过期或被停用，请联系分享人。';
    } finally {
      loading.value = false;
    }
  }

  /** 发送公开问答，并仅携带浏览器内最近的文本上下文。 */
  async function send() {
    const text = question.value.trim();
    if (!text || sending.value) return;
    const history = messages.value.slice(-12).map((entry) => ({
      role: entry.role,
      content: entry.content,
    }));
    const userEntry = { role: 'user', content: text };
    messages.value.push(userEntry);
    question.value = '';
    sending.value = true;
    persistMessages();
    scrollBottom();
    try {
      const response = await api.publicAssistantShareChat(shareCode, {
        question: text,
        history,
      });
      const result = response.data;
      messages.value.push({
        role: 'assistant',
        content: result.content,
        citations: result.citations || [],
      });
      persistMessages();
    } catch (error) {
      messages.value = messages.value.filter((entry) => entry !== userEntry);
      question.value = text;
      persistMessages();
      message.error(error?.response?.data?.msg || '发送失败，分享链接可能已失效');
    } finally {
      sending.value = false;
      scrollBottom();
    }
  }

  /** 回车发送，Shift+Enter 保留换行。 */
  function handleKeydown(event) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return;
    event.preventDefault();
    send();
  }

  /** 复制一条助手回答。 */
  async function copyAnswer(content) {
    await navigator.clipboard.writeText(String(content || ''));
    message.success('回答已复制');
  }

  /** 切换并记住公开问答页的显示色系。 */
  function toggleTheme() {
    darkMode.value = !darkMode.value;
    localStorage.setItem('nexora-share-theme', darkMode.value ? 'dark' : 'light');
  }

  /** 格式化链接有效期。 */
  function formatTime(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
  }

  /** 新消息出现后滚动到问答区底部。 */
  async function scrollBottom() {
    await nextTick();
    if (messageArea.value) messageArea.value.scrollTop = messageArea.value.scrollHeight;
  }

  onMounted(loadShare);
</script>

<style scoped lang="less">
  .public-chat {
    --page-bg: #f5f8fb;
    --panel-bg: #ffffff;
    --soft-bg: #eef4f8;
    --border: #dce5ec;
    --text: #0b1728;
    --muted: #6f7d91;
    min-height: 100vh;
    color: var(--text);
    background: var(--page-bg);
    transition: background 0.2s ease, color 0.2s ease;
  }

  .public-chat--dark {
    --page-bg: #07121b;
    --panel-bg: #0e1b26;
    --soft-bg: #142431;
    --border: #2b3b49;
    --text: #f5f8fb;
    --muted: #9babbd;
  }

  .public-chat__header {
    position: sticky;
    z-index: 5;
    top: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 72px;
    padding: 0 32px;
    border-bottom: 1px solid var(--border);
    background: color-mix(in srgb, var(--page-bg) 92%, transparent);
    backdrop-filter: blur(14px);
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 22px;
    font-weight: 700;
  }

  .brand__mark {
    display: grid;
    width: 38px;
    height: 38px;
    place-items: center;
    border-radius: 6px;
    color: #00131b;
    background: #11c8e8;
  }

  .public-chat__loading {
    width: 100%;
    min-height: calc(100vh - 72px);
  }

  .public-chat__workspace {
    display: grid;
    grid-template-rows: auto auto minmax(260px, 1fr) auto;
    width: min(960px, 100%);
    min-height: calc(100vh - 72px);
    margin: 0 auto;
    padding: 30px 28px 18px;
  }

  .assistant-heading,
  .assistant-heading__title,
  .knowledge-strip,
  .welcome-message,
  .message-row {
    display: flex;
    align-items: center;
  }

  .assistant-heading {
    gap: 16px;
  }

  .assistant-heading__avatar,
  .message-avatar {
    display: grid;
    flex: 0 0 auto;
    place-items: center;
    color: #03bfe4;
    background: #dff8fd;
  }

  .assistant-heading__avatar {
    width: 58px;
    height: 58px;
    border-radius: 8px;
    font-size: 28px;
  }

  .assistant-heading__title {
    gap: 10px;
  }

  .assistant-heading h1 {
    margin: 0;
    color: var(--text);
    font-size: 26px;
    letter-spacing: 0;
  }

  .assistant-heading p {
    margin: 5px 0 0;
    color: var(--muted);
  }

  .online-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #16c784;
  }

  .knowledge-strip {
    gap: 10px;
    margin-top: 22px;
    padding: 14px 18px;
    border: 1px solid var(--border);
    border-radius: 6px;
    color: var(--muted);
    background: var(--panel-bg);
  }

  .knowledge-strip span {
    flex: 1;
  }

  .public-chat__messages {
    overflow-y: auto;
    padding: 30px 4px;
  }

  .welcome-message,
  .message-row {
    align-items: flex-start;
    gap: 14px;
    margin-bottom: 24px;
  }

  .message-row--user {
    justify-content: flex-end;
  }

  .message-avatar {
    width: 42px;
    height: 42px;
    border-radius: 8px;
    font-size: 20px;
  }

  .message-content {
    max-width: min(720px, calc(100% - 56px));
  }

  .assistant-bubble,
  .user-bubble {
    padding: 15px 18px;
    border-radius: 8px;
    font-size: 15px;
    line-height: 1.75;
  }

  .assistant-bubble {
    border: 1px solid var(--border);
    background: var(--panel-bg);
  }

  .user-bubble {
    color: #00131b;
    background: #28cee9;
  }

  .citations {
    display: grid;
    gap: 6px;
    margin-top: 9px;
    color: var(--muted);
    font-size: 13px;
  }

  .copy-answer {
    margin-top: 5px;
    color: var(--muted);
  }

  .thinking {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--muted);
  }

  .composer {
    position: sticky;
    bottom: 0;
    padding-top: 12px;
    background: var(--page-bg);
  }

  .composer__box {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    padding: 10px 12px 10px 18px;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--panel-bg);
    box-shadow: 0 10px 28px rgb(14 36 52 / 8%);
  }

  .composer :deep(.ant-input) {
    color: var(--text);
    background: transparent;
    resize: none;
  }

  .composer p {
    margin: 8px 0 0;
    color: var(--muted);
    text-align: center;
    font-size: 12px;
  }

  .public-chat--dark :deep(.knowledge-markdown),
  .public-chat--dark :deep(.knowledge-markdown h1),
  .public-chat--dark :deep(.knowledge-markdown h2),
  .public-chat--dark :deep(.knowledge-markdown h3) {
    color: var(--text);
  }

  @media (max-width: 640px) {
    .public-chat__header {
      height: 60px;
      padding: 0 16px;
    }

    .brand {
      font-size: 18px;
    }

    .brand__mark {
      width: 32px;
      height: 32px;
    }

    .public-chat__workspace {
      min-height: calc(100vh - 60px);
      padding: 20px 16px 12px;
    }

    .assistant-heading h1 {
      font-size: 21px;
    }

    .assistant-heading p {
      font-size: 12px;
    }

    .knowledge-strip {
      align-items: flex-start;
      flex-wrap: wrap;
    }

    .knowledge-strip span {
      flex-basis: calc(100% - 30px);
    }

    .public-chat__messages {
      padding: 24px 0;
    }

    .message-content {
      max-width: calc(100% - 52px);
    }

    .message-row--user .message-content {
      max-width: 86%;
    }
  }
</style>
