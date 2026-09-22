<template>
  <wd-avatar
    shape="square"
    :size="size"
    :icon="appearance.icon"
    :bg-color="appearance.background"
    :color="appearance.color"
  />
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    assistant: {
      type: Object,
      default: () => ({}),
    },
    size: {
      type: Number,
      default: 48,
    },
  });

  const appearances = [
    { icon: 'file', background: '#e6f6ff', color: '#1689e6' },
    { icon: 'goods', background: '#eafaf0', color: '#0aa45b' },
    { icon: 'service', background: '#fff4e5', color: '#d96a00' },
    { icon: 'code', background: '#f1eaff', color: '#6c35d4' },
    { icon: 'chat', background: '#ffecef', color: '#df334d' },
  ];

  const appearance = computed(() => {
    const assistantId = String(props.assistant?.assistantId || props.assistant?.id || '');
    const index = assistantId
      .split('')
      .reduce((total, character) => total + character.charCodeAt(0), 0);
    return appearances[index % appearances.length];
  });
</script>
