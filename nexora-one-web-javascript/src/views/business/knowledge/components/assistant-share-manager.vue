<template>
  <a-modal
    :open="open"
    :title="`${assistantName || '智能助手'} · 分享`"
    width="900px"
    :footer="null"
    destroy-on-close
    @cancel="close"
  >
    <a-tabs v-model:active-key="activeTab">
      <a-tab-pane v-if="allowCreate" key="create" tab="创建分享">
        <a-alert
          type="warning"
          show-icon
          message="分享链接在有效期内无需登录即可访问"
          description="公开问答仅使用该助手的知识库和模型，不执行 MCP 或其他业务工具。助手下架、删除、失去可见性或分享人账号不可用时，链接会自动失效。"
        />
        <a-form class="share-create-form" layout="vertical">
          <a-form-item label="有效期">
            <a-segmented v-model:value="expiryPreset" :options="expiryOptions" @change="applyExpiryPreset" />
          </a-form-item>
          <a-form-item v-if="expiryPreset === 'custom'" label="到期时间" required>
            <a-date-picker
              v-model:value="customExpireTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              :disabled-date="disablePastDate"
              style="width: 100%"
            />
          </a-form-item>
          <a-button type="primary" :loading="creating" @click="createShare">
            <ShareAltOutlined />生成免登录分享链接
          </a-button>
        </a-form>

        <section v-if="createdShare" class="created-share">
          <div>
            <strong>分享链接已生成</strong>
            <span>有效期至 {{ formatTime(createdShare.share.expireTime) }}</span>
          </div>
          <a-input :value="createdShare.share.shareUrl" readonly>
            <template #addonAfter>
              <a-space>
                <a-tooltip title="复制链接">
                  <CopyOutlined @click="copyLink(createdShare.share.shareUrl)" />
                </a-tooltip>
                <a-tooltip title="打开链接">
                  <ExportOutlined @click="openLink(createdShare.share.shareUrl)" />
                </a-tooltip>
              </a-space>
            </template>
          </a-input>
        </section>
      </a-tab-pane>

      <a-tab-pane key="records" tab="分享记录">
        <div class="share-record-toolbar">
          <span class="share-record-tip">分享人可停用本人链接，助手创建者可管理该助手的全部分享。</span>
          <a-button :loading="loading" @click="loadRecords"><ReloadOutlined />刷新</a-button>
        </div>
        <a-table
          :data-source="records"
          :loading="loading"
          :row-key="(row) => row.share.shareId"
          :pagination="{ pageSize: 8 }"
          :scroll="{ x: 900 }"
          size="middle"
        >
          <a-table-column title="分享人" :width="120">
            <template #default="{ record }">{{ record.share.sharerName || record.share.sharerId }}</template>
          </a-table-column>
          <a-table-column title="状态" :width="110">
            <template #default="{ record }">
              <a-tag :color="statusMeta(record.status).color">{{ record.statusText }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="有效期至" :width="175">
            <template #default="{ record }">{{ formatTime(record.share.expireTime) }}</template>
          </a-table-column>
          <a-table-column title="访问次数" data-index="accessCount" :width="90">
            <template #default="{ record }">{{ record.share.accessCount || 0 }}</template>
          </a-table-column>
          <a-table-column title="创建时间" :width="175">
            <template #default="{ record }">{{ formatTime(record.share.createTime) }}</template>
          </a-table-column>
          <a-table-column title="失效原因" :width="150">
            <template #default="{ record }">{{ record.disableReasonText || '-' }}</template>
          </a-table-column>
          <a-table-column title="操作" :width="190" fixed="right">
            <template #default="{ record }">
              <a-button type="link" @click="copyLink(record.share.shareUrl)">复制</a-button>
              <a-button type="link" :disabled="record.status !== 'ACTIVE'" @click="openLink(record.share.shareUrl)">
                打开
              </a-button>
              <a-popconfirm
                v-if="record.canDisable && record.status === 'ACTIVE'"
                title="停用后该链接会立即失效，确认继续？"
                @confirm="disableShare(record)"
              >
                <a-button type="link" danger>停用</a-button>
              </a-popconfirm>
            </template>
          </a-table-column>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup>
  import dayjs from 'dayjs';
  import { message } from 'ant-design-vue';
  import { ref, watch } from 'vue';
  import {
    CopyOutlined,
    ExportOutlined,
    ReloadOutlined,
    ShareAltOutlined,
  } from '@ant-design/icons-vue';
  import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
  import { nexoraSentry } from '/@/lib/nexora-sentry';

  const props = defineProps({
    open: { type: Boolean, default: false },
    assistantId: { type: [Number, String], default: null },
    assistantName: { type: String, default: '' },
    allowCreate: { type: Boolean, default: false },
    initialTab: { type: String, default: 'records' },
  });
  const emit = defineEmits(['update:open']);

  const activeTab = ref('records');
  const expiryPreset = ref('7');
  const customExpireTime = ref(null);
  const creating = ref(false);
  const loading = ref(false);
  const createdShare = ref(null);
  const records = ref([]);
  const expiryOptions = [
    { label: '1 天', value: '1' },
    { label: '7 天', value: '7' },
    { label: '30 天', value: '30' },
    { label: '90 天', value: '90' },
    { label: '自定义', value: 'custom' },
  ];

  /** 关闭分享弹窗。 */
  function close() {
    emit('update:open', false);
  }

  /** 切换预设时准备自定义时间的默认值。 */
  function applyExpiryPreset(value) {
    if (value === 'custom' && !customExpireTime.value) {
      customExpireTime.value = dayjs().add(7, 'day');
    }
  }

  /** 禁止选择今天之前的日期，具体时刻由提交校验兜底。 */
  function disablePastDate(current) {
    return current && current.endOf('day').isBefore(dayjs());
  }

  /** 计算当前表单对应的分享过期时间。 */
  function resolveExpireTime() {
    if (expiryPreset.value === 'custom') return customExpireTime.value;
    return dayjs().add(Number(expiryPreset.value), 'day');
  }

  /** 创建无需登录的助手分享链接。 */
  async function createShare() {
    const expireTime = resolveExpireTime();
    if (!expireTime || !expireTime.isAfter(dayjs())) {
      message.warning('请选择晚于当前时间的有效期');
      return;
    }
    creating.value = true;
    try {
      const response = await api.createAssistantShare(props.assistantId, {
        expireTime: expireTime.format('YYYY-MM-DD HH:mm:ss'),
        accessOrigin: window.location.origin,
      });
      createdShare.value = response.data;
      message.success('分享链接已生成');
      await loadRecords();
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      creating.value = false;
    }
  }

  /** 查询当前用户可见的分享记录。 */
  async function loadRecords() {
    if (!props.assistantId) return;
    loading.value = true;
    try {
      const response = await api.assistantShareRecords(props.assistantId);
      records.value = response.data || [];
    } catch (error) {
      nexoraSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  /** 主动停用一条仍生效的分享记录。 */
  async function disableShare(record) {
    await api.disableAssistantShare(props.assistantId, record.share.shareId);
    message.success('分享链接已停用');
    await loadRecords();
  }

  /** 复制分享链接，并兼容非安全上下文浏览器。 */
  async function copyLink(url) {
    try {
      await navigator.clipboard.writeText(url);
    } catch {
      const input = document.createElement('textarea');
      input.value = url;
      input.style.position = 'fixed';
      input.style.opacity = '0';
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      input.remove();
    }
    message.success('链接已复制');
  }

  /** 在新窗口打开公开问答链接。 */
  function openLink(url) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  /** 格式化服务端时间。 */
  function formatTime(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
  }

  /** 返回分享状态的标签颜色。 */
  function statusMeta(status) {
    return (
      {
        ACTIVE: { color: 'green' },
        EXPIRED: { color: 'default' },
        USER_DISABLED: { color: 'orange' },
        SYSTEM_DISABLED: { color: 'red' },
      }[status] || { color: 'default' }
    );
  }

  watch(
    () => [props.open, props.assistantId, props.initialTab],
    ([open]) => {
      if (!open) return;
      activeTab.value = props.allowCreate ? props.initialTab : 'records';
      createdShare.value = null;
      loadRecords();
    },
    { immediate: true },
  );
</script>

<style scoped lang="less">
  .share-create-form {
    max-width: 620px;
    margin-top: 20px;
  }

  .created-share {
    display: grid;
    gap: 12px;
    margin-top: 20px;
    padding: 16px;
    border: 1px solid #b7ebc6;
    border-radius: 6px;
    background: #f6ffed;
  }

  .created-share > div {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    color: #52616f;
  }

  .created-share strong {
    color: #162332;
  }

  .share-record-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
  }

  .share-record-tip {
    color: #667085;
  }

  @media (max-width: 640px) {
    .created-share > div,
    .share-record-toolbar {
      align-items: flex-start;
      flex-direction: column;
    }
  }
</style>
