<!--
  * iframe 组件
  * 
  * @Author:    NexoraOne-主任：卓大 
  * @Date:      2022-09-01 23:14:49 
  * @Wechat:    NexoraOne 
  * @Email:     NexoraOne 
  * @Copyright  NexoraOne （ # ），Since 2012 
-->
<template>
  <iframe :id="name" :src="url" frameborder="0" height="800" scrolling="yes" width="100%"></iframe>
</template>
<script setup>
  let props = defineProps({
    name: String,
    url: String,
  });
</script>
