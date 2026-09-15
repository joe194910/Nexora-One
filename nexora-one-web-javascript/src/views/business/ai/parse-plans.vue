<template>
  <div class="ai-page">
    <header class="ai-page__header"><div><h1 class="ai-page__title">解析方案</h1><div class="ai-page__subtitle">知识库文件解析与文本切片规则</div></div>
      <a-button v-privilege="'ai:parse-plan:save'" type="primary" @click="edit()"><PlusOutlined />新增方案</a-button></header>
    <div class="ai-summary">
      <div v-for="item in summary" :key="item.label" class="ai-summary__item"><span class="ai-summary__icon"><FileTextOutlined /></span><div><div class="ai-summary__label">{{ item.label }}</div><div class="ai-summary__value">{{ item.value }}</div></div></div>
    </div>
    <section class="ai-panel">
      <div class="ai-query"><div><label class="ai-query__label">方案名称</label><a-input v-model:value="filter.keyword" allow-clear placeholder="搜索方案" /></div>
        <div><label class="ai-query__label">解析器</label><a-select v-model:value="filter.parser" allow-clear style="width:100%" placeholder="全部"><a-select-option value="TIKA">Apache Tika</a-select-option></a-select></div>
        <div><label class="ai-query__label">状态</label><a-select v-model:value="filter.status" allow-clear style="width:100%" placeholder="全部"><a-select-option value="enabled">启用</a-select-option><a-select-option value="disabled">停用</a-select-option></a-select></div>
        <a-space><a-button @click="load"><ReloadOutlined />刷新</a-button><a-button @click="Object.assign(filter,{keyword:'',parser:undefined,status:undefined})">重置</a-button></a-space>
      </div>
    </section>
    <section class="ai-panel"><a-table :data-source="filtered" :loading="loading" :row-key="record => record.configId" :scroll="{x:1100}" :pagination="{pageSize:10}">
      <a-table-column title="方案名称" data-index="planName" :width="210"><template #default="{record}"><strong>{{ record.planName }}</strong> <a-tag v-if="record.defaultFlag" color="blue">默认</a-tag><div class="ai-muted">{{ record.description }}</div></template></a-table-column>
      <a-table-column title="支持格式" :width="205"><template #default="{record}"><a-tag v-for="format in (record.supportedFormats || '').split(',').slice(0,5)" :key="format">{{ format.toUpperCase() }}</a-tag></template></a-table-column>
      <a-table-column title="解析器" :width="130"><template #default="{record}">{{ serviceName(record.parserServiceId) }}</template></a-table-column>
      <a-table-column title="切片方式" :width="130"><template #default="{record}">{{ record.chunkMethod === 'FIXED' ? '固定长度' : '递归字符' }}</template></a-table-column>
      <a-table-column title="长度 / 重叠" :width="120"><template #default="{record}">{{ record.chunkSize }} / {{ record.chunkOverlap }}</template></a-table-column>
      <a-table-column title="OCR" :width="80"><template #default="{record}">{{ record.ocrEnabled ? '启用' : '关闭' }}</template></a-table-column>
      <a-table-column title="知识库" data-index="baseCount" :width="85" />
      <a-table-column title="状态" :width="85"><template #default="{record}"><a-tag :color="record.enabledFlag ? 'green' : 'default'">{{ record.enabledFlag ? '启用' : '停用' }}</a-tag></template></a-table-column>
      <a-table-column title="操作" :width="220" fixed="right"><template #default="{record}">
        <a-space size="small"><a-button v-privilege="'ai:parse-plan:save'" type="link" size="small" @click="edit(record)">编辑</a-button>
          <a-upload :show-upload-list="false" :before-upload="file => test(record.configId, file)"><a-button v-privilege="'ai:parse-plan:test'" type="link" size="small">测试</a-button></a-upload>
          <a-dropdown><a-button type="link" size="small">更多 <DownOutlined /></a-button><template #overlay><a-menu>
            <a-menu-item v-privilege="'ai:parse-plan:save'" @click="copy(record.configId)">复制</a-menu-item>
            <a-menu-item v-privilege="'ai:parse-plan:save'" @click="toggle(record)">{{ record.enabledFlag ? '停用' : '启用' }}</a-menu-item>
            <a-menu-item v-if="!record.defaultFlag" v-privilege="'ai:parse-plan:save'" @click="makeDefault(record)">设为默认</a-menu-item>
            <a-menu-item v-if="!record.defaultFlag && !record.baseCount" v-privilege="'ai:parse-plan:save'" danger @click="remove(record)">删除</a-menu-item>
          </a-menu></template></a-dropdown></a-space>
      </template></a-table-column></a-table></section>

    <a-drawer v-model:open="drawer" :title="form.configId ? '编辑解析方案' : '新增解析方案'" width="min(640px, 100vw)" :destroy-on-close="true">
      <a-form layout="vertical" :model="form"><h3 class="ai-panel__title">基本信息</h3>
        <a-form-item label="方案名称" required><a-input v-model:value="form.planName" :maxlength="100" /></a-form-item>
        <a-form-item label="方案描述"><a-textarea v-model:value="form.description" :maxlength="200" :rows="2" /></a-form-item>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="启用方案"><a-switch v-model:checked="form.enabledFlag" /></a-form-item></a-col><a-col :span="12"><a-form-item label="默认方案"><a-switch v-model:checked="form.defaultFlag" /></a-form-item></a-col></a-row>
        <h3 class="ai-panel__title">文件规则</h3>
        <a-form-item label="支持格式" required><a-checkbox-group v-model:value="form.supportedFormats" :options="formatOptions" /></a-form-item>
        <h3 class="ai-panel__title">解析规则</h3>
        <a-form-item label="文档解析服务" required><a-select v-model:value="form.parserServiceId" placeholder="请选择已启用的 Apache Tika 服务"><a-select-option v-for="service in options('TIKA')" :key="service.parseServiceId" :value="service.parseServiceId">{{ service.serviceName }}</a-select-option></a-select></a-form-item>
        <a-form-item label="表格提取"><a-switch v-model:checked="form.extractTable" /></a-form-item>
        <a-row :gutter="12"><a-col :span="12"><a-form-item label="表格解析服务"><a-select v-model:value="form.tableServiceId" allow-clear placeholder="Tika 默认提取"><a-select-option v-for="service in options('TABLE')" :key="service.parseServiceId" :value="service.parseServiceId">{{ service.serviceName }}</a-select-option></a-select></a-form-item></a-col>
        </a-row>
        <a-row :gutter="12"><a-col :span="12"><a-form-item label="启用 OCR"><a-switch v-model:checked="form.ocrEnabled" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="图片理解"><a-switch v-model:checked="form.extractImage" /></a-form-item></a-col></a-row>
        <a-form-item v-if="form.ocrEnabled" label="OCR 服务" required><a-select v-model:value="form.ocrServiceId" placeholder="请选择 OCR 服务"><a-select-option v-for="service in options('OCR')" :key="service.parseServiceId" :value="service.parseServiceId">{{ service.serviceName }}</a-select-option></a-select></a-form-item>
        <a-form-item v-if="form.extractImage" label="图片理解服务" required><a-select v-model:value="form.imageServiceId"><a-select-option v-for="service in options('IMAGE')" :key="service.parseServiceId" :value="service.parseServiceId">{{ service.serviceName }}</a-select-option></a-select></a-form-item>
        <h3 class="ai-panel__title">切片规则</h3>
        <a-form-item label="切片方式"><a-select v-model:value="form.chunkMethod"><a-select-option value="RECURSIVE">递归字符</a-select-option><a-select-option value="FIXED">固定长度</a-select-option></a-select></a-form-item>
        <a-row :gutter="12"><a-col :span="8"><a-form-item label="切片长度"><a-input-number v-model:value="form.chunkSize" :min="100" :max="10000" style="width:100%" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="重叠长度"><a-input-number v-model:value="form.chunkOverlap" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="最小长度"><a-input-number v-model:value="form.minChunkSize" :min="1" style="width:100%" /></a-form-item></a-col></a-row>
        <a-form-item label="分隔符"><a-input v-model:value="form.separators" /></a-form-item>
        <a-form-item label="相同文件处理"><a-select v-model:value="form.duplicateStrategy"><a-select-option value="SKIP">跳过已入库文件</a-select-option><a-select-option value="REPLACE">成功后替换旧版本</a-select-option></a-select></a-form-item>
        <a-form-item label="文件处理超时（分钟）"><a-input-number v-model:value="form.timeoutMinutes" :min="1" style="width:100%" /></a-form-item>
      </a-form><template #extra><a-space><a-button @click="drawer=false">取消</a-button><a-button type="primary" :loading="saving" @click="save">保存</a-button></a-space></template>
    </a-drawer>
    <a-modal v-model:open="preview.open" title="解析测试结果" width="760px" :footer="null">
      <a-descriptions bordered size="small" :column="3"><a-descriptions-item label="文件">{{ preview.data.fileName }}</a-descriptions-item><a-descriptions-item label="字符数">{{ preview.data.characters }}</a-descriptions-item><a-descriptions-item label="切片数">{{ preview.data.chunkCount }}</a-descriptions-item></a-descriptions>
      <a-collapse style="margin-top:16px"><a-collapse-panel v-for="(chunk,index) in preview.data.chunks" :key="index" :header="`切片 ${index+1}`"><div class="ai-code-block">{{ chunk }}</div></a-collapse-panel></a-collapse>
    </a-modal>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { PlusOutlined, FileTextOutlined, ReloadOutlined, DownOutlined } from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { aiPlatformApi as api } from '/@/api/business/ai/ai-platform-api';
import { smartSentry } from '/@/lib/smart-sentry';
import './ai-platform.less';

const plans = ref([]), services = ref([]), loading = ref(false), saving = ref(false), drawer = ref(false);
const filter = reactive({ keyword:'', parser:undefined, status:undefined });
const preview = reactive({ open:false, data:{} });
const formatOptions = ['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','md','csv','png','jpg','jpeg'].map(value => ({label:value.toUpperCase(),value}));
const defaults = {planName:'',description:'',supportedFormats:['pdf','docx','txt','md'],parserType:'TIKA',pdfMode:'FLOW',extractTable:true,preserveHeading:false,extractImage:false,ocrEnabled:false,ocrService:'',chunkMethod:'RECURSIVE',chunkSize:800,chunkOverlap:100,minChunkSize:100,separators:'\\n\\n,\\n,。！？；',preserveTitleMetadata:false,concurrency:1,timeoutMinutes:10,retryCount:0,duplicateStrategy:'SKIP',continueOnError:false,defaultFlag:false,enabledFlag:true};
const form = reactive({ ...defaults });
const filtered = computed(() => plans.value.filter(p => (!filter.keyword || p.planName?.includes(filter.keyword)) && (!filter.parser || p.parserType === filter.parser) && (!filter.status || p.enabledFlag === (filter.status === 'enabled'))));
const summary = computed(() => [{label:'方案总数',value:plans.value.length},{label:'启用方案',value:plans.value.filter(p=>p.enabledFlag).length},{label:'默认方案',value:plans.value.filter(p=>p.defaultFlag).length},{label:'关联知识库',value:plans.value.reduce((n,p)=>n+(p.baseCount||0),0)}]);
const options = type => services.value.filter(s=>s.serviceType===type && s.enabledFlag);
const serviceName = id => services.value.find(s=>s.parseServiceId===id)?.serviceName || '未配置';

/** 加载真实解析方案及服务选项。 */
async function load() { loading.value=true; try { const [p,s]=await Promise.all([api.parsePlans(),api.parseServices()]); plans.value=p.data||[];services.value=s.data||[]; } catch(e) {smartSentry.captureError(e);} finally {loading.value=false;} }
/** 将实体转换为保存表单，避免传入格式字符串。 */
function payload(record) { return {...record,supportedFormats:typeof record.supportedFormats==='string' ? record.supportedFormats.split(',').filter(Boolean) : record.supportedFormats}; }
/** 打开方案编辑抽屉。 */
function edit(record) { Object.keys(form).forEach(key=>delete form[key]);Object.assign(form,record?payload(record):{...defaults,supportedFormats:[...defaults.supportedFormats],parserServiceId:options('TIKA')[0]?.parseServiceId});drawer.value=true; }
/** 保存并刷新方案列表。 */
async function save() { if(!form.planName?.trim() || !form.supportedFormats?.length || !form.parserServiceId)return message.warning('请填写名称、格式和解析服务');saving.value=true;try {await api.saveParsePlan(payload(form));message.success('方案已保存');drawer.value=false;await load();} catch(e){smartSentry.captureError(e);} finally{saving.value=false;} }
/** 测试方案对真实上传文件的解析效果。 */
function test(id,file) {const body=new FormData();body.append('file',file);api.testParsePlan(id,body).then(r=>{preview.data=r.data||{};preview.open=true;}).catch(smartSentry.captureError);return false;}
/** 复制方案并刷新列表。 */
async function copy(id){try{await api.copyParsePlan(id);message.success('已复制');await load();}catch(e){smartSentry.captureError(e);}}
/** 根据关联约束切换启停。 */
async function toggle(record){try{await api.saveParsePlan({...payload(record),enabledFlag:!record.enabledFlag});await load();}catch(e){smartSentry.captureError(e);}}
/** 将方案设为默认。 */
async function makeDefault(record){try{await api.saveParsePlan({...payload(record),enabledFlag:true,defaultFlag:true});await load();}catch(e){smartSentry.captureError(e);}}
/** 删除未被使用的方案。 */
function remove(record){Modal.confirm({title:`删除 ${record.planName}？`,onOk:async()=>{await api.deleteParsePlan(record.configId);await load();}});}
onMounted(load);
</script>
