<!--
  * 数据 字典
  *
  * @Author:    NexoraOne-主任：卓大
  * @Date:      2025-03-26 21:50:41
  * @Wechat:    NexoraOne
  * @Email:     NexoraOne
  * @Copyright  NexoraOne （ # ），Since 2012
-->
<template>
  <a-form class="nexora-query-form">
    <a-row class="nexora-query-form-row">
      <a-form-item label="关键字" class="nexora-query-form-item">
        <a-input style="width: 300px" v-model:value="queryForm.keywords" placeholder="编码/名称/备注" />
      </a-form-item>
      <a-form-item label="禁用" class="nexora-query-form-item">
        <BooleanSelect v-model:value="queryForm.disabledFlag" style="width: 150px" />
      </a-form-item>
      <a-form-item class="nexora-query-form-item nexora-margin-left10">
        <a-button-group>
          <a-button type="primary" @click="onSearch">
            <template #icon>
              <SearchOutlined />
            </template>
            查询
          </a-button>
          <a-button @click="resetQuery">
            <template #icon>
              <ReloadOutlined />
            </template>
            重置
          </a-button>
        </a-button-group>
      </a-form-item>
    </a-row>
  </a-form>

  <a-card size="small" :bordered="false" :hoverable="true">
    <a-row class="nexora-table-btn-block">
      <div class="nexora-table-operate-block">
        <a-button @click="addOrUpdateDict" v-privilege="'support:dict:add'" type="primary">
          <template #icon>
            <PlusOutlined />
          </template>
          新建
        </a-button>

        <a-button @click="confirmBatchDelete" v-privilege="'support:dict:delete'" type="primary" danger :disabled="selectedRowKeyList.length === 0">
          <template #icon>
            <DeleteOutlined />
          </template>
          批量删除
        </a-button>
      </div>
      <div class="nexora-table-setting-block">
        <TableOperator class="nexora-margin-bottom5" v-model="columns" :tableId="TABLE_ID_CONST.SUPPORT.DICT" :refresh="ajaxQuery" />
      </div>
    </a-row>

    <a-table
      size="small"
      :dataSource="tableData"
      :columns="columns"
      :loading="tableLoading"
      rowKey="dictId"
      :pagination="false"
      bordered
      :row-selection="{ selectedRowKeys: selectedRowKeyList, onChange: onSelectChange }"
    >
      <template #bodyCell="{ record, column }">
        <template v-if="column.dataIndex === 'dictCode'">
          <a @click="showDictData(record)">{{ record.dictCode }}</a>
        </template>
        <template v-if="column.dataIndex === 'disabledFlag'">
          <a-switch
            @change="(checked) => handleChangeDisabled(checked, record)"
            v-model:checked="record.enabled"
            checked-children="启用中"
            un-checked-children="已禁用"
          />
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <div class="nexora-table-operate">
            <a-button @click="addOrUpdateDict(record)" v-privilege="'support:dict:update'" type="link">编辑</a-button>
          </div>
        </template>
      </template>
    </a-table>

    <div class="nexora-query-table-page">
      <a-pagination
        showSizeChanger
        showQuickJumper
        show-less-items
        :pageSizeOptions="PAGE_SIZE_OPTIONS"
        :defaultPageSize="queryForm.pageSize"
        v-model:current="queryForm.pageNum"
        v-model:pageSize="queryForm.pageSize"
        :total="total"
        @change="ajaxQuery"
        :show-total="(total) => `共${total}条`"
      />
    </div>

    <DictFormModal ref="dictFormModalRef" @reloadList="ajaxQuery" />
    <!-- 值列表 -->
    <DictDataModal ref="dictDataModalRef" />
  </a-card>
</template>
<script setup>
  import DictFormModal from './components/dict-form-modal.vue';
  import DictDataModal from './components/dict-data-modal.vue';
  import { onMounted, reactive, ref } from 'vue';
  import { message, Modal } from 'ant-design-vue';
  import { NexoraLoading } from '/@/components/framework/nexora-loading';
  import { dictApi } from '/@/api/support/dict-api';
  import { PAGE_SIZE_OPTIONS } from '/@/constants/common-const';
  import { nexoraSentry } from '/@/lib/nexora-sentry';
  import TableOperator from '/@/components/support/table-operator/index.vue';
  import { TABLE_ID_CONST } from '/@/constants/support/table-id-const';
  import BooleanSelect from '/@/components/framework/boolean-select/index.vue';

  const columns = ref([
    {
      title: 'ID',
      width: 90,
      dataIndex: 'dictId',
    },
    {
      title: '编码',
      dataIndex: 'dictCode',
    },
    {
      title: '名称',
      dataIndex: 'dictName',
    },
    {
      title: '备注',
      dataIndex: 'remark',
    },
    {
      title: '状态',
      width: 90,
      dataIndex: 'disabledFlag',
    },
    {
      title: '更新时间',
      width: 160,
      dataIndex: 'updateTime',
    },
    {
      title: '操作',
      dataIndex: 'action',
      fixed: 'right',
      width: 50,
    },
  ]);

  // ---------------- 查询数据 -----------------

  const queryFormState = {
    keywords: '',
    disabledFlag: null,
    pageNum: 1,
    pageSize: 10,
  };
  const queryForm = reactive({ ...queryFormState });
  const tableLoading = ref(false);
  const selectedRowKeyList = ref([]);
  const tableData = ref([]);
  const total = ref(0);
  const dictFormModalRef = ref();
  const dictDataModalRef = ref();

  // 显示操作记录弹窗
  function showDictData(dict) {
    dictDataModalRef.value.showModal(dict.dictId, dict.dictCode);
  }

  function onSelectChange(selectedRowKeys) {
    selectedRowKeyList.value = selectedRowKeys;
  }

  function resetQuery() {
    Object.assign(queryForm, queryFormState);
    ajaxQuery();
  }
  function onSearch() {
    queryForm.pageNum = 1;
    ajaxQuery();
  }
  async function ajaxQuery() {
    try {
      tableLoading.value = true;
      let responseData = await dictApi.queryDict(queryForm);
      const list = responseData.data.list;
      for (let item of list) {
        item.enabled = !item.disabledFlag;
      }
      total.value = responseData.data.total;
      tableData.value = list;
    } catch (e) {
      nexoraSentry.captureError(e);
    } finally {
      tableLoading.value = false;
    }
  }

  // ----------------------- 启用/禁用 ------------------------
  async function handleChangeDisabled(disabledFlag, dict) {
    NexoraLoading.show();
    try {
      await dictApi.updateDisabled(dict.dictId);
      dict.disabledFlag = !disabledFlag;
      message.success('操作成功');
      onSearch();
    } catch (e) {
      nexoraSentry.captureError(e);
    } finally {
      NexoraLoading.hide();
    }
  }

  // ---------------- 批量 删除 -----------------

  function confirmBatchDelete() {
    Modal.confirm({
      title: '提示',
      content: '确定要删除选中的字典吗?',
      okText: '删除',
      okType: 'danger',
      onOk() {
        batchDelete();
      },
      cancelText: '取消',
      onCancel() {},
    });
  }

  async function batchDelete() {
    try {
      NexoraLoading.show();
      await dictApi.batchDeleteDict(selectedRowKeyList.value);
      message.success('删除成功');
      await ajaxQuery();
    } catch (e) {
      nexoraSentry.captureError(e);
    } finally {
      NexoraLoading.hide();
    }
  }

  // ---------------- 添加/更新 -----------------

  function addOrUpdateDict(rowData) {
    dictFormModalRef.value.showModal(rowData);
  }

  onMounted(ajaxQuery);
</script>
