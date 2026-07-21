<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">组织</p>
        <h2>同事检索</h2>
      </div>
    </section>

    <section class="search-row">
      <input
        v-model.trim="keyword"
        placeholder="输入姓名、工号或手机号搜索同事"
        @keyup.enter="search"
      />
      <button @click="search">检索</button>
    </section>
    <p v-if="message" class="notice">{{ message }}</p>

    <section class="colleague-list">
      <div v-if="colleagues.length === 0 && searched" class="empty-state">
        暂无匹配的同事
      </div>
      <div v-for="col in colleagues" :key="col.userId" class="colleague-card">
        <div class="colleague-avatar">
          <img v-if="col.avatar" :src="col.avatar" :alt="col.realName" />
          <span v-else class="avatar-placeholder">{{ (col.realName || '?')[0] }}</span>
        </div>
        <div class="colleague-info">
          <div class="colleague-name">
            {{ col.realName }}
            <span class="colleague-role">{{ col.roleName }}</span>
          </div>
          <div class="colleague-meta">
            <span>{{ col.deptName }}</span>
            <span v-if="col.positionName">{{ col.positionName }}</span>
            <span>工号: {{ col.employeeNo }}</span>
          </div>
          <div class="colleague-contact">
            <span v-if="col.phone">电话: {{ col.phone }}</span>
            <span v-if="col.email">邮箱: {{ col.email }}</span>
            <span v-if="col.hireDate">入职: {{ col.hireDate }}</span>
          </div>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import { searchColleagues } from '../api/colleagues';

const keyword = ref('');
const colleagues = ref([]);
const message = ref('');
const searched = ref(false);

async function search() {
  message.value = '';
  searched.value = true;
  try {
    const result = await searchColleagues(keyword.value);
    colleagues.value = result.data || [];
    if (colleagues.value.length === 0) {
      message.value = '未找到匹配的同事';
    }
  } catch (e) {
    message.value = e.message || '查询失败';
    colleagues.value = [];
  }
}
</script>
