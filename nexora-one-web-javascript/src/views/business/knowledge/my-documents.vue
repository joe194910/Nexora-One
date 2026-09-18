<template>
  <div class="knowledge-page">
    <header class="knowledge-header"><div><h1>我的文档</h1><p>已完成向量化的文档可用于多个知识库</p></div>
      <a-space><a-button @click="load"><ReloadOutlined />刷新</a-button><a-button type="primary" @click="uploadOpen=true"><UploadOutlined />上传文档</a-button></a-space></header>
    <div class="knowledge-metrics"><div v-for="metric in metrics" :key="metric.name" class="knowledge-metric"><span>{{metric.name}}</span><strong>{{metric.count}}</strong></div></div>
    <section class="knowledge-panel"><div class="knowledge-filters"><a-input-search v-model:value="keyword" style="width:260px" allow-clear placeholder="文档名称" @search="load" />
      <a-select v-model:value="status" style="width:150px" allow-clear placeholder="全部状态" @change="load"><a-select-option v-for="(label,key) in statusLabels" :key="key" :value="key">{{label}}</a-select-option></a-select>
      <a-button @click="reset">重置</a-button></div></section>
    <section class="knowledge-panel"><a-table :data-source="rows" :loading="loading" :row-key="r=>r.documentId" :scroll="{x:990}" :pagination="{pageSize:10,showSizeChanger:true}">
      <a-table-column title="文档名称" data-index="fileName" :width="225" /><a-table-column title="类型" data-index="fileType" :width="80" />
      <a-table-column title="大小" :width="95"><template #default="{record}">{{(record.fileSize/1024/1024).toFixed(2)}} MB</template></a-table-column>
      <a-table-column title="解析方案" :width="175"><template #default="{record}">{{plans.find(p=>p.configId===record.parsePlanId)?.planName || record.parsePlanId}}</template></a-table-column>
      <a-table-column title="切片 / 向量" :width="120"><template #default="{record}">{{record.chunkCount ?? '—'}} / {{record.indexedCount ?? '—'}}</template></a-table-column>
      <a-table-column title="状态" :width="90"><template #default="{record}"><a-tag :color="record.status==='READY'?'green':record.status==='FAILED'?'red':'blue'">{{statusLabels[record.status]}}</a-tag></template></a-table-column>
      <a-table-column title="上传时间" data-index="createTime" :width="170" />
      <a-table-column title="操作" :width="185" fixed="right"><template #default="{record}"><div class="knowledge-actions"><a-button type="link" @click="showDetail(record)">查看</a-button>
        <a-button type="link" @click="api.download(record)">下载</a-button><a-button v-if="record.status==='FAILED'" type="link" @click="retry(record)">重试</a-button>
        <a-button v-if="record.status==='PROCESSING'" type="link" @click="cancel(record)">取消</a-button>
        <a-popconfirm title="确认删除该文档及向量？已关联知识库的文档无法删除。" @confirm="remove(record)"><a-button type="link" danger>删除</a-button></a-popconfirm></div></template></a-table-column>
    </a-table></section>
    <a-drawer v-model:open="uploadOpen" title="上传文档" width="min(480px, 100vw)" :destroy-on-close="true">
      <label>解析方案</label><a-select v-model:value="planId" style="width:100%;margin:8px 0 18px" placeholder="默认解析方案" allow-clear>
        <a-select-option v-for="plan in plans" :key="plan.configId" :value="plan.configId">{{plan.planName}}{{plan.defaultFlag?'（默认）':''}}</a-select-option></a-select>
      <label class="knowledge-upload" @dragover.prevent @drop.prevent="addDropped"><CloudUploadOutlined /><strong>选择或拖入文件</strong><small>每个文件不超过 50 MB</small>
        <input type="file" multiple hidden @change="addFiles" /></label>
      <a-list :data-source="files" style="margin-top:14px" size="small"><template #renderItem="{item,index}"><a-list-item>{{item.name}}<a-button type="text" danger @click="files.splice(index,1)">移除</a-button></a-list-item></template></a-list>
      <a-progress v-if="uploading" :percent="Math.round(uploaded/files.length*100)" /><template #footer><a-space><a-button @click="uploadOpen=false">取消</a-button><a-button type="primary" :loading="uploading" :disabled="!files.length" @click="submitFiles">开始上传处理</a-button></a-space></template>
    </a-drawer>
    <a-drawer v-model:open="detailOpen" title="文档处理详情" width="min(540px, 100vw)">
      <template v-if="detail"><h3>{{detail.document.fileName}}</h3><a-tag :color="detail.document.status==='READY'?'green':detail.document.status==='FAILED'?'red':'blue'">{{statusLabels[detail.document.status]}}</a-tag>
        <a-descriptions bordered size="small" :column="1" style="margin:16px 0"><a-descriptions-item label="任务编号">{{detail.task.task.taskId}}</a-descriptions-item>
          <a-descriptions-item label="切片 / 入库">{{detail.task.task.chunkCount}} / {{detail.task.task.indexedCount}}</a-descriptions-item>
          <a-descriptions-item v-if="detail.task.task.errorMessage" label="错误原因">{{detail.task.task.errorMessage}}</a-descriptions-item></a-descriptions>
        <a-tabs v-model:active-key="detailTab" @change="onDetailTab">
          <a-tab-pane key="task" tab="处理记录"><a-steps direction="vertical" size="small" :current="Math.max(0,stages.indexOf(detail.task.task.currentStage))" :status="detail.document.status==='FAILED'?'error':detail.document.status==='READY'?'finish':'process'">
            <a-step v-for="stage in stages" :key="stage" :title="stageLabels[stage]"><template #description><div v-for="step in detail.task.steps.filter(s=>s.stage===stage||s.stageCode===stage)" :key="step.stepId">
              {{step.resultSummary || step.errorMessage || step.status}} · {{step.durationMs}} ms</div></template></a-step></a-steps></a-tab-pane>
          <a-tab-pane key="chunks" tab="文档切片" :disabled="detail.document.status!=='READY'">
            <a-spin :spinning="chunksLoading"><a-empty v-if="!chunks.length" description="暂无切片" /><div v-for="chunk in chunks" :key="chunk.pointId" class="knowledge-chunk">
              <strong>切片 {{chunk.chunkIndex + 1}}</strong><p>{{chunk.text}}</p></div></a-spin>
            <a-button v-if="chunkOffset" block @click="loadChunks">加载更多</a-button>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-drawer>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { ReloadOutlined, UploadOutlined, CloudUploadOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
import './knowledge.less';
const statusLabels={READY:'已就绪',PROCESSING:'处理中',FAILED:'失败'};
const stages=['UPLOAD','PARSE','CHUNK','INDEX'],stageLabels={UPLOAD:'文件上传',PARSE:'内容解析',CHUNK:'文本切片',INDEX:'向量化入库'};
const rows=ref([]),plans=ref([]),loading=ref(false),keyword=ref(''),status=ref(undefined),uploadOpen=ref(false),detailOpen=ref(false),detail=ref(null),detailTab=ref('task'),chunks=ref([]),chunkOffset=ref(null),chunksLoading=ref(false),files=ref([]),planId=ref(undefined),uploading=ref(false),uploaded=ref(0);
const metrics=computed(()=>[{name:'文档总数',count:rows.value.length},{name:'已就绪',count:rows.value.filter(r=>r.status==='READY').length},{name:'处理中',count:rows.value.filter(r=>r.status==='PROCESSING').length},{name:'失败',count:rows.value.filter(r=>r.status==='FAILED').length}]);
const MAX_UPLOAD_SIZE=50*1024*1024;
let timer;
/** 加载本人文档及允许选择的解析方案。 */
async function load(){loading.value=true;try{rows.value=(await api.documents({keyword:keyword.value,status:status.value})).data||[];plans.value=(await api.options()).data.plans||[];}finally{loading.value=false;}}
/** 恢复完整列表。 */
function reset(){keyword.value='';status.value=undefined;load();}
/** 将符合 50 MB 限制的文件加入上传队列。 */
function appendFiles(selectedFiles){for(const file of Array.from(selectedFiles||[])){if(file.size>MAX_UPLOAD_SIZE){message.error(`${file.name} 超过 50 MB，无法上传`);continue;}files.value.push(file);}}
/** 选择多个待上传文件。 */
function addFiles(event){appendFiles(event.target.files);event.target.value='';}
/** 拖放文件进入上传队列。 */
function addDropped(event){appendFiles(event.dataTransfer.files);}
/** 逐个上传；重复内容由后端返回已存在文档而不重新解析。 */
async function submitFiles(){uploading.value=true;uploaded.value=0;const queue=[...files.value];for(const file of queue){try{await api.upload(file,planId.value);uploaded.value++;}catch(error){message.error(`${file.name} 上传失败`);}}
  uploading.value=false;files.value=[];uploadOpen.value=false;await load();if(uploaded.value)message.success(`已接收 ${uploaded.value} 个文档`);}
/** 展示真实解析任务及阶段结果。 */
async function showDetail(record){detail.value=(await api.detail(record.documentId)).data;detailTab.value='task';chunks.value=[];chunkOffset.value=null;detailOpen.value=true;}
/** 展示真实入库切片，并使用 Qdrant 游标加载后续内容。 */
async function loadChunks(){chunksLoading.value=true;try{const page=(await api.chunks(detail.value.document.documentId,chunkOffset.value)).data;chunks.value.push(...page.items);chunkOffset.value=page.nextOffset;}finally{chunksLoading.value=false;}}
/** 切换到切片视图时按需读取，避免文档列表批量拉取向量内容。 */
function onDetailTab(key){if(key==='chunks'&&!chunks.value.length)loadChunks();}
/** 重试失败任务。 */
async function retry(record){await api.retry(record.documentId);message.success('已重新排队');load();}
/** 取消未完成的底层解析任务。 */
async function cancel(record){await api.cancel(record.documentId);message.success('已请求取消');load();}
/** 删除本人未被引用的文档。 */
async function remove(record){await api.deleteDocument(record.documentId);message.success('文档已删除');load();}
onMounted(()=>{load();timer=setInterval(()=>{if(rows.value.some(r=>r.status==='PROCESSING'))load();},5000);});
onBeforeUnmount(()=>clearInterval(timer));
</script>
