<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { menuApi, roleApi } from '../api/oa'
import ListPagination from './ListPagination.vue'

const props = defineProps({ permissions: { type: Array, default: () => [] } })
const emit = defineEmits(['back'])
const activeTab = ref('roles')
const roles = ref([])
const menus = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const currentPage = ref(1)
const pageSize = 3
const editDialog = ref({ visible: false, type: 'role', mode: 'create', form: {}, error: '', saving: false })
const menuAssignDialog = ref({ visible: false, role: null, menuIds: [], error: '', saving: false })
const permissionSet = computed(() => new Set(props.permissions))
const rootMenus = computed(() => menus.value.filter((item) => Number(item.parentId || 0) === 0))
const currentItems = computed(() => activeTab.value === 'roles' ? roles.value : menus.value)
const paginatedItems = computed(() => currentItems.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))

watch(activeTab, () => { currentPage.value = 1 })
watch(() => currentItems.value.length, (total) => {
  currentPage.value = Math.min(currentPage.value, Math.max(1, Math.ceil(total / pageSize)))
})

onMounted(load)
function can(code) { return permissionSet.value.has(code) }

async function load() {
  loading.value = true
  error.value = ''
  try { const [roleResult, menuResult] = await Promise.all([roleApi.list(), menuApi.list()]); roles.value = roleResult.data || []; menus.value = menuResult.data || [] } catch (err) { error.value = err.message } finally { loading.value = false }
}

function roleForm(item = {}) { return { roleId: item.roleId || null, roleName: item.roleName || '', roleCode: item.roleCode || '', description: item.description || '' } }
function menuForm(item = {}) { return { menuId: item.menuId || null, parentId: item.parentId ?? 0, menuName: item.menuName || '', menuPath: item.menuPath || '', component: item.component || '', icon: item.icon || '', sortOrder: item.sortOrder ?? 1, visible: item.visible ?? 1 } }
function openCreate(type) { editDialog.value = { visible: true, type, mode: 'create', form: type === 'role' ? roleForm() : menuForm(), error: '', saving: false } }
function openEdit(type, item) { editDialog.value = { visible: true, type, mode: 'edit', form: type === 'role' ? roleForm(item) : menuForm(item), error: '', saving: false } }

async function save() {
  const state = editDialog.value
  state.error = ''
  state.saving = true
  try {
    if (state.type === 'role') {
      const form = state.form
      const payload = { ...(state.mode === 'edit' ? { roleId: form.roleId } : {}), roleName: form.roleName.trim(), roleCode: form.roleCode.trim().toUpperCase(), description: form.description.trim() || null }
      await roleApi[state.mode === 'create' ? 'create' : 'update'](payload)
    } else {
      const form = state.form
      const payload = { ...(state.mode === 'edit' ? { menuId: form.menuId } : {}), parentId: Number(form.parentId || 0), menuName: form.menuName.trim(), menuPath: form.menuPath.trim() || null, component: form.component.trim() || null, icon: form.icon.trim() || null, sortOrder: Number(form.sortOrder), visible: Number(form.visible) }
      await menuApi[state.mode === 'create' ? 'create' : 'update'](payload)
    }
    state.visible = false
    showNotice(`${state.type === 'role' ? '角色' : '菜单'}${state.mode === 'create' ? '创建' : '修改'}成功`)
    await load()
  } catch (err) { state.error = err.message } finally { state.saving = false }
}

async function remove(type, item) {
  const label = type === 'role' ? item.roleName : item.menuName
  if (!window.confirm(`确定删除“${label}”吗？`)) return
  try { await (type === 'role' ? roleApi.remove(item.roleId) : menuApi.remove(item.menuId)); showNotice(`${type === 'role' ? '角色' : '菜单'}已删除`); await load() } catch (err) { error.value = err.message }
}

function openMenuAssign(role) { menuAssignDialog.value = { visible: true, role, menuIds: [...(role.menuIds || [])], error: '', saving: false } }
async function saveRoleMenus() {
  const state = menuAssignDialog.value
  state.saving = true
  state.error = ''
  try { await roleApi.assignMenus(state.role.roleId, state.menuIds.map(Number)); state.visible = false; showNotice('角色菜单保存成功'); await load() } catch (err) { state.error = err.message } finally { state.saving = false }
}

function childMenus(parentId) { return menus.value.filter((item) => Number(item.parentId) === Number(parentId)) }
function parentName(parentId) { return menus.value.find((item) => Number(item.menuId) === Number(parentId))?.menuName || '顶级菜单' }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
function showNotice(message) { notice.value = message; window.setTimeout(() => { if (notice.value === message) notice.value = '' }, 2600) }
</script>

<template>
  <section class="access-page">
    <div class="management-heading"><div><button class="back-button" @click="emit('back')">← 返回工作台</button><h1>权限与菜单</h1><p>维护系统角色以及角色可访问的功能菜单</p></div><button class="access-refresh" :disabled="loading" @click="load">↻ {{ loading ? '刷新中' : '刷新' }}</button></div>
    <div class="access-tabs"><button :class="{ active: activeTab === 'roles' }" @click="activeTab = 'roles'">角色管理 <span>{{ roles.length }}</span></button><button :class="{ active: activeTab === 'menus' }" @click="activeTab = 'menus'">菜单管理 <span>{{ menus.length }}</span></button></div>
    <div v-if="notice" class="attendance-notice">✓ {{ notice }}</div><div v-if="error" class="api-error">{{ error }}</div>

    <template v-if="activeTab === 'roles'"><div class="access-section-heading"><div><h2>系统角色</h2><p>角色决定用户的数据范围和可访问菜单</p></div><button v-if="can('role:add')" class="primary-button" @click="openCreate('role')">＋ 新建角色</button></div><div class="role-card-grid"><article v-for="role in paginatedItems" :key="role.roleId" class="role-card"><header><span>{{ role.roleName.slice(0, 1) }}</span><div><h3>{{ role.roleName }}</h3><code>{{ role.roleCode }}</code></div></header><p>{{ role.description || '暂无角色说明' }}</p><dl><div><dt>已授权菜单</dt><dd>{{ (role.menuIds || []).length }}</dd></div><div><dt>功能权限</dt><dd>{{ (role.permissionIds || []).length }}</dd></div></dl><footer><button v-if="can('role:menu')" @click="openMenuAssign(role)">菜单授权</button><button v-if="can('role:update')" @click="openEdit('role', role)">编辑</button><button v-if="can('role:delete')" class="danger" @click="remove('role', role)">删除</button></footer></article></div></template>

    <template v-else><div class="access-section-heading"><div><h2>系统菜单</h2><p>配置后端菜单树中的名称、路径和显示顺序</p></div><button v-if="can('menu:add')" class="primary-button" @click="openCreate('menu')">＋ 新建菜单</button></div><div class="data-panel menu-table"><table><thead><tr><th>菜单名称</th><th>上级菜单</th><th>访问路径</th><th>组件</th><th>排序</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="menu in paginatedItems" :key="menu.menuId"><td class="name-cell"><span class="menu-level" :class="{ child: menu.parentId }">{{ menu.parentId ? '↳' : '▦' }}</span>{{ menu.menuName }}</td><td>{{ parentName(menu.parentId) }}</td><td><code>{{ menu.menuPath || '—' }}</code></td><td>{{ menu.component || '—' }}</td><td>{{ menu.sortOrder }}</td><td><span :class="['status-tag', menu.visible ? 'on' : 'off']">{{ menu.visible ? '显示' : '隐藏' }}</span></td><td>{{ formatTime(menu.updateTime) }}</td><td><button v-if="can('menu:update')" class="text-action" @click="openEdit('menu', menu)">编辑</button><button v-if="can('menu:delete')" class="text-action danger" @click="remove('menu', menu)">删除</button></td></tr></tbody></table></div></template>
    <ListPagination v-model:page="currentPage" :total="currentItems.length" :page-size="pageSize" />

    <div v-if="editDialog.visible" class="dialog-mask" @click.self="editDialog.visible = false"><form class="data-dialog access-dialog" @submit.prevent="save"><div class="dialog-heading"><div><h2>{{ editDialog.mode === 'create' ? '新建' : '编辑' }}{{ editDialog.type === 'role' ? '角色' : '菜单' }}</h2><p>{{ editDialog.type === 'role' ? '设置角色基础信息' : '设置菜单层级与访问路径' }}</p></div><button type="button" @click="editDialog.visible = false">×</button></div><div v-if="editDialog.type === 'role'" class="form-grid"><label>角色名称<input v-model.trim="editDialog.form.roleName" maxlength="50" required /></label><label>角色编码<input v-model.trim="editDialog.form.roleCode" maxlength="50" placeholder="例如 REVIEWER" required /></label><label class="full-field">角色说明<textarea v-model.trim="editDialog.form.description" maxlength="255"></textarea></label></div><div v-else class="form-grid"><label>菜单名称<input v-model.trim="editDialog.form.menuName" required /></label><label>上级菜单<select v-model.number="editDialog.form.parentId"><option :value="0">顶级菜单</option><option v-for="menu in menus.filter((item) => item.menuId !== editDialog.form.menuId)" :key="menu.menuId" :value="menu.menuId">{{ menu.menuName }}</option></select></label><label>访问路径<input v-model.trim="editDialog.form.menuPath" placeholder="/attendance" /></label><label>组件标识<input v-model.trim="editDialog.form.component" placeholder="AttendanceView" /></label><label>图标标识<input v-model.trim="editDialog.form.icon" /></label><label>排序<input v-model.number="editDialog.form.sortOrder" type="number" min="0" required /></label><label>显示状态<select v-model.number="editDialog.form.visible"><option :value="1">显示</option><option :value="0">隐藏</option></select></label></div><div v-if="editDialog.error" class="form-error">{{ editDialog.error }}</div><div class="dialog-actions"><button type="button" @click="editDialog.visible = false">取消</button><button class="primary-button" :disabled="editDialog.saving">{{ editDialog.saving ? '保存中…' : '保存' }}</button></div></form></div>

    <div v-if="menuAssignDialog.visible" class="dialog-mask" @click.self="menuAssignDialog.visible = false"><form class="data-dialog menu-assign-dialog" @submit.prevent="saveRoleMenus"><div class="dialog-heading"><div><h2>{{ menuAssignDialog.role?.roleName }} · 菜单授权</h2><p>勾选该角色登录后可以访问的菜单</p></div><button type="button" @click="menuAssignDialog.visible = false">×</button></div><div class="menu-checkbox-tree"><section v-for="root in rootMenus" :key="root.menuId"><label><input v-model="menuAssignDialog.menuIds" type="checkbox" :value="root.menuId" /><strong>{{ root.menuName }}</strong><code>{{ root.menuPath || '' }}</code></label><label v-for="child in childMenus(root.menuId)" :key="child.menuId" class="child-menu"><input v-model="menuAssignDialog.menuIds" type="checkbox" :value="child.menuId" /><span>{{ child.menuName }}</span><code>{{ child.menuPath || '' }}</code></label></section></div><div v-if="menuAssignDialog.error" class="form-error">{{ menuAssignDialog.error }}</div><div class="dialog-actions"><button type="button" @click="menuAssignDialog.visible = false">取消</button><button class="primary-button" :disabled="menuAssignDialog.saving">{{ menuAssignDialog.saving ? '保存中…' : '保存授权' }}</button></div></form></div>
  </section>
</template>

<style scoped>
.access-page { width: min(1500px, 100%); margin: 0 auto; padding: 36px 42px 60px; }.access-refresh { height: 39px; padding: 0 16px; border: 1px solid #dfe3e9; border-radius: 9px; color: #3370ff; background: #fff; cursor: pointer; }
.access-tabs { display: flex; gap: 5px; margin-bottom: 22px; padding: 5px; border-radius: 11px; background: #eef0f3; }.access-tabs button { height: 38px; padding: 0 17px; border: 0; border-radius: 8px; color: #646a73; background: transparent; cursor: pointer; }.access-tabs button span { margin-left: 6px; color: #8f959e; font-size: 9px; }.access-tabs button.active { color: #3370ff; background: #fff; box-shadow: 0 3px 12px rgba(31,35,41,.08); }.access-tabs button.active span { color: #3370ff; }
.access-section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 13px; }.access-section-heading h2 { margin: 0 0 5px; font-size: 20px; }.access-section-heading p { margin: 0; color: #8f959e; font-size: 10px; }.access-section-heading .primary-button { margin: 0; }
.access-page :deep(.table-pagination) { min-width: 0; margin-top: 14px; border: 1px solid #e8ebf0; border-radius: 12px; background: #fff; }
.role-card-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }.role-card { padding: 20px; border: 1px solid #e8ebf0; border-radius: 15px; background: #fff; box-shadow: 0 7px 24px rgba(31,35,41,.04); }.role-card header { display: flex; align-items: center; gap: 11px; }.role-card header > span { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 12px; color: #3370ff; background: #edf3ff; font-weight: 700; }.role-card h3 { margin: 0 0 5px; font-size: 16px; }.role-card code, .menu-table code, .menu-checkbox-tree code { color: #7b61ff; font-size: 9px; }.role-card > p { min-height: 34px; margin: 18px 0; color: #8f959e; font-size: 11px; line-height: 1.6; }.role-card dl { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 0; }.role-card dl div { padding: 11px; border-radius: 9px; background: #f7f8fa; }.role-card dt { color: #a2a7ae; font-size: 9px; }.role-card dd { margin: 4px 0 0; font-size: 17px; font-weight: 700; }.role-card footer { display: flex; justify-content: flex-end; gap: 2px; margin-top: 16px; padding-top: 12px; border-top: 1px solid #f0f1f3; }.role-card footer button { padding: 5px 7px; border: 0; color: #3370ff; background: transparent; cursor: pointer; }.role-card footer .danger { color: #d83931; }
.menu-table table { min-width: 1050px; }.menu-level { display: inline-block; width: 25px; color: #3370ff; }.menu-level.child { padding-left: 7px; color: #8f959e; }.access-dialog { width: min(700px, 100%); }.menu-assign-dialog { width: min(650px, 100%); }.menu-checkbox-tree { max-height: 460px; overflow-y: auto; display: grid; gap: 9px; }.menu-checkbox-tree section { padding: 12px; border: 1px solid #e8ebf0; border-radius: 10px; }.menu-checkbox-tree label { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 9px; font-size: 12px; }.menu-checkbox-tree input { width: 15px; height: 15px; }.menu-checkbox-tree .child-menu { margin: 10px 0 0 24px; color: #646a73; }
@media (max-width: 1000px) { .role-card-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .access-page { padding: 25px 17px 45px; }.role-card-grid { grid-template-columns: 1fr; }.access-section-heading { align-items: flex-start; flex-direction: column; }.access-section-heading .primary-button { width: 100%; } }
</style>
