<template>
  <uni-popup ref="popupRef" background-color="#fff" type="bottom" :is-mask-click="false">
    <view class="query-form-pop">
      <view class="nexora-form">
        <uni-forms :label-width="100" :modelValue="form" label-position="left">
          <view class="nexora-form-group">
            <view class="nexora-form-group-title"> 产地 </view>
            <view class="nexora-form-group-content">
              <DictSelect keyCode="GODOS_PLACE" v-model="form.place" @change="onPlaceChange" />
            </view>
          </view>

          <view class="nexora-form-group">
            <view class="nexora-form-group-title"> 状态 </view>
            <view class="nexora-form-group-content">
              <NexoraEnumSelect enumName="GOODS_STATUS_ENUM" @change="onGoodsStatusChange" v-model="form.goodsStatus" />
            </view>
          </view>
        </uni-forms>

        <view class="nexora-form-submit nexora-margin-top20">
          <button class="nexora-form-submit-btn nexora-margin-right20" type="default" @click="cancel">取消</button>
          <button class="nexora-form-submit-btn" type="warn" @click="reset">重置</button>
          <button class="nexora-form-submit-btn" type="primary" @click="ok">确定</button>
        </view>
      </view>
    </view>
  </uni-popup>
</template>

<script setup>
  import { reactive, ref, toRaw } from 'vue';
  import DictSelect from '@/components/dict-select/index.vue';
  import NexoraEnumSelect from '@/components/nexora-enum-select/index.vue';

  const emits = defineEmits(['close']);
  defineExpose({ show });

  // --------------------- 显示 隐藏 ---------------------
  const popupRef = ref();

  function show() {
    popupRef.value.open();
  }

  function close() {
    popupRef.value.close();
  }

  // --------------------- 数据变化 ---------------------

  function onPlaceChange(value, text) {
    form.placeName = text;
  }

  function onGoodsStatusChange(value, text) {
    form.goodsStatusName = text;
  }

  // --------------------- 表单 ---------------------

  const defaultFormData = {
    // 商品状态
    goodsStatus: undefined,
    goodsStatusName: undefined,
    // 产地
    place: undefined,
    placeName: undefined,
  };

  const form = reactive({ ...defaultFormData });

  // ----------------------- 表单操作 ------------------------

  // 取消
  function cancel() {
    close();
    emits('close', null);
  }

  // 重置
  function reset() {
    Object.assign(form, defaultFormData);
    close();
    emits('close', toRaw(form));
  }

  // 确定
  function ok() {
    close();
    emits('close', toRaw(form));
  }
</script>

<style lang="scss" scoped>
  .query-form-pop {
    height: 800rpx;
    overflow-y: scroll;
  }
</style>
