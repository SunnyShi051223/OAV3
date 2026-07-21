<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">考勤配置</p>
        <h2>考勤规则</h2>
      </div>
      <button class="secondary" @click="load">刷新</button>
    </section>
    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <form class="attendance-form" @submit.prevent="save">
        <label>
          规则名称
          <input v-model.trim="form.ruleName" placeholder="如：研发部标准班次" />
        </label>
        <label>
          适用部门
          <select v-model="form.deptId">
            <option :value="null">全公司</option>
            <option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">
              {{ dept.deptName }}
            </option>
          </select>
        </label>
        <label>
          生效开始日期
          <input v-model="form.effectiveStartDate" type="date" />
        </label>
        <label>
          生效结束日期
          <input v-model="form.effectiveEndDate" type="date" />
        </label>
        <label>
          上班时间
          <input v-model="form.workStartTime" type="time" />
        </label>
        <label>
          下班时间
          <input v-model="form.workEndTime" type="time" />
        </label>
        <label>
          正常签到窗口开始
          <input :value="form.workStartTime" type="time" readonly />
          <small>固定与上班时间一致；此时间至正常签到截止均记为正常签到。</small>
        </label>
        <label>
          正常签到窗口截止
          <input v-model="form.checkInEndTime" type="time" />
          <small>超过此时间仍可签到，直到下班时间为止，但签到状态记为迟到。</small>
        </label>
        <label>
          签退开始
          <input v-model="form.checkOutStartTime" type="time" />
        </label>
        <label>
          签退截止
          <input v-model="form.checkOutEndTime" type="time" />
        </label>
        <label>
          迟到宽限分钟（备用）
          <input v-model.number="form.lateThreshold" type="number" min="0" placeholder="默认 10" />
          <small>仅在未配置正常签到窗口截止时生效，按上班时间加该分钟数判断迟到。</small>
        </label>
        <label>
          早退宽限分钟
          <input v-model.number="form.earlyThreshold" type="number" min="0" placeholder="默认 10" />
          <small>下班时间前允许的缓冲时间。例：18:00 下班，填 10 表示 17:50 前算早退。</small>
        </label>
        <label>
          加班判定分钟
          <input v-model.number="form.overtimeThreshold" type="number" min="0" placeholder="默认 30" />
          <small>超过下班时间多少分钟后算加班。例：18:00 下班，填 30 表示 18:30 后算加班。</small>
        </label>
        <label>
          工作日配置
          <input v-model.trim="form.workDays" placeholder="默认 [1,2,3,4,5]" />
          <small>用数字表示星期：1 周一，2 周二，...，7 周日。默认 [1,2,3,4,5] 表示周一到周五。</small>
        </label>
        <label class="check-row">
          <input v-model="form.requireWifi" type="checkbox" :true-value="1" :false-value="0" />
          需要 WiFi 校验
        </label>
        <label class="check-row">
          <input v-model="form.requireLocation" type="checkbox" :true-value="1" :false-value="0" />
          需要定位校验
        </label>
        <label class="check-row">
          <input v-model="form.enabled" type="checkbox" :true-value="1" :false-value="0" />
          启用规则
        </label>

        <div class="sub-panel form-span">
          <div class="panel-title">
            <h3>允许 WiFi</h3>
            <button class="small secondary" type="button" @click="addWifi">新增 WiFi</button>
          </div>
          <div v-for="(wifi, index) in form.wifiList" :key="index" class="inline-grid">
            <input v-model.trim="wifi.wifiSsid" placeholder="WiFi SSID" />
            <input v-model.trim="wifi.wifiBssid" placeholder="BSSID/MAC，可为空" />
            <button class="small danger" type="button" @click="removeWifi(index)">删除</button>
          </div>
        </div>

        <div class="sub-panel form-span">
          <div class="panel-title">
            <h3>办公地点</h3>
            <button class="small secondary" type="button" @click="addLocation">新增地点</button>
          </div>
          <div v-for="(location, index) in form.locationList" :key="index" class="location-grid">
            <input v-model.trim="location.locationName" placeholder="地点名称" />
            <input v-model.number="location.latitude" type="number" step="0.000001" placeholder="纬度" />
            <input v-model.number="location.longitude" type="number" step="0.000001" placeholder="经度" />
            <input v-model.number="location.radius" type="number" min="1" placeholder="半径（米）" />
            <button class="small danger" type="button" @click="removeLocation(index)">删除</button>
          </div>
        </div>

        <div class="form-actions form-span">
          <button type="submit">{{ form.ruleId ? '更新规则' : '新增规则' }}</button>
          <button class="secondary" type="button" @click="reset">清空</button>
        </div>
      </form>
    </section>

    <section class="panel table-panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>规则</th>
            <th>部门</th>
            <th>班次</th>
            <th>判定设置</th>
            <th>校验</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in rules" :key="item.ruleId">
            <td>{{ item.ruleName }}</td>
            <td>{{ item.deptName || '全公司' }}</td>
            <td>{{ formatTime(item.workStartTime) }} - {{ formatTime(item.workEndTime) }}</td>
            <td>迟到 {{ item.lateThreshold || 0 }} 分 / 早退 {{ item.earlyThreshold || 0 }} 分 / 加班 {{ item.overtimeThreshold || 0 }} 分</td>
            <td>
              <span>{{ item.requireWifi === 1 ? 'WiFi' : '无 WiFi' }}</span>
              <span> / </span>
              <span>{{ item.requireLocation === 1 ? '定位' : '无定位' }}</span>
            </td>
            <td>{{ item.enabled === 1 ? '启用' : '停用' }}</td>
            <td>
              <button class="small secondary" @click="edit(item)">编辑</button>
              <button class="small danger" @click="remove(item.ruleId)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import { listDepartments } from '../api/departments';
import {
  createAttendanceRule,
  deleteAttendanceRule,
  listAttendanceRules,
  updateAttendanceRule
} from '../api/attendance';

const rules = ref([]);
const departments = ref([]);
const message = ref('');
const error = ref('');
const form = reactive(defaultForm());

onMounted(async () => {
  await Promise.all([load(), loadDepartments()]);
});

function defaultForm() {
  return {
    ruleId: null,
    ruleName: '',
    deptId: null,
    effectiveStartDate: '',
    effectiveEndDate: '',
    workStartTime: '09:00',
    workEndTime: '18:00',
    checkInStartTime: '09:00',
    checkInEndTime: '09:10',
    checkOutStartTime: '17:30',
    checkOutEndTime: '22:00',
    lateThreshold: 10,
    earlyThreshold: 10,
    overtimeThreshold: 30,
    workDays: '[1,2,3,4,5]',
    requireWifi: 1,
    requireLocation: 1,
    enabled: 1,
    wifiList: [],
    locationList: []
  };
}

async function load() {
  error.value = '';
  const result = await listAttendanceRules();
  rules.value = result.data || [];
}

async function loadDepartments() {
  const result = await listDepartments();
  departments.value = result.data || [];
}

function reset() {
  Object.assign(form, defaultForm());
}

function edit(item) {
  Object.assign(form, {
    ...defaultForm(),
    ...item,
    deptId: item.deptId ?? null,
    effectiveStartDate: normalizeDate(item.effectiveStartDate),
    effectiveEndDate: normalizeDate(item.effectiveEndDate),
    workStartTime: formatTime(item.workStartTime),
    workEndTime: formatTime(item.workEndTime),
    checkInStartTime: formatTime(item.checkInStartTime),
    checkInEndTime: formatTime(item.checkInEndTime),
    checkOutStartTime: formatTime(item.checkOutStartTime),
    checkOutEndTime: formatTime(item.checkOutEndTime),
    wifiList: (item.wifiList || []).map((wifi) => ({ ...wifi })),
    locationList: (item.locationList || []).map((location) => ({ ...location }))
  });
}

async function save() {
  error.value = '';
  const payload = normalizePayload();
  const result = form.ruleId ? await updateAttendanceRule(payload) : await createAttendanceRule(payload);
  message.value = result.msg || '保存成功';
  reset();
  await load();
}

async function remove(ruleId) {
  const result = await deleteAttendanceRule(ruleId);
  message.value = result.msg || '删除成功';
  await load();
}

function addWifi() {
  form.wifiList.push({ wifiSsid: '', wifiBssid: '', enabled: 1 });
}

function removeWifi(index) {
  form.wifiList.splice(index, 1);
}

function addLocation() {
  form.locationList.push({ locationName: '', latitude: null, longitude: null, radius: 100, enabled: 1 });
}

function removeLocation(index) {
  form.locationList.splice(index, 1);
}

function normalizePayload() {
  return {
    ...form,
    checkInStartTime: form.workStartTime,
    deptId: form.deptId === '' ? null : form.deptId,
    effectiveStartDate: form.effectiveStartDate || null,
    effectiveEndDate: form.effectiveEndDate || null,
    requireWifi: Number(form.requireWifi),
    requireLocation: Number(form.requireLocation),
    enabled: Number(form.enabled),
    wifiList: form.wifiList.filter((wifi) => wifi.wifiSsid),
    locationList: form.locationList.filter((location) => location.locationName && location.latitude && location.longitude)
  };
}

function normalizeDate(value) {
  return value ? String(value).slice(0, 10) : '';
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : '';
}
</script>
