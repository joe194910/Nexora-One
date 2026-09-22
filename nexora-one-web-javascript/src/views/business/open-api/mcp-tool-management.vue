<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div>
        <h1 class="open-api-page__title">MCP工具管理</h1>
        <div class="open-api-page__subtitle">统一管理标准 MCP、平台 API 工具和旧系统 HTTP 适配工具。</div>
      </div>
      <a-space>
        <a-button @click="showAccessGuide"><FileTextOutlined />接入文档</a-button>
        <a-button @click="openExternalDrawer" v-privilege="'mcp:tool:save'"> <ApiOutlined />添加HTTP适配工具 </a-button>
        <a-button type="primary" @click="openServerDrawer()" v-privilege="'mcp:server:save'"> <PlusOutlined />注册MCP Server </a-button>
      </a-space>
    </header>

    <section class="mcp-view-switch">
      <a-segmented v-model:value="activeView" :options="viewOptions" />
    </section>

    <template v-if="activeView === 'tools'">
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
              <a-select-option value="STANDARD_MCP">标准MCP</a-select-option>
              <a-select-option value="EXTERNAL_HTTP">HTTP适配</a-select-option>
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
        <a-table :loading="loading" :data-source="rows" :columns="columns" row-key="toolId" :pagination="false" :scroll="{ x: 1450 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'toolName'">
              <strong>{{ record.toolName }}</strong>
              <div class="open-api-muted">{{ record.toolCode }}</div>
              <div class="open-api-api-cell__description">{{ record.description }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'sourceType'">
              <a-tag :color="sourceMeta(record.sourceType).color">
                {{ sourceMeta(record.sourceType).text }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'sourceName'">
              <strong>{{ record.sourceName || '-' }}</strong>
              <div class="open-api-muted">{{ record.sourceCode || '-' }}</div>
              <div v-if="record.sourceVersion" class="open-api-muted">
                {{ record.sourceVersion }}
                <a-tag v-if="record.syncAvailable" color="orange">可同步 {{ record.latestVersion }}</a-tag>
              </div>
              <a-tag v-if="record.sourceType === 'STANDARD_MCP' && record.schemaSyncRequired" color="orange">
                Schema待同步
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'toolType'">
              {{ record.toolType === 'ACTION' ? '数据操作' : '数据查询' }}
            </template>
            <template v-else-if="column.dataIndex === 'riskLevel'">
              <a-tag :color="riskMeta(record.riskLevel).color">{{ riskMeta(record.riskLevel).text }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'auditStatus'">
              <a-badge :status="auditMeta(record.auditStatus).badge" :text="auditMeta(record.auditStatus).text" />
              <div v-if="record.lastTestStatus" class="open-api-muted">测试：{{ record.lastTestStatus === 'SUCCESS' ? '成功' : '失败' }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'enabledStatus'">
              <a-badge
                :status="record.enabledStatus === 'ENABLED' ? 'success' : 'default'"
                :text="record.enabledStatus === 'ENABLED' ? '已启用' : '已停用'"
              />
              <div class="open-api-muted">在线：{{ onlineText(record.onlineStatus) }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'assistantCount'"> {{ record.assistantCount || 0 }} 个 </template>
            <template v-else-if="column.dataIndex === 'lastCallTime'">
              <div>{{ record.lastCallTime || '-' }}</div>
              <div class="open-api-muted">{{ formatNumber(record.totalCallCount) }} 次</div>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <div class="open-api-actions">
                <a-button type="link" @click="openDetail(record)">详情</a-button>
                <a-popconfirm
                  v-if="record.sourceType === 'STANDARD_MCP' && record.schemaSyncRequired"
                  title="同步后将采用远端最新Schema，并重新进入测试、审核和启用流程，确认继续？"
                  @confirm="syncServerToolSchema(record)"
                >
                  <a-button type="link" v-privilege="'mcp:server:probe'">同步定义</a-button>
                </a-popconfirm>
                <a-button type="link" @click="openTest(record)" v-privilege="'mcp:tool:test'">测试</a-button>
                <a-button type="link" @click="openReview(record)" v-privilege="'mcp:tool:review'"> 审核 </a-button>
                <a-button
                  v-if="record.sourceType === 'PLATFORM_API' && record.syncAvailable"
                  type="link"
                  @click="syncTool(record)"
                  v-privilege="'mcp:tool:save'"
                >
                  同步版本
                </a-button>
                <a-popconfirm :title="record.enabledStatus === 'ENABLED' ? '确认停用该工具？' : '确认启用该工具？'" @confirm="toggleStatus(record)">
                  <a-button type="link" :danger="record.enabledStatus === 'ENABLED'" v-privilege="'mcp:tool:save'">
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
    </template>

    <template v-else>
      <section class="open-api-summary">
        <div v-for="item in serverSummaryItems" :key="item.key" class="open-api-summary__item">
          <span class="open-api-summary__icon" :class="item.iconClass">
            <component :is="item.icon" />
          </span>
          <div>
            <div class="open-api-summary__label">{{ item.label }}</div>
            <div class="open-api-summary__value">{{ serverSummary[item.key] || 0 }}</div>
          </div>
        </div>
      </section>

      <section class="open-api-panel">
        <div class="mcp-server-query">
          <div>
            <label class="open-api-query__label">Server名称 / 编码</label>
            <a-input v-model:value="serverQuery.searchWord" allow-clear placeholder="请输入 Server 名称或编码" @pressEnter="queryServers" />
          </div>
          <div>
            <label class="open-api-query__label">所属应用</label>
            <a-select
              v-model:value="serverQuery.applicationId"
              allow-clear
              show-search
              option-filter-prop="label"
              placeholder="请选择所属应用"
              :options="applicationOptions"
            />
          </div>
          <div>
            <label class="open-api-query__label">在线状态</label>
            <a-select v-model:value="serverQuery.onlineStatus" allow-clear placeholder="请选择在线状态">
              <a-select-option value="UNKNOWN">未探活</a-select-option>
              <a-select-option value="ONLINE">在线</a-select-option>
              <a-select-option value="OFFLINE">离线</a-select-option>
              <a-select-option value="ABNORMAL">异常</a-select-option>
            </a-select>
          </div>
          <div>
            <label class="open-api-query__label">启用状态</label>
            <a-select v-model:value="serverQuery.enabledFlag" allow-clear placeholder="请选择启用状态">
              <a-select-option :value="true">已启用</a-select-option>
              <a-select-option :value="false">已停用</a-select-option>
            </a-select>
          </div>
          <a-space>
            <a-button type="primary" @click="queryServers"><SearchOutlined />查询</a-button>
            <a-button @click="resetServerQuery"><ReloadOutlined />重置</a-button>
          </a-space>
        </div>
      </section>

      <section class="open-api-panel">
        <h2 class="open-api-panel__title">MCP Server列表</h2>
        <a-table
          :loading="serverLoading"
          :data-source="serverRows"
          :columns="serverColumns"
          row-key="serverId"
          :pagination="false"
          :scroll="{ x: 1380 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'serverName'">
              <strong>{{ record.serverName }}</strong>
              <div class="open-api-muted">{{ record.serverCode }}</div>
              <div class="open-api-api-cell__description">{{ record.description || '-' }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'applicationName'">
              <strong>{{ record.applicationName || '-' }}</strong>
              <div class="open-api-muted">{{ record.applicationCode || '-' }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'endpointUrl'">
              <div class="mcp-endpoint">{{ record.endpointUrl }}</div>
              <div class="open-api-muted">{{ authText(record.authType) }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'protocolVersion'">
              <div>{{ record.protocolVersion || '未协商' }}</div>
              <div class="open-api-muted">Server {{ record.serverVersion || '-' }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'onlineStatus'">
              <a-badge :status="serverOnlineMeta(record.onlineStatus).badge" :text="serverOnlineMeta(record.onlineStatus).text" />
              <div class="open-api-muted">{{ record.discoveredToolCount || 0 }} 个工具</div>
            </template>
            <template v-else-if="column.dataIndex === 'lastProbeTime'">
              <div>{{ record.lastProbeTime || '-' }}</div>
              <div class="open-api-api-cell__description">{{ record.lastProbeMessage || '尚未探活' }}</div>
            </template>
            <template v-else-if="column.dataIndex === 'enabledFlag'">
              <a-badge :status="record.enabledFlag ? 'success' : 'default'" :text="record.enabledFlag ? '已启用' : '已停用'" />
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <div class="open-api-actions">
                <a-button type="link" @click="openServerDetail(record)">详情</a-button>
                <a-button type="link" @click="openServerDrawer(record)" v-privilege="'mcp:server:save'">编辑</a-button>
                <a-button type="link" :disabled="!record.enabledFlag" @click="probeServer(record)" v-privilege="'mcp:server:probe'">
                  探活并刷新目录
                </a-button>
                <a-popconfirm
                  :title="record.enabledFlag ? '确认停用该 Server 及其工具？' : '确认启用该 Server？启用后需要重新探活。'"
                  @confirm="toggleServerStatus(record)"
                >
                  <a-button type="link" :danger="record.enabledFlag" v-privilege="'mcp:server:save'">
                    {{ record.enabledFlag ? '停用' : '启用' }}
                  </a-button>
                </a-popconfirm>
              </div>
            </template>
          </template>
        </a-table>
        <div class="open-api-pagination">
          <a-pagination
            v-model:current="serverQuery.pageNum"
            v-model:page-size="serverQuery.pageSize"
            show-size-changer
            :total="serverTotal"
            :show-total="(value) => `共 ${value} 条`"
            @change="queryServers"
          />
        </div>
      </section>
    </template>

    <a-drawer v-model:open="externalDrawer.open" title="登记HTTP适配工具" width="min(680px, 100vw)">
      <a-alert
        type="warning"
        show-icon
        message="这是旧系统 HTTP 适配能力，不是标准 MCP。平台会使用现有应用密钥对请求签名，对方只需校验签名并返回业务数据。"
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

    <a-drawer v-model:open="serverDrawer.open" :title="serverForm.serverId ? '编辑MCP Server' : '注册MCP Server'" width="min(720px, 100vw)">
      <a-alert
        type="info"
        show-icon
        message="请填写对方系统提供的标准 MCP Streamable HTTP endpoint。这里只配置静态 Bearer Token 或 API Key，不调用对方登录接口。"
        style="margin-bottom: 18px"
      />
      <a-form layout="vertical">
        <a-form-item label="所属应用" required>
          <a-select
            v-model:value="serverForm.applicationId"
            show-search
            option-filter-prop="label"
            placeholder="请选择当前可管理的应用"
            :options="applicationOptions"
            :disabled="Boolean(serverForm.serverId)"
          />
        </a-form-item>
        <div class="mcp-form-grid">
          <a-form-item label="Server名称" required>
            <a-input v-model:value="serverForm.serverName" :maxlength="100" />
          </a-form-item>
          <a-form-item label="Server编码" required extra="以字母开头，支持字母、数字、中划线和下划线">
            <a-input v-model:value="serverForm.serverCode" :maxlength="100" :disabled="Boolean(serverForm.serverId)" />
          </a-form-item>
        </div>
        <a-form-item label="Streamable HTTP endpoint" required>
          <a-input v-model:value="serverForm.endpointUrl" placeholder="https://partner.example.com/mcp" />
        </a-form-item>
        <div class="mcp-form-grid">
          <a-form-item label="鉴权方式" required>
            <a-select v-model:value="serverForm.authType" @change="handleServerAuthTypeChange">
              <a-select-option value="NONE">无需鉴权</a-select-option>
              <a-select-option value="BEARER">Bearer Token</a-select-option>
              <a-select-option value="API_KEY_HEADER">API Key</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="超时时间" required>
            <a-input-number v-model:value="serverForm.timeoutSeconds" :min="1" :max="60" addon-after="秒" style="width: 100%" />
          </a-form-item>
          <a-form-item v-if="serverForm.authType === 'API_KEY_HEADER'" label="请求头名称" required>
            <a-input v-model:value="serverForm.authHeaderName" placeholder="X-API-Key" :maxlength="100" />
          </a-form-item>
          <a-form-item
            v-if="serverForm.authType !== 'NONE'"
            label="鉴权凭证"
            :required="!serverForm.serverId || !serverForm.credentialConfigured"
            :extra="serverCredentialHelp"
          >
            <a-input-password
              v-model:value="serverForm.authSecret"
              autocomplete="new-password"
              :placeholder="serverForm.authType === 'BEARER' ? '请输入 Token，不需要填写 Bearer 前缀' : '请输入 API Key'"
            />
          </a-form-item>
        </div>
        <a-form-item label="能力说明">
          <a-textarea v-model:value="serverForm.description" :rows="4" :maxlength="2000" show-count />
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="serverForm.enabledFlag" />
          <span class="mcp-inline-help">启用后才能探活、发现和调用工具</span>
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="publish-tool-footer">
          <a-button @click="serverDrawer.open = false">取消</a-button>
          <a-button :loading="serverDrawer.saving" @click="saveServer(false)">仅保存</a-button>
          <a-button type="primary" :loading="serverDrawer.saving" :disabled="!serverForm.enabledFlag" @click="saveServer(true)">
            保存并探活
          </a-button>
        </div>
      </template>
    </a-drawer>

    <a-drawer v-model:open="serverDetail.open" title="MCP Server详情" width="min(820px, 100vw)">
      <a-spin :spinning="serverDetail.loading">
        <template v-if="serverDetail.record">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="Server名称">{{ serverDetail.record.serverName }}</a-descriptions-item>
            <a-descriptions-item label="Server编码">{{ serverDetail.record.serverCode }}</a-descriptions-item>
            <a-descriptions-item label="所属应用">{{ serverDetail.record.applicationName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="传输协议">Streamable HTTP</a-descriptions-item>
            <a-descriptions-item label="Endpoint" :span="2">
              <span class="mcp-endpoint">{{ serverDetail.record.endpointUrl }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="鉴权方式">{{ authText(serverDetail.record.authType) }}</a-descriptions-item>
            <a-descriptions-item label="凭证状态">
              {{ serverDetail.record.authType === 'NONE' ? '无需凭证' : serverDetail.record.credentialConfigured ? '已配置' : '未配置' }}
            </a-descriptions-item>
            <a-descriptions-item label="协议版本">{{ serverDetail.record.protocolVersion || '-' }}</a-descriptions-item>
            <a-descriptions-item label="Server版本">{{ serverDetail.record.serverVersion || '-' }}</a-descriptions-item>
            <a-descriptions-item label="在线状态">{{ serverOnlineMeta(serverDetail.record.onlineStatus).text }}</a-descriptions-item>
            <a-descriptions-item label="发现工具">{{ serverDetail.record.discoveredToolCount || 0 }} 个</a-descriptions-item>
            <a-descriptions-item label="最近探活" :span="2">
              {{ serverDetail.record.lastProbeTime || '-' }} · {{ serverDetail.record.lastProbeMessage || '尚未探活' }}
            </a-descriptions-item>
          </a-descriptions>
          <h3 class="mcp-schema-title">发现的工具</h3>
          <a-table :data-source="serverDetail.record.tools || []" :columns="serverToolColumns" row-key="toolId" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'toolName'">
                <strong>{{ record.toolName }}</strong>
                <div class="open-api-muted">{{ record.toolCode }}</div>
                <div v-if="record.lastTestMessage" class="open-api-api-cell__description">
                  {{ record.lastTestMessage }}
                </div>
              </template>
              <template v-else-if="column.dataIndex === 'auditStatus'">
                {{ auditMeta(record.auditStatus).text }}
              </template>
              <template v-else-if="column.dataIndex === 'enabledStatus'">
                {{ record.enabledStatus === 'ENABLED' ? '已启用' : '已停用' }}
              </template>
              <template v-else-if="column.dataIndex === 'onlineStatus'">
                {{ onlineText(record.onlineStatus) }}
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-popconfirm
                  v-if="record.schemaSyncRequired"
                  title="同步后将采用远端最新Schema，并重新进入测试、审核和启用流程，确认继续？"
                  @confirm="syncServerToolSchema(record)"
                >
                  <a-button type="link" v-privilege="'mcp:server:probe'">同步定义</a-button>
                </a-popconfirm>
                <span v-else class="open-api-muted">-</span>
              </template>
            </template>
          </a-table>
        </template>
      </a-spin>
    </a-drawer>

    <a-drawer v-model:open="detailDrawer.open" title="MCP工具详情" width="min(760px, 100vw)">
      <a-spin :spinning="detailDrawer.loading">
        <template v-if="detailDrawer.record">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="工具名称">{{ detailDrawer.record.toolName }}</a-descriptions-item>
            <a-descriptions-item label="工具编码">{{ detailDrawer.record.toolCode }}</a-descriptions-item>
            <a-descriptions-item label="工具来源">
              {{ sourceMeta(detailDrawer.record.sourceType).text }}
            </a-descriptions-item>
            <a-descriptions-item label="来源对象">{{ detailDrawer.record.sourceName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="风险等级">{{ riskMeta(detailDrawer.record.riskLevel).text }}</a-descriptions-item>
            <a-descriptions-item label="确认策略">
              {{ detailDrawer.record.confirmationPolicy === 'REQUIRED' ? '调用前确认' : '自动执行' }}
            </a-descriptions-item>
            <a-descriptions-item label="调用方式">
              {{ detailDrawer.record.sourceType === 'STANDARD_MCP' ? 'MCP tools/call' : detailDrawer.record.httpMethod }}
            </a-descriptions-item>
            <a-descriptions-item label="超时时间">{{ detailDrawer.record.timeoutSeconds }} 秒</a-descriptions-item>
            <a-descriptions-item label="调用地址" :span="2">
              {{ detailDrawer.record.endpointUrl || detailDrawer.record.callbackUrl || '由平台 API 版本配置继承' }}
            </a-descriptions-item>
            <a-descriptions-item v-if="detailDrawer.record.remoteToolName" label="远端工具名" :span="2">
              {{ detailDrawer.record.remoteToolName }}
            </a-descriptions-item>
            <a-descriptions-item label="工具说明" :span="2">{{ detailDrawer.record.description }}</a-descriptions-item>
          </a-descriptions>
          <h3 class="mcp-schema-title">输入Schema</h3>
          <pre class="mcp-schema">{{ prettyJson(detailDrawer.record.inputSchema) }}</pre>
          <h3 class="mcp-schema-title">输出Schema</h3>
          <pre class="mcp-schema">{{ prettyJson(detailDrawer.record.outputSchema) }}</pre>
        </template>
      </a-spin>
      <template #footer>
        <div class="publish-tool-footer">
          <a-button @click="detailDrawer.open = false">关闭</a-button>
          <a-button
            type="primary"
            :disabled="detailDrawer.record?.auditStatus !== 'APPROVED' || detailDrawer.record?.enabledStatus !== 'ENABLED'"
            @click="goToAssistants"
          >
            去关联智能助手
          </a-button>
        </div>
      </template>
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
        <a-form-item label="最终工具类型" required>
          <a-radio-group v-model:value="reviewModal.toolType" @change="syncReviewSafety">
            <a-radio value="QUERY">查询工具</a-radio>
            <a-radio value="ACTION">操作工具</a-radio>
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
          <a-radio-group v-model:value="reviewModal.confirmationPolicy" :disabled="reviewModal.toolType === 'ACTION'">
            <a-radio value="AUTO">自动执行</a-radio>
            <a-radio value="REQUIRED">调用前确认</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见">
          <a-textarea v-model:value="reviewModal.remark" :rows="4" :maxlength="1000" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="guideOpen" title="MCP接入说明" :footer="null" width="min(1120px, 96vw)" wrap-class-name="mcp-guide-modal">
      <a-alert
        type="info"
        show-icon
        message="推荐使用标准 MCP。对方系统只需提供一个 Streamable HTTP endpoint，并通过 tools/list 暴露工具，无需向 NexoraOne 提供 Swagger 或登录接口。"
        style="margin-bottom: 18px"
      />
      <a-steps
        direction="vertical"
        :items="[
          { title: '实现 MCP Server', description: '使用官方 SDK 包装已有 REST、ERP、CRM 等业务能力，并实现 initialize、tools/list、tools/call。' },
          { title: '注册 endpoint', description: '在 MCP Server 视图填写 Streamable HTTP 地址和静态 Bearer Token 或 API Key。' },
          { title: '探活并发现工具', description: '平台连接远端 Server，自动读取工具说明和 JSON Schema，新工具进入待审核状态。' },
          { title: '测试、审核和启用', description: '使用真实参数测试工具，确定查询/操作类型、风险等级和确认策略后启用。' },
          { title: '关联智能助手', description: '在智能助手配置页选择已审核且已启用的工具，保存后即可参与 AI 问答。' },
        ]"
      />
      <a-divider />
      <a-tabs v-model:activeKey="activeGuideEdition" class="mcp-guide-tabs">
        <a-tab-pane v-for="edition in mcpGuideEditions" :key="edition.key" :tab="edition.tab">
          <section class="mcp-guide-edition">
            <div class="mcp-guide-edition__intro">
              <div>
                <h3>{{ edition.title }}</h3>
                <p>{{ edition.description }}</p>
              </div>
              <a-tag :color="edition.tagColor">{{ edition.tagText }}</a-tag>
            </div>
            <a-alert
              :type="edition.alertType"
              show-icon
              :message="edition.notice"
              class="mcp-guide-edition__notice"
            />
            <ol class="mcp-guide-checklist">
              <li v-for="item in edition.checklist" :key="item">{{ item }}</li>
            </ol>
            <section v-for="section in edition.sections" :key="section.key" class="mcp-guide-code">
              <header class="mcp-guide-code__header">
                <div>
                  <strong>{{ section.title }}</strong>
                  <span>{{ section.fileName }}</span>
                </div>
                <a-button size="small" @click="copyGuideCode(section.code)">
                  <CopyOutlined />复制代码
                </a-button>
              </header>
              <p v-if="section.description" class="mcp-guide-code__description">{{ section.description }}</p>
              <pre><code>{{ section.code }}</code></pre>
            </section>
          </section>
        </a-tab-pane>
      </a-tabs>
      <a-divider />
      <a-alert
        type="warning"
        show-icon
        message="无法改造成 MCP 的老系统可使用 HTTP 适配工具。该模式由 NexoraOne 组装 HTTP 请求并签名，只是兼容方案，不会标记为标准 MCP。"
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
    CopyOutlined,
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
  import { mcpGuideEditions } from './mcp-guide';
  import './open-api.less';

  const route = useRoute();
  const router = useRouter();
  const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
  const activeView = ref('tools');
  const viewOptions = [
    { label: '工具目录', value: 'tools' },
    { label: 'MCP Server', value: 'servers' },
  ];
  const loading = ref(false);
  const rows = ref([]);
  const total = ref(0);
  const applications = ref([]);
  const guideOpen = ref(false);
  const activeGuideEdition = ref('spring-boot-3');
  const summary = reactive({
    total: 0,
    platformApi: 0,
    standardMcp: 0,
    externalHttp: 0,
    pending: 0,
  });
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
  const serverLoading = ref(false);
  const serverRows = ref([]);
  const serverTotal = ref(0);
  const serverSummary = reactive({ total: 0, online: 0, tools: 0, abnormal: 0 });
  const serverQuery = reactive({
    pageNum: 1,
    pageSize: 10,
    searchWord: '',
    applicationId: undefined,
    onlineStatus: undefined,
    enabledFlag: undefined,
  });
  const externalDrawer = reactive({ open: false, saving: false });
  const externalForm = reactive(defaultExternalForm());
  const serverDrawer = reactive({ open: false, saving: false });
  const serverForm = reactive(defaultServerForm());
  const serverDetail = reactive({ open: false, loading: false, record: null });
  const detailDrawer = reactive({ open: false, loading: false, record: null });
  const testModal = reactive({ open: false, loading: false, record: null, argumentsJson: '{}', result: null });
  const reviewModal = reactive({
    open: false,
    loading: false,
    record: null,
    auditStatus: 'APPROVED',
    toolType: 'QUERY',
    riskLevel: 'LOW',
    confirmationPolicy: 'AUTO',
    remark: '',
  });

  const summaryItems = computed(() => [
    { key: 'total', label: '工具总数', icon: ToolOutlined, iconClass: '' },
    { key: 'platformApi', label: '平台API工具', icon: ApiOutlined, iconClass: 'is-success' },
    { key: 'standardMcp', label: '标准MCP工具', icon: GlobalOutlined, iconClass: 'is-purple' },
    { key: 'externalHttp', label: 'HTTP适配工具', icon: ApiOutlined, iconClass: 'is-warning' },
    { key: 'pending', label: '待审核', icon: ClockCircleOutlined, iconClass: 'is-danger' },
  ]);
  const serverSummaryItems = computed(() => [
    { key: 'total', label: 'Server总数', icon: GlobalOutlined, iconClass: '' },
    { key: 'online', label: '在线Server', icon: ApiOutlined, iconClass: 'is-success' },
    { key: 'tools', label: '发现工具', icon: ToolOutlined, iconClass: 'is-purple' },
    { key: 'abnormal', label: '离线/异常', icon: ClockCircleOutlined, iconClass: 'is-danger' },
  ]);
  const serverCredentialHelp = computed(() => {
    const retained = serverForm.serverId && serverForm.credentialConfigured ? '；编辑时留空表示保留原凭证' : '';
    if (serverForm.authType === 'BEARER') {
      return `平台会自动使用 Authorization: Bearer <token> 请求远端 MCP Server${retained}`;
    }
    if (serverForm.authType === 'API_KEY_HEADER') {
      return `平台会把 API Key 原样放入指定请求头${retained}`;
    }
    return '';
  });
  const applicationOptions = computed(() =>
    applications.value.map((item) => ({
      value: item.applicationId,
      label: `${item.applicationName}（${item.applicationCode}）`,
    }))
  );
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
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 400 },
  ];
  const serverColumns = [
    { title: 'Server名称 / 编码', dataIndex: 'serverName', width: 260 },
    { title: '所属应用', dataIndex: 'applicationName', width: 180 },
    { title: 'Endpoint / 鉴权', dataIndex: 'endpointUrl', width: 300 },
    { title: '协议 / 版本', dataIndex: 'protocolVersion', width: 150 },
    { title: '在线状态', dataIndex: 'onlineStatus', width: 135 },
    { title: '最近探活', dataIndex: 'lastProbeTime', width: 250 },
    { title: '启用状态', dataIndex: 'enabledFlag', width: 110 },
    { title: '操作', dataIndex: 'action', fixed: 'right', width: 300 },
  ];
  const serverToolColumns = [
    { title: '工具名称 / 编码', dataIndex: 'toolName', width: 240 },
    { title: '远端工具名', dataIndex: 'remoteToolName', width: 180 },
    { title: '审核', dataIndex: 'auditStatus', width: 100 },
    { title: '启停', dataIndex: 'enabledStatus', width: 100 },
    { title: '在线', dataIndex: 'onlineStatus', width: 100 },
    { title: '操作', dataIndex: 'action', width: 120 },
  ];

  /** 返回登记 HTTP 适配工具时使用的默认表单。 */
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
      inputSchema: JSON.stringify(
        {
          type: 'object',
          properties: {
            keyword: { type: 'string', description: '查询关键字' },
          },
          required: ['keyword'],
          additionalProperties: false,
        },
        null,
        2
      ),
      outputSchema: '',
    };
  }

  /** 返回注册标准 MCP Server 时使用的默认表单。 */
  function defaultServerForm() {
    return {
      serverId: null,
      applicationId: undefined,
      serverName: '',
      serverCode: '',
      description: '',
      endpointUrl: '',
      authType: 'NONE',
      authHeaderName: '',
      authSecret: '',
      timeoutSeconds: 30,
      enabledFlag: true,
      credentialConfigured: false,
    };
  }

  /** 返回工具来源对应的中文文案和标签颜色。 */
  function sourceMeta(value) {
    return (
      {
        PLATFORM_API: { color: 'blue', text: '平台API' },
        STANDARD_MCP: { color: 'cyan', text: '标准MCP' },
        EXTERNAL_HTTP: { color: 'purple', text: 'HTTP适配' },
      }[value] || { color: 'default', text: '未知来源' }
    );
  }

  /** 返回风险等级对应的中文文案和标签颜色。 */
  function riskMeta(value) {
    return (
      {
        LOW: { color: 'green', text: '低' },
        MEDIUM: { color: 'orange', text: '中' },
        HIGH: { color: 'red', text: '高' },
      }[value] || { color: 'default', text: '未知' }
    );
  }

  /** 返回审核状态对应的中文文案和徽标状态。 */
  function auditMeta(value) {
    return (
      {
        DRAFT: { badge: 'default', text: '草稿' },
        PENDING: { badge: 'warning', text: '待审核' },
        APPROVED: { badge: 'success', text: '已通过' },
        REJECTED: { badge: 'error', text: '已驳回' },
      }[value] || { badge: 'default', text: '未知' }
    );
  }

  /** 将工具在线状态转换为中文。 */
  function onlineText(value) {
    return { ONLINE: '在线', OFFLINE: '离线', ABNORMAL: '异常', UNKNOWN: '未知' }[value] || '未知';
  }

  /** 返回 MCP Server 在线状态对应的徽标状态和中文文案。 */
  function serverOnlineMeta(value) {
    return (
      {
        ONLINE: { badge: 'success', text: '在线' },
        OFFLINE: { badge: 'default', text: '离线' },
        ABNORMAL: { badge: 'error', text: '异常' },
        UNKNOWN: { badge: 'warning', text: '未探活' },
      }[value] || { badge: 'default', text: '未知' }
    );
  }

  /** 返回 MCP Server 鉴权方式的中文文案。 */
  function authText(value) {
    return (
      {
        NONE: '无需鉴权',
        BEARER: 'Bearer Token',
        API_KEY_HEADER: 'API Key',
      }[value] || '未知鉴权'
    );
  }

  /** 切换鉴权方式时清理不适用字段，并为 API Key 提供通用请求头默认值。 */
  function handleServerAuthTypeChange(value) {
    serverForm.authSecret = '';
    serverForm.credentialConfigured = false;
    if (value === 'API_KEY_HEADER' && !serverForm.authHeaderName) {
      serverForm.authHeaderName = 'X-API-Key';
    } else if (value !== 'API_KEY_HEADER') {
      serverForm.authHeaderName = '';
    }
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

  /** 校验字符串是否为 HTTP 或 HTTPS 地址。 */
  function isHttpUrl(value) {
    try {
      const url = new URL(value);
      return ['http:', 'https:'].includes(url.protocol);
    } catch {
      return false;
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

  /** 加载工具统计和当前用户可管理的应用选项。 */
  async function loadMeta() {
    try {
      const [summaryResponse, applicationsResponse] = await Promise.all([mcpToolApi.summary(), mcpToolApi.applications()]);
      Object.assign(summary, summaryResponse.data || {});
      applications.value = applicationsResponse.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 分页加载当前用户可管理的标准 MCP Server。 */
  async function queryServers() {
    serverLoading.value = true;
    try {
      const response = await mcpToolApi.queryServers(serverQuery);
      serverRows.value = response.data?.list || [];
      serverTotal.value = response.data?.total || 0;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      serverLoading.value = false;
    }
  }

  /** 加载标准 MCP Server 的在线和工具发现统计。 */
  async function loadServerMeta() {
    try {
      const response = await mcpToolApi.serverSummary();
      Object.assign(serverSummary, response.data || {});
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 同时刷新 MCP Server、工具目录及相关统计。 */
  async function refreshMcpData() {
    await Promise.all([queryServers(), loadServerMeta(), queryData(), loadMeta()]);
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

  /** 重置标准 MCP Server 筛选条件并重新查询。 */
  function resetServerQuery() {
    Object.assign(serverQuery, {
      pageNum: 1,
      pageSize: 10,
      searchWord: '',
      applicationId: undefined,
      onlineStatus: undefined,
      enabledFlag: undefined,
    });
    queryServers();
  }

  /** 打开 HTTP 适配工具登记抽屉并重置表单。 */
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

  /** 审核确认操作类工具时强制使用调用前确认和至少中风险。 */
  function syncReviewSafety() {
    if (reviewModal.toolType === 'ACTION') {
      reviewModal.confirmationPolicy = 'REQUIRED';
      if (reviewModal.riskLevel === 'LOW') reviewModal.riskLevel = 'MEDIUM';
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

  /** 保存 HTTP 适配工具并进入统一审核流程。 */
  async function saveExternal() {
    if (!externalForm.applicationId || !externalForm.toolName.trim() || !externalForm.description.trim() || !externalForm.callbackUrl.trim()) {
      message.warning('请完整填写所属应用、工具名称、回调地址和工具说明');
      return;
    }
    if (!/^[A-Za-z][A-Za-z0-9_]{2,99}$/.test(externalForm.toolCode)) {
      message.warning('工具编码需以字母开头，仅支持字母、数字和下划线');
      return;
    }
    if (!isHttpUrl(externalForm.callbackUrl)) {
      message.warning('回调地址必须是有效的 HTTP 或 HTTPS 地址');
      return;
    }
    if (!validateJson(externalForm.inputSchema, '输入Schema')) return;
    if (externalForm.outputSchema.trim() && !validateJson(externalForm.outputSchema, '输出Schema')) return;
    externalDrawer.saving = true;
    try {
      await mcpToolApi.saveExternal({ ...externalForm });
      message.success('HTTP 适配工具已登记并进入待审核状态');
      externalDrawer.open = false;
      await Promise.all([queryData(), loadMeta()]);
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      externalDrawer.saving = false;
    }
  }

  /** 打开新增或编辑 MCP Server 抽屉，并按需加载脱敏详情。 */
  async function openServerDrawer(record) {
    Object.assign(serverForm, defaultServerForm());
    serverDrawer.open = true;
    if (!record?.serverId) return;
    serverDrawer.saving = true;
    try {
      const response = await mcpToolApi.serverDetail(record.serverId);
      Object.assign(serverForm, response.data || {}, { authSecret: '' });
    } catch (error) {
      smartSentry.captureError(error);
      serverDrawer.open = false;
    } finally {
      serverDrawer.saving = false;
    }
  }

  /** 校验 MCP Server 注册表单是否具备可探活的完整配置。 */
  function validateServerForm() {
    if (!serverForm.applicationId || !serverForm.serverName.trim() || !serverForm.serverCode.trim() || !serverForm.endpointUrl.trim()) {
      message.warning('请完整填写所属应用、Server名称、编码和Endpoint');
      return false;
    }
    if (!/^[A-Za-z][A-Za-z0-9_-]{2,99}$/.test(serverForm.serverCode)) {
      message.warning('Server编码需以字母开头，仅支持字母、数字、中划线和下划线');
      return false;
    }
    if (!isHttpUrl(serverForm.endpointUrl)) {
      message.warning('Endpoint必须是有效的 HTTP 或 HTTPS 地址');
      return false;
    }
    if (serverForm.authType === 'API_KEY_HEADER' && !serverForm.authHeaderName.trim()) {
      message.warning('API Key鉴权必须填写请求头名称');
      return false;
    }
    if (serverForm.authType !== 'NONE' && !serverForm.authSecret && (!serverForm.serverId || !serverForm.credentialConfigured)) {
      message.warning('请填写鉴权凭证');
      return false;
    }
    return true;
  }

  /** 保存 MCP Server，并可在保存后立即执行真实探活和工具发现。 */
  async function saveServer(probeAfterSave) {
    if (!validateServerForm()) return;
    serverDrawer.saving = true;
    try {
      const response = await mcpToolApi.saveServer({
        serverId: serverForm.serverId,
        applicationId: serverForm.applicationId,
        serverName: serverForm.serverName.trim(),
        serverCode: serverForm.serverCode.trim(),
        description: serverForm.description?.trim() || '',
        endpointUrl: serverForm.endpointUrl.trim(),
        authType: serverForm.authType,
        authHeaderName: serverForm.authType === 'API_KEY_HEADER' ? serverForm.authHeaderName.trim() : null,
        authSecret: serverForm.authType === 'NONE' ? null : serverForm.authSecret,
        timeoutSeconds: serverForm.timeoutSeconds,
        enabledFlag: serverForm.enabledFlag,
      });
      const serverId = response.data?.serverId;
      if (probeAfterSave && serverId) {
        try {
          const probeResponse = await mcpToolApi.probeServer(serverId);
          const result = probeResponse.data || {};
          message.success(`探活成功，新发现 ${result.createdToolCount || 0} 个工具`);
        } catch (error) {
          smartSentry.captureError(error);
          message.warning('Server配置已保存，但探活失败，请检查Endpoint、网络和鉴权后重试');
        }
      } else {
        message.success('MCP Server配置已保存');
      }
      serverDrawer.open = false;
      await refreshMcpData();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      serverDrawer.saving = false;
    }
  }

  /** 打开 MCP Server 详情并展示真实发现的工具。 */
  async function openServerDetail(record) {
    serverDetail.open = true;
    serverDetail.loading = true;
    try {
      const response = await mcpToolApi.serverDetail(record.serverId);
      serverDetail.record = response.data;
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      serverDetail.loading = false;
    }
  }

  /** 对已保存的 MCP Server 执行 initialize 和 tools/list，刷新工具目录及变更状态。 */
  async function probeServer(record) {
    serverLoading.value = true;
    try {
      const response = await mcpToolApi.probeServer(record.serverId);
      const result = response.data || {};
      message.success(
        `探活成功：新增 ${result.createdToolCount || 0}，未变化 ${result.unchangedToolCount || 0}，待手动同步 ${result.schemaChangedToolCount || 0}`
      );
      await refreshMcpData();
    } catch (error) {
      smartSentry.captureError(error);
      await Promise.all([queryServers(), loadServerMeta()]);
    } finally {
      serverLoading.value = false;
    }
  }

  /** 启用或停用 MCP Server；停用不会删除工具及助手绑定关系。 */
  async function toggleServerStatus(record) {
    try {
      const enabledFlag = !record.enabledFlag;
      await mcpToolApi.updateServerStatus({ serverId: record.serverId, enabledFlag });
      message.success(enabledFlag ? 'Server已启用，请重新探活后再启用工具' : 'Server及其工具已停用');
      await refreshMcpData();
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 明确接受远端变化后的工具 Schema，并让工具重新进入测试审核流程。 */
  async function syncServerToolSchema(record) {
    try {
      const serverId = record.mcpServerId || serverDetail.record?.serverId;
      if (!serverId) {
        message.error('未找到工具所属的MCP Server，请刷新页面后重试');
        return;
      }
      await mcpToolApi.syncServerToolSchema(serverId, record.toolId);
      message.success('Schema已同步，助手绑定关系保留；请重新测试、审核并启用工具');
      await refreshMcpData();
      if (serverDetail.open && serverDetail.record?.serverId === serverId) {
        await openServerDetail({ serverId });
      }
    } catch (error) {
      smartSentry.captureError(error);
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

  /** 跳转到现有智能助手配置页，由管理员选择需要绑定该工具的助手。 */
  async function goToAssistants() {
    detailDrawer.open = false;
    await router.push('/knowledge/assistants');
  }

  /** 根据输入 Schema 生成可直接编辑的测试参数样例。 */
  function exampleArguments(record) {
    try {
      const schema = JSON.parse(record.inputSchema || '{}');
      return Object.fromEntries(
        Object.entries(schema.properties || {}).map(([name, property]) => {
          const value = property.example ?? property.default;
          if (value !== undefined) return [name, value];
          if (property.type === 'number' || property.type === 'integer') return [name, 0];
          if (property.type === 'boolean') return [name, false];
          if (property.type === 'array') return [name, []];
          if (property.type === 'object') return [name, {}];
          return [name, ''];
        })
      );
    } catch {
      return {};
    }
  }

  /** 打开工具测试窗口并填充示例参数。 */
  function openTest(record) {
    if (record.sourceType === 'STANDARD_MCP' && record.schemaSyncRequired) {
      message.warning('远端Schema已变化，请先点击“同步定义”，确认采用新定义后再测试');
      return;
    }
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

  /** 打开审核窗口并回填当前类型、风险与确认策略。 */
  function openReview(record) {
    Object.assign(reviewModal, {
      open: true,
      loading: false,
      record,
      auditStatus: record.auditStatus === 'REJECTED' ? 'REJECTED' : 'APPROVED',
      toolType: record.toolType || 'QUERY',
      riskLevel: record.riskLevel || 'LOW',
      confirmationPolicy: record.toolType === 'ACTION' ? 'REQUIRED' : record.confirmationPolicy || 'AUTO',
      remark: record.auditRemark || '',
    });
  }

  /** 提交审核结论，服务端会再次校验测试结果、来源状态和安全规则。 */
  async function submitReview() {
    reviewModal.loading = true;
    try {
      await mcpToolApi.review({
        toolId: reviewModal.record.toolId,
        auditStatus: reviewModal.auditStatus,
        toolType: reviewModal.toolType,
        riskLevel: reviewModal.riskLevel,
        confirmationPolicy: reviewModal.toolType === 'ACTION' ? 'REQUIRED' : reviewModal.confirmationPolicy,
        remark: reviewModal.remark,
      });
      message.success(reviewModal.auditStatus === 'APPROVED' ? '工具审核已通过，请手动启用' : '工具已驳回');
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
      message.success('Schema已同步到API最新发布版本，助手关联保持不变');
      await queryData();
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  /** 展示标准 MCP 与 HTTP 适配工具的接入说明。 */
  function showAccessGuide() {
    guideOpen.value = true;
  }

  /** 复制接入示例代码，并兼容非安全上下文中的浏览器。 */
  async function copyGuideCode(code) {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(code);
      } else {
        const textArea = document.createElement('textarea');
        textArea.value = code;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.select();
        const copied = document.execCommand('copy');
        document.body.removeChild(textArea);
        if (!copied) throw new Error('浏览器未允许复制');
      }
      message.success('代码已复制');
    } catch (error) {
      smartSentry.captureError(error);
      message.error('复制失败，请手动选择代码复制');
    }
  }

  /** 初始化工具、Server、统计信息，并恢复路由中指定的工具详情。 */
  async function initialize() {
    activeView.value = route.query.view === 'servers' ? 'servers' : 'tools';
    await Promise.all([queryData(), loadMeta(), queryServers(), loadServerMeta()]);
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

  .mcp-view-switch {
    display: flex;
    justify-content: flex-start;
    margin-bottom: 12px;
  }

  .open-api-summary {
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  }

  .mcp-server-query {
    display: grid;
    grid-template-columns: 1.2fr 1fr minmax(150px, 0.7fr) minmax(150px, 0.7fr) auto;
    gap: 14px;
    align-items: end;
  }

  .mcp-server-query .ant-select {
    width: 100%;
  }

  .mcp-endpoint {
    overflow-wrap: anywhere;
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
    .mcp-tool-query,
    .mcp-server-query {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .mcp-tool-query,
    .mcp-server-query,
    .mcp-form-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
