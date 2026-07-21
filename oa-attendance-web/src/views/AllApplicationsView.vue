<template>
  <AppShell>
    <section class="toolbar">
      <div>
        <p class="eyebrow">审批管理</p>
        <h2>全部审批</h2>
      </div>
      <button class="secondary" @click="refresh">刷新</button>
    </section>

    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div class="search-row">
        <input v-model.trim="search" placeholder="搜索申请人、申请类型、原因..." @input="onSearch" />
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>申请人</th>
            <th>所属部门</th>
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
          <tr v-for="item in filteredList" :key="item.applicationId">
            <td>{{ item.applicantName }}</td>
            <td>{{ item.applicantDeptName || '-' }}</td>
            <td>{{ item.applicationTypeLabel }}</td>
            <td>{{ formatDateTime(item.startTime) }}</td>
            <td>{{ formatDateTime(item.endTime) }}</td>
            <td>{{ truncate(item.reason, 15) }}</td>
            <td><span :class="['status-pill', statusClass(item.status)]">{{ item.statusLabel }}</span></td>
            <td>{{ formatDateTime(item.createTime) }}</td>
            <td>
              <button class="small secondary" @click="openDetail(item)">详情</button>
            </td>
          </tr>
          <tr v-if="filteredList.length === 0">
            <td colspan="9" class="muted">{{ search ? '没有匹配的申请记录' : '暂无申请记录' }}</td>
          </tr>
        </tbody>
      </table>
    </section>

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
import { computed, onMounted, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import { listAllApplications, getApplicationDetail } from '../api/applications';

const applications = ref([]);
const message = ref('');
const error = ref('');
const search = ref('');
const filteredList = ref([]);

const detailVisible = ref(false);
const detail = ref(null);

function onSearch() {
  const keyword = search.value.toLowerCase();
  filteredList.value = keyword
    ? applications.value.filter(item =>
        (item.applicantName || '').toLowerCase().includes(keyword) ||
        (item.applicantDeptName || '').toLowerCase().includes(keyword) ||
        (item.applicationTypeLabel || '').toLowerCase().includes(keyword) ||
        (item.reason || '').toLowerCase().includes(keyword)
      )
    : applications.value;
}

onMounted(refresh);

async function refresh() {
  error.value = '';
  try {
    const result = await listAllApplications();
    applications.value = result.data || [];
    onSearch();
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
    REJECTED: 'status-absent'
  };
  return map[decision] || 'status-pending';
}

function decisionLabel(decision) {
  const map = {
    APPROVED: '通过',
    REJECTED: '驳回'
  };
  return map[decision] || decision || '处理中';
}
</script>

<style scoped>
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

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.attachment-list a {
  color: #146c5a;
  text-decoration: none;
  font-size: 14px;
}

.attachment-list a:hover {
  text-decoration: underline;
}

h4 {
  margin: 0 0 12px;
  color: #53655e;
}
</style>
