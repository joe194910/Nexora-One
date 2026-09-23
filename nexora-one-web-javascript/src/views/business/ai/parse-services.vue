<template>
  <div class="ai-page"><header class="ai-page__header"><div><h1 class="ai-page__title">解析服务</h1><div class="ai-page__subtitle">文档提取、OCR、表格与图片理解能力</div></div><a-button v-privilege="'ai:parse-service:save'" type="primary" @click="edit()"><PlusOutlined />新增解析服务</a-button></header>
    <div class="ai-summary"><div v-for="item in summary" :key="item.label" class="ai-summary__item"><span class="ai-summary__icon"><ApiOutlined /></span><div><div class="ai-summary__label">{{item.label}}</div><div class="ai-summary__value">{{item.value}}</div></div></div></div>
    <section class="ai-panel"><div class="ai-query"><div><label class="ai-query__label">服务名称</label><a-input v-model:value="filter.name" allow-clear placeholder="搜索服务" /></div>
      <div><label class="ai-query__label">服务类型</label><a-select v-model:value="filter.type" allow-clear style="width:100%" placeholder="全部"><a-select-option v-for="(label,key) in types" :key="key" :value="key">{{label}}</a-select-option></a-select></div>
      <div><label class="ai-query__label">部署方式</label><a-select v-model:value="filter.deployment" allow-clear style="width:100%" placeholder="全部"><a-select-option value="BUILTIN">Java 内置</a-select-option><a-select-option value="HTTP">HTTP API</a-select-option></a-select></div>
      <a-button @click="load"><ReloadOutlined />刷新</a-button></div></section>
    <section class="ai-panel"><a-table :data-source="filtered" :loading="loading" :row-key="record=>record.parseServiceId" :scroll="{x:950}" :pagination="{pageSize:10}">
      <a-table-column title="服务名称" data-index="serviceName" :width="185" /><a-table-column title="类型" :width="110"><template #default="{record}">{{types[record.serviceType]}}</template></a-table-column>
      <a-table-column title="部署方式" :width="115"><template #default="{record}">{{record.deploymentType==='BUILTIN'?'Java 内置':'HTTP API'}}</template></a-table-column>
      <a-table-column title="地址 / 实现" :width="210"><template #default="{record}"><span class="ai-muted">{{record.endpoint || record.implementation}}</span></template></a-table-column>
      <a-table-column title="支持格式" :width="155"><template #default="{record}">{{record.supportedFormats}}</template></a-table-column>
      <a-table-column title="连接状态" :width="160"><template #default="{record}"><a-tooltip :title="record.lastError || '最近检测：' + (record.lastTestTime || '未检测')"><a-tag :color="record.connectionStatus==='CONNECTED'?'green':record.connectionStatus==='FAILED'?'red':'orange'">{{statuses[record.connectionStatus] || '未检测'}}</a-tag></a-tooltip></template></a-table-column>
      <a-table-column title="启用" :width="75"><template #default="{record}"><a-tag :color="record.enabledFlag?'green':'default'">{{record.enabledFlag?'启用':'停用'}}</a-tag></template></a-table-column>
      <a-table-column title="操作" :width="205" fixed="right"><template #default="{record}"><a-space size="small"><a-button v-privilege="'ai:parse-service:save'" type="link" size="small" @click="edit(record)">配置</a-button><a-button v-privilege="'ai:parse-service:test'" type="link" size="small" @click="test(record)">测试连接</a-button><a-popconfirm v-if="!record.defaultFlag" title="删除此服务？" @confirm="remove(record)"><a-button v-privilege="'ai:parse-service:save'" type="link" danger size="small">删除</a-button></a-popconfirm></a-space></template></a-table-column>
    </a-table></section>
    <a-drawer v-model:open="drawer" :title="form.parseServiceId?'编辑解析服务':'新增解析服务'" width="min(570px, 100vw)">
      <a-form layout="vertical" :model="form"><h3 class="ai-panel__title">基本信息</h3><a-form-item label="服务名称" required><a-input v-model:value="form.serviceName" :maxlength="100" /></a-form-item>
        <a-form-item label="服务类型" required><a-select v-model:value="form.serviceType" :disabled="!!form.parseServiceId" @change="typeChanged"><a-select-option v-for="(label,key) in types" :key="key" :value="key">{{label}}</a-select-option></a-select></a-form-item>
        <a-form-item label="部署方式" required><a-select v-model:value="form.deploymentType"><a-select-option value="BUILTIN" :disabled="!['TIKA','TABLE'].includes(form.serviceType)">Java 内置（Apache Tika）</a-select-option><a-select-option value="HTTP">HTTP API</a-select-option></a-select></a-form-item>
        <a-form-item v-if="form.deploymentType==='HTTP'" label="服务地址" required><a-input v-model:value="form.endpoint" placeholder="https://example.com" /><div class="ai-muted">GET /health 检测；POST /parse multipart file，返回 JSON text</div></a-form-item>
        <a-form-item v-if="form.deploymentType==='HTTP'" label="API Key"><a-input-password v-model:value="form.apiKey" :placeholder="form.parseServiceId?'留空保持原密钥':'可选'" /></a-form-item>
        <a-form-item label="支持格式" required><a-checkbox-group v-model:value="form.supportedFormats" :options="formatOptions" /></a-form-item>
        <a-form-item label="请求超时（秒）"><a-input-number v-model:value="form.timeoutSeconds" :min="1" :max="600" style="width:100%" /></a-form-item>
        <a-row :gutter="12"><a-col :span="12"><a-form-item label="启用服务"><a-switch v-model:checked="form.enabledFlag" /></a-form-item></a-col><a-col :span="12"><a-form-item label="默认服务"><a-switch v-model:checked="form.defaultFlag" /></a-form-item></a-col></a-row>
      </a-form><a-alert v-if="testResult" :type="testResult.status==='CONNECTED'?'success':'error'" show-icon :message="testResult.message" />
      <template #extra><a-space><a-button @click="drawer=false">取消</a-button><a-button type="primary" :loading="saving" @click="save">保存</a-button></a-space></template>
    </a-drawer>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { PlusOutlined, ApiOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { aiPlatformApi as api } from '/@/api/business/ai/ai-platform-api';
import { nexoraSentry } from '/@/lib/nexora-sentry';
import './ai-platform.less';
const types={TIKA:'文档解析',OCR:'OCR',TABLE:'表格解析',IMAGE:'图片理解'};
const statuses={CONNECTED:'就绪',FAILED:'异常',UNTESTED:'未检测'};
const formatOptions=['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','md','csv','png','jpg','jpeg'].map(value=>({label:value.toUpperCase(),value}));
const defaults={serviceName:'',serviceType:'TIKA',deploymentType:'BUILTIN',endpoint:'',apiKey:'',supportedFormats:['pdf','doc','docx','txt','md'],timeoutSeconds:60,defaultFlag:false,enabledFlag:true};
const services=ref([]),loading=ref(false),saving=ref(false),drawer=ref(false),testResult=ref(null);
const form=reactive({...defaults}),filter=reactive({name:'',type:undefined,deployment:undefined});
const filtered=computed(()=>services.value.filter(s=>(!filter.name || s.serviceName?.includes(filter.name)) && (!filter.type || s.serviceType===filter.type) && (!filter.deployment || s.deploymentType===filter.deployment)));
const summary=computed(()=>[{label:'服务总数',value:services.value.length},{label:'运行正常',value:services.value.filter(s=>s.enabledFlag && s.connectionStatus==='CONNECTED').length},{label:'未配置',value:services.value.filter(s=>s.connectionStatus==='UNTESTED').length},{label:'异常',value:services.value.filter(s=>s.connectionStatus==='FAILED').length}]);
/** 查询真实服务及连接状态。 */
async function load(){loading.value=true;try{services.value=(await api.parseServices()).data||[];}catch(e){nexoraSentry.captureError(e);}finally{loading.value=false;}}
/** 打开服务配置抽屉。 */
function edit(record){Object.keys(form).forEach(k=>delete form[k]);Object.assign(form,record?{...record,apiKey:record.apiKeyCipher==='******'?'******':'',supportedFormats:(record.supportedFormats||'').split(',') }:{...defaults,supportedFormats:[...defaults.supportedFormats]});testResult.value=null;drawer.value=true;}
/** 根据服务类型切换有实现的部署方式。 */
function typeChanged(){if(!['TIKA','TABLE'].includes(form.serviceType))form.deploymentType='HTTP';form.supportedFormats= form.serviceType==='OCR'||form.serviceType==='IMAGE'?['pdf','png','jpg','jpeg']:form.serviceType==='TABLE'?['xls','xlsx','csv']:[...defaults.supportedFormats];}
/** 保存解析服务配置。 */
async function save(){if(!form.serviceName?.trim() || !form.supportedFormats?.length)return message.warning('请填写名称和支持格式');saving.value=true;try{await api.saveParseService(form);message.success('服务已保存');drawer.value=false;await load();}catch(e){nexoraSentry.captureError(e);}finally{saving.value=false;}}
/** 发起后端内置/外部健康检测。 */
async function test(record){try{testResult.value=(await api.testParseService(record.parseServiceId)).data;message.info(testResult.value.message);await load();}catch(e){nexoraSentry.captureError(e);}}
/** 删除未被方案关联的服务。 */
async function remove(record){try{await api.deleteParseService(record.parseServiceId);message.success('已删除');await load();}catch(e){nexoraSentry.captureError(e);}}
onMounted(load);
</script>
