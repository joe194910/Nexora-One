<template>
  <wd-tabbar
    v-model="activeTab"
    fixed
    placeholder
    safe-area-inset-bottom
    :active-color="primaryColor"
    :inactive-color="mutedColor"
    :custom-style="tabbarStyle"
    @change="handleChange"
  >
    <wd-tabbar-item name="assistant" title="助手" icon="view-module" />
    <wd-tabbar-item name="mine" title="我的" icon="user" />
  </wd-tabbar>
</template>

<script setup>
  import { computed, ref, watch } from 'vue';
  import { useThemeStore } from '@/store/modules/system/theme';

  const props = defineProps({
    modelValue: {
      type: String,
      default: 'assistant',
    },
  });

  const themeStore = useThemeStore();
  const activeTab = ref(props.modelValue);
  const primaryColor = '#04bfe5';
  const mutedColor = computed(() => (themeStore.isDark ? '#92a0b6' : '#657189'));
  const tabbarStyle = computed(
    () =>
      `background:${themeStore.isDark ? '#0d1721' : '#ffffff'};` +
      `border-top:1px solid ${themeStore.isDark ? '#22303d' : '#e7ebf2'};`,
  );

  watch(
    () => props.modelValue,
    (value) => {
      activeTab.value = value;
    },
  );

  /**
   * 切换底部主导航。
   *
   * @param {Object} event Wot Design Uni 变更事件
   */
  function handleChange(event) {
    const nextTab = event.value;
    if (nextTab === props.modelValue) {
      return;
    }
    uni.reLaunch({
      url: nextTab === 'mine' ? '/pages/mine/mine' : '/pages/home/index',
    });
  }
</script>
