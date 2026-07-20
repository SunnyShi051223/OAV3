<template>
  <AppShell>
    <CrudLayout title="部门管理" :message="message" @refresh="load">
      <form class="crud-form" @submit.prevent="save">
        <input v-model.trim="form.deptName" placeholder="部门名称" />
        <input v-model.trim="form.deptCode" placeholder="部门代码" />
        <select v-model.number="form.parentId">
          <option :value="0">顶级部门</option>
          <option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option>
        </select>
        <input v-model.trim="form.description" placeholder="部门描述" />
        <select v-model.number="form.status">
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
        <button type="submit">{{ form.deptId ? '更新部门' : '新增部门' }}</button>
        <button class="secondary" type="button" @click="reset">清空</button>
      </form>

      <table class="data-table">
        <thead><tr><th>名称</th><th>代码</th><th>上级</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in departments" :key="item.deptId">
            <td>{{ item.deptName }}</td>
            <td>{{ item.deptCode }}</td>
            <td>{{ item.parentDeptName || '顶级' }}</td>
            <td>{{ item.status === 1 ? '启用' : '禁用' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
              <button class="small danger" @click="remove(item.deptId)">删除</button>
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
import { createDepartment, deleteDepartment, listDepartments, updateDepartment } from '../api/departments';

const departments = ref([]);
const message = ref('');
const form = reactive(defaultForm());

onMounted(load);

function defaultForm() {
  return { deptId: null, parentId: 0, deptName: '', deptCode: '', leaderId: null, description: '', status: 1 };
}

async function load() {
  const result = await listDepartments();
  departments.value = result.data || [];
}

function reset() {
  Object.assign(form, defaultForm());
}

function edit(item) {
  Object.assign(form, { ...defaultForm(), ...item });
}

async function save() {
  const result = form.deptId ? await updateDepartment(form) : await createDepartment(form);
  message.value = result.msg;
  reset();
  await load();
}

async function remove(id) {
  const result = await deleteDepartment(id);
  message.value = result.msg;
  await load();
}
</script>
