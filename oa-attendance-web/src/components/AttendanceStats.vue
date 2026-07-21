<script setup>
import { onMounted, ref } from 'vue'
import { attendanceApi, departmentApi } from '../api/oa'

const props = defineProps({ roleCode: { type: String, default: '' }, currentDeptId: { type: Number, default: null } })
const emit = defineEmits(['back'])
const departments = ref([])
const stats = ref({})
const loading = ref(false)
const error = ref('')
const filters = ref(defaultFilters())

onMounted(async () => {
  if (props.roleCode === 'MANAGER') filters.value.deptId = props.currentDeptId
  await Promise.all([loadDepartments(), load()])
})

function defaultFilters() {
  const today = new Date().toISOString().slice(0, 10)
  return { startDate: `${today.slice(0, 7)}-01`, endDate: today, deptId: '' }
}

async function loadDepartments() {
  try { const result = await departmentApi.list(); departments.value = result.data || [] } catch (err) { error.value = err.message }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const form = filters.value
    const result = await attendanceApi.stats({ startDate: form.startDate, endDate: form.endDate, ...(form.deptId !== '' && form.deptId != null ? { deptId: form.deptId } : {}) })
    stats.value = result.data || {}
  } catch (err) { error.value = err.message } finally { loading.value = false }
}

function rateNumber(value) { const number = Number.parseFloat(String(value || '0').replace('%', '')); return Number.isFinite(number) ? Math.min(100, Math.max(0, number)) : 0 }
</script>

<template>
  <section class="stats-page">
    <div class="management-heading"><div><button class="back-button" @click="emit('back')">← 返回工作台</button><h1>考勤数据报表</h1><p>按日期和部门统计出勤、迟到、缺勤与请假情况</p></div><button class="stats-refresh" :disabled="loading" @click="load">↻ {{ loading ? '统计中' : '刷新' }}</button></div>
    <form class="stats-filter" @submit.prevent="load"><label><span>开始日期</span><input v-model="filters.startDate" type="date" required /></label><label><span>结束日期</span><input v-model="filters.endDate" type="date" :min="filters.startDate" required /></label><label><span>统计部门</span><select v-model="filters.deptId" :disabled="roleCode === 'MANAGER'"><option value="">全部部门</option><option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option></select></label><button :disabled="loading">查询报表</button></form>
    <div v-if="error" class="api-error">{{ error }}</div>
    <div class="stats-context"><span>{{ stats.deptName || '全部部门' }}</span><strong>{{ stats.startDate || filters.startDate }} 至 {{ stats.endDate || filters.endDate }}</strong></div>
    <div class="stats-cards">
      <article class="primary-stat"><header><span>综合出勤率</span><b>目标 100%</b></header><strong>{{ stats.attendanceRate || '0.00%' }}</strong><div class="rate-track"><i :style="{ width: `${rateNumber(stats.attendanceRate)}%` }"></i></div><p>正常出勤 {{ stats.normalCount || 0 }} / 应出勤 {{ stats.expectedCount || 0 }}</p></article>
      <article><span>迟到率</span><strong>{{ stats.lateRate || '0.00%' }}</strong><p>{{ stats.lateCount || 0 }} 次迟到</p><div class="mini-rate orange"><i :style="{ width: `${rateNumber(stats.lateRate)}%` }"></i></div></article>
      <article><span>缺勤率</span><strong>{{ stats.absentRate || '0.00%' }}</strong><p>{{ stats.absentCount || 0 }} 次缺勤</p><div class="mini-rate red"><i :style="{ width: `${rateNumber(stats.absentRate)}%` }"></i></div></article>
      <article><span>请假率</span><strong>{{ stats.leaveRate || '0.00%' }}</strong><p>{{ stats.leaveCount || 0 }} 次请假</p><div class="mini-rate purple"><i :style="{ width: `${rateNumber(stats.leaveRate)}%` }"></i></div></article>
    </div>
    <section class="stats-breakdown"><div class="stats-section-title"><div><p class="eyebrow">ATTENDANCE OVERVIEW</p><h2>考勤构成</h2></div></div><div class="breakdown-grid"><article><i class="normal">正</i><div><span>正常出勤</span><strong>{{ stats.normalCount || 0 }}</strong></div></article><article><i class="late">迟</i><div><span>迟到</span><strong>{{ stats.lateCount || 0 }}</strong></div></article><article><i class="absent">缺</i><div><span>缺勤</span><strong>{{ stats.absentCount || 0 }}</strong></div></article><article><i class="leave">假</i><div><span>请假</span><strong>{{ stats.leaveCount || 0 }}</strong></div></article><article><i class="overtime">加</i><div><span>加班</span><strong>{{ stats.overtimeCount || 0 }}</strong></div></article></div></section>
    <div v-if="!loading && !(stats.expectedCount || stats.normalCount || stats.lateCount || stats.absentCount || stats.leaveCount)" class="stats-empty"><span>表</span><strong>当前统计范围暂无考勤数据</strong><p>员工完成打卡后，统计结果会自动汇总到这里。</p></div>
  </section>
</template>

<style scoped>
.stats-page { width: min(1500px, 100%); margin: 0 auto; padding: 36px 42px 60px; }
.stats-refresh { height: 39px; padding: 0 16px; border: 1px solid #dfe3e9; border-radius: 9px; color: #3370ff; background: #fff; cursor: pointer; }
.stats-filter { display: grid; grid-template-columns: 1fr 1fr 1.2fr auto; align-items: end; gap: 12px; margin-bottom: 14px; padding: 18px; border: 1px solid #eaecf0; border-radius: 14px; background: #fff; }
.stats-filter label { display: flex; flex-direction: column; gap: 7px; }.stats-filter label span { color: #8f959e; font-size: 10px; }.stats-filter input, .stats-filter select, .stats-filter button { width: 100%; height: 39px; border: 1px solid #dfe3e9; border-radius: 9px; background: #fff; }.stats-filter input, .stats-filter select { padding: 0 11px; outline: 0; }.stats-filter button { padding: 0 19px; border-color: #3370ff; color: #fff; background: #3370ff; cursor: pointer; }
.stats-context { display: flex; align-items: center; gap: 9px; margin: 18px 0 12px; }.stats-context span { padding: 5px 10px; border-radius: 12px; color: #3370ff; background: #edf3ff; font-size: 10px; }.stats-context strong { color: #8f959e; font-size: 11px; font-weight: 400; }
.stats-cards { display: grid; grid-template-columns: 1.5fr repeat(3, 1fr); gap: 13px; }.stats-cards article { min-height: 180px; padding: 22px; border: 1px solid #eaecf0; border-radius: 16px; background: #fff; box-shadow: 0 7px 24px rgba(31,35,41,.04); }.stats-cards article > span { color: #8f959e; font-size: 11px; }.stats-cards article > strong { display: block; margin: 18px 0 7px; font-size: 29px; }.stats-cards article > p { margin: 0; color: #8f959e; font-size: 10px; }
.primary-stat { color: #fff; border: 0 !important; background: linear-gradient(145deg, #3370ff, #6d5dfc) !important; }.primary-stat header { display: flex; justify-content: space-between; }.primary-stat header span { font-size: 11px; opacity: .82; }.primary-stat header b { font-size: 9px; opacity: .7; }.primary-stat > strong { margin: 20px 0 13px !important; font-size: 36px !important; }.primary-stat p { color: rgba(255,255,255,.74) !important; }.rate-track, .mini-rate { height: 5px; overflow: hidden; margin: 0 0 11px; border-radius: 3px; background: rgba(255,255,255,.2); }.rate-track i, .mini-rate i { display: block; height: 100%; border-radius: inherit; background: #fff; }.mini-rate { margin-top: 25px; background: #f0f1f3; }.mini-rate.orange i { background: #ff9f43; }.mini-rate.red i { background: #f54a6e; }.mini-rate.purple i { background: #7b61ff; }
.stats-breakdown { margin-top: 28px; }.stats-section-title h2 { margin: 6px 0 14px; font-size: 22px; }.breakdown-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }.breakdown-grid article { display: flex; align-items: center; gap: 13px; padding: 18px; border: 1px solid #eaecf0; border-radius: 14px; background: #fff; }.breakdown-grid i { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; font-size: 12px; font-style: normal; }.breakdown-grid i.normal { color: #16865a; background: #e8f8f1; }.breakdown-grid i.late { color: #b86400; background: #fff4df; }.breakdown-grid i.absent { color: #d83931; background: #fff1f0; }.breakdown-grid i.leave { color: #7b61ff; background: #f1edff; }.breakdown-grid i.overtime { color: #3370ff; background: #edf3ff; }.breakdown-grid span, .breakdown-grid strong { display: block; }.breakdown-grid span { color: #8f959e; font-size: 9px; }.breakdown-grid strong { margin-top: 5px; font-size: 19px; }
.stats-empty { min-height: 230px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #8f959e; text-align: center; }.stats-empty > span { width: 56px; height: 56px; display: grid; place-items: center; border-radius: 18px; color: #3370ff; background: #edf3ff; }.stats-empty strong { margin-top: 13px; color: #50555d; }.stats-empty p { margin: 7px 0 0; font-size: 11px; }
@media (max-width: 1120px) { .stats-cards { grid-template-columns: repeat(2, 1fr); }.breakdown-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 760px) { .stats-page { padding: 25px 17px 45px; }.stats-filter { grid-template-columns: 1fr; }.stats-cards, .breakdown-grid { grid-template-columns: 1fr; } }
</style>
