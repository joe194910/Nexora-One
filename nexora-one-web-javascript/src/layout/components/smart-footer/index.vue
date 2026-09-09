<!--
  * 底部：版权、公司等信息
  *
  * @Author:    NexoraOne-主任：卓大
  * @Date:      2022-09-06 20:29:12
  * @Wechat:    NexoraOne
  * @Email:     NexoraOne
  * @Copyright  NexoraOne （ # ），Since 2012
-->
<template>
  <div class="version">
    <a target="_blank" class="smart-copyright" href="#"> ©2012-{{ currentYear }} NexoraOne | NexoraOne </a>
  </div>
</template>
<script setup>
  import dayjs from 'dayjs';

  const currentYear = dayjs().year();
</script>

<style lang="less" scoped>
  .version {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    a {
      color: rgba(0, 0, 0, 0.45);
    }
    a:hover {
      color: @primary-color;
    }
  }
</style>
