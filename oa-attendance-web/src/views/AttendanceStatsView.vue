<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">Attendance</p>
        <h2>考勤统计</h2>
      </div>
      <button class="secondary" @click="load">刷新</button>
    </section>
    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <form class="attendance-form compact" @submit.prevent="load">
        <input v-model="filters.startDate" type="date" />
        <input v-model="filters.endDate" type="date" />
        <select v-model="filters.deptId">
          <option :value="''">全部部门</option>
          <option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">
            {{ dept.deptName }}
          </option>
        </select>
        <button type="submit">查询</button>
      </form>
    </section>

    <section class="grid">
      <article class="card">
        <span>出勤率</span>
        <strong>{{ stats.attendanceRate || '0.00%' }}</strong>
        <span>{{ stats.normalCount || 0 }} / {{ stats.expectedCount || 0 }}</span>
      </article>
      <article class="card">
        <span>迟到率</span>
        <strong>{{ stats.lateRate || '0.00%' }}</strong>
        <span>{{ stats.lateCount || 0 }} 次</span>
      </article>
      <article class="card">
        <span>缺勤率</span>
        <strong>{{ stats.absentRate || '0.00%' }}</strong>
        <span>{{ stats.absentCount || 0 }} 次</span>
      </article>
      <article class="card">
        <span>请假率</span>
        <strong>{{ stats.leaveRate || '0.00%' }}</strong>
        <span>{{ stats.leaveCount || 0 }} 次</span>
      </article>
      <article class="card">
        <span>加班次数</span>
        <strong>{{ stats.overtimeCount || 0 }}</strong>
        <span>{{ stats.deptName || '全部部门' }}</span>
      </article>
      <article class="card">
        <span>统计周期</span>
        <strong>{{ stats.startDate || filters.startDate }}</strong>
        <span>至 {{ stats.endDate || filters.endDate }}</span>
      </article>
    </section>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import { listDepartments } from '../api/departments';
import { getAttendanceStats } from '../api/attendance';

const departments = ref([]);
const stats = reactive({});
const error = ref('');
const filters = reactive(defaultFilters());

onMounted(async () => {
  await Promise.all([loadDepartments(), load()]);
});

function defaultFilters() {
  const now = new Date();
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  return {
    startDate: `${month}-01`,
    endDate: new Date().toISOString().slice(0, 10),
    deptId: ''
  };
}

async function loadDepartments() {
  const result = await listDepartments();
  departments.value = result.data || [];
}

async function load() {
  error.value = '';
  try {
    const result = await getAttendanceStats({
      startDate: filters.startDate,
      endDate: filters.endDate,
      deptId: filters.deptId || undefined
    });
    Object.assign(stats, result.data || {});
  } catch (err) {
    error.value = err.message;
  }
}
</script>
