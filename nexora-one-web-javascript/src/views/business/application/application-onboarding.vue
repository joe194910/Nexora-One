<template>
  <div class="application-page">
    <header class="application-page__header">
      <div>
        <h1 class="application-page__title">{{ applicationId ? '应用接入配置' : '创建应用' }}</h1>
        <div class="application-page__subtitle">完成凭证、登录、安全、API 权限、上架资料和发布范围配置。</div>
      </div>
      <a-tag v-if="detail.configLocked" color="warning">配置已锁定</a-tag>
    </header>

    <section class="application-panel application-steps">
      <a-steps :current="currentStep - 1" size="small" :items="steps" />
    </section>

    <a-spin :spinning="loading">
      <section v-if="currentStep === 1" class="application-panel">
        <h2 class="application-panel__title">应用基本信息</h2>
        <a-alert type="info" show-icon message="创建成功后系统自动生成 App ID 和 App Secret，完整密钥仅展示一次。" />
        <a-form ref="baseFormRef" class="mt16" :model="baseForm" :rules="baseRules" layout="vertical">
          <div class="application-form-grid">
            <a-form-item label="应用名称" name="applicationName">
              <a-input v-model:value="baseForm.applicationName" :maxlength="50" />
            </a-form-item>
            <a-form-item label="应用编码" name="applicationCode">
              <a-input v-model:value="baseForm.applicationCode" :disabled="!!applicationId" placeholder="例如 supply-chain" />
            </a-form-item>
            <a-form-item label="应用类型" name="applicationType">
              <a-radio-group v-model:value="baseForm.applicationType">
                <a-radio :value="1">企业内部应用</a-radio>
                <a-radio :value="2">第三方应用</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="所属企业">
              <a-input v-model:value="baseForm.enterpriseName" />
            </a-form-item>
            <a-form-item label="负责人" name="ownerName">
              <a-input v-model:value="baseForm.ownerName" />
            </a-form-item>
            <a-form-item label="联系方式" name="contact">
              <a-input v-model:value="baseForm.contact" placeholder="手机号或企业邮箱" />
            </a-form-item>
            <a-form-item label="应用首页地址">
              <a-input v-model:value="baseForm.homeUrl" placeholder="https://app.example.com" />
            </a-form-item>
            <a-form-item label="应用图标">
              <Upload
                accept=".jpg,.jpeg,.png"
                :max-upload-size="1"
                :max-size="2"
                button-text="上传图标"
                :default-file-list="iconFileList"
                @change="changeIcon"
              />
            </a-form-item>
            <a-form-item class="application-form-span" label="应用简介" name="summary">
              <a-textarea v-model:value="baseForm.summary" :rows="3" :maxlength="500" show-count />
            </a-form-item>
            <a-form-item class="application-form-span" label="备注">
              <a-textarea v-model:value="baseForm.remark" :rows="2" :maxlength="200" show-count />
            </a-form-item>
          </div>
        </a-form>
      </section>

      <section v-else-if="currentStep === 2" class="application-panel">
        <h2 class="application-panel__title">应用凭证</h2>
        <div class="credential-success">
          <CheckCircleFilled class="credential-success__icon" />
          <h2>应用凭证已生成</h2>
          <p>请立即保存，用于身份认证和 API 调用。</p>
        </div>
        <div class="credential-box">
          <strong>App ID</strong>
          <code>{{ credential.appId }}</code>
          <a-button @click="copyText(credential.appId)"><CopyOutlined />复制</a-button>
        </div>
        <div class="credential-box">
          <strong>App Secret</strong>
          <code>{{ secretVisible ? credential.appSecret || credential.maskedSecret : maskedText }}</code>
          <a-space>
            <a-button @click="secretVisible = !secretVisible"><EyeOutlined />{{ secretVisible ? '隐藏' : '显示' }}</a-button>
            <a-button v-if="credential.appSecret" @click="copyText(credential.appSecret)"><CopyOutlined />复制</a-button>
          </a-space>
        </div>
        <a-alert type="warning" show-icon message="离开本页后无法找回完整 App Secret，只能重新生成；重置后旧密钥立即失效。" />
        <a-space class="mt16">
          <a-button @click="downloadCredential"><DownloadOutlined />下载凭证</a-button>
          <a-popconfirm title="确认重新生成密钥吗？" @confirm="resetSecret">
            <a-button danger :disabled="detail.configLocked"><ReloadOutlined />重新生成密钥</a-button>
          </a-popconfirm>
        </a-space>
      </section>

      <section v-else-if="currentStep === 3" class="application-panel">
        <h2 class="application-panel__title">登录与单点跳转配置</h2>
        <a-form layout="vertical">
          <a-form-item label="接入协议" required>
            <a-radio-group v-model:value="loginForm.protocol" button-style="solid">
              <a-radio-button value="OIDC">OIDC / OAuth 2.0</a-radio-button>
              <a-radio-button value="AUTH_CODE">免登录授权码</a-radio-button>
              <a-radio-button value="DIRECT">仅门户跳转</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <div class="application-form-grid">
            <a-form-item label="应用首页地址" required><a-input v-model:value="loginForm.homeUrl" /></a-form-item>
            <a-form-item label="单点退出回调地址"><a-input v-model:value="loginForm.logoutCallback" /></a-form-item>
            <a-form-item class="application-form-span" label="授权回调地址" required>
              <a-select v-model:value="loginForm.callbackUrls" mode="tags" placeholder="输入回调地址后回车，可配置多个" />
            </a-form-item>
            <a-form-item label="Token 有效期（秒）"><a-input-number v-model:value="loginForm.tokenTtl" :min="60" style="width: 100%" /></a-form-item>
            <a-form-item label="授权码有效期（秒）"><a-input-number v-model:value="loginForm.codeTtl" :min="30" :max="600" style="width: 100%" /></a-form-item>
            <a-form-item label="启用单点退出"><a-switch v-model:checked="loginForm.singleLogout" /></a-form-item>
            <a-form-item label="自动创建本地用户"><a-switch v-model:checked="loginForm.autoCreateUser" /></a-form-item>
          </div>
        </a-form>
      </section>

      <section v-else-if="currentStep === 4" class="application-panel">
        <h2 class="application-panel__title">接口鉴权与安全配置</h2>
        <div class="security-options">
          <div v-for="option in authOptions" :key="option.value" class="security-option" :class="{ active: securityForm.authMode === option.value }" @click="securityForm.authMode = option.value">
            <a-radio :checked="securityForm.authMode === option.value">{{ option.label }}</a-radio>
            <div class="application-page__subtitle">{{ option.description }}</div>
          </div>
        </div>
        <a-divider />
        <a-form layout="vertical">
          <div class="application-form-grid">
            <a-form-item label="签名算法"><a-select v-model:value="securityForm.signatureAlgorithm"><a-select-option value="HMAC-SHA256">HMAC-SHA256</a-select-option><a-select-option value="HMAC-SHA512">HMAC-SHA512</a-select-option></a-select></a-form-item>
            <a-form-item label="签名请求头"><a-input v-model:value="securityForm.signatureHeader" /></a-form-item>
            <a-form-item label="时间戳请求头"><a-input v-model:value="securityForm.timestampHeader" /></a-form-item>
            <a-form-item label="随机数请求头"><a-input v-model:value="securityForm.nonceHeader" /></a-form-item>
            <a-form-item label="防重放有效期（秒）"><a-input-number v-model:value="securityForm.replayTtl" :min="30" style="width: 100%" /></a-form-item>
            <a-form-item label="每秒请求上限"><a-input-number v-model:value="securityForm.qpsLimit" :min="1" style="width: 100%" /></a-form-item>
            <a-form-item label="每日调用上限"><a-input-number v-model:value="securityForm.dailyLimit" :min="1" style="width: 100%" /></a-form-item>
            <a-form-item label="请求超时（秒）"><a-input-number v-model:value="securityForm.timeoutSeconds" :min="1" style="width: 100%" /></a-form-item>
            <a-form-item class="application-form-span" label="IP 白名单"><a-select v-model:value="securityForm.ipWhitelist" mode="tags" placeholder="输入 IP 或 CIDR 后回车" /></a-form-item>
            <a-form-item label="强制 HTTPS"><a-switch v-model:checked="securityForm.forceHttps" /></a-form-item>
            <a-form-item label="启用防重放"><a-switch v-model:checked="securityForm.replayProtection" /></a-form-item>
            <a-form-item label="响应数据加密"><a-switch v-model:checked="securityForm.responseEncryption" /></a-form-item>
            <a-form-item label="敏感字段脱敏"><a-switch v-model:checked="securityForm.maskSensitiveData" /></a-form-item>
          </div>
        </a-form>
      </section>

      <section v-else-if="currentStep === 5" class="application-panel">
        <h2 class="application-panel__title">API 权限申请</h2>
        <div class="api-layout">
          <div class="api-category">
            <div class="api-category__item" :class="{ active: !selectedCategory }" @click="selectedCategory = ''">全部 API</div>
            <div v-for="category in apiCategories" :key="category" class="api-category__item" :class="{ active: selectedCategory === category }" @click="selectedCategory = category">{{ category }}</div>
          </div>
          <a-table
            :data-source="filteredApis"
            :columns="apiColumns"
            row-key="openApiId"
            :row-selection="{ selectedRowKeys: apiForm.openApiIdList, onChange: onApiSelect }"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'apiName'"><strong>{{ record.apiName }}</strong><div class="application-page__subtitle">{{ record.description }}</div></template>
              <template v-else-if="column.dataIndex === 'requestMethod'"><a-tag :color="record.requestMethod === 'GET' ? 'green' : 'blue'">{{ record.requestMethod }}</a-tag></template>
              <template v-else-if="column.dataIndex === 'permissionLevel'"><a-tag :color="permissionMeta(record.permissionLevel).color">{{ permissionMeta(record.permissionLevel).text }}</a-tag></template>
            </template>
          </a-table>
        </div>
        <a-form-item class="mt16" label="申请原因" required>
          <a-textarea v-model:value="apiForm.applyReason" :rows="3" :maxlength="500" show-count />
        </a-form-item>
      </section>

      <section v-else-if="currentStep === 6" class="application-panel">
        <h2 class="application-panel__title">应用上架资料</h2>
        <a-form layout="vertical">
          <div class="application-form-grid">
            <a-form-item label="应用市场名称" required><a-input v-model:value="listingForm.marketName" /></a-form-item>
            <a-form-item label="应用副标题" required><a-input v-model:value="listingForm.subtitle" /></a-form-item>
            <a-form-item label="应用分类" required><a-select v-model:value="listingForm.category"><a-select-option value="办公协同">办公协同</a-select-option><a-select-option value="企业服务">企业服务</a-select-option><a-select-option value="数据分析">数据分析</a-select-option><a-select-option value="研发工具">研发工具</a-select-option></a-select></a-form-item>
            <a-form-item label="应用标签"><a-select v-model:value="listingForm.tags" mode="tags" /></a-form-item>
            <a-form-item label="版本号" required><a-input v-model:value="listingForm.versionNo" /></a-form-item>
            <a-form-item label="更新说明" required><a-input v-model:value="listingForm.releaseNotes" /></a-form-item>
            <a-form-item class="application-form-span" label="应用详细介绍" required><a-textarea v-model:value="listingForm.description" :rows="5" :maxlength="2000" show-count /></a-form-item>
            <a-form-item label="服务商名称" required><a-input v-model:value="listingForm.providerName" /></a-form-item>
            <a-form-item label="联系邮箱" required><a-input v-model:value="listingForm.contactEmail" /></a-form-item>
            <a-form-item label="隐私政策 URL" required><a-input v-model:value="listingForm.privacyUrl" /></a-form-item>
            <a-form-item label="用户协议 URL" required><a-input v-model:value="listingForm.termsUrl" /></a-form-item>
            <a-form-item label="帮助文档 URL"><a-input v-model:value="listingForm.helpUrl" /></a-form-item>
            <a-form-item label="应用封面" required>
              <Upload
                accept=".jpg,.jpeg,.png"
                :max-upload-size="1"
                :max-size="2"
                button-text="上传应用封面"
                :default-file-list="bannerFileList"
                @change="changeBanner"
              />
            </a-form-item>
            <a-form-item class="application-form-span" label="应用截图">
              <Upload
                accept=".jpg,.jpeg,.png"
                :multiple="true"
                :max-upload-size="3"
                :max-size="2"
                button-text="上传应用截图"
                :default-file-list="screenshotFileList"
                @change="changeScreenshots"
              />
            </a-form-item>
          </div>
        </a-form>
      </section>

      <section v-else-if="currentStep === 7" class="application-panel">
        <h2 class="application-panel__title">发布范围与可见权限</h2>
        <div class="publish-scope">
          <div v-for="option in scopeOptions" :key="option.value" class="publish-scope__item" :class="{ active: publishForm.scopeType === option.value }" @click="publishForm.scopeType = option.value">
            <a-radio :checked="publishForm.scopeType === option.value">{{ option.label }}</a-radio>
            <div class="application-page__subtitle">{{ option.description }}</div>
          </div>
        </div>
        <a-divider />
        <a-form layout="vertical">
          <div class="application-form-grid">
            <a-form-item label="可见企业 / 组织"><a-select v-model:value="publishForm.organizationNames" mode="tags" /></a-form-item>
            <a-form-item label="可见角色"><a-select v-model:value="publishForm.roleNames" mode="tags" /></a-form-item>
            <a-form-item label="门户排序号"><a-input-number v-model:value="publishForm.sort" :min="0" style="width: 100%" /></a-form-item>
            <a-form-item label="推荐标签"><a-input v-model:value="publishForm.recommendTag" /></a-form-item>
            <a-form-item label="门户展示"><a-switch v-model:checked="publishForm.portalVisible" /></a-form-item>
            <a-form-item label="允许用户收藏"><a-switch v-model:checked="publishForm.allowFavorite" /></a-form-item>
            <a-form-item label="允许搜索发现"><a-switch v-model:checked="publishForm.searchable" /></a-form-item>
            <a-form-item label="打开方式"><a-radio-group v-model:value="publishForm.openMode"><a-radio value="NEW_TAB">新页签</a-radio><a-radio value="CURRENT">当前页</a-radio></a-radio-group></a-form-item>
          </div>
        </a-form>
      </section>

      <section v-else class="preview-grid">
        <div class="application-panel">
          <h2 class="application-panel__title">应用市场预览</h2>
          <div class="application-hero">
            <div class="application-icon"><img v-if="detail.iconUrl" :src="detail.iconUrl" alt="" /><AppstoreOutlined v-else /></div>
            <div><h2>{{ listingForm.marketName || detail.applicationName }}</h2><p>{{ listingForm.subtitle || detail.summary }}</p><a-tag v-for="tag in listingForm.tags" :key="tag" color="blue">{{ tag }}</a-tag></div>
          </div>
          <a-divider />
          <p>{{ listingForm.description }}</p>
        </div>
        <div class="application-panel">
          <h2 class="application-panel__title">提交检查</h2>
          <div class="completion-list">
            <div v-for="(completed, name) in completion" :key="name" class="completion-item">
              <span><CheckCircleFilled :style="{ color: completed ? '#22a447' : '#faad14' }" /> {{ name }}</span>
              <span>{{ completed ? '已完成' : '待完成' }}</span>
            </div>
          </div>
          <a-form-item class="mt16" label="审核说明"><a-textarea v-model:value="submitForm.submitRemark" :rows="4" :maxlength="500" show-count /></a-form-item>
          <a-checkbox v-model:checked="submitForm.confirmed">我已确认以上信息真实有效，提交后锁定当前配置。</a-checkbox>
        </div>
      </section>
    </a-spin>

    <div class="application-footer-actions">
      <a-button @click="backToList">返回应用列表</a-button>
      <div class="application-footer-actions__right">
        <a-button v-if="currentStep > 1" @click="currentStep--">上一步</a-button>
        <a-button v-if="currentStep < 8" type="primary" :loading="saving" :disabled="detail.configLocked" @click="saveAndNext">
          {{ currentStep === 1 ? (applicationId ? '保存并继续' : '创建并继续') : '保存并下一步' }}
        </a-button>
        <a-button v-else type="primary" :loading="saving" :disabled="detail.configLocked || !submitForm.confirmed" @click="submitReview">提交审核</a-button>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { AppstoreOutlined, CheckCircleFilled, CopyOutlined, DownloadOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons-vue';
  import { applicationApi } from '/@/api/business/application/application-api';
  import Upload from '/@/components/support/file-upload/index.vue';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './application.less';

  const route = useRoute();
  const router = useRouter();
  const applicationId = ref(route.query.applicationId ? Number(route.query.applicationId) : null);
  const currentStep = ref(Math.min(Math.max(Number(route.query.step) || (applicationId.value ? 2 : 1), 1), 8));
  const loading = ref(false);
  const saving = ref(false);
  const secretVisible = ref(true);
  const maskedText = '****************************';
  const baseFormRef = ref();
  const detail = reactive({});
  const credential = reactive({});
  const completion = reactive({});
  const openApis = ref([]);
  const selectedCategory = ref('');

  const steps = ['基本信息', '应用凭证', '登录接入', '接口安全', 'API 权限', '上架资料', '发布范围', '预览提交'].map((title) => ({ title }));
  const baseForm = reactive({ applicationName: '', applicationCode: '', applicationType: 1, enterpriseId: null, enterpriseName: '', ownerName: '', contact: '', iconUrl: '', summary: '', homeUrl: '', remark: '' });
  const baseRules = {
    applicationName: [{ required: true, message: '请输入应用名称' }],
    applicationCode: [{ required: true, pattern: /^[a-z][a-z0-9-]{3,31}$/, message: '请输入合法的小写应用编码' }],
    applicationType: [{ required: true, message: '请选择应用类型' }],
    ownerName: [{ required: true, message: '请输入负责人' }],
    contact: [{ required: true, message: '请输入联系方式' }],
    summary: [{ required: true, message: '请输入应用简介' }],
  };
  const loginForm = reactive({ protocol: 'OIDC', homeUrl: '', logoutCallback: '', callbackUrls: [], tokenTtl: 7200, codeTtl: 60, singleLogout: true, autoCreateUser: true });
  const securityForm = reactive({ authMode: 'SIGNATURE', signatureAlgorithm: 'HMAC-SHA256', signatureHeader: 'X-Signature', timestampHeader: 'X-Timestamp', nonceHeader: 'X-Nonce', replayTtl: 300, qpsLimit: 50, dailyLimit: 100000, timeoutSeconds: 10, ipWhitelist: [], forceHttps: true, replayProtection: true, responseEncryption: false, maskSensitiveData: true });
  const apiForm = reactive({ openApiIdList: [], applyReason: '' });
  const listingForm = reactive({ marketName: '', subtitle: '', category: '企业服务', tags: [], versionNo: 'v1.0.0', releaseNotes: '', description: '', providerName: 'NexoraOne', contactEmail: '', privacyUrl: '', termsUrl: '', helpUrl: '', bannerUrl: '', screenshotUrls: [] });
  const publishForm = reactive({ scopeType: 'ENTERPRISE', organizationNames: [], roleNames: [], sort: 100, recommendTag: '', portalVisible: true, allowFavorite: true, searchable: true, openMode: 'NEW_TAB' });
  const submitForm = reactive({ submitRemark: '', confirmed: false });
  const authOptions = [
    { value: 'SIGNATURE', label: 'Access Token + 请求签名', description: '推荐生产环境使用' },
    { value: 'TOKEN', label: '仅 Access Token', description: '适合内部低风险系统' },
    { value: 'IP', label: 'IP 白名单', description: '适合固定服务器调用' },
  ];
  const scopeOptions = [
    { value: 'ENTERPRISE', label: '仅本企业', description: '仅当前企业内可见和使用' },
    { value: 'SELECTED', label: '指定企业或组织', description: '按企业、部门、角色控制可见范围' },
    { value: 'PUBLIC', label: '全平台公开', description: '在 NexoraOne 应用市场公开展示' },
  ];
  const apiColumns = [
    { title: 'API 名称', dataIndex: 'apiName', width: 240 },
    { title: 'API 编码', dataIndex: 'apiCode' },
    { title: '路径', dataIndex: 'requestPath' },
    { title: '方式', dataIndex: 'requestMethod', width: 80 },
    { title: '权限级别', dataIndex: 'permissionLevel', width: 110 },
  ];
  const apiCategories = computed(() => [...new Set(openApis.value.map((item) => item.categoryName))]);
  const filteredApis = computed(() => selectedCategory.value ? openApis.value.filter((item) => item.categoryName === selectedCategory.value) : openApis.value);
  const iconFileList = computed(() => buildUploadFileList(baseForm.iconUrl ? [baseForm.iconUrl] : [], 'icon'));
  const bannerFileList = computed(() => buildUploadFileList(listingForm.bannerUrl ? [listingForm.bannerUrl] : [], 'banner'));
  const screenshotFileList = computed(() => buildUploadFileList(listingForm.screenshotUrls || [], 'screenshot'));

  function permissionMeta(value) {
    return { 1: { color: 'green', text: '公开' }, 2: { color: 'blue', text: '申请授权' }, 3: { color: 'red', text: '敏感审核' } }[value];
  }

  function onApiSelect(keys) {
    apiForm.openApiIdList = keys;
  }

  function buildUploadFileList(urls, prefix) {
    return urls.filter(Boolean).map((fileUrl, index) => ({
      fileId: `${prefix}-${index}`,
      fileName: fileUrl.split('/').pop() || `${prefix}-${index + 1}.png`,
      fileType: fileUrl.split('.').pop()?.toLowerCase() || 'png',
      fileUrl,
    }));
  }

  function getUploadedUrl(file) {
    return file?.fileUrl || file?.url || '';
  }

  function changeIcon(files) {
    baseForm.iconUrl = getUploadedUrl(files[0]);
  }

  function changeBanner(files) {
    listingForm.bannerUrl = getUploadedUrl(files[0]);
  }

  function changeScreenshots(files) {
    listingForm.screenshotUrls = files.map(getUploadedUrl).filter(Boolean);
  }

  function fillDetail(data) {
    Object.assign(detail, data);
    Object.assign(baseForm, data);
    Object.assign(credential, data.credential || {});
    Object.assign(loginForm, data.loginConfig || {});
    Object.assign(securityForm, data.securityConfig || {});
    Object.assign(listingForm, data.listingConfig || {});
    Object.assign(publishForm, data.publishConfig || {});
    Object.assign(completion, data.completion || {});
    apiForm.openApiIdList = (data.apiPermissions || []).map((item) => item.openApiId);
    apiForm.applyReason = data.apiPermissions?.[0]?.applyReason || '';
  }

  async function loadDetail() {
    if (!applicationId.value) return;
    loading.value = true;
    try {
      const response = await applicationApi.detail(applicationId.value);
      fillDetail(response.data);
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  async function loadCatalog() {
    try {
      const response = await applicationApi.queryOpenApiCatalog();
      openApis.value = response.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  async function saveAndNext() {
    saving.value = true;
    try {
      if (currentStep.value === 1) {
        await baseFormRef.value.validate();
        if (applicationId.value) {
          await applicationApi.updateBase({ applicationId: applicationId.value, ...baseForm });
        } else {
          const response = await applicationApi.create(baseForm);
          applicationId.value = response.data.applicationId;
          fillDetail(response.data);
          Object.assign(credential, response.data.credential);
          await router.replace({ path: '/application/onboarding', query: { applicationId: applicationId.value, step: 2 } });
        }
      } else if (currentStep.value === 3) {
        validateRequired(loginForm.homeUrl && loginForm.callbackUrls.length, '请填写应用首页和授权回调地址');
        await applicationApi.saveStep({ applicationId: applicationId.value, step: 3, data: loginForm });
      } else if (currentStep.value === 4) {
        await applicationApi.saveStep({ applicationId: applicationId.value, step: 4, data: securityForm });
      } else if (currentStep.value === 5) {
        validateRequired(apiForm.openApiIdList.length && apiForm.applyReason, '请选择 API 并填写申请原因');
        await applicationApi.saveApiPermissions({ applicationId: applicationId.value, ...apiForm });
      } else if (currentStep.value === 6) {
        validateRequired(
          listingForm.marketName
            && listingForm.subtitle
            && listingForm.versionNo
            && listingForm.releaseNotes
            && listingForm.description
            && listingForm.providerName
            && listingForm.contactEmail
            && listingForm.privacyUrl
            && listingForm.termsUrl
            && listingForm.bannerUrl,
          '请完善必填的上架资料'
        );
        await applicationApi.saveStep({ applicationId: applicationId.value, step: 6, data: listingForm });
      } else if (currentStep.value === 7) {
        await applicationApi.saveStep({ applicationId: applicationId.value, step: 7, data: publishForm });
      }
      currentStep.value += 1;
      if (currentStep.value === 8) await loadDetail();
    } catch (error) {
      if (error?.message) message.warning(error.message);
      smartSentry.captureError(error);
    } finally {
      saving.value = false;
    }
  }

  function validateRequired(value, text) {
    if (!value) throw new Error(text);
  }

  async function submitReview() {
    saving.value = true;
    try {
      await applicationApi.submit({ applicationId: applicationId.value, versionNo: listingForm.versionNo, ...submitForm });
      message.success('应用已提交审核');
      router.push({ path: '/application/detail', query: { applicationId: applicationId.value } });
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      saving.value = false;
    }
  }

  async function resetSecret() {
    try {
      const response = await applicationApi.resetSecret(applicationId.value);
      Object.assign(credential, response.data);
      secretVisible.value = true;
      message.success('新密钥已生成');
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  async function copyText(value) {
    if (!value) return;
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(value);
      } else {
        copyTextFallback(value);
      }
      message.success('已复制');
    } catch (error) {
      copyTextFallback(value);
      message.success('已复制');
    }
  }

  function copyTextFallback(value) {
    const textarea = document.createElement('textarea');
    textarea.value = value;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  }

  function downloadCredential() {
    const content = `NexoraOne Application Credential\nApp ID: ${credential.appId || ''}\nApp Secret: ${credential.appSecret || '请在控制台重置后重新下载'}\n`;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([content], { type: 'text/plain;charset=utf-8' }));
    link.download = `${detail.applicationCode || baseForm.applicationCode || 'application'}-credential.txt`;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  function backToList() {
    router.push({ path: '/application/access' });
  }

  onMounted(async () => {
    await Promise.all([loadDetail(), loadCatalog()]);
  });
</script>
