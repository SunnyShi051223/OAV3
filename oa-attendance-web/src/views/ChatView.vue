<template>
  <AppShell>
    <div class="chat-layout">
      <!-- 左侧通讯录 -->
      <aside class="chat-sidebar">
        <div class="sidebar-header">
          <h3>通讯录</h3>
          <input v-model.trim="filterText" placeholder="搜索联系人" class="contact-search" />
        </div>
        <ul class="contact-list">
          <li
            v-for="contact in filteredContacts"
            :key="contact.userId"
            :class="['contact-item', { active: activeContact && activeContact.userId === contact.userId }]"
            @click="selectContact(contact)"
          >
            <div class="contact-avatar">
              <img v-if="contact.avatar" :src="contact.avatar" :alt="contact.realName" />
              <span v-else class="avatar-placeholder">{{ (contact.realName || '?')[0] }}</span>
            </div>
            <div class="contact-body">
              <div class="contact-top">
                <span class="contact-name">{{ contact.realName }}</span>
                <span class="contact-time">{{ contact.lastMessageTime }}</span>
              </div>
              <div class="contact-bottom">
                <span class="contact-dept">{{ contact.deptName }}</span>
                <span v-if="contact.unreadCount > 0" class="unread-badge">{{ contact.unreadCount }}</span>
              </div>
            </div>
          </li>
        </ul>
      </aside>

      <!-- 右侧聊天窗口 -->
      <section class="chat-main">
        <template v-if="activeContact">
          <header class="chat-header">
            <span class="chat-contact-name">{{ activeContact.realName }}</span>
            <span class="chat-contact-info">{{ activeContact.deptName }} {{ activeContact.positionName || '' }} {{ activeContact.roleName }}</span>
          </header>
          <div ref="msgArea" class="chat-messages">
            <div
              v-for="msg in messages"
              :key="msg.messageId"
              :class="['message-row', { 'message-self': msg.senderId === currentUserId }]"
            >
              <div class="message-bubble">
                <div class="message-text">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
            <div v-if="messages.length === 0" class="empty-chat">暂无消息，发送第一条消息吧</div>
          </div>
          <footer class="chat-input">
            <textarea
              v-model="inputText"
              placeholder="输入消息..."
              rows="2"
              @keydown.enter.exact.prevent="doSend"
            ></textarea>
            <button :disabled="!inputText.trim()" @click="doSend">发送</button>
          </footer>
        </template>
        <div v-else class="chat-placeholder">请从左侧通讯录选择联系人开始聊天</div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import AppShell from '../components/AppShell.vue';
import { authStore } from '../store/auth';
import { getContacts, getMessages, sendMessage } from '../api/chat';

const filterText = ref('');
const contacts = ref([]);
const activeContact = ref(null);
const messages = ref([]);
const inputText = ref('');
const msgArea = ref(null);

const currentUserId = computed(() => authStore.user?.userId);

const filteredContacts = computed(() => {
  if (!filterText.value) return contacts.value;
  const keyword = filterText.value.toLowerCase();
  return contacts.value.filter(
    (c) => c.realName && c.realName.toLowerCase().includes(keyword)
  );
});

let pollTimer = null;

onMounted(async () => {
  await loadContacts();
  pollTimer = setInterval(pollMessages, 3000);
});

onBeforeUnmount(() => {
  clearInterval(pollTimer);
});

async function loadContacts() {
  try {
    const result = await getContacts();
    contacts.value = result.data || [];
  } catch (e) {
    // ignore
  }
}

async function selectContact(contact) {
  activeContact.value = contact;
  contact.unreadCount = 0;
  try {
    const result = await getMessages(contact.userId);
    messages.value = result.data || [];
  } catch (e) {
    messages.value = [];
  }
  await nextTick();
  scrollToBottom();
}

async function doSend() {
  const text = inputText.value.trim();
  if (!text || !activeContact.value) return;
  inputText.value = '';
  try {
    const result = await sendMessage({
      receiverId: activeContact.value.userId,
      content: text,
    });
    messages.value.push(result.data);
    await nextTick();
    scrollToBottom();
  } catch (e) {
    alert(e.message || '发送失败');
  }
}

async function pollMessages() {
  try {
    const result = await getContacts();
    const freshContacts = result.data || [];
    // 增量合并：只更新字段，避免替换整个列表导致 UI 闪烁
    const freshMap = new Map(freshContacts.map((c) => [c.userId, c]));
    for (const contact of contacts.value) {
      const fresh = freshMap.get(contact.userId);
      if (fresh) {
        contact.lastMessage = fresh.lastMessage;
        contact.lastMessageTime = fresh.lastMessageTime;
        contact.unreadCount = fresh.unreadCount;
      }
    }
  } catch (e) {
    // ignore
  }
  if (activeContact.value) {
    try {
      const result = await getMessages(activeContact.value.userId);
      if (result.data && result.data.length > 0) {
        const existingIds = new Set(messages.value.map((m) => m.messageId));
        const newMsgs = result.data.filter((m) => !existingIds.has(m.messageId));
        if (newMsgs.length > 0) {
          messages.value = result.data;
          scrollToBottom();
        }
      }
    } catch (e) {
      // ignore
    }
  }
}

function scrollToBottom() {
  if (msgArea.value) {
    msgArea.value.scrollTop = msgArea.value.scrollHeight;
  }
}

function formatTime(value) {
  if (!value) return '';
  const d = new Date(value);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: calc(100vh - 60px);
  border: 1px solid var(--border-color, #e0e0e0);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

/* 左侧通讯录 */
.chat-sidebar {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid var(--border-color, #e0e0e0);
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
}

.sidebar-header h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.contact-search {
  width: 100%;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 13px;
  box-sizing: border-box;
}

.contact-list {
  flex: 1;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 0;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}

.contact-item:hover {
  background: #f0f0f0;
}

.contact-item.active {
  background: #e3f2fd;
}

.contact-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 10px;
}

.contact-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #1976d2;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.contact-body {
  flex: 1;
  min-width: 0;
}

.contact-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.contact-name {
  font-size: 14px;
  font-weight: 500;
}

.contact-time {
  font-size: 11px;
  color: #999;
}

.contact-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2px;
}

.contact-dept {
  font-size: 12px;
  color: #888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-badge {
  background: #f44336;
  color: #fff;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  padding: 0 4px;
  flex-shrink: 0;
}

/* 右侧聊天窗口 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
  background: #fafafa;
}

.chat-contact-name {
  font-size: 16px;
  font-weight: 600;
  display: block;
}

.chat-contact-info {
  font-size: 12px;
  color: #888;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.message-self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.message-row:not(.message-self) .message-bubble {
  background: #fff;
  border: 1px solid #e8e8e8;
}

.message-self .message-bubble {
  background: #1976d2;
  color: #fff;
}

.message-text {
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
}

.empty-chat {
  text-align: center;
  color: #bbb;
  padding: 40px 0;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 15px;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  padding: 10px 16px;
  border-top: 1px solid var(--border-color, #e0e0e0);
  gap: 8px;
  background: #fafafa;
}

.chat-input textarea {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  font-size: 14px;
  line-height: 1.4;
  outline: none;
  font-family: inherit;
}

.chat-input textarea:focus {
  border-color: #1976d2;
}

.chat-input button {
  padding: 8px 20px;
  background: #1976d2;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.chat-input button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
