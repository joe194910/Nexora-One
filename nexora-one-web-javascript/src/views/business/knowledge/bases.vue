<template>
  <div class="knowledge-page">
    <header class="knowledge-header"><div><h1>知识库管理</h1><p>将已就绪文档组合成供助手检索的知识库</p></div>
      <a-button type="primary" @click="edit()"><PlusOutlined />新建知识库</a-button></header>
    <div class="knowledge-metrics"><div v-for="metric in metrics" :key="metric.name" class="knowledge-metric"><span>{{metric.name}}</span><strong>{{metric.count}}</strong></div></div>
    <section class="knowledge-panel"><div class="knowledge-filters"><a-input-search v-model:value="keyword" allow-clear placeholder="搜索知识库" style="width:270px" @search="load" /><a-button @click="load"><ReloadOutlined />刷新</a-button></div></section>
    <section class="knowledge-panel"><a-table :data-source="rows" :loading="loading" :row-key="r=>r.base.baseId" :scroll="{x:800}" :pagination="{pageSize:10,showSizeChanger:true}">
      <a-table-column title="知识库" :width="190"><template #default="{record}"><strong>{{record.base.baseName}}</strong></template></a-table-column>
      <a-table-column title="描述" :width="270"><template #default="{record}">{{record.base.description || '—'}}</template></a-table-column>
      <a-table-column title="文档数" :width="85"><template #default="{record}">{{record.documentIds.length}}</template></a-table-column>
      <a-table-column title="已关联文档" :width="240"><template #default="{record}"><span v-for="id in record.documentIds.slice(0,3)" :key="id" class="knowledge-muted">{{documents.find(d=>d.documentId===id)?.fileName || id}} · </span></template></a-table-column>
      <a-table-column title="状态" :width="85"><template #default="{record}"><a-tag :color="record.base.enabledFlag?'green':'default'">{{record.base.enabledFlag?'已启用':'已停用'}}</a-tag></template></a-table-column>
      <a-table-column title="更新时间" :width="160"><template #default="{record}">{{record.base.updateTime}}</template></a-table-column>
      <a-table-column title="操作" :width="150" fixed="right"><template #default="{record}"><a-button type="link" @click="edit(record)">编辑</a-button>
        <a-popconfirm title="删除该知识库？被助手引用时无法删除，文档不会被删除。" @confirm="remove(record)"><a-button type="link" danger>删除</a-button></a-popconfirm></template></a-table-column>
    </a-table></section>
    <a-drawer v-model:open="drawer" :title="form.baseId?'编辑知识库':'新建知识库'" width="min(530px, 100vw)">
      <a-form layout="vertical"><a-form-item label="知识库名称" required><a-input v-model:value="form.baseName" :maxlength="100" placeholder="例如：产品知识库" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :maxlength="500" :rows="3" show-count /></a-form-item>
        <a-form-item label="已就绪文档"><a-input-search v-model:value="documentKeyword" allow-clear placeholder="搜索我的文档" />
          <div style="margin-top:10px;max-height:310px;overflow:auto"><a-checkbox-group v-model:value="form.documentIds" style="width:100%">
            <div v-for="doc in readyDocuments" :key="doc.documentId" style="padding:9px 0;border-bottom:1px solid #edf1f5"><a-checkbox :value="doc.documentId">{{doc.fileName}} <span class="knowledge-muted">· {{doc.fileType.toUpperCase()}}</span></a-checkbox></div>
          </a-checkbox-group><a-empty v-if="!readyDocuments.length" description="暂无已就绪文档" /></div></a-form-item>
        <a-form-item label="启用知识库"><a-switch v-model:checked="form.enabledFlag" /></a-form-item></a-form>
      <template #footer><a-space><a-button @click="drawer=false">取消</a-button><a-button type="primary" :loading="saving" @click="save">保存知识库</a-button></a-space></template>
    </a-drawer>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { knowledgeApi as api } from '/@/api/business/knowledge/knowledge-api';
import './knowledge.less';
const rows=ref([]),documents=ref([]),keyword=ref(''),documentKeyword=ref(''),loading=ref(false),saving=ref(false),drawer=ref(false);
const form=reactive({baseId:null,baseName:'',description:'',documentIds:[],enabledFlag:true});
const readyDocuments=computed(()=>documents.value.filter(d=>d.status==='READY'&&d.fileName.toLowerCase().includes(documentKeyword.value.toLowerCase())));
const metrics=computed(()=>[{name:'知识库',count:rows.value.length},{name:'已启用',count:rows.value.filter(r=>r.base.enabledFlag).length},{name:'关联文档',count:rows.value.reduce((n,r)=>n+r.documentIds.length,0)}]);
/** 读取本人知识库和实际文档状态。 */
async function load(){loading.value=true;try{const [bases,docs]=await Promise.all([api.bases({keyword:keyword.value}),api.documents({})]);rows.value=bases.data||[];documents.value=docs.data||[];}finally{loading.value=false;}}
/** 加载已有知识库的关联文档；不复制源文件。 */
function edit(row){Object.assign(form,{baseId:row?.base.baseId||null,baseName:row?.base.baseName||'',description:row?.base.description||'',enabledFlag:row?.base.enabledFlag??true,documentIds:[...(row?.documentIds||[])]});documentKeyword.value='';drawer.value=true;}
/** 将选择的已就绪文档原子保存为知识库关联。 */
async function save(){if(!form.baseName.trim())return message.warning('请输入知识库名称');saving.value=true;try{await api.saveBase({...form});drawer.value=false;message.success('知识库已保存');await load();}finally{saving.value=false;}}
/** 删除无助手引用的知识库。 */
async function remove(row){await api.deleteBase(row.base.baseId);message.success('知识库已删除');load();}
onMounted(load);
</script>
