<template>
  <section class="toolbar">
    <div>
      <p class="eyebrow">第一阶段</p>
      <h2>{{ title }}</h2>
    </div>
    <button class="secondary" @click="$emit('refresh')">刷新</button>
  </section>
  <p v-if="message" class="notice">{{ message }}</p>
  <section class="panel">
    <slot />
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  message: { type: String, default: '' }
});

defineEmits(['refresh']);
</script>
