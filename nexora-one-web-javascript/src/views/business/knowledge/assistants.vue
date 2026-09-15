<template>
  <div class="knowledge-page">
    <header class="knowledge-header"><div><h1>智能助手</h1><p>关联知识库并进行有来源的 AI 问答</p></div>
      <a-button type="primary" @click="edit()"><PlusOutlined />新建助手</a-button></header>
    <section v-if="!active" class="knowledge-panel"><a-table :data-source="rows" :loading="loading" :row-key="r=>r.assistant.assistantId" :scroll="{x:820}" :pagination="{pageSize:10}">
      <a-table-column title="助手" :width="190"><template #default="{record}"><strong>{{record.assistant.assistantName}}</strong></template></a-table-column>
      <a-table-column title="对话模型" :width="160"><template #default="{record}">{{models.find(m=>m.modelId===record.assistant.modelId)?.modelName||record.assistant.modelId}}</template></a-table-column>
      <a-table-column title="关联知识库" :width="260"><template #default="{record}">{{record.baseIds.map(id=>bases.find(b=>b.base.baseId===id)?.base.baseName||id).join('、')||'未关联'}}</template></a-table-column>
      <a-table-column title="TopK / 阈值" :width="110"><template #default="{record}">{{record.assistant.topK}} / {{record.assistant.scoreThreshold}}</template></a-table-column>
      <a-table-column title="状态" :width="85"><template #default="{record}"><a-tag :color="record.assistant.enabledFlag?'green':'default'">{{record.assistant.enabledFlag?'已启用':'已停用'}}</a-tag></template></a-table-column>
      <a-table-column title="操作" :width="190" fixed="right"><template #default="{record}"><a-button type="link" :disabled="!record.assistant.enabledFlag" @click="openChat(record)">问答</a-button><a-button type="link" @click="edit(record)">编辑</a-button>
        <a-popconfirm title="删除助手及全部会话？知识库和文档不会被删除。" @confirm="remove(record)"><a-button type="link" danger>删除</a-button></a-popconfirm></template></a-table-column>
    </a-table></section>
    <template v-else><div class="knowledge-header"><a-space><a-button @click="active=null"><ArrowLeftOutlined /></a-button><strong>{{active.assistant.assistantName}}</strong><span class="knowledge-muted">{{active.baseIds.map(id=>bases.find(b=>b.base.baseId===id)?.base.baseName).filter(Boolean).join('、')}}</span></a-space><a-button @click="newConversation"><PlusOutlined />新会话</a-button></div>
      <div class="knowledge-chat"><aside class="knowledge-chat__side"><a-list :data-source="conversations" size="small"><template #renderItem="{item}"><a-list-item style="cursor:pointer" @click="selectConversation(item)">
        <a-space><MessageOutlined /><span :style="{fontWeight:conversationId===item.conversationId?600:400}">{{item.title}}</span></a-space>
        <a-popconfirm title="删除此会话？" @confirm.stop="deleteConversation(item)"><a-button type="text" size="small" @click.stop><DeleteOutlined /></a-button></a-popconfirm></a-list-item></template></a-list>
      </aside><main class="knowledge-chat__main"><div ref="messageArea" class="knowledge-chat__messages">
        <a-empty v-if="!messages.length" description="开始提问" />
        <div v-for="(entry,index) in messages" :key="index" class="knowledge-chat__bubble" :class="{'knowledge-chat__bubble--user':entry.role==='user'}">
          {{entry.content}}<div v-if="entry.role==='assistant'&&citations(entry).length" class="knowledge-chat__citation">
            <div v-for="hit in citations(entry)" :key="`${hit.documentId}-${hit.chunkIndex}`"><FileTextOutlined /> {{hit.fileName}} · 切片 {{hit.chunkIndex}} · 相似度 {{Number(hit.score).toFixed(3)}}</div></div>
        </div><div v-if="sending" class="knowledge-chat__bubble">正在生成回答…</div>
      </div><div class="knowledge-chat__composer"><a-textarea v-model:value="question" :rows="2" :maxlength="4000" placeholder="输入问题" @keydown.ctrl.enter="send" />
        <div style="display:flex;justify-content:flex-end;margin-top:8px"><a-button type="primary" :loading="sending" :disabled="!question.trim()" @click="send"><SendOutlined />发送</a-button></div></div></main></div>
    </template>
    <a-drawer v-model:open="drawer" :title="form.assistantId?'编辑智能助手':'新建智能助手'" width="min(550px, 100vw)">
      <a-form layout="vertical"><a-form-item label="助手名称" required><a-input v-model:value="form.assistantName" :maxlength="100" /></a-form-item>
        <a-form-item label="对话模型" required><a-select v-model:value="form.modelId" style="width:100%" placeholder="选择已启用模型"><a-select-option v-for="model in models" :key="model.modelId" :value="model.modelId">{{model.modelName}}</a-select-option></a-select></a-form-item>
        <a-form-item label="系统提示词"><a-textarea v-model:value="form.systemPrompt" :rows="5" :maxlength="8000" show-count /></a-form-item>
        <a-form-item label="关联知识库"><a-select v-model:value="form.baseIds" mode="multiple" style="width:100%" placeholder="可选择多个知识库">
          <a-select-option v-for="row in bases.filter(r=>r.base.enabledFlag)" :key="row.base.baseId" :value="row.base.baseId">{{row.base.baseName}} · {{row.documentIds.length}} 个文档</a-select-option></a-select></a-form-item>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="检索 TopK"><a-input-number v-model:value="form.topK" :min="1" :max="20" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="相似度阈值"><a-input-number v-model:value="form.scoreThreshold" :min="0" :max="1" :step="0.05" style="width:100%" /></a-form-item></a-col></a-row>
        <a-form-item label="展示引用来源"><a-switch v-model:checked="form.showCitations" /></a-form-item><a-form-item label="启用助手"><a-switch v-model:checked="form.enabledFlag" /></a-form-item></a-form>
      <template #footer><a-space><a-button @click="drawer=false">取消</a-button><a-button type="primary" :loading="saving" @click="save">保存助手</a-button></a-space></template>
    </a-drawer>
  </div>
</template>
<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue';
import { PlusOutlined, ArrowLeftOutlined, MessageOutlined, DeleteOutlined, FileTextOutlined, SendOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
import './knowledge.less';
const rows=ref([]),bases=ref([]),models=ref([]),loading=ref(false),drawer=ref(false),saving=ref(false),active=ref(null),conversations=ref([]),conversationId=ref(null),messages=ref([]),question=ref(''),sending=ref(false),messageArea=ref(null);
const form=reactive({assistantId:null,assistantName:'',modelId:undefined,systemPrompt:'',baseIds:[],topK:5,scoreThreshold:0.65,showCitations:true,enabledFlag:true});
/** 获取本人助手、知识库和可选对话模型。 */
async function load(){loading.value=true;try{const [assistants,baseRows,options]=await Promise.all([api.assistants(),api.bases({}),api.options()]);rows.value=assistants.data||[];bases.value=baseRows.data||[];models.value=options.data.chatModels||[];}finally{loading.value=false;}}
/** 编辑模型与多个知识库的关联配置。 */
function edit(row){Object.assign(form,{assistantId:row?.assistant.assistantId||null,assistantName:row?.assistant.assistantName||'',modelId:row?.assistant.modelId||undefined,systemPrompt:row?.assistant.systemPrompt||'',baseIds:[...(row?.baseIds||[])],topK:row?.assistant.topK||5,scoreThreshold:Number(row?.assistant.scoreThreshold??0.65),showCitations:row?.assistant.showCitations??true,enabledFlag:row?.assistant.enabledFlag??true});drawer.value=true;}
/** 保存助手配置，服务端复核所有知识库归属。 */
async function save(){if(!form.assistantName.trim()||!form.modelId)return message.warning('请填写助手名称并选择对话模型');saving.value=true;try{await api.saveAssistant({...form});drawer.value=false;message.success('助手已保存');await load();}finally{saving.value=false;}}
/** 删除助手及其会话。 */
async function remove(row){await api.deleteAssistant(row.assistant.assistantId);message.success('助手已删除');load();}
/** 进入助手工作台并加载本人会话。 */
async function openChat(row){active.value=row;newConversation();await reloadConversations();}
/** 开始新会话。 */
function newConversation(){conversationId.value=null;messages.value=[];question.value='';}
/** 加载助手历史会话。 */
async function reloadConversations(){if(active.value)conversations.value=(await api.conversations(active.value.assistant.assistantId)).data||[];}
/** 切换本人会话并读取历史及引用。 */
async function selectConversation(item){conversationId.value=item.conversationId;messages.value=(await api.messages(active.value.assistant.assistantId,item.conversationId)).data||[];scrollBottom();}
/** 删除一段本人会话。 */
async function deleteConversation(item){await api.deleteConversation(active.value.assistant.assistantId,item.conversationId);if(conversationId.value===item.conversationId)newConversation();await reloadConversations();}
/** 按后端真实检索和模型响应发送问题，失败时回滚临时消息。 */
async function send(){if(!question.value.trim()||sending.value)return;const text=question.value.trim();question.value='';messages.value.push({role:'user',content:text});sending.value=true;scrollBottom();
  try{const result=(await api.chat(active.value.assistant.assistantId,{conversationId:conversationId.value,question:text})).data;
    conversationId.value=result.conversationId;messages.value.push({role:'assistant',content:result.content,citationsJson:JSON.stringify(result.citations||[])});await reloadConversations();scrollBottom();
  }catch(error){messages.value.pop();question.value=text;}finally{sending.value=false;}}
/** 从已持久化消息中解析实际命中引用。 */
function citations(entry){try{return JSON.parse(entry.citationsJson||'[]');}catch{return [];}}
/** 新消息出现后滚动到可见区域。 */
async function scrollBottom(){await nextTick();if(messageArea.value)messageArea.value.scrollTop=messageArea.value.scrollHeight;}
onMounted(load);
</script>
