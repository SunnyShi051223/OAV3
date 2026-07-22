<script setup>
import { computed, watch } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 },
})
const emit = defineEmits(['update:page'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

function go(page) {
  emit('update:page', Math.min(totalPages.value, Math.max(1, page)))
}

watch([() => props.page, () => props.total, () => props.pageSize], () => {
  const validPage = Math.min(totalPages.value, Math.max(1, props.page))
  if (validPage !== props.page) emit('update:page', validPage)
}, { immediate: true })
</script>

<template>
  <div v-if="total > 0" class="table-pagination">
    <span>第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
    <div class="pagination-actions">
      <button :disabled="page <= 1" aria-label="上一页" title="上一页" @click="go(page - 1)">‹</button>
      <strong>{{ page }}</strong>
      <button :disabled="page >= totalPages" aria-label="下一页" title="下一页" @click="go(page + 1)">›</button>
    </div>
  </div>
</template>
