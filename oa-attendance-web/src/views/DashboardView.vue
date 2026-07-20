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
        <h3>个人信息</h3>
        <button class="secondary" @click="fillProfile">恢复当前资料</button>
      </div>
      <form class="profile-form" @submit.prevent="saveProfile">
        <label>
          <span>昵称</span>
          <input v-model.trim="profile.nickname" />
        </label>
        <label>
          <span>性别</span>
          <select v-model="profile.gender">
            <option value="">未填写</option>
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
        </label>
        <label>
          <span>手机号</span>
          <input v-model.trim="profile.phone" />
        </label>
        <label>
          <span>邮箱</span>
          <input v-model.trim="profile.email" />
        </label>
        <label>
          <span>生日</span>
          <input v-model="profile.birthDate" type="date" />
        </label>
        <button type="submit">保存个人信息</button>
      </form>
      <p v-if="message" class="notice">{{ message }}</p>
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
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '../components/AppShell.vue';
import { logout } from '../api/auth';
import { updateProfile } from '../api/users';
import { authStore } from '../store/auth';

const router = useRouter();
const message = ref('');
const profile = reactive({
  nickname: '',
  gender: '',
  phone: '',
  email: '',
  birthDate: ''
});

onMounted(fillProfile);

function fillProfile() {
  const user = authStore.user || {};
  profile.nickname = user.nickname || '';
  profile.gender = user.gender || '';
  profile.phone = user.phone || '';
  profile.email = user.email || '';
  profile.birthDate = user.birthDate || '';
}

async function saveProfile() {
  const result = await updateProfile(profile);
  message.value = result.msg;
  if (result.code === 200 && result.data) {
    authStore.user = result.data;
    localStorage.setItem('oa_user', JSON.stringify(result.data));
    fillProfile();
  }
}

async function handleLogout() {
  try {
    await logout();
  } finally {
    authStore.clear();
    router.push('/login');
  }
}
</script>
