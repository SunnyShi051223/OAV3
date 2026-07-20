<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">Elasticsearch</p>
        <h2>制度文档检索</h2>
      </div>
      <button v-if="authStore.hasPermission('document:index')" class="secondary" @click="rebuildIndex">
        重建索引
      </button>
    </section>

    <section class="search-row">
      <input v-model.trim="keyword" placeholder="输入关键词、制度名称或标签" @keyup.enter="search" />
      <button @click="search">检索</button>
    </section>
    <p v-if="message" class="notice">{{ message }}</p>

    <section v-if="authStore.hasPermission('document:add')" class="panel">
      <div class="panel-title">
        <h3>新增制度文档</h3>
      </div>
      <form class="document-form" @submit.prevent="create">
        <input v-model.trim="form.title" placeholder="标题" />
        <textarea v-model.trim="form.content" placeholder="内容"></textarea>
        <button type="submit">保存并同步 ES</button>
      </form>
    </section>

    <section class="document-list">
      <article v-for="doc in documents" :key="doc.docId" class="document-item">
        <h3 v-html="doc.titleHighlight || doc.title"></h3>
        <div v-if="doc.tagsHighlight || doc.tags" class="tag-list">
          <span v-if="doc.tagsHighlight" class="tag-chip" v-html="doc.tagsHighlight"></span>
          <span v-for="tag in splitTags(doc.tags)" v-else :key="`${doc.docId}-${tag}`" class="tag-chip">
            {{ tag }}
          </span>
        </div>
        <p v-html="doc.contentHighlight || doc.content"></p>
        <div class="document-meta">
          <span>{{ doc.authorName || '系统' }}</span>
          <span>{{ formatTime(doc.updateTime || doc.createTime) }}</span>
        </div>
      </article>
    </section>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import { authStore } from '../store/auth';
import { createDocument, listDocuments, rebuildDocumentIndex, searchDocuments } from '../api/documents';

const keyword = ref('请假');
const documents = ref([]);
const message = ref('');
const form = reactive({
  title: '',
  content: ''
});

onMounted(load);

async function load() {
  const result = await listDocuments();
  documents.value = result.data || [];
}

async function search() {
  message.value = '';
  const result = await searchDocuments(keyword.value);
  documents.value = result.data || [];
}

async function rebuildIndex() {
  const result = await rebuildDocumentIndex();
  message.value = result.data || result.msg;
  await search();
}

async function create() {
  const result = await createDocument(form);
  message.value = result.data || result.msg;
  form.title = '';
  form.content = '';
  await load();
}

function splitTags(tags) {
  if (!tags) {
    return [];
  }
  return tags.split(',').map((tag) => tag.trim()).filter(Boolean);
}

function formatTime(value) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}
</script>
