<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">个人考勤</p>
        <h2>我的考勤</h2>
      </div>
      <button class="secondary" @click="refreshAll">刷新</button>
    </section>
    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <section class="attendance-hero">
      <article class="panel rule-summary">
        <div class="panel-title">
          <div>
            <p class="eyebrow">今日可选考勤</p>
            <h3>{{ currentRule?.ruleName || '暂无可用规则' }}</h3>
          </div>
          <span :class="['status-pill', currentRule ? 'status-normal' : 'status-absent']">
            {{ availableRules.length }} 条
          </span>
        </div>

        <label class="rule-select">
          选择本次考勤规则
          <select v-model.number="selectedRuleId">
            <option :value="null">请选择考勤规则</option>
            <option v-for="rule in availableRules" :key="rule.ruleId" :value="rule.ruleId">
              {{ rule.ruleName }}（{{ formatTime(rule.workStartTime) }} - {{ formatTime(rule.workEndTime) }}）
            </option>
          </select>
        </label>

        <div v-if="currentRule" class="rule-grid">
          <div>
            <span>适用部门</span>
            <strong>{{ currentRule.deptName || '全公司' }}</strong>
          </div>
          <div>
            <span>上班时间</span>
            <strong>{{ formatTime(currentRule.workStartTime) }}</strong>
          </div>
          <div>
            <span>下班时间</span>
            <strong>{{ formatTime(currentRule.workEndTime) }}</strong>
          </div>
          <div>
            <span>校验方式</span>
            <strong>{{ ruleCheckText(currentRule) }}</strong>
          </div>
          <div>
            <span>正常签到窗口</span>
            <strong>{{ formatTime(currentRule.workStartTime) || '-' }} 至 {{ formatTime(currentRule.checkInEndTime) || '-' }}</strong>
          </div>
          <div>
            <span>迟到签到截止</span>
            <strong>{{ formatTime(currentRule.workEndTime) || '-' }}</strong>
          </div>
          <div>
            <span>签退窗口</span>
            <strong>{{ formatTime(currentRule.checkOutStartTime) || '-' }} 至 {{ formatTime(currentRule.checkOutEndTime) || '-' }}</strong>
          </div>
        </div>
        <p v-else class="muted">请选择考勤规则；如果没有可选项，请联系管理员或部门主管配置今日生效规则。</p>
      </article>

      <article class="panel today-summary">
        <div class="panel-title">
          <div>
            <p class="eyebrow">所选规则下今日状态</p>
            <h3>{{ statusLabel(todayStatus) }}</h3>
          </div>
          <span :class="['status-pill', statusClass(todayStatus)]">{{ statusLabel(todayStatus) }}</span>
        </div>
        <div class="timeline">
          <div>
            <span>签到</span>
            <strong>{{ formatDateTime(selectedRecord?.checkInTime) || '未签到' }}</strong>
            <em>{{ statusLabel(selectedRecord?.checkInStatus) || '待完成' }}</em>
          </div>
          <div>
            <span>签退</span>
            <strong>{{ formatDateTime(selectedRecord?.checkOutTime) || '未签退' }}</strong>
            <em>{{ statusLabel(selectedRecord?.checkOutStatus) || '待完成' }}</em>
          </div>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>签到 / 签退</h3>
        <div class="form-actions">
          <button class="small secondary" type="button" @click="getLocation">一键获取定位</button>
          <button class="small secondary" type="button" @click="getWifi">一键获取 WiFi</button>
        </div>
      </div>
      <form class="attendance-form compact" @submit.prevent>
        <input v-model.trim="checkForm.wifiSsid" readonly placeholder="WiFi SSID（通过按钮获取）" />
        <input v-model.trim="checkForm.wifiBssid" readonly placeholder="WiFi BSSID/MAC（通过按钮获取）" />
        <input v-model.number="checkForm.latitude" readonly type="number" step="0.000001" placeholder="纬度（通过按钮获取）" />
        <input v-model.number="checkForm.longitude" readonly type="number" step="0.000001" placeholder="经度（通过按钮获取）" />
        <input v-model.trim="checkForm.locationAddress" readonly class="form-span" placeholder="位置来源说明" />
        <p class="muted form-span">定位可由浏览器授权后获取；出于浏览器隐私限制，网页无法读取真实 WiFi SSID/BSSID，需要桌面客户端或企业移动端能力支持。</p>
        <div class="form-actions form-span">
          <button type="button" :disabled="!selectedRuleId" @click="submitCheckIn">签到</button>
          <button class="secondary" type="button" :disabled="!selectedRuleId" @click="submitCheckOut">签退</button>
        </div>
      </form>
    </section>

    <section class="toolbar attendance-toolbar">
      <div>
        <p class="eyebrow">月度汇总</p>
        <h2>{{ selectedMonth }} 考勤</h2>
      </div>
      <div class="month-picker">
        <input v-model="selectedMonth" type="month" />
        <button class="secondary" @click="loadMonth">查询</button>
      </div>
    </section>

    <section class="grid">
      <article class="card">
        <span>月度工时</span>
        <strong>{{ minutesToHours(monthData.workMinutes) }}</strong>
      </article>
      <article class="card">
        <span>迟到次数</span>
        <strong>{{ monthData.lateCount || 0 }}</strong>
      </article>
      <article class="card">
        <span>缺勤次数</span>
        <strong>{{ monthData.absentCount || 0 }}</strong>
      </article>
      <article class="card">
        <span>加班次数</span>
        <strong>{{ monthData.overtimeCount || 0 }}</strong>
      </article>
      <article class="card">
        <span>加班时长</span>
        <strong>{{ minutesToHours(monthData.overtimeMinutes) }}</strong>
      </article>
      <article class="card">
        <span>请假次数</span>
        <strong>{{ monthData.leaveCount || 0 }}</strong>
      </article>
    </section>

    <section class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>日期</th>
            <th>适用规则</th>
            <th>签到</th>
            <th>签退</th>
            <th>规则下状态</th>
            <th>工时</th>
            <th>加班</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in monthData.calendar" :key="item.recordId">
            <td>{{ item.attendanceDate }}</td>
            <td>{{ item.ruleName || '-' }}</td>
            <td>{{ formatDateTime(item.checkInTime) || '-' }}</td>
            <td>{{ formatDateTime(item.checkOutTime) || '-' }}</td>
            <td><span :class="['status-pill', statusClass(item.attendanceStatus)]">{{ statusLabel(item.attendanceStatus) }}</span></td>
            <td>{{ minutesToHours(item.workMinutes) }}</td>
            <td>{{ minutesToHours(item.overtimeMinutes) }}</td>
          </tr>
          <tr v-if="!monthData.calendar.length">
            <td colspan="7" class="muted">本月暂无考勤记录</td>
          </tr>
        </tbody>
      </table>
    </section>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import AppShell from '../components/AppShell.vue';
import {
  checkIn,
  checkOut,
  getAvailableAttendanceRules,
  getMyAttendanceMonth,
  getTodayAttendanceRecords
} from '../api/attendance';

const availableRules = ref([]);
const todayRecords = ref([]);
const selectedRuleId = ref(null);
const monthData = reactive({ calendar: [] });
const message = ref('');
const error = ref('');
const selectedMonth = ref(new Date().toISOString().slice(0, 7));
const checkForm = reactive({
  wifiSsid: '',
  wifiBssid: '',
  latitude: null,
  longitude: null,
  locationAddress: '',
  clientInfo: navigator.userAgent
});

const currentRule = computed(() => availableRules.value.find((rule) => rule.ruleId === selectedRuleId.value) || null);
const selectedRecord = computed(() => todayRecords.value.find((record) => record.ruleId === selectedRuleId.value) || null);
const todayStatus = computed(() => {
  if (!currentRule.value) {
    return 'NO_RULE';
  }
  return selectedRecord.value?.attendanceStatus || 'PENDING';
});

watch(selectedRuleId, () => {
  message.value = '';
  error.value = '';
});

onMounted(refreshAll);

async function refreshAll() {
  await Promise.all([loadAvailableRules(), loadTodayRecords(), loadMonth()]);
}

async function loadAvailableRules() {
  const result = await getAvailableAttendanceRules();
  availableRules.value = result.data || [];
  if (!selectedRuleId.value && availableRules.value.length) {
    selectedRuleId.value = availableRules.value[0].ruleId;
  }
  if (selectedRuleId.value && !availableRules.value.some((rule) => rule.ruleId === selectedRuleId.value)) {
    selectedRuleId.value = availableRules.value[0]?.ruleId || null;
  }
}

async function loadTodayRecords() {
  const result = await getTodayAttendanceRecords();
  todayRecords.value = result.data || [];
}

async function loadMonth() {
  const result = await getMyAttendanceMonth(selectedMonth.value);
  Object.assign(monthData, {
    workMinutes: 0,
    lateCount: 0,
    absentCount: 0,
    overtimeCount: 0,
    overtimeMinutes: 0,
    leaveCount: 0,
    leaveMinutes: 0,
    calendar: [],
    ...(result.data || {})
  });
}

async function submitCheckIn() {
  await submitCheck(checkIn, '签到成功');
}

async function submitCheckOut() {
  await submitCheck(checkOut, '签退成功');
}

async function submitCheck(action, successText) {
  error.value = '';
  message.value = '';
  try {
    const result = await action({ ...checkForm, ruleId: selectedRuleId.value });
    message.value = result.msg || successText;
    await refreshAll();
  } catch (err) {
    error.value = err.message;
  }
}

function getLocation() {
  error.value = '';
  if (!navigator.geolocation) {
    error.value = '当前浏览器不支持定位';
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (position) => {
      checkForm.latitude = Number(position.coords.latitude.toFixed(6));
      checkForm.longitude = Number(position.coords.longitude.toFixed(6));
      checkForm.locationAddress = '浏览器定位已获取';
      message.value = '定位已获取';
    },
    (err) => {
      error.value = err.message || '定位获取失败';
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
  );
}

function getWifi() {
  error.value = '浏览器网页无法读取真实 WiFi SSID/BSSID；如规则要求 WiFi 校验，需要接入桌面客户端或移动端能力。';
}

function ruleCheckText(rule) {
  const checks = [];
  if (rule.requireWifi === 1) {
    checks.push('WiFi');
  }
  if (rule.requireLocation === 1) {
    checks.push('定位');
  }
  return checks.length ? checks.join(' + ') : '无需校验';
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : '';
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '';
}

function minutesToHours(value) {
  const minutes = Number(value || 0);
  return `${(minutes / 60).toFixed(1)} 小时`;
}

function statusLabel(value) {
  const map = {
    NORMAL: '正常',
    LATE: '迟到',
    EARLY: '早退',
    ABSENT: '缺勤',
    LEAVE: '请假',
    OVERTIME: '加班',
    PENDING: '待打卡',
    NO_RULE: '无规则'
  };
  return map[value] || '待确认';
}

function statusClass(value) {
  const map = {
    NORMAL: 'status-normal',
    OVERTIME: 'status-overtime',
    LATE: 'status-warning',
    EARLY: 'status-warning',
    LEAVE: 'status-leave',
    ABSENT: 'status-absent',
    NO_RULE: 'status-absent',
    PENDING: 'status-pending'
  };
  return map[value] || 'status-pending';
}
</script>
