<template>
  <main class="login-page">
    <section class="login-panel">
      <div>
        <p class="eyebrow">OA Attendance</p>
        <h1>企业OA考勤系统</h1>
      </div>

      <form class="form" @submit.prevent="submit">
        <label>
          <span>账号</span>
          <input v-model.trim="form.username" autocomplete="username" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" autocomplete="current-password" />
        </label>
        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '../api/auth';
import { authStore } from '../store/auth';

const router = useRouter();
const loading = ref(false);
const error = ref('');
const form = reactive({
  username: 'admin',
  password: '123456'
});

async function submit() {
  error.value = '';
  loading.value = true;
  try {
    const result = await login(form);
    if (result.code !== 200) {
      throw new Error(result.msg);
    }
    authStore.setSession(result.data);
    router.push('/dashboard');
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
}
</script>
