<template>
  <div class="ai-page"><header class="ai-page__header"><div><h1 class="ai-page__title">解析任务</h1><div class="ai-page__subtitle">知识库文档从上传到向量入库的执行记录</div></div><a-button @click="load"><ReloadOutlined />刷新任务</a-button></header>
    <div class="ai-summary"><div v-for="item in summary" :key="item.label" class="ai-summary__item"><span class="ai-summary__icon"><FileTextOutlined /></span><div><div class="ai-summary__label">{{item.label}}</div><div class="ai-summary__value">{{item.value}}</div></div></div></div>
    <section class="ai-panel"><div class="ai-query"><div><label class="ai-query__label">任务号 / 文件名</label><a-input v-model:value="query.keyword" allow-clear placeholder="搜索任务" /></div>
      <div><label class="ai-query__label">知识库</label><a-select v-model:value="query.knowledgeBaseId" allow-clear style="width:100%" placeholder="全部"><a-select-option v-for="base in bases" :key="base.knowledgeBaseId" :value="base.knowledgeBaseId">{{base.baseName}}</a-select-option></a-select></div>
      <div><label class="ai-query__label">解析方案</label><a-select v-model:value="query.parsePlanId" allow-clear style="width:100%" placeholder="全部"><a-select-option v-for="plan in plans" :key="plan.configId" :value="plan.configId">{{plan.planName}}</a-select-option></a-select></div>
      <div><label class="ai-query__label">状态</label><a-select v-model:value="query.status" allow-clear style="width:100%" placeholder="全部"><a-select-option v-for="(label,key) in statuses" :key="key" :value="key">{{label}}</a-select-option></a-select></div>
      <a-space><a-button type="primary" @click="search">查询</a-button><a-button @click="reset">重置</a-button></a-space></div>
      <div style="margin-top:12px"><a-space><span>当前阶段</span><a-select v-model:value="query.currentStage" allow-clear style="width:170px" placeholder="全部阶段"><a-select-option v-for="(label,key) in stages" :key="key" :value="key">{{label}}</a-select-option></a-select></a-space></div>
    </section>
    <section class="ai-panel"><a-table :data-source="tasks" :loading="loading" :row-key="record=>record.taskId" :pagination="pagination" :scroll="{x:1120}" @change="pageChange">
      <a-table-column title="任务编号" data-index="taskId" :width="95" /><a-table-column title="文件名称" data-index="fileName" :width="175" />
      <a-table-column title="知识库" :width="120"><template #default="{record}">{{bases.find(b=>b.knowledgeBaseId===record.knowledgeBaseId)?.baseName||record.knowledgeBaseId}}</template></a-table-column>
      <a-table-column title="解析方案" :width="140"><template #default="{record}">{{plans.find(p=>p.configId===record.parsePlanId)?.planName||record.parsePlanId}}</template></a-table-column>
      <a-table-column title="当前阶段" :width="105"><template #default="{record}">{{stages[record.currentStage]}}</template></a-table-column>
      <a-table-column title="切片 / 入库" :width="105"><template #default="{record}">{{record.chunkCount}} / {{record.indexedCount}}</template></a-table-column>
      <a-table-column title="状态" :width="90"><template #default="{record}"><a-tag :color="record.status==='SUCCESS'?'green':record.status==='FAILED'?'red':record.status==='RUNNING'?'blue':'default'">{{statuses[record.status]}}</a-tag></template></a-table-column>
      <a-table-column title="错误原因" :width="180"><template #default="{record}"><a-tooltip :title="record.errorMessage"><span class="ai-muted">{{record.errorMessage ? record.errorMessage.slice(0,22) : '—'}}</span></a-tooltip></template></a-table-column>
      <a-table-column title="创建时间" data-index="createTime" :width="170" />
      <a-table-column title="操作" :width="160" fixed="right"><template #default="{record}"><a-space size="small"><a-button type="link" size="small" @click="detail(record)">查看</a-button><a-popconfirm v-if="['QUEUED','RUNNING'].includes(record.status)" title="取消此任务？" @confirm="operate(record,'cancel')"><a-button v-privilege="'ai:parse-task:operate'" type="link" danger size="small">取消</a-button></a-popconfirm><a-button v-if="record.status==='FAILED'" v-privilege="'ai:parse-task:operate'" type="link" size="small" @click="operate(record,'retry')">重试</a-button></a-space></template></a-table-column>
    </a-table></section>
    <a-drawer v-model:open="drawer" title="解析任务详情" width="min(560px, 100vw)" @close="stopPolling">
      <template v-if="selected"><div class="ai-task-file"><FileTextOutlined /><div><strong>{{selected.fileName}}</strong><div class="ai-muted">任务 #{{selected.taskId}} · {{bases.find(b=>b.knowledgeBaseId===selected.knowledgeBaseId)?.baseName}}</div></div><a-tag :color="selected.status==='SUCCESS'?'green':selected.status==='FAILED'?'red':'blue'">{{statuses[selected.status]}}</a-tag></div>
        <a-steps direction="vertical" size="small" :current="Math.max(0,stageOrder.indexOf(selected.currentStage))" :status="selected.status==='FAILED'?'error':selected.status==='SUCCESS'?'finish':'process'">
          <a-step v-for="key in stageOrder" :key="key" :title="stages[key]"><template #description><div v-for="step in stageLogs(key)" :key="step.stepId" class="ai-task-step">
            <a-tag :color="step.status==='SUCCESS'?'green':step.status==='FAILED'?'red':'default'">{{step.status==='SUCCESS'?'完成':step.status==='FAILED'?'失败':'已取消'}}</a-tag>
            <span>{{step.resultSummary || step.errorMessage || '无记录'}}</span><div class="ai-muted">{{step.durationMs}} ms · {{step.finishTime}}</div>
          </div><span v-if="!stageLogs(key).length" class="ai-muted">尚未执行</span></template></a-step>
        </a-steps>
        <a-descriptions title="基本信息" bordered size="small" :column="1"><a-descriptions-item label="解析方案">{{plans.find(p=>p.configId===selected.parsePlanId)?.planName}}</a-descriptions-item><a-descriptions-item label="切片 / 入库">{{selected.chunkCount}} / {{selected.indexedCount}}</a-descriptions-item><a-descriptions-item label="文件大小">{{(selected.fileSize/1024/1024).toFixed(2)}} MB</a-descriptions-item><a-descriptions-item label="开始 / 完成">{{selected.startTime||'—'}} / {{selected.finishTime||'—'}}</a-descriptions-item><a-descriptions-item v-if="selected.errorMessage" label="失败原因">{{selected.errorMessage}}</a-descriptions-item></a-descriptions>
      </template><template #extra><a-space><a-button v-if="selected?.status==='FAILED'" v-privilege="'ai:parse-task:operate'" @click="operate(selected,'retry')">重试</a-button><a-button v-if="selected && ['QUEUED','RUNNING'].includes(selected.status)" v-privilege="'ai:parse-task:operate'" danger @click="operate(selected,'cancel')">取消</a-button><a-button @click="download"><DownloadOutlined />下载文档</a-button></a-space></template>
    </a-drawer>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { FileTextOutlined, ReloadOutlined, DownloadOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { aiPlatformApi as api } from '/@/api/business/ai/ai-platform-api';
import { request } from '/@/lib/axios';
import { smartSentry } from '/@/lib/smart-sentry';
import './ai-platform.less';
const statuses={QUEUED:'排队中',RUNNING:'处理中',SUCCESS:'成功',FAILED:'失败',CANCELLED:'已取消'};
const stages={UPLOAD:'文件上传',PARSE:'内容解析',CHUNK:'文本切片',INDEX:'向量化入库'},stageOrder=Object.keys(stages);
const tasks=ref([]),bases=ref([]),plans=ref([]),loading=ref(false),drawer=ref(false),selected=ref(null),steps=ref([]),total=ref(0);
const query=reactive({pageNum:1,pageSize:10,keyword:'',knowledgeBaseId:undefined,parsePlanId:undefined,status:undefined,currentStage:undefined});
const pagination=computed(()=>({current:query.pageNum,pageSize:query.pageSize,total:total.value,showSizeChanger:true,showTotal:n=>`共 ${n} 条`}));
const summary=computed(()=>[{label:'本页任务',value:tasks.value.length},{label:'本页排队 / 处理',value:tasks.value.filter(t=>['QUEUED','RUNNING'].includes(t.status)).length},{label:'本页成功',value:tasks.value.filter(t=>t.status==='SUCCESS').length},{label:'本页失败',value:tasks.value.filter(t=>t.status==='FAILED').length}]);
let timer;
/** 加载分页任务数据及其关联知识库、解析方案。 */
async function load(){loading.value=true;try{const [t,b,p]=await Promise.all([api.parseTasks(query),api.knowledgeBases(),api.parsePlans()]);tasks.value=t.data?.list||[];total.value=t.data?.total||0;bases.value=b.data||[];plans.value=p.data||[];}catch(e){smartSentry.captureError(e);}finally{loading.value=false;}}
/** 重置页码后执行筛选。 */
function search(){query.pageNum=1;load();}
/** 重置任务筛选条件。 */
function reset(){Object.assign(query,{pageNum:1,keyword:'',knowledgeBaseId:undefined,parsePlanId:undefined,status:undefined,currentStage:undefined});load();}
/** 分页变化时保持查询条件。 */
function pageChange(page){query.pageNum=page.current;query.pageSize=page.pageSize;load();}
/** 显示选中任务的四阶段历史结果。 */
function detail(record){selected.value=record;steps.value=[];drawer.value=true;refreshDetail();stopPolling();timer=setInterval(()=>{if(selected.value && ['QUEUED','RUNNING'].includes(selected.value.status))refreshDetail();else stopPolling();},2500);}
/** 获取最新阶段记录。 */
async function refreshDetail(){if(!selected.value)return;try{const result=(await api.parseTaskDetail(selected.value.taskId)).data;selected.value=result.task;steps.value=result.steps||[];if(!['QUEUED','RUNNING'].includes(result.task.status)){stopPolling();load();}}catch(e){smartSentry.captureError(e);stopPolling();}}
/** 按阶段获取各次执行记录，保留重试历史。 */
const stageLogs=key=>steps.value.filter(step=>step.stage===key);
/** 取消或重试任务，并刷新详情及列表。 */
async function operate(record,action){try{await (action==='cancel'?api.cancelParseTask(record.taskId):api.retryParseTask(record.taskId));message.success(action==='cancel'?'已取消':'已重新排队');await load();if(drawer.value)await refreshDetail();}catch(e){smartSentry.captureError(e);}}
/** 停止详情轮询。 */
function stopPolling(){if(timer){clearInterval(timer);timer=undefined;}}
/** 带身份凭证下载该任务的真实源文件。 */
async function download(){if(!selected.value)return;try{const response=await request({url:`/ai/document/tasks/${selected.value.taskId}/download`,method:'get',responseType:'blob'});const blob=response.data;const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=selected.value.fileName;link.click();URL.revokeObjectURL(url);}catch(e){smartSentry.captureError(e);}}
onMounted(load);onBeforeUnmount(stopPolling);
</script>
<style scoped>
.ai-task-file{display:flex;align-items:center;gap:12px;margin-bottom:24px;padding-bottom:16px;border-bottom:1px solid #e7ebf1}
.ai-task-file>.anticon{color:#1677ff;font-size:28px}.ai-task-file>div{flex:1;min-width:0;overflow-wrap:anywhere}
.ai-task-step{margin:8px 0 14px;padding:10px;background:#f7f9fc;border:1px solid #e6ebf2;border-radius:4px;overflow-wrap:anywhere}
</style>
