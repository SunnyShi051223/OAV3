<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">审批管理</p>
        <h2>我的申请</h2>
      </div>
      <div class="form-actions">
        <button class="secondary" @click="refresh">刷新</button>
        <button @click="showCreateDialog = true">新建申请</button>
      </div>
    </section>

    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>申请类型</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>申请原因</th>
            <th>状态</th>
            <th>提交时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in applications" :key="item.applicationId">
            <td>{{ item.applicationTypeLabel }}</td>
            <td>{{ formatDateTime(item.startTime) }}</td>
            <td>{{ formatDateTime(item.endTime) }}</td>
            <td>{{ truncate(item.reason, 20) }}</td>
            <td><span :class="['status-pill', statusClass(item.status)]">{{ item.statusLabel }}</span></td>
            <td>{{ formatDateTime(item.createTime) }}</td>
            <td>
              <button class="small secondary" @click="openDetail(item)">详情</button>
              <button v-if="item.canCancel" class="small danger" @click="cancel(item)">撤回</button>
            </td>
          </tr>
          <tr v-if="applications.length === 0">
            <td colspan="7" class="muted">暂无申请记录</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 新建申请对话框 -->
    <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
      <div class="modal-panel">
        <div class="panel-title">
          <h3>新建申请</h3>
          <button class="small secondary" @click="showCreateDialog = false">关闭</button>
        </div>
        <form class="form" @submit.prevent="submitApplication">
          <label>
            申请类型
            <select v-model="createForm.applicationType" required>
              <option value="">请选择申请类型</option>
              <option v-for="t in types" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </label>
          <label v-if="createForm.applicationType === 'MAKEUP'">
            需要补卡的考勤记录
            <select v-model="createForm.attendanceRecordId" required @change="applyMakeupSuggestion">
              <option :value="null">请选择异常考勤记录</option>
              <option v-for="item in makeupOptions" :key="item.recordId" :value="item.recordId">
                {{ makeupOptionLabel(item) }}
              </option>
            </select>
            <small v-if="makeupOptions.length === 0">当前没有可补卡的缺勤、迟到、早退或缺卡记录</small>
          </label>
          <label>
            {{ createForm.applicationType === 'MAKEUP' ? '补卡签到时间' : '开始时间' }}
            <input v-model="createForm.startTime" type="datetime-local" required />
          </label>
          <label>
            {{ createForm.applicationType === 'MAKEUP' ? '补卡签退时间' : '结束时间' }}
            <input v-model="createForm.endTime" type="datetime-local" required />
          </label>
          <label>
            申请原因
            <textarea v-model="createForm.reason" placeholder="请详细说明申请原因" required></textarea>
          </label>
          <label>
            备注
            <input v-model="createForm.remark" placeholder="可选备注" />
          </label>
          <label>
            附件地址
            <textarea v-model="attachmentText" placeholder="每行填写一个 http、https 或站内文件地址"></textarea>
            <small>最多 10 个附件地址</small>
          </label>
          <div class="form-actions">
            <button type="submit" :disabled="submitting">提交申请</button>
            <button type="button" class="secondary" @click="showCreateDialog = false">取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 申请详情对话框 -->
    <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
      <div class="modal-panel modal-wide">
        <div class="panel-title">
          <h3>申请详情</h3>
          <button class="small secondary" @click="detailVisible = false">关闭</button>
        </div>
        <div v-if="detail" class="detail-grid">
          <div class="detail-item">
            <span>申请类型</span>
            <strong>{{ detail.applicationTypeLabel }}</strong>
          </div>
          <div class="detail-item">
            <span>申请人</span>
            <strong>{{ detail.applicantName }}</strong>
          </div>
          <div class="detail-item">
            <span>所属部门</span>
            <strong>{{ detail.applicantDeptName || '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>状态</span>
            <span :class="['status-pill', statusClass(detail.status)]">{{ detail.statusLabel }}</span>
          </div>
          <div v-if="detail.attendanceRecordId" class="detail-item">
            <span>关联考勤记录</span>
            <strong>#{{ detail.attendanceRecordId }}</strong>
          </div>
          <div class="detail-item">
            <span>开始时间</span>
            <strong>{{ formatDateTime(detail.startTime) }}</strong>
          </div>
          <div class="detail-item">
            <span>结束时间</span>
            <strong>{{ formatDateTime(detail.endTime) }}</strong>
          </div>
          <div class="detail-item detail-span">
            <span>申请原因</span>
            <p>{{ detail.reason }}</p>
          </div>
          <div v-if="detail.remark" class="detail-item detail-span">
            <span>备注</span>
            <p>{{ detail.remark }}</p>
          </div>
          <div v-if="detail.attachmentUrls?.length" class="detail-item detail-span">
            <span>申请附件</span>
            <div class="attachment-list">
              <a v-for="(url, index) in detail.attachmentUrls" :key="url" :href="url" target="_blank" rel="noopener noreferrer">
                附件 {{ index + 1 }}
              </a>
            </div>
          </div>
          <div class="detail-item">
            <span>提交时间</span>
            <strong>{{ formatDateTime(detail.createTime) }}</strong>
          </div>
          <div v-if="detail.completeTime" class="detail-item">
            <span>完成时间</span>
            <strong>{{ formatDateTime(detail.completeTime) }}</strong>
          </div>
        </div>

        <div v-if="detail.approvalHistory && detail.approvalHistory.length" class="sub-panel">
          <h4>审批记录</h4>
          <table class="data-table">
            <thead>
              <tr>
                <th>审批节点</th>
                <th>审批人</th>
                <th>审批结果</th>
                <th>审批意见</th>
                <th>开始时间</th>
                <th>完成时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(h, idx) in detail.approvalHistory" :key="idx">
                <td>{{ h.taskName }}</td>
                <td>{{ h.approverName || h.approverUsername || '-' }}</td>
                <td><span :class="['status-pill', decisionClass(h.decision)]">{{ decisionLabel(h.decision) }}</span></td>
                <td>{{ h.comment || '-' }}</td>
                <td>{{ formatDateTime(h.startTime) }}</td>
                <td>{{ formatDateTime(h.endTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import AppShell from '../components/AppShell.vue';
import {
  listMyApplications,
  listApplicationTypes,
  listMakeupRecordOptions,
  submitApplication as submitApp,
  getApplicationDetail,
  cancelApplication
} from '../api/applications';

const applications = ref([]);
const types = ref([]);
const makeupOptions = ref([]);
const message = ref('');
const error = ref('');
const submitting = ref(false);
const showCreateDialog = ref(false);
const attachmentText = ref('');

const createForm = reactive({
  applicationType: '',
  attendanceRecordId: null,
  startTime: '',
  endTime: '',
  reason: '',
  remark: ''
});

const detailVisible = ref(false);
const detail = ref(null);

onMounted(refresh);

async function refresh() {
  error.value = '';
  try {
    const [appRes, typeRes, makeupRes] = await Promise.all([
      listMyApplications(),
      listApplicationTypes(),
      listMakeupRecordOptions()
    ]);
    applications.value = appRes.data || [];
    types.value = typeRes.data || [];
    makeupOptions.value = makeupRes.data || [];
  } catch (err) {
    error.value = err.message;
  }
}

async function submitApplication() {
  error.value = '';
  message.value = '';
  submitting.value = true;
  try {
    const result = await submitApp({
      ...createForm,
      attachmentUrls: attachmentText.value
        .split(/\r?\n/)
        .map((item) => item.trim())
        .filter(Boolean)
    });
    message.value = result.msg || '提交成功';
    showCreateDialog.value = false;
    Object.assign(createForm, {
      applicationType: '',
      attendanceRecordId: null,
      startTime: '',
      endTime: '',
      reason: '',
      remark: ''
    });
    attachmentText.value = '';
    await refresh();
  } catch (err) {
    error.value = err.message;
  } finally {
    submitting.value = false;
  }
}

watch(() => createForm.applicationType, (type) => {
  if (type !== 'MAKEUP') {
    createForm.attendanceRecordId = null;
  }
});

function applyMakeupSuggestion() {
  const option = makeupOptions.value.find(
    (item) => String(item.recordId) === String(createForm.attendanceRecordId)
  );
  if (!option) return;
  createForm.startTime = toDateTimeInput(option.suggestedStartTime);
  createForm.endTime = toDateTimeInput(option.suggestedEndTime);
}

function makeupOptionLabel(item) {
  return `${item.attendanceDate} · ${item.ruleName || '考勤规则'} · ${item.issueLabel || '异常'}`;
}

function toDateTimeInput(value) {
  return value ? String(value).slice(0, 16) : '';
}

async function cancel(item) {
  if (!window.confirm('确认撤回这条申请吗？撤回后不能恢复。')) return;
  error.value = '';
  message.value = '';
  try {
    const result = await cancelApplication(item.applicationId);
    message.value = result.msg || '申请已撤回';
    await refresh();
  } catch (err) {
    error.value = err.message;
  }
}

async function openDetail(item) {
  error.value = '';
  try {
    const result = await getApplicationDetail(item.applicationId);
    detail.value = result.data;
    detailVisible.value = true;
  } catch (err) {
    error.value = err.message;
  }
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '';
}

function truncate(text, max) {
  if (!text) return '';
  return text.length > max ? text.slice(0, max) + '...' : text;
}

function statusClass(status) {
  const map = {
    PROCESSING: 'status-pending',
    APPROVED: 'status-normal',
    REJECTED: 'status-absent',
    CANCELED: 'status-absent',
    DRAFT: 'status-pending'
  };
  return map[status] || 'status-pending';
}

function decisionClass(decision) {
  const map = {
    APPROVED: 'status-normal',
    REJECTED: 'status-absent',
    CANCELED: 'status-absent'
  };
  return map[decision] || 'status-pending';
}

function decisionLabel(decision) {
  const map = {
    APPROVED: '通过',
    REJECTED: '驳回',
    CANCELED: '申请人撤回'
  };
  return map[decision] || decision || '处理中';
}
</script>

<style scoped>
.panel {
  overflow-x: auto;
}

.panel > .data-table {
  min-width: 860px;
}

.panel > .data-table th,
.panel > .data-table td {
  white-space: nowrap;
}

.panel > .data-table th:nth-child(4),
.panel > .data-table td:nth-child(4) {
  min-width: 180px;
  white-space: normal;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  z-index: 100;
  padding: 24px;
}

.modal-panel {
  width: min(560px, 100%);
  max-height: 85vh;
  overflow-y: auto;
  border-radius: 8px;
  padding: 24px;
  background: #fff;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.modal-wide {
  width: min(720px, 100%);
}

.modal-panel .panel-title {
  margin-bottom: 18px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-item {
  border: 1px solid #e2e9e6;
  border-radius: 6px;
  padding: 14px;
  background: #f8fbfa;
}

.detail-item span {
  display: block;
  margin-bottom: 6px;
  color: #65756f;
  font-size: 13px;
}

.detail-item strong,
.detail-item p {
  color: #172026;
  font-size: 15px;
}

.detail-item p {
  margin: 0;
  line-height: 1.6;
}

.detail-span {
  grid-column: 1 / -1;
}

h4 {
  margin: 0 0 12px;
  color: #53655e;
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attachment-list a {
  color: #146c5a;
}
</style>
