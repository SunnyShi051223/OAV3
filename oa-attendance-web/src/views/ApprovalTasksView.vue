<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">审批管理</p>
        <h2>审批处理</h2>
      </div>
      <button class="secondary" @click="refresh">刷新</button>
    </section>

    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div class="tab-bar">
        <button :class="['tab', { active: activeTab === 'pending' }]" @click="activeTab = 'pending'">
          待处理 ({{ pendingList.length }})
        </button>
        <button :class="['tab', { active: activeTab === 'handled' }]" @click="activeTab = 'handled'">
          已处理 ({{ handledList.length }})
        </button>
      </div>

      <!-- 待处理 -->
      <div v-if="activeTab === 'pending'">
        <table class="data-table">
          <thead>
            <tr>
              <th>申请人</th>
              <th>所属部门</th>
              <th>申请类型</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>申请原因</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pendingList" :key="item.applicationId">
              <td>{{ item.applicantName }}</td>
              <td>{{ item.applicantDeptName || '-' }}</td>
              <td>{{ item.applicationTypeLabel }}</td>
              <td>{{ formatDateTime(item.startTime) }}</td>
              <td>{{ formatDateTime(item.endTime) }}</td>
              <td>{{ truncate(item.reason, 15) }}</td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td>
                <div class="form-actions">
                  <button class="small" @click="openAction(item, 'approve')">通过</button>
                  <button class="small danger" @click="openAction(item, 'reject')">驳回</button>
                  <button class="small secondary" @click="openDetail(item)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="pendingList.length === 0">
              <td colspan="8" class="muted">暂无待处理审批</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 已处理 -->
      <div v-if="activeTab === 'handled'">
        <table class="data-table">
          <thead>
            <tr>
              <th>申请人</th>
              <th>所属部门</th>
              <th>申请类型</th>
              <th>时间范围</th>
              <th>申请原因</th>
              <th>审批结果</th>
              <th>完成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in handledList" :key="item.applicationId">
              <td>{{ item.applicantName }}</td>
              <td>{{ item.applicantDeptName || '-' }}</td>
              <td>{{ item.applicationTypeLabel }}</td>
              <td>{{ formatDateTime(item.startTime) }} ~ {{ formatDateTime(item.endTime) }}</td>
              <td>{{ truncate(item.reason, 15) }}</td>
              <td><span :class="['status-pill', statusClass(item.status)]">{{ item.statusLabel }}</span></td>
              <td>{{ formatDateTime(item.completeTime) }}</td>
              <td>
                <button class="small secondary" @click="openDetail(item)">详情</button>
              </td>
            </tr>
            <tr v-if="handledList.length === 0">
              <td colspan="8" class="muted">暂无已处理审批</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 审批操作对话框 -->
    <div v-if="actionVisible" class="modal-overlay" @click.self="actionVisible = false">
      <div class="modal-panel">
        <div class="panel-title">
          <h3>{{ actionType === 'approve' ? '通过申请' : '驳回申请' }}</h3>
          <button class="small secondary" @click="actionVisible = false">关闭</button>
        </div>
        <div v-if="actionTarget" class="action-info">
          <div class="detail-item">
            <span>申请人</span>
            <strong>{{ actionTarget.applicantName }}</strong>
          </div>
          <div class="detail-item">
            <span>申请类型</span>
            <strong>{{ actionTarget.applicationTypeLabel }}</strong>
          </div>
          <div class="detail-item">
            <span>时间范围</span>
            <strong>{{ formatDateTime(actionTarget.startTime) }} ~ {{ formatDateTime(actionTarget.endTime) }}</strong>
          </div>
          <div class="detail-item detail-span">
            <span>申请原因</span>
            <p>{{ actionTarget.reason }}</p>
          </div>
        </div>
        <form class="form" @submit.prevent="executeAction">
          <label>
            审批意见
            <textarea v-model="actionComment" :placeholder="actionType === 'reject' ? '驳回必须填写审批意见' : '可选填写审批意见'" :required="actionType === 'reject'"></textarea>
          </label>
          <div class="form-actions">
            <button type="submit" :disabled="executing">
              {{ actionType === 'approve' ? '确认通过' : '确认驳回' }}
            </button>
            <button type="button" class="secondary" @click="actionVisible = false">取消</button>
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
import { onMounted, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import {
  listPendingTasks,
  listHandledTasks,
  approveTask,
  rejectTask,
  getApplicationDetail
} from '../api/applications';

const activeTab = ref('pending');
const pendingList = ref([]);
const handledList = ref([]);
const message = ref('');
const error = ref('');
const executing = ref(false);

const actionVisible = ref(false);
const actionTarget = ref(null);
const actionType = ref('approve');
const actionComment = ref('');

const detailVisible = ref(false);
const detail = ref(null);

onMounted(refresh);

async function refresh() {
  error.value = '';
  try {
    const [pendingRes, handledRes] = await Promise.all([
      listPendingTasks(),
      listHandledTasks()
    ]);
    pendingList.value = pendingRes.data || [];
    handledList.value = handledRes.data || [];
  } catch (err) {
    error.value = err.message;
  }
}

function openAction(item, type) {
  actionTarget.value = item;
  actionType.value = type;
  actionComment.value = '';
  actionVisible.value = true;
}

async function executeAction() {
  error.value = '';
  message.value = '';
  executing.value = true;
  try {
    const taskId = actionTarget.value.taskId;
    const payload = { comment: actionComment.value };
    const result = actionType.value === 'approve'
      ? await approveTask(taskId, payload)
      : await rejectTask(taskId, payload);
    message.value = result.msg || '操作成功';
    actionVisible.value = false;
    await refresh();
  } catch (err) {
    error.value = err.message;
  } finally {
    executing.value = false;
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

.panel .data-table {
  min-width: 900px;
}

.panel .data-table th:nth-child(1),
.panel .data-table td:nth-child(1),
.panel .data-table th:nth-child(2),
.panel .data-table td:nth-child(2),
.panel .data-table th:nth-child(3),
.panel .data-table td:nth-child(3),
.panel .data-table th:nth-child(7),
.panel .data-table td:nth-child(7),
.panel .data-table th:nth-child(8),
.panel .data-table td:nth-child(8) {
  white-space: nowrap;
}

.panel .data-table th:nth-child(6),
.panel .data-table td:nth-child(6) {
  min-width: 180px;
  white-space: normal;
}

.tab-bar {
  display: flex;
  gap: 0;
  margin-bottom: 18px;
  border-bottom: 2px solid #d6e0dc;
}

.tab {
  border: 0;
  border-radius: 0;
  padding: 10px 20px;
  color: #65756f;
  background: transparent;
  font-size: 15px;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
}

.tab.active {
  color: #146c5a;
  border-bottom-color: #146c5a;
}

.tab:hover:not(.active) {
  color: #172026;
  background: #f6faf8;
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

.action-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
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
