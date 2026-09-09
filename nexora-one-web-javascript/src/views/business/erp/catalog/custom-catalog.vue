<!--
  * 目录  demo
  *
  * @Author:    NexoraOne-主任：卓大
  * @Date:      2022-08-21 19:52:43
  * @Wechat:    NexoraOne
  * @Email:     NexoraOne
  * @Copyright  NexoraOne （ # ），Since 2012
-->
<template>
  <div>
    <CategoryTreeTable :category-type="CATEGORY_TYPE_ENUM.DEMO.value" :privilegePrefix="'custom:'"/>
  </div>
</template>
<script setup>
  import CategoryTreeTable from './components/category-tree-table.vue';
  import { CATEGORY_TYPE_ENUM } from '/@/constants/business/erp/category-const';
</script>
