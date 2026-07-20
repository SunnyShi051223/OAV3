<template>
  <AppShell>
    <CrudLayout title="用户管理" :message="message" @refresh="load">
      <form class="crud-form" @submit.prevent="save">
        <input v-model.trim="form.employeeNo" placeholder="工号" />
        <input v-model.trim="form.username" placeholder="登录账号" :disabled="Boolean(form.userId)" />
        <input v-if="!form.userId" v-model="form.password" type="password" placeholder="初始密码" />
        <input v-model.trim="form.realName" placeholder="姓名" />
        <input v-model.trim="form.phone" placeholder="手机号" />
        <input v-model.trim="form.email" placeholder="邮箱" />
        <select v-model.number="form.deptId">
          <option :value="null">选择部门</option>
          <option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option>
        </select>
        <select v-model.number="form.positionId">
          <option :value="null">选择职位</option>
          <option v-for="position in positions" :key="position.positionId" :value="position.positionId">
            {{ position.positionName }}
          </option>
        </select>
        <select v-model.number="form.roleId">
          <option :value="null">选择角色</option>
          <option v-for="role in roles" :key="role.roleId" :value="role.roleId">{{ role.roleName }}</option>
        </select>
        <select v-model.number="form.status">
          <option :value="1">在职/启用</option>
          <option :value="0">离职/禁用</option>
        </select>
        <button type="submit">{{ form.userId ? '更新用户' : '新增用户' }}</button>
        <button class="secondary" type="button" @click="reset">清空</button>
      </form>

      <table class="data-table">
        <thead>
          <tr><th>工号</th><th>姓名</th><th>部门</th><th>职位</th><th>角色</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in users" :key="item.userId">
            <td>{{ item.employeeNo }}</td>
            <td>{{ item.realName }}</td>
            <td>{{ item.deptName || '-' }}</td>
            <td>{{ item.positionName || '-' }}</td>
            <td>{{ item.roleName || '-' }}</td>
            <td>{{ item.status === 1 ? '启用' : '禁用' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
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
import { createUser, deleteUser, listUsers, updateUser } from '../api/users';
import { listDepartments } from '../api/departments';
import { listPositions } from '../api/positions';
import { listRoles } from '../api/roles';

const users = ref([]);
const departments = ref([]);
const positions = ref([]);
const roles = ref([]);
const message = ref('');
const form = reactive(defaultForm());

onMounted(load);

function defaultForm() {
  return { userId: null, employeeNo: '', username: '', password: '123456', realName: '', phone: '', email: '', deptId: null, positionId: null, roleId: null, status: 1 };
}

async function load() {
  const [userRes, deptRes, positionRes, roleRes] = await Promise.all([listUsers(), listDepartments(), listPositions(), listRoles()]);
  users.value = userRes.data || [];
  departments.value = deptRes.data || [];
  positions.value = positionRes.data || [];
  roles.value = roleRes.data || [];
}

function reset() {
  Object.assign(form, defaultForm());
}

function edit(item) {
  Object.assign(form, { ...defaultForm(), ...item, password: '' });
}

async function save() {
  const payload = { ...form };
  const result = form.userId ? await updateUser(payload) : await createUser(payload);
  message.value = result.msg;
  reset();
  await load();
}

async function remove(id) {
  const result = await deleteUser(id);
  message.value = result.msg;
  await load();
}
</script>
