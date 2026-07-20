<template>
  <AppShell>
    <CrudLayout title="职位管理" :message="message" @refresh="load">
      <form class="crud-form" @submit.prevent="save">
        <input v-model.trim="form.positionName" placeholder="职位名称" />
        <select v-model.number="form.deptId">
          <option :value="null">选择部门</option>
          <option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option>
        </select>
        <input v-model.number="form.level" type="number" placeholder="职位等级" />
        <input v-model.trim="form.description" placeholder="职位描述" />
        <button type="submit">{{ form.positionId ? '更新职位' : '新增职位' }}</button>
        <button class="secondary" type="button" @click="reset">清空</button>
      </form>

      <table class="data-table">
        <thead><tr><th>职位</th><th>部门</th><th>等级</th><th>描述</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in positions" :key="item.positionId">
            <td>{{ item.positionName }}</td>
            <td>{{ item.deptName || '-' }}</td>
            <td>{{ item.level }}</td>
            <td>{{ item.description || '-' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
              <button class="small danger" @click="remove(item.positionId)">删除</button>
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
import { listDepartments } from '../api/departments';
import { createPosition, deletePosition, listPositions, updatePosition } from '../api/positions';

const positions = ref([]);
const departments = ref([]);
const message = ref('');
const form = reactive(defaultForm());

onMounted(load);

function defaultForm() {
  return { positionId: null, positionName: '', deptId: null, level: 1, description: '' };
}

async function load() {
  const [positionRes, deptRes] = await Promise.all([listPositions(), listDepartments()]);
  positions.value = positionRes.data || [];
  departments.value = deptRes.data || [];
}

function reset() {
  Object.assign(form, defaultForm());
}

function edit(item) {
  Object.assign(form, { ...defaultForm(), ...item });
}

async function save() {
  const result = form.positionId ? await updatePosition(form) : await createPosition(form);
  message.value = result.msg;
  reset();
  await load();
}

async function remove(id) {
  const result = await deletePosition(id);
  message.value = result.msg;
  await load();
}
</script>
