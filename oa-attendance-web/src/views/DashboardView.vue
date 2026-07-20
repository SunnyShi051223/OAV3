<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">当前身份</p>
        <h2>{{ authStore.user?.realName || '-' }}</h2>
      </div>
      <button class="secondary" @click="handleLogout">退出登录</button>
    </section>

    <section class="grid">
      <article class="card">
        <span>角色</span>
        <strong>{{ authStore.user?.roleName }} / {{ authStore.user?.roleCode }}</strong>
      </article>
      <article class="card">
        <span>部门</span>
        <strong>{{ authStore.user?.deptName || '-' }}</strong>
      </article>
      <article class="card">
        <span>职位</span>
        <strong>{{ authStore.user?.positionName || '-' }}</strong>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>权限</h3>
      </div>
      <div class="permission-list">
        <span v-for="item in authStore.user?.permissions" :key="item">{{ item }}</span>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { useRouter } from 'vue-router';
import AppShell from '../components/AppShell.vue';
import { logout } from '../api/auth';
import { authStore } from '../store/auth';

const router = useRouter();

async function handleLogout() {
  try {
    await logout();
  } finally {
    authStore.clear();
    router.push('/login');
  }
}
</script>
