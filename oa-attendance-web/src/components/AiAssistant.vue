<script setup>
import { nextTick, ref, watch } from 'vue'
import request from '../api/request'

const props = defineProps({
  currentUser: { type: Object, default: null },
})

const open = ref(false)
const messages = ref([])
const input = ref('')
const sending = ref(false)
const chatBody = ref(null)

function toggle() {
  open.value = !open.value
  if (open.value && !messages.value.length) {
    messages.value.push({
      role: 'ai',
      text: '你好！我是小汇 AI 助手。\n\n我可以帮你：\n• 打卡签到/签退\n• 提交请假/加班申请\n• 审批通过或驳回\n• 查询考勤/个人信息\n\n直接用自然语言跟我说吧！',
    })
  }
  scrollBottom()
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  sending.value = true
  scrollBottom()
  try {
    const result = await request.post('/agent/chat', { message: text }, { timeout: 45000 })
    const data = result.data
    messages.value.push({
      role: 'ai',
      text: data.reply,
      intent: data.intent,
      params: data.params,
    })
  } catch (e) {
    messages.value.push({ role: 'ai', text: '抱歉，AI 服务暂时不可用：' + e.message })
  } finally {
    sending.value = false
    scrollBottom()
  }
}

async function confirmAction(msg) {
  if (!msg.params) return
  sending.value = true
  try {
    // Resend with confirmation
    const result = await request.post('/agent/chat', {
      message: '确认提交 ' + JSON.stringify(msg.params),
    }, { timeout: 45000 })
    messages.value.push({ role: 'ai', text: result.data.reply, intent: result.data.intent })
  } catch (e) {
    messages.value.push({ role: 'ai', text: '操作失败：' + e.message })
  } finally {
    sending.value = false
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => {
    const el = chatBody.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

watch(open, (val) => {
  if (val) scrollBottom()
})
</script>

<template>
  <div class="ai-assistant" :class="{ open }">
    <button class="ai-fab" @click="toggle" :title="open ? '关闭助手' : 'AI 助手'">
      <span v-if="!open">AI</span>
      <span v-else>✕</span>
    </button>

    <div v-if="open" class="ai-panel">
      <div class="ai-header">
        <div>
          <strong>小汇 AI 助手</strong>
          <small>智能办公助理</small>
        </div>
        <button @click="open = false">—</button>
      </div>
      <div ref="chatBody" class="ai-body">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['ai-msg', msg.role]">
          <span class="ai-avatar">{{ msg.role === 'ai' ? 'AI' : (currentUser?.realName || '我').slice(0,1) }}</span>
          <div class="ai-bubble">
            <div class="ai-text" v-html="msg.text.replace(/\n/g, '<br>')"></div>
            <div v-if="msg.intent && msg.intent.startsWith('confirm_') && msg.params" class="ai-actions">
              <button class="ai-confirm-btn" @click="confirmAction(msg)">确认提交</button>
              <span class="ai-confirm-hint">或继续输入修改</span>
            </div>
          </div>
        </div>
        <div v-if="sending" class="ai-msg ai"><span class="ai-avatar">AI</span><div class="ai-bubble"><span class="ai-typing">思考中…</span></div></div>
      </div>
      <div class="ai-footer">
        <textarea v-model="input" :disabled="sending" placeholder="输入你想做的事，如：明天请假一天" @keydown="onKeydown" rows="1"></textarea>
        <button :disabled="sending || !input.trim()" @click="send">{{ sending ? '…' : '发送' }}</button>
      </div>
    </div>
  </div>
</template>
