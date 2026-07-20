<template>
  <AppShell>
    <CrudLayout title="用户管理" :message="message" @refresh="load">
      <p v-if="error" class="error">{{ error }}</p>

      <form class="crud-form" @submit.prevent="save">
        <label>
          工号
          <input v-model.trim="form.employeeNo" placeholder="如 E0002" />
        </label>
        <label>
          登录账号
          <input v-model.trim="form.username" :disabled="Boolean(form.userId)" placeholder="员工登录账号" />
        </label>
        <label v-if="!form.userId">
          初始密码
          <input v-model.trim="form.password" type="text" placeholder="默认 123456，可修改" />
          <small>创建后员工使用该密码登录；编辑用户不会修改密码。</small>
        </label>
        <label>
          姓名
          <input v-model.trim="form.realName" placeholder="员工姓名" />
        </label>
        <label>
          手机号
          <input v-model.trim="form.phone" placeholder="手机号" />
        </label>
        <label>
          邮箱
          <input v-model.trim="form.email" placeholder="邮箱" />
        </label>
        <label>
          部门
          <select v-model.number="form.deptId">
            <option :value="null">选择部门</option>
            <option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option>
          </select>
        </label>
        <label>
          职位
          <select v-model.number="form.positionId">
            <option :value="null">选择职位</option>
            <option v-for="position in positions" :key="position.positionId" :value="position.positionId">
              {{ position.positionName }}
            </option>
          </select>
        </label>
        <label>
          角色
          <select v-model.number="form.roleId">
            <option :value="null">选择角色</option>
            <option v-for="role in roles" :key="role.roleId" :value="role.roleId">{{ role.roleName }}</option>
          </select>
        </label>
        <label>
          状态
          <select v-model.number="form.status">
            <option :value="1">在职 / 启用</option>
            <option :value="0">离职 / 禁用</option>
          </select>
        </label>
        <button type="submit">{{ form.userId ? '更新用户' : '新增用户' }}</button>
        <button class="secondary" type="button" @click="reset">清空</button>
      </form>

      <table class="data-table">
        <thead>
          <tr>
            <th>工号</th>
            <th>登录账号</th>
            <th>姓名</th>
            <th>部门</th>
            <th>职位</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in users" :key="item.userId">
            <td>{{ item.employeeNo }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName }}</td>
            <td>{{ item.deptName || '-' }}</td>
            <td>{{ item.positionName || '-' }}</td>
            <td>{{ item.roleName || '-' }}</td>
            <td>{{ item.status === 1 ? '启用' : '禁用' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
              <button class="small secondary" @click="resetPassword(item)">重置密码</button>
              <button class="small danger" @click="remove(item.userId)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </CrudLayout>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import CrudLayout from '../components/CrudLayout.vue';
import { createUser, deleteUser, listUsers, resetUserPassword, updateUser } from '../api/users';
import { listDepartments } from '../api/departments';
import { listPositions } from '../api/positions';
import { listRoles } from '../api/roles';

const users = ref([]);
const departments = ref([]);
const positions = ref([]);
const roles = ref([]);
const message = ref('');
const error = ref('');
const form = reactive(defaultForm());

onMounted(load);

function defaultForm() {
  return {
    userId: null,
    employeeNo: '',
    username: '',
    password: '123456',
    realName: '',
    phone: '',
    email: '',
    deptId: null,
    positionId: null,
    roleId: null,
    status: 1
  };
}

async function load() {
  error.value = '';
  const [userRes, deptRes, positionRes, roleRes] = await Promise.all([
    listUsers(),
    listDepartments(),
    listPositions(),
    listRoles()
  ]);
  users.value = userRes.data || [];
  departments.value = deptRes.data || [];
  positions.value = positionRes.data || [];
  roles.value = roleRes.data || [];
}

function reset() {
  error.value = '';
  Object.assign(form, defaultForm());
}

function edit(item) {
  error.value = '';
  Object.assign(form, { ...defaultForm(), ...item, password: '' });
}

async function save() {
  error.value = '';
  if (!form.userId && !form.password) {
    error.value = '请填写初始密码';
    return;
  }
  try {
    const payload = normalizePayload();
    const result = form.userId ? await updateUser(payload) : await createUser(payload);
    message.value = result.msg || '保存成功';
    reset();
    await load();
  } catch (err) {
    error.value = err.message;
  }
}

async function remove(id) {
  error.value = '';
  try {
    const result = await deleteUser(id);
    message.value = result.msg || '删除成功';
    await load();
  } catch (err) {
    error.value = err.message;
  }
}

async function resetPassword(item) {
  error.value = '';
  try {
    const result = await resetUserPassword(item.userId, '123456');
    message.value = `${item.username} ${result.msg || '密码已重置'}，初始密码为 123456`;
  } catch (err) {
    error.value = err.message;
  }
}

function normalizePayload() {
  const payload = { ...form };
  if (payload.userId) {
    delete payload.password;
  }
  return payload;
}
</script>
