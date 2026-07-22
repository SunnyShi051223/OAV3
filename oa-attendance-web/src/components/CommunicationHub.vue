<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { chatApi, colleagueApi } from '../api/oa'

const props = defineProps({ mode: { type: String, required: true }, currentUser: { type: Object, default: null }, canChat: Boolean })
const emit = defineEmits(['open-messages', 'unread-change'])

const colleagueKeyword = ref('')
const colleagueResults = ref([])
const colleagueSearched = ref(false)
const colleagueLoading = ref(false)
const contacts = ref([])
const contactKeyword = ref('')
const selectedContact = ref(null)
const messages = ref([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const messagesContainer = ref(null)
const pendingContactId = ref(null)

const displayName = computed(() => props.currentUser?.realName || props.currentUser?.username || '我')
const filteredContacts = computed(() => {
  const keyword = contactKeyword.value.trim().toLowerCase()
  if (!keyword) return contacts.value
  return contacts.value.filter((item) => `${item.realName}${item.deptName || ''}${item.positionName || ''}`.toLowerCase().includes(keyword))
})
const totalUnread = computed(() => contacts.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0))

async function searchColleagues() {
  if (!colleagueKeyword.value.trim()) return
  colleagueLoading.value = true
  colleagueSearched.value = true
  error.value = ''
  try {
    const result = await colleagueApi.search(colleagueKeyword.value.trim())
    colleagueResults.value = result.data || []
  } catch (requestError) {
    error.value = requestError.message
    colleagueResults.value = []
  } finally {
    colleagueLoading.value = false
  }
}

async function loadContacts(autoSelect = true) {
  loading.value = true
  error.value = ''
  try {
    const result = await chatApi.contacts()
    contacts.value = result.data || []
    emit('unread-change', totalUnread.value)
    const targetId = pendingContactId.value || selectedContact.value?.userId
    const target = contacts.value.find((item) => Number(item.userId) === Number(targetId))
    if (target) await selectContact(target)
    else if (autoSelect && contacts.value.length && !selectedContact.value) await selectContact(contacts.value[0])
    pendingContactId.value = null
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

async function selectContact(contact) {
  selectedContact.value = contact
  loading.value = true
  error.value = ''
  try {
    const result = await chatApi.messages(contact.userId)
    messages.value = result.data || []
    const localContact = contacts.value.find((item) => Number(item.userId) === Number(contact.userId))
    if (localContact) localContact.unreadCount = 0
    emit('unread-change', totalUnread.value)
    await scrollToBottom()
  } catch (requestError) {
    error.value = requestError.message
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  const content = draft.value.trim()
  if (!content || !selectedContact.value || sending.value) return
  sending.value = true
  error.value = ''
  try {
    const result = await chatApi.send(selectedContact.value.userId, content)
    if (result.data) messages.value.push(result.data)
    draft.value = ''
    await scrollToBottom()
    await loadContacts(false)
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    sending.value = false
  }
}

function startChat(person) {
  pendingContactId.value = person.userId
  emit('open-messages')
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
}

function initial(name) { return String(name || '同').slice(0, 1) }
function formatMessageTime(value) {
  if (!value) return ''
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? String(value).slice(0, 16) : date.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

watch(() => props.mode, (mode) => { if (mode === '消息') loadContacts() })
onMounted(() => { if (props.mode === '消息') loadContacts() })
</script>

<template>
  <section v-if="mode === '通讯录'" class="communication-page contacts-page">
    <div class="communication-heading"><div><p class="eyebrow">CONTACTS</p><h1>查找同事</h1><p>通过姓名、工号或手机号查找权限范围内的同事。</p></div></div>
    <form class="colleague-search" @submit.prevent="searchColleagues"><span>⌕</span><input v-model.trim="colleagueKeyword" placeholder="搜索姓名、工号或手机号" autofocus /><button :disabled="colleagueLoading">{{ colleagueLoading ? '搜索中…' : '搜索' }}</button></form>
    <div v-if="error" class="api-error">{{ error }}</div>
    <div v-if="colleagueLoading" class="communication-empty"><span class="empty-illustration">⌕</span><strong>正在查找同事…</strong></div>
    <div v-else-if="colleagueResults.length" class="colleague-grid">
      <article v-for="person in colleagueResults" :key="person.userId" class="colleague-card">
        <div class="colleague-card-top"><span class="person-avatar large-person">{{ initial(person.realName) }}</span><div><h3>{{ person.realName }}</h3><p>{{ person.positionName || person.roleName || '员工' }}</p></div><span class="department-pill">{{ person.deptName || '未分配部门' }}</span></div>
        <dl><div><dt>工号</dt><dd>{{ person.employeeNo || '—' }}</dd></div><div><dt>性别</dt><dd>{{ person.gender || '—' }}</dd></div><div><dt>手机号</dt><dd>{{ person.phone || '—' }}</dd></div><div><dt>邮箱</dt><dd>{{ person.email || '—' }}</dd></div><div><dt>入职日期</dt><dd>{{ person.hireDate || '—' }}</dd></div></dl>
        <button v-if="canChat" class="contact-message-button" @click="startChat(person)"><span>✉</span> 发消息</button>
      </article>
    </div>
    <div v-else-if="colleagueSearched" class="communication-empty"><span class="empty-illustration">无</span><strong>没有找到相关同事</strong><p>请尝试完整姓名、工号或手机号。</p></div>
    <div v-else class="communication-empty"><span class="empty-illustration">人</span><strong>搜索并联系你的同事</strong><p>可查看部门、职位和联系方式。</p></div>
  </section>

  <section v-else class="chat-page">
    <aside class="chat-contact-panel">
      <div class="chat-panel-heading"><div><h1>消息</h1><span v-if="totalUnread">{{ totalUnread }} 条未读</span></div><button title="刷新联系人" :disabled="loading" @click="loadContacts">↻</button></div>
      <label class="chat-contact-search"><span>⌕</span><input v-model="contactKeyword" placeholder="搜索联系人" /></label>
      <div class="chat-contact-list"><button v-for="contact in filteredContacts" :key="contact.userId" :class="['chat-contact-item', { active: selectedContact?.userId === contact.userId }]" @click="selectContact(contact)"><span class="person-avatar">{{ initial(contact.realName) }}</span><span class="chat-contact-copy"><strong>{{ contact.realName }}</strong><small>{{ contact.lastMessage || `${contact.deptName || ''} ${contact.positionName || contact.roleName || ''}`.trim() || '同事' }}</small></span><span class="chat-contact-meta"><time>{{ contact.lastMessageTime || '' }}</time><b v-if="contact.unreadCount">{{ contact.unreadCount > 99 ? '99+' : contact.unreadCount }}</b></span></button><div v-if="!loading && !filteredContacts.length" class="chat-list-empty">暂无联系人</div></div>
    </aside>
    <section v-if="selectedContact" class="conversation-panel">
      <header class="conversation-heading"><span class="person-avatar">{{ initial(selectedContact.realName) }}</span><div><strong>{{ selectedContact.realName }}</strong><small>{{ selectedContact.deptName || '未分配部门' }} · {{ selectedContact.positionName || selectedContact.roleName || '员工' }}</small></div></header>
      <div v-if="error" class="chat-error">{{ error }}</div>
      <div ref="messagesContainer" class="message-list"><div v-if="loading && !messages.length" class="chat-loading">正在加载聊天记录…</div><div v-else-if="!messages.length" class="conversation-empty"><span>✉</span><strong>开始聊天吧</strong><p>发送一条消息向 {{ selectedContact.realName }} 打个招呼。</p></div><div v-for="message in messages" :key="message.messageId" :class="['message-row', { mine: Number(message.senderId) === Number(currentUser?.userId) }]"><span class="person-avatar message-avatar">{{ initial(message.senderName || (Number(message.senderId) === Number(currentUser?.userId) ? displayName : selectedContact.realName)) }}</span><div class="message-content"><div class="message-bubble">{{ message.content }}</div><time>{{ formatMessageTime(message.createTime) }}</time></div></div></div>
      <form class="message-composer" @submit.prevent="sendMessage"><textarea v-model="draft" maxlength="2000" placeholder="输入消息，Enter 发送，Shift + Enter 换行" @keydown.enter.exact.prevent="sendMessage"></textarea><div><span>{{ draft.length }}/2000</span><button :disabled="sending || !draft.trim()">{{ sending ? '发送中…' : '发送' }}</button></div></form>
    </section>
    <section v-else class="no-conversation"><span>✉</span><h2>选择一个联系人</h2><p>从左侧联系人列表开始聊天。</p></section>
  </section>
</template>
