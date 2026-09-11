<template>
  <div class="open-api-page">
    <header class="open-api-page__header">
      <div class="open-api-editor__intro">
        <span class="open-api-editor__icon"><ApiOutlined /></span>
        <div>
          <h1 class="open-api-page__title">{{ pageTitle }}</h1>
          <div class="open-api-page__subtitle">{{ pageSubtitle }}</div>
        </div>
      </div>
      <a-button @click="backToList"><ArrowLeftOutlined />返回API列表</a-button>
    </header>

    <section class="open-api-editor__steps">
      <a-steps v-model:current="currentStep" :items="stepItems" size="small" @change="changeStep" />
    </section>

    <a-spin :spinning="loading">
      <section v-if="currentStep === 0" class="open-api-panel">
        <h2 class="open-api-panel__title">基本信息</h2>
        <a-alert
          message="API 编码创建后保持稳定，应用授权关系将通过开放 API 主键关联。"
          type="info"
          show-icon
          closable
          style="margin-bottom: 20px"
        />
        <a-form ref="basicFormRef" :model="basicForm" :rules="basicRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
          <div class="open-api-form-grid">
            <a-form-item label="API名称" name="apiName">
              <a-input v-model:value="basicForm.apiName" :disabled="readOnly" placeholder="例如：查询员工详情" />
            </a-form-item>
            <a-form-item label="API编码" name="apiCode">
              <a-input v-model:value="basicForm.apiCode" :disabled="readOnly || Boolean(openApiId)" placeholder="例如：employee.getDetail">
                <template #suffix>
                  <a-tooltip v-if="codeAvailable === true" title="编码可用"><CheckCircleOutlined style="color: #16a34a" /></a-tooltip>
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="所属分类" name="categoryName">
              <a-auto-complete v-model:value="basicForm.categoryName" :disabled="readOnly" :options="categoryOptions" placeholder="例如：组织架构" />
            </a-form-item>
            <a-form-item label="所属服务" name="serviceName">
              <a-input v-model:value="basicForm.serviceName" :disabled="readOnly" placeholder="例如：nexora-one-admin" />
            </a-form-item>
            <a-form-item label="接口版本" name="versionNo">
              <a-input v-model:value="basicForm.versionNo" :disabled="readOnly" placeholder="v1.0.0" />
            </a-form-item>
            <a-form-item label="请求方式" name="requestMethod">
              <a-select v-model:value="basicForm.requestMethod" :disabled="readOnly">
                <a-select-option v-for="item in methodOptions" :key="item" :value="item">{{ item }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="网关路径" name="gatewayPath">
              <a-input v-model:value="basicForm.gatewayPath" :disabled="readOnly" placeholder="/open-api/v1/employees/{employeeId}" />
            </a-form-item>
            <a-form-item label="内部转发地址">
              <a-input v-model:value="basicForm.internalPath" :disabled="readOnly" placeholder="/api/employee/get" />
            </a-form-item>
            <a-form-item label="Content-Type" name="contentType">
              <a-select v-model:value="basicForm.contentType" :disabled="readOnly">
                <a-select-option value="application/json">application/json</a-select-option>
                <a-select-option value="multipart/form-data">multipart/form-data</a-select-option>
                <a-select-option value="application/x-www-form-urlencoded">application/x-www-form-urlencoded</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="接口负责人">
              <a-input v-model:value="basicForm.ownerName" :disabled="readOnly" placeholder="请输入负责人姓名" />
            </a-form-item>
            <a-form-item label="权限级别" name="permissionLevel">
              <a-radio-group v-model:value="basicForm.permissionLevel" :disabled="readOnly">
                <a-radio :value="1">公开</a-radio>
                <a-radio :value="2">申请授权</a-radio>
                <a-radio :value="3">敏感</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="请求超时" name="timeoutSeconds">
              <a-input-number v-model:value="basicForm.timeoutSeconds" :disabled="readOnly" :min="1" :max="300" addon-after="秒" style="width: 100%" />
            </a-form-item>
            <a-form-item label="标签">
              <a-input v-model:value="basicForm.tags" :disabled="readOnly" placeholder="多个标签用英文逗号分隔" />
            </a-form-item>
            <a-form-item class="is-full" label="API简介" name="description" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
              <a-textarea v-model:value="basicForm.description" :disabled="readOnly" :rows="3" :maxlength="1000" show-count />
            </a-form-item>
          </div>
        </a-form>

        <h2 class="open-api-panel__title">发布环境</h2>
        <div style="overflow-x: auto">
          <table class="open-api-environment-table">
            <thead>
              <tr>
                <th>环境名称</th>
                <th>环境编码</th>
                <th>基础地址（Base URL）</th>
                <th>启用</th>
                <th>在线调试</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in basicForm.environments" :key="item.environmentCode">
                <td><a-input v-model:value="item.environmentName" :disabled="readOnly" /></td>
                <td><a-input v-model:value="item.environmentCode" :disabled="readOnly || Boolean(openApiId)" /></td>
                <td>
                  <a-input
                    v-model:value="item.baseUrl"
                    :disabled="readOnly"
                    placeholder="请输入真实服务地址，如 https://service.company.com"
                  />
                </td>
                <td><a-switch v-model:checked="item.enabledFlag" :disabled="readOnly" /></td>
                <td><a-switch v-model:checked="item.onlineDebugFlag" :disabled="readOnly" /></td>
                <td><a-input v-model:value="item.description" :disabled="readOnly" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else-if="currentStep === 1 || currentStep === 2" class="open-api-panel">
        <div class="open-api-table-toolbar">
          <h2 class="open-api-panel__title" style="margin-bottom: 0">{{ currentStep === 1 ? '入参定义' : '出参定义' }}</h2>
          <a-button v-if="!readOnly" type="primary" @click="addParameter"><PlusOutlined />添加参数</a-button>
        </div>
        <a-alert
          v-if="currentStep === 1"
          message="请求参数可定义 Header、Path、Query 和 Body，公共鉴权请求头由网关模块统一处理。"
          type="info"
          show-icon
          style="margin-bottom: 14px"
        />
        <div v-else class="open-api-table-toolbar">
          <a-checkbox v-model:checked="unifiedResponseFlag" :disabled="readOnly">启用统一响应结构</a-checkbox>
          <a-checkbox v-model:checked="dataMaskingFlag" :disabled="readOnly">响应数据脱敏</a-checkbox>
        </div>
        <div style="overflow-x: auto">
          <table class="open-api-parameter-table">
            <thead>
              <tr>
                <th style="min-width: 120px">参数位置</th>
                <th style="min-width: 160px">参数名称</th>
                <th v-if="currentStep === 2" style="min-width: 160px">父字段</th>
                <th style="min-width: 140px">中文名称</th>
                <th style="min-width: 130px">数据类型</th>
                <th style="min-width: 80px">{{ currentStep === 1 ? '必填' : '可空' }}</th>
                <th style="min-width: 140px">示例值</th>
                <th style="min-width: 180px">校验规则</th>
                <th style="min-width: 200px">参数说明</th>
                <th v-if="!readOnly" style="width: 64px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in activeParameters" :key="item.rowKey">
                <td>
                  <a-select v-model:value="item.location" :disabled="readOnly" style="width: 100%">
                    <a-select-option v-for="location in parameterLocations" :key="location" :value="location">{{ location }}</a-select-option>
                  </a-select>
                </td>
                <td><a-input v-model:value="item.parameterName" :disabled="readOnly" placeholder="参数名称" /></td>
                <td v-if="currentStep === 2">
                  <a-select
                    v-model:value="item.parentRowKey"
                    :disabled="readOnly"
                    allow-clear
                    placeholder="顶级字段"
                    style="width: 100%"
                  >
                    <a-select-option
                      v-for="parent in responseParameters.slice(0, index)"
                      :key="parent.rowKey"
                      :value="parent.rowKey"
                    >
                      {{ parent.parameterName || parent.chineseName || '未命名字段' }}
                    </a-select-option>
                  </a-select>
                </td>
                <td><a-input v-model:value="item.chineseName" :disabled="readOnly" placeholder="中文名称" /></td>
                <td>
                  <a-select v-model:value="item.dataType" :disabled="readOnly" style="width: 100%">
                    <a-select-option v-for="type in dataTypes" :key="type" :value="type">{{ type }}</a-select-option>
                  </a-select>
                </td>
                <td>
                  <a-switch v-if="currentStep === 1" v-model:checked="item.requiredFlag" :disabled="readOnly" />
                  <a-switch v-else v-model:checked="item.nullableFlag" :disabled="readOnly" />
                </td>
                <td><a-input v-model:value="item.exampleValue" :disabled="readOnly" /></td>
                <td><a-input v-model:value="item.validationRule" :disabled="readOnly" placeholder="例如：最小值 1" /></td>
                <td><a-input v-model:value="item.description" :disabled="readOnly" /></td>
                <td v-if="!readOnly">
                  <a-button type="text" danger title="删除参数" @click="removeParameter(index)"><DeleteOutlined /></a-button>
                </td>
              </tr>
              <tr v-if="activeParameters.length === 0">
                <td :colspan="parameterTableColspan">
                  <a-empty :description="readOnly ? '暂无参数' : '点击添加参数开始定义'" :image="simpleImage" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else-if="currentStep === 3" class="open-api-panel">
        <h2 class="open-api-panel__title">请求与响应示例</h2>
        <div class="open-api-json-grid">
          <div class="open-api-json-box">
            <div class="open-api-json-box__title">请求示例</div>
            <a-textarea v-model:value="requestExample" :disabled="readOnly" :rows="14" />
          </div>
          <div class="open-api-json-box">
            <div class="open-api-json-box__title">成功响应示例</div>
            <a-textarea v-model:value="responseExample" :disabled="readOnly" :rows="14" />
          </div>
        </div>

        <div class="open-api-table-toolbar" style="margin-top: 20px">
          <h2 class="open-api-panel__title" style="margin-bottom: 0">错误码管理</h2>
          <a-button v-if="!readOnly" type="primary" @click="addErrorCode"><PlusOutlined />添加错误码</a-button>
        </div>
        <div style="overflow-x: auto">
          <table class="open-api-error-table">
            <thead>
              <tr>
                <th style="min-width: 110px">HTTP状态码</th>
                <th style="min-width: 150px">业务错误码</th>
                <th style="min-width: 180px">错误信息</th>
                <th style="min-width: 220px">触发条件</th>
                <th style="min-width: 220px">处理建议</th>
                <th v-if="!readOnly" style="width: 64px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in errorCodes" :key="item.rowKey">
                <td><a-input-number v-model:value="item.httpStatus" :disabled="readOnly" :min="100" :max="599" /></td>
                <td><a-input v-model:value="item.businessCode" :disabled="readOnly" /></td>
                <td><a-input v-model:value="item.errorMessage" :disabled="readOnly" /></td>
                <td><a-input v-model:value="item.triggerCondition" :disabled="readOnly" /></td>
                <td><a-input v-model:value="item.handlingAdvice" :disabled="readOnly" /></td>
                <td v-if="!readOnly">
                  <a-button type="text" danger title="删除错误码" @click="errorCodes.splice(index, 1)"><DeleteOutlined /></a-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div style="margin-top: 18px">
          <label class="open-api-query__label">版本更新说明</label>
          <a-textarea v-model:value="changeLog" :disabled="readOnly" :rows="3" :maxlength="500" show-count />
        </div>
      </section>

      <section v-else class="open-api-panel open-api-reserved">
        <div>
          <div class="open-api-reserved__icon">
            <SafetyCertificateOutlined v-if="currentStep === 4" />
            <RocketOutlined v-else />
          </div>
          <div class="open-api-reserved__title">{{ currentStep === 4 ? '安全策略模块' : '上架发布模块' }}</div>
          <div class="open-api-reserved__text">
            {{
              currentStep === 4
                ? '安全策略已接入真实网关执行链，包含签名认证、防重放、授权校验、限流和请求转发。'
                : '保存当前 API 后，可在上架发布页面完善市场资料并提交平台审核。'
            }}
          </div>
        </div>
      </section>
    </a-spin>

    <div class="open-api-editor__footer">
      <a-button :disabled="currentStep === 0" @click="previousStep"><ArrowLeftOutlined />上一步</a-button>
      <div class="open-api-editor__footer-right">
        <a-button @click="backToList">返回列表</a-button>
        <a-button v-if="!readOnly && currentStep <= 3" type="primary" :loading="saving" @click="saveAndNext">
          {{ currentStep === 3 ? '保存阶段配置' : '保存并下一步' }}
          <ArrowRightOutlined />
        </a-button>
        <a-button v-else-if="currentStep < 5" type="primary" @click="nextStep">下一步<ArrowRightOutlined /></a-button>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import {
    ApiOutlined,
    ArrowLeftOutlined,
    ArrowRightOutlined,
    CheckCircleOutlined,
    DeleteOutlined,
    PlusOutlined,
    RocketOutlined,
    SafetyCertificateOutlined,
  } from '@ant-design/icons-vue';
  import { Empty, message } from 'ant-design-vue';
  import { openApiApi } from '/@/api/business/open-api/open-api-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import './open-api.less';

  const route = useRoute();
  const router = useRouter();
  const basicFormRef = ref();
  const loading = ref(false);
  const saving = ref(false);
  const currentStep = ref(Math.max(0, Math.min(Number(route.query.step || 1) - 1, 5)));
  const openApiId = ref(route.query.openApiId ? Number(route.query.openApiId) : undefined);
  const versionId = ref();
  const readOnly = computed(() => route.query.mode === 'detail');
  const codeAvailable = ref();
  const categories = ref([]);
  const requestParameters = ref([]);
  const responseParameters = ref([]);
  const unifiedResponseFlag = ref(true);
  const dataMaskingFlag = ref(true);
  const requestExample = ref('{\n  "employeeId": 10001\n}');
  const responseExample = ref('{\n  "code": 1,\n  "message": "success",\n  "data": {}\n}');
  const changeLog = ref('');
  const errorCodes = ref([
    createErrorCode(400, 'PARAM_ERROR', '参数错误', '请求参数缺失或格式不正确', '检查请求参数是否符合接口要求'),
    createErrorCode(401, 'TOKEN_INVALID', '访问令牌无效', '未携带令牌或令牌已过期', '重新获取有效的访问令牌'),
  ]);

  const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
  const parameterLocations = ['header', 'path', 'query', 'body', 'response'];
  const dataTypes = ['String', 'Integer', 'Long', 'Decimal', 'Boolean', 'Object', 'Array<Object>', 'Date', 'DateTime'];
  const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
  const stepItems = [
    { title: '基本信息' },
    { title: '入参定义' },
    { title: '出参定义' },
    { title: '示例与错误码' },
    { title: '安全策略' },
    { title: '上架发布' },
  ];
  const basicForm = reactive({
    apiName: '',
    apiCode: '',
    categoryName: '',
    serviceName: 'nexora-one-admin',
    versionNo: 'v1.0.0',
    requestMethod: 'GET',
    gatewayPath: '/open-api/v1/',
    internalPath: '',
    contentType: 'application/json',
    permissionLevel: 2,
    timeoutSeconds: 10,
    ownerName: '',
    tags: '',
    description: '',
    environments: [
      {
        environmentCode: 'test',
        environmentName: '测试环境',
        baseUrl: '',
        enabledFlag: true,
        onlineDebugFlag: true,
        description: '用于功能测试和联调',
      },
      {
        environmentCode: 'prod',
        environmentName: '生产环境',
        baseUrl: '',
        enabledFlag: true,
        onlineDebugFlag: false,
        description: '正式生产环境',
      },
    ],
  });
  const basicRules = {
    apiName: [{ required: true, message: '请输入 API 名称' }],
    apiCode: [
      { required: true, message: '请输入 API 编码' },
      { pattern: /^[A-Za-z][A-Za-z0-9._:-]{2,99}$/, message: '以字母开头，仅支持字母、数字、点、短横线、下划线和冒号' },
    ],
    categoryName: [{ required: true, message: '请输入所属分类' }],
    serviceName: [{ required: true, message: '请输入所属服务' }],
    versionNo: [{ required: true, pattern: /^v?\d+\.\d+\.\d+$/, message: '请输入 v1.0.0 格式的版本号' }],
    requestMethod: [{ required: true, message: '请选择请求方式' }],
    gatewayPath: [{ required: true, pattern: /^\//, message: '网关路径必须以 / 开头' }],
    contentType: [{ required: true, message: '请选择 Content-Type' }],
    permissionLevel: [{ required: true, message: '请选择权限级别' }],
    timeoutSeconds: [{ required: true, message: '请输入请求超时' }],
    description: [{ required: true, message: '请输入 API 简介' }],
  };

  const pageTitle = computed(() => {
    if (readOnly.value) return basicForm.apiName || 'API详情';
    return openApiId.value ? `编辑API：${basicForm.apiName || ''}` : '创建API';
  });
  const pageSubtitle = computed(() => {
    if (readOnly.value) return `${basicForm.requestMethod} ${basicForm.gatewayPath}`;
    return '定义接口基本信息、请求参数、响应结构以及示例错误码。';
  });
  const categoryOptions = computed(() => categories.value.map((value) => ({ value })));
  const activeParameters = computed(() => (currentStep.value === 1 ? requestParameters.value : responseParameters.value));
  const parameterTableColspan = computed(() => 8 + (currentStep.value === 2 ? 1 : 0) + (readOnly.value ? 0 : 1));

  function createParameter(direction) {
    return {
      rowKey: `${Date.now()}-${Math.random()}`,
      parentRowKey: undefined,
      location: direction === 1 ? 'query' : 'response',
      parameterName: '',
      chineseName: '',
      dataType: 'String',
      requiredFlag: false,
      nullableFlag: true,
      defaultValue: '',
      exampleValue: '',
      validationRule: '',
      description: '',
      maskingFlag: false,
      sort: 0,
    };
  }

  function createErrorCode(httpStatus = 400, businessCode = '', errorMessage = '', triggerCondition = '', handlingAdvice = '') {
    return { rowKey: `${Date.now()}-${Math.random()}`, httpStatus, businessCode, errorMessage, triggerCondition, handlingAdvice, sort: 0 };
  }

  function addParameter() {
    activeParameters.value.push(createParameter(currentStep.value === 1 ? 1 : 2));
  }

  function removeParameter(index) {
    const [removed] = activeParameters.value.splice(index, 1);
    if (currentStep.value === 2 && removed) {
      responseParameters.value.forEach((item) => {
        if (item.parentRowKey === removed.rowKey) {
          item.parentRowKey = undefined;
        }
      });
    }
  }

  function addErrorCode() {
    errorCodes.value.push(createErrorCode());
  }

  function buildBasicPayload() {
    return {
      openApiId: openApiId.value,
      versionId: versionId.value,
      ...basicForm,
    };
  }

  function normalizeParameters(rows, direction) {
    return rows.map((item, index) => ({
      ...item,
      location: direction === 2 ? 'response' : item.location,
      sort: index,
    }));
  }

  function validateParameterRows(rows) {
    const invalid = rows.find((item) => !item.location || !item.parameterName || !item.dataType);
    if (invalid) {
      message.warning('请完整填写参数位置、参数名称和数据类型');
      return false;
    }
    return true;
  }

  function validateExamples() {
    try {
      JSON.parse(requestExample.value || '{}');
      JSON.parse(responseExample.value || '{}');
    } catch (error) {
      message.warning('请求示例和响应示例必须是有效 JSON');
      return false;
    }
    if (errorCodes.value.some((item) => !item.httpStatus || !item.businessCode || !item.errorMessage)) {
      message.warning('请完整填写错误码、业务错误码和错误信息');
      return false;
    }
    return true;
  }

  function validateEnvironments() {
    if (!basicForm.environments.length) {
      message.warning('请至少配置一个 API 发布环境');
      return false;
    }
    const environmentCodes = new Set();
    for (const environment of basicForm.environments) {
      const environmentCode = environment.environmentCode?.trim().toLowerCase();
      if (!environmentCode || !environment.environmentName?.trim()) {
        message.warning('请完整填写环境名称和环境编码');
        return false;
      }
      if (environmentCodes.has(environmentCode)) {
        message.warning('环境编码不能重复');
        return false;
      }
      environmentCodes.add(environmentCode);
      try {
        const baseUrl = new URL(environment.baseUrl?.trim());
        if (
          !['http:', 'https:'].includes(baseUrl.protocol) ||
          baseUrl.username ||
          baseUrl.password ||
          baseUrl.search ||
          baseUrl.hash
        ) {
          throw new Error('invalid base url');
        }
        environment.baseUrl = baseUrl.href.replace(/\/$/, '');
        environment.environmentCode = environmentCode;
        environment.environmentName = environment.environmentName.trim();
      } catch (error) {
        message.warning(`请为“${environment.environmentName || environmentCode}”填写有效的 HTTP 或 HTTPS 服务地址`);
        return false;
      }
    }
    return true;
  }

  async function saveBasic() {
    await basicFormRef.value.validate();
    if (!validateEnvironments()) {
      return false;
    }
    const codeResponse = await openApiApi.checkCode(basicForm.apiCode, openApiId.value);
    codeAvailable.value = Boolean(codeResponse.data);
    if (!codeAvailable.value) {
      message.warning('API 编码已存在或格式不正确');
      return false;
    }
    if (openApiId.value) {
      await openApiApi.updateBasic(buildBasicPayload());
    } else {
      const response = await openApiApi.create(buildBasicPayload());
      openApiId.value = response.data.openApiId;
      versionId.value = response.data.versionId;
      await router.replace({ path: '/open-api/editor', query: { openApiId: openApiId.value, step: 1 } });
    }
    return true;
  }

  async function saveParameters(direction) {
    const rows = direction === 1 ? requestParameters.value : responseParameters.value;
    if (!validateParameterRows(rows)) return false;
    await openApiApi.saveParameters({
      openApiId: openApiId.value,
      versionId: versionId.value,
      direction,
      unifiedResponseFlag: unifiedResponseFlag.value,
      dataMaskingFlag: dataMaskingFlag.value,
      parameters: normalizeParameters(rows, direction),
    });
    return true;
  }

  async function saveExamples() {
    if (!validateExamples()) return false;
    await openApiApi.saveExamples({
      openApiId: openApiId.value,
      versionId: versionId.value,
      examples: [
        { exampleType: 'request', exampleName: '请求示例', content: requestExample.value, sort: 0 },
        { exampleType: 'success_response', exampleName: '成功响应示例', content: responseExample.value, sort: 1 },
      ],
      errorCodes: errorCodes.value.map((item, index) => ({ ...item, sort: index })),
      changeLog: changeLog.value,
    });
    return true;
  }

  async function saveAndNext() {
    saving.value = true;
    try {
      let saved = false;
      if (currentStep.value === 0) saved = await saveBasic();
      if (currentStep.value === 1) saved = await saveParameters(1);
      if (currentStep.value === 2) saved = await saveParameters(2);
      if (currentStep.value === 3) saved = await saveExamples();
      if (!saved) return;
      message.success('保存成功');
      currentStep.value = Math.min(currentStep.value + 1, 5);
      await syncRouteStep();
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      saving.value = false;
    }
  }

  function previousStep() {
    currentStep.value = Math.max(0, currentStep.value - 1);
    syncRouteStep();
  }

  function nextStep() {
    currentStep.value = Math.min(5, currentStep.value + 1);
    syncRouteStep();
  }

  function changeStep(step) {
    if (!openApiId.value && step > 0) {
      currentStep.value = 0;
      message.info('请先保存 API 基本信息');
      return;
    }
    syncRouteStep();
  }

  async function syncRouteStep() {
    await router.replace({
      path: '/open-api/editor',
      query: { ...route.query, openApiId: openApiId.value, step: currentStep.value + 1 },
    });
  }

  function backToList() {
    router.push({ path: '/open-api/manage' });
  }

  function mapParameters(rows) {
    return (rows || []).map((item, index) => ({
      ...item,
      rowKey: String(item.parameterId || `${Date.now()}-${index}`),
      parentRowKey: item.parentId ? String(item.parentId) : undefined,
    }));
  }

  async function loadDetail() {
    if (!openApiId.value) return;
    loading.value = true;
    try {
      const response = await openApiApi.detail(openApiId.value);
      const detail = response.data;
      const api = detail.api || {};
      const version = detail.version || {};
      versionId.value = version.versionId;
      Object.assign(basicForm, {
        apiName: api.apiName || '',
        apiCode: api.apiCode || '',
        categoryName: api.categoryName || '',
        serviceName: api.serviceName || '',
        versionNo: version.versionNo || api.apiVersion || 'v1.0.0',
        requestMethod: version.requestMethod || api.requestMethod || 'GET',
        gatewayPath: version.gatewayPath || api.requestPath || '',
        internalPath: version.internalPath || '',
        contentType: version.contentType || 'application/json',
        permissionLevel: version.permissionLevel || api.permissionLevel || 2,
        timeoutSeconds: version.timeoutSeconds || 10,
        ownerName: api.ownerName || '',
        tags: api.tags || '',
        description: version.description || api.description || '',
        environments: detail.environments || [],
      });
      unifiedResponseFlag.value = version.unifiedResponseFlag !== false;
      dataMaskingFlag.value = version.dataMaskingFlag !== false;
      changeLog.value = version.changeLog || '';
      requestParameters.value = mapParameters(detail.requestParameters);
      responseParameters.value = mapParameters(detail.responseParameters);
      const examples = detail.examples || [];
      requestExample.value = examples.find((item) => item.exampleType === 'request')?.content || requestExample.value;
      responseExample.value = examples.find((item) => item.exampleType === 'success_response')?.content || responseExample.value;
      errorCodes.value = (detail.errorCodes || []).map((item, index) => ({
        ...item,
        rowKey: String(item.errorCodeId || `${Date.now()}-${index}`),
      }));
    } catch (error) {
      smartSentry.captureError(error);
    } finally {
      loading.value = false;
    }
  }

  async function loadCategories() {
    try {
      const response = await openApiApi.categories();
      categories.value = response.data || [];
    } catch (error) {
      smartSentry.captureError(error);
    }
  }

  onMounted(() => {
    loadCategories();
    loadDetail();
  });
</script>
