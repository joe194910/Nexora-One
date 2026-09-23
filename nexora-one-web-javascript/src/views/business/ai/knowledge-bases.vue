<template>
  <div class="ai-page"><header class="ai-page__header"><div><h1 class="ai-page__title">知识库</h1><div class="ai-page__subtitle">管理解析方案、向量存储和文档</div></div><a-button v-privilege="'ai:knowledge:save'" type="primary" @click="edit()"><PlusOutlined />创建知识库</a-button></header>
    <section class="ai-panel"><a-table :data-source="bases" :loading="loading" :row-key="record=>record.knowledgeBaseId" :pagination="{pageSize:10}" :scroll="{x:920}">
      <a-table-column title="知识库" :width="200"><template #default="{record}"><strong>{{record.baseName}}</strong><div class="ai-muted">{{record.description}}</div></template></a-table-column>
      <a-table-column title="解析方案" :width="170"><template #default="{record}">{{plans.find(p=>p.configId===record.parsePlanId)?.planName||'方案不可用'}}</template></a-table-column>
      <a-table-column title="向量模型" :width="160"><template #default="{record}">{{models.find(m=>m.modelId===record.embeddingModelId)?.modelName||'模型不可用'}}</template></a-table-column>
      <a-table-column title="向量实例 / 集合" :width="205"><template #default="{record}">{{vectors.find(v=>v.vectorDatabaseId===record.vectorDatabaseId)?.instanceName||'实例不可用'}}<div class="ai-muted">{{record.collectionName}}</div></template></a-table-column>
      <a-table-column title="更新时间" data-index="updateTime" :width="170" />
      <a-table-column title="操作" fixed="right" :width="155"><template #default="{record}"><a-space><a-button v-privilege="'ai:knowledge:save'" type="link" size="small" @click="edit(record)">编辑</a-button><a-upload :show-upload-list="false" :before-upload="file=>upload(record,file)"><a-button v-privilege="'ai:knowledge:upload'" type="link" size="small" :loading="uploading===record.knowledgeBaseId">上传文档</a-button></a-upload></a-space></template></a-table-column>
    </a-table></section>
    <a-drawer v-model:open="drawer" :title="form.knowledgeBaseId?'编辑知识库':'创建知识库'" width="min(510px, 100vw)">
      <a-form layout="vertical" :model="form"><a-form-item label="知识库名称" required><a-input v-model:value="form.baseName" :maxlength="100" /></a-form-item><a-form-item label="描述"><a-textarea v-model:value="form.description" :maxlength="500" :rows="3" /></a-form-item>
        <a-form-item label="解析方案" required><a-select v-model:value="form.parsePlanId" placeholder="请选择已启用方案"><a-select-option v-for="plan in plans.filter(p=>p.enabledFlag)" :key="plan.configId" :value="plan.configId">{{plan.planName}}</a-select-option></a-select></a-form-item>
        <a-form-item label="向量模型" required><a-select v-model:value="form.embeddingModelId" placeholder="请选择已启用 Embedding 模型"><a-select-option v-for="model in models.filter(m=>m.enabledFlag)" :key="model.modelId" :value="model.modelId">{{model.modelName}} / {{model.modelCode}}</a-select-option></a-select></a-form-item>
        <a-form-item label="向量数据库" required><a-select v-model:value="form.vectorDatabaseId" :disabled="!!form.knowledgeBaseId" placeholder="请选择 Qdrant 实例"><a-select-option v-for="vector in vectors.filter(v=>v.enabledFlag && v.databaseType==='QDRANT')" :key="vector.vectorDatabaseId" :value="vector.vectorDatabaseId">{{vector.instanceName}}</a-select-option></a-select></a-form-item>
      </a-form><template #extra><a-space><a-button @click="drawer=false">取消</a-button><a-button type="primary" :loading="saving" @click="save">保存</a-button></a-space></template>
    </a-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { aiPlatformApi as api } from '/@/api/business/ai/ai-platform-api';
import { nexoraSentry } from '/@/lib/nexora-sentry';
import './ai-platform.less';
const bases=ref([]),plans=ref([]),models=ref([]),vectors=ref([]),loading=ref(false),saving=ref(false),drawer=ref(false),uploading=ref(null);
const form=reactive({baseName:'',description:'',parsePlanId:undefined,embeddingModelId:undefined,vectorDatabaseId:undefined});
/** 读取知识库和解析/向量依赖选项。 */
async function load(){loading.value=true;try{const [b,p,m,v]=await Promise.all([api.knowledgeBases(),api.parsePlans(),api.queryModels({pageNum:1,pageSize:500,type:'EMBEDDING'}),api.queryVectors({pageNum:1,pageSize:500})]);bases.value=b.data||[];plans.value=p.data||[];models.value=m.data?.list||[];vectors.value=v.data?.list||[];}catch(e){nexoraSentry.captureError(e);}finally{loading.value=false;}}
/** 打开知识库创建或编辑抽屉。 */
function edit(record){Object.keys(form).forEach(k=>delete form[k]);Object.assign(form,record||{baseName:'',description:'',parsePlanId:plans.value.find(p=>p.defaultFlag && p.enabledFlag)?.configId,embeddingModelId:undefined,vectorDatabaseId:vectors.value.find(v=>v.defaultFlag && v.enabledFlag)?.vectorDatabaseId});drawer.value=true;}
/** 保存解析方案与向量依赖的绑定。 */
async function save(){if(!form.baseName?.trim()||!form.parsePlanId||!form.embeddingModelId||!form.vectorDatabaseId)return message.warning('请填写名称、解析方案、向量模型与实例');saving.value=true;try{await api.saveKnowledgeBase(form);message.success('知识库已保存');drawer.value=false;await load();}catch(e){nexoraSentry.captureError(e);}finally{saving.value=false;}}
/** 上传文档并确认后端创建任务。 */
function upload(record,file){const body=new FormData();body.append('file',file);uploading.value=record.knowledgeBaseId;api.uploadKnowledgeDocument(record.knowledgeBaseId,body).then(r=>message.success(`文档已入队，任务 #${r.data.taskId}`)).catch(nexoraSentry.captureError).finally(()=>uploading.value=null);return false;}
onMounted(load);
</script>
