<template>
  <AppShell>
    <CrudLayout title="角色管理" :message="message" @refresh="load">
      <form class="crud-form" @submit.prevent="save">
        <input v-model.trim="form.roleName" placeholder="角色名称" />
        <input v-model.trim="form.roleCode" placeholder="角色编码" />
        <input v-model.trim="form.description" placeholder="角色描述" />
        <button type="submit">{{ form.roleId ? '更新角色' : '新增角色' }}</button>
        <button class="secondary" type="button" @click="reset">清空</button>
      </form>

      <table class="data-table">
        <thead><tr><th>角色</th><th>编码</th><th>描述</th><th>菜单ID</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in roles" :key="item.roleId">
            <td>{{ item.roleName }}</td>
            <td>{{ item.roleCode }}</td>
            <td>{{ item.description || '-' }}</td>
            <td>{{ (item.menuIds || []).join(',') || '-' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
              <button class="small secondary" @click="selectRole(item)">菜单授权</button>
              <button class="small danger" @click="remove(item.roleId)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <section v-if="selectedRole" class="sub-panel">
        <h3>{{ selectedRole.roleName }} 菜单授权</h3>
        <div class="checkbox-list">
          <label v-for="menu in menus" :key="menu.menuId">
            <input v-model="selectedMenuIds" type="checkbox" :value="menu.menuId" />
            {{ menu.menuName }} - {{ menu.menuPath }}
          </label>
        </div>
        <button @click="saveMenus">保存菜单授权</button>
      </section>
    </CrudLayout>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import CrudLayout from '../components/CrudLayout.vue';
import { listMenus } from '../api/menus';
import { assignRoleMenus, createRole, deleteRole, listRoles, updateRole } from '../api/roles';

const roles = ref([]);
const menus = ref([]);
const message = ref('');
const selectedRole = ref(null);
const selectedMenuIds = ref([]);
const form = reactive(defaultForm());

onMounted(load);

function defaultForm() {
  return { roleId: null, roleName: '', roleCode: '', description: '' };
}

async function load() {
  const [roleRes, menuRes] = await Promise.all([listRoles(), listMenus()]);
  roles.value = roleRes.data || [];
  menus.value = menuRes.data || [];
}

function reset() {
  Object.assign(form, defaultForm());
}

function edit(item) {
  Object.assign(form, { ...defaultForm(), ...item });
}

function selectRole(item) {
  selectedRole.value = item;
  selectedMenuIds.value = [...(item.menuIds || [])];
}

async function save() {
  const result = form.roleId ? await updateRole(form) : await createRole(form);
  message.value = result.msg;
  reset();
  await load();
}

async function saveMenus() {
  const result = await assignRoleMenus({ roleId: selectedRole.value.roleId, menuIds: selectedMenuIds.value });
  message.value = result.msg;
  selectedRole.value = null;
  await load();
}

async function remove(id) {
  const result = await deleteRole(id);
  message.value = result.msg;
  await load();
}
</script>
