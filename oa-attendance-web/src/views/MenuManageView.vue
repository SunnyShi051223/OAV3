<template>
  <AppShell>
    <CrudLayout title="菜单管理" :message="message" @refresh="load">
      <form class="crud-form" @submit.prevent="save">
        <input v-model.trim="form.menuName" placeholder="菜单名称" />
        <input v-model.trim="form.menuPath" placeholder="路由路径" />
        <input v-model.trim="form.component" placeholder="组件名称" />
        <input v-model.trim="form.icon" placeholder="图标标识" />
        <select v-model.number="form.parentId">
          <option :value="0">顶级菜单</option>
          <option v-for="menu in menus" :key="menu.menuId" :value="menu.menuId">{{ menu.menuName }}</option>
        </select>
        <input v-model.number="form.sortOrder" type="number" placeholder="排序" />
        <select v-model.number="form.visible">
          <option :value="1">显示</option>
          <option :value="0">隐藏</option>
        </select>
        <button type="submit">{{ form.menuId ? '更新菜单' : '新增菜单' }}</button>
        <button class="secondary" type="button" @click="reset">清空</button>
      </form>

      <table class="data-table">
        <thead><tr><th>名称</th><th>路径</th><th>父级ID</th><th>排序</th><th>显示</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in menus" :key="item.menuId">
            <td>{{ item.menuName }}</td>
            <td>{{ item.menuPath || '-' }}</td>
            <td>{{ item.parentId }}</td>
            <td>{{ item.sortOrder }}</td>
            <td>{{ item.visible === 1 ? '显示' : '隐藏' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
              <button class="small danger" @click="remove(item.menuId)">删除</button>
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
import { createMenu, deleteMenu, listMenus, updateMenu } from '../api/menus';

const menus = ref([]);
const message = ref('');
const form = reactive(defaultForm());

onMounted(load);

function defaultForm() {
  return { menuId: null, parentId: 0, menuName: '', menuPath: '', component: '', icon: '', sortOrder: 1, visible: 1 };
}

async function load() {
  const result = await listMenus();
  menus.value = result.data || [];
}

function reset() {
  Object.assign(form, defaultForm());
}

function edit(item) {
  Object.assign(form, { ...defaultForm(), ...item });
}

async function save() {
  const result = form.menuId ? await updateMenu(form) : await createMenu(form);
  message.value = result.msg;
  reset();
  await load();
}

async function remove(id) {
  const result = await deleteMenu(id);
  message.value = result.msg;
  await load();
}
</script>
