<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const emit = defineEmits(['navigate'])
const opened = ref(false)
const draft = ref('')
const thinking = ref(false)
const messageList = ref(null)
const messages = ref([])
const launcher = ref(null)
const launcherPosition = ref(null)
const dragging = ref(false)
let replyTimer
let nextId = 1
let dragState = null
let suppressLauncherClick = false

const positionStorageKey = 'oa-agent-launcher-position'

const shortcuts = ['我要请假', '帮我补卡', '我要打卡', '搜索制度']

function open() { opened.value = true; scrollToBottom() }
function minimize() { opened.value = false }
function close() { opened.value = false; draft.value = ''; thinking.value = false; messages.value = []; window.clearTimeout(replyTimer) }

async function send(content = draft.value) {
  const text = String(content || '').trim()
  if (!text || thinking.value) return
  messages.value.push({ id: nextId++, role: 'user', type: 'text', content: text })
  draft.value = ''
  thinking.value = true
  await scrollToBottom()
  replyTimer = window.setTimeout(() => {
    messages.value.push(buildMockReply(text))
    thinking.value = false
    scrollToBottom()
  }, 650)
}

function buildMockReply(text) {
  if (/请假|休假|年假|病假|事假/.test(text)) return {
    id: nextId++, role: 'assistant', type: 'action', content: '我识别到你想发起请假申请。',
    action: { kind: 'leave', title: '请假申请', fields: [['请假类型', /病假/.test(text) ? '病假' : /年假/.test(text) ? '年假' : '待确认'], ['时间', '待补充'], ['原因', text]], target: '请假审批' },
  }
  if (/补卡|忘记打卡|漏打卡/.test(text)) return {
    id: nextId++, role: 'assistant', type: 'action', content: '我识别到你想处理异常考勤。',
    action: { kind: 'makeup', title: '补卡申请', fields: [['异常日期', '待选择'], ['补卡类型', '待确认'], ['原因', text]], target: '补卡申请' },
  }
  if (/打卡|签到|签退|下班/.test(text)) return {
    id: nextId++, role: 'assistant', type: 'action', content: '打卡属于敏感操作，需要你确认后再进入考勤页面。',
    action: { kind: 'punch', title: '确认打卡', fields: [['打卡类型', /签退|下班/.test(text) ? '签退' : '签到'], ['当前时间', new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })]], target: '考勤管理' },
  }
  if (/制度|年假|加班规定|员工手册/.test(text)) return {
    id: nextId++, role: 'assistant', type: 'source', content: '当前是前端 Mock 演示。接入知识库接口后，这里会展示基于制度原文生成的回答，并标注引用来源。', target: '文档',
  }
  return { id: nextId++, role: 'assistant', type: 'text', content: '当前是 Agent 前端交互演示。我可以演示请假、补卡、打卡和制度检索，业务执行接口尚未接入。' }
}

function confirmAction(action) {
  messages.value.push({ id: nextId++, role: 'assistant', type: 'text', content: '当前为前端 Mock 演示，没有提交真实业务。你可以点击“打开页面”继续操作。' })
  scrollToBottom()
}
function cancelAction(message) { message.cancelled = true }
function navigate(target) { minimize(); emit('navigate', target) }

function clampPosition(left, top) {
  const width = launcher.value?.offsetWidth || 104
  const height = launcher.value?.offsetHeight || 46
  const padding = 12
  return {
    left: Math.min(Math.max(padding, left), Math.max(padding, window.innerWidth - width - padding)),
    top: Math.min(Math.max(padding, top), Math.max(padding, window.innerHeight - height - padding)),
  }
}

function restoreLauncherPosition() {
  try {
    const saved = JSON.parse(window.localStorage.getItem(positionStorageKey) || 'null')
    if (Number.isFinite(saved?.left) && Number.isFinite(saved?.top)) {
      launcherPosition.value = clampPosition(saved.left, saved.top)
      return
    }
  } catch (_) { /* 忽略损坏的本地位置记录 */ }
  const width = launcher.value?.offsetWidth || 104
  const height = launcher.value?.offsetHeight || 46
  launcherPosition.value = clampPosition(window.innerWidth - width - 28, window.innerHeight - height - 28)
}

function launcherStyle() {
  if (!launcherPosition.value) return undefined
  return { left: `${launcherPosition.value.left}px`, top: `${launcherPosition.value.top}px`, right: 'auto', bottom: 'auto' }
}

function startDrag(event) {
  if (event.button !== 0) return
  const rect = event.currentTarget.getBoundingClientRect()
  dragState = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, left: rect.left, top: rect.top }
  event.currentTarget.setPointerCapture(event.pointerId)
}

function moveDrag(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY
  if (!dragging.value && Math.hypot(deltaX, deltaY) < 5) return
  dragging.value = true
  suppressLauncherClick = true
  launcherPosition.value = clampPosition(dragState.left + deltaX, dragState.top + deltaY)
}

function endDrag(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
  if (dragging.value && launcherPosition.value) {
    window.localStorage.setItem(positionStorageKey, JSON.stringify(launcherPosition.value))
  }
  dragState = null
  dragging.value = false
  window.setTimeout(() => { suppressLauncherClick = false }, 0)
}

function handleLauncherClick() {
  if (!suppressLauncherClick) open()
}

function keepLauncherInViewport() {
  if (launcherPosition.value) launcherPosition.value = clampPosition(launcherPosition.value.left, launcherPosition.value.top)
}

async function scrollToBottom() { await nextTick(); if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight }

defineExpose({ open })
onMounted(async () => {
  await nextTick()
  restoreLauncherPosition()
  window.addEventListener('resize', keepLauncherInViewport)
})
onBeforeUnmount(() => {
  window.clearTimeout(replyTimer)
  window.removeEventListener('resize', keepLauncherInViewport)
})
</script>

<template>
  <button v-if="!opened" ref="launcher" :class="['agent-launcher', { dragging }]" :style="launcherStyle()" title="拖动可调整位置，点击打开智汇AI助手" @click="handleLauncherClick" @pointerdown="startDrag" @pointermove="moveDrag" @pointerup="endDrag" @pointercancel="endDrag"><span class="launcher-icon">✦</span><strong>AI 助手</strong></button>
  <aside v-else class="agent-drawer" aria-label="智汇AI助手">
    <header class="agent-header"><div><span class="agent-logo">AI</span><div><strong>智汇AI助手</strong><small>前端 Mock 演示</small></div></div><div><button title="最小化" @click="minimize">−</button><button title="关闭并清空" @click="close">×</button></div></header>
    <div ref="messageList" class="agent-messages">
      <section v-if="!messages.length" class="agent-welcome"><span class="agent-welcome-icon">✦</span><h2>你好，我是智汇AI助手</h2><p>可以用一句话描述你想办理的事情。</p><div class="agent-shortcuts"><button v-for="item in shortcuts" :key="item" @click="send(item)">{{ item }}<span>›</span></button></div></section>
      <div v-for="message in messages" :key="message.id" :class="['agent-message', message.role]">
        <span v-if="message.role === 'assistant'" class="agent-avatar">AI</span>
        <div class="agent-message-body"><p class="agent-bubble">{{ message.content }}</p>
          <article v-if="message.type === 'action'" :class="['agent-action-card', { cancelled: message.cancelled }]">
            <header><span>{{ message.action.kind === 'leave' ? '假' : message.action.kind === 'makeup' ? '补' : '勤' }}</span><div><strong>{{ message.action.title }}</strong><small>{{ message.cancelled ? '已取消' : '等待确认' }}</small></div></header>
            <dl><div v-for="field in message.action.fields" :key="field[0]"><dt>{{ field[0] }}</dt><dd>{{ field[1] }}</dd></div></dl>
            <div v-if="!message.cancelled" class="agent-card-actions"><button @click="cancelAction(message)">取消</button><button @click="navigate(message.action.target)">打开页面</button><button class="primary" @click="confirmAction(message.action)">确认</button></div>
          </article>
          <button v-if="message.type === 'source'" class="agent-source" @click="navigate(message.target)"><span>文</span><div><strong>查看制度文档</strong><small>接入后将定位到引用章节</small></div><b>›</b></button>
        </div>
      </div>
      <div v-if="thinking" class="agent-message assistant"><span class="agent-avatar">AI</span><div class="agent-thinking"><i></i><i></i><i></i><span>思考中</span></div></div>
    </div>
    <form class="agent-composer" @submit.prevent="send()"><textarea v-model="draft" maxlength="1000" placeholder="输入你想办理的事情…" @keydown.enter.exact.prevent="send()"></textarea><div><span>Agent 接口待接入</span><button :disabled="thinking || !draft.trim()">发送</button></div></form>
  </aside>
</template>

<style scoped>
.agent-launcher { position: fixed; right: 28px; bottom: 28px; z-index: 45; height: 46px; display: flex; align-items: center; gap: 9px; padding: 0 16px 0 8px; border: 1px solid #dfe5f2; border-radius: 23px; color: #30343a; background: rgba(255,255,255,.96); box-shadow: 0 8px 24px rgba(31,35,41,.12); cursor: grab; transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease; backdrop-filter: blur(10px); touch-action: none; user-select: none; -webkit-user-select: none; }
.agent-launcher:hover { transform: translateY(-2px); border-color: #a9c0ff; box-shadow: 0 12px 30px rgba(51,112,255,.2); }.agent-launcher:active { transform: translateY(0); }.launcher-icon { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: #3370ff; font-size: 14px; box-shadow: 0 4px 10px rgba(51,112,255,.24); }.agent-launcher strong { font-size: 12px; font-weight: 600; white-space: nowrap; }
.agent-launcher.dragging { cursor: grabbing; transform: none; transition: none; box-shadow: 0 14px 34px rgba(51,112,255,.25); }
.agent-drawer { position: fixed; right: 22px; bottom: 22px; z-index: 46; width: min(400px, calc(100vw - 32px)); height: min(70vh, 680px); min-height: 480px; display: flex; flex-direction: column; overflow: hidden; border: 1px solid #e2e6ee; border-radius: 20px; background: #f7f8fa; box-shadow: 0 26px 80px rgba(31,35,41,.23); }
.agent-header { min-height: 66px; flex: 0 0 66px; display: flex; align-items: center; justify-content: space-between; padding: 0 14px 0 17px; border-bottom: 1px solid #e8eaed; background: #fff; }.agent-header > div { display: flex; align-items: center; gap: 10px; }.agent-logo, .agent-avatar { display: grid; place-items: center; color: #fff; background: linear-gradient(145deg, #3370ff, #7b61ff); font-weight: 800; }.agent-logo { width: 36px; height: 36px; border-radius: 11px; font-size: 11px; }.agent-header strong, .agent-header small { display: block; }.agent-header strong { font-size: 14px; }.agent-header small { margin-top: 3px; color: #8f959e; font-size: 9px; }.agent-header button { width: 30px; height: 30px; border: 0; border-radius: 8px; color: #7b8088; background: transparent; font-size: 19px; cursor: pointer; }.agent-header button:hover { background: #f2f3f5; }
.agent-messages { min-height: 0; flex: 1 1 auto; overflow-y: auto; padding: 18px; scroll-behavior: smooth; }.agent-welcome { padding: 17px 4px 8px; text-align: center; }.agent-welcome-icon { width: 50px; height: 50px; display: grid; place-items: center; margin: 0 auto; border-radius: 16px; color: #3370ff; background: #eaf0ff; font-size: 21px; }.agent-welcome h2 { margin: 13px 0 6px; font-size: 17px; }.agent-welcome p { margin: 0 0 18px; color: #8f959e; font-size: 11px; }.agent-shortcuts { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }.agent-shortcuts button { display: flex; justify-content: space-between; padding: 10px 11px; border: 1px solid #e2e6ee; border-radius: 10px; color: #50555d; background: #fff; font-size: 11px; text-align: left; cursor: pointer; }.agent-shortcuts button:hover { border-color: #aac0ff; color: #3370ff; }
.agent-message { display: flex; align-items: flex-start; gap: 8px; margin: 14px 0; }.agent-message.user { justify-content: flex-end; }.agent-avatar { width: 30px; height: 30px; flex: 0 0 30px; border-radius: 10px; font-size: 9px; }.agent-message-body { max-width: 84%; }.agent-bubble { margin: 0; padding: 9px 11px; border: 1px solid #e3e6eb; border-radius: 4px 12px 12px 12px; color: #3f444b; background: #fff; font-size: 11px; line-height: 1.6; }.user .agent-bubble { border-color: #3370ff; border-radius: 12px 4px 12px 12px; color: #fff; background: #3370ff; }
.agent-action-card { margin-top: 8px; overflow: hidden; border: 1px solid #dfe4ec; border-radius: 13px; background: #fff; }.agent-action-card.cancelled { opacity: .62; }.agent-action-card header { display: flex; align-items: center; gap: 9px; padding: 12px; border-bottom: 1px solid #eef0f3; }.agent-action-card header > span { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 10px; color: #3370ff; background: #edf3ff; font-size: 11px; }.agent-action-card header strong, .agent-action-card header small { display: block; }.agent-action-card header strong { font-size: 12px; }.agent-action-card header small { margin-top: 3px; color: #8f959e; font-size: 9px; }.agent-action-card dl { margin: 0; padding: 10px 12px; }.agent-action-card dl div { display: grid; grid-template-columns: 64px 1fr; gap: 7px; padding: 5px 0; }.agent-action-card dt { color: #8f959e; font-size: 9px; }.agent-action-card dd { margin: 0; color: #3f444b; font-size: 10px; word-break: break-word; }.agent-card-actions { display: flex; justify-content: flex-end; gap: 6px; padding: 9px 10px; border-top: 1px solid #eef0f3; }.agent-card-actions button { height: 29px; padding: 0 10px; border: 1px solid #dfe3e9; border-radius: 8px; color: #50555d; background: #fff; font-size: 9px; cursor: pointer; }.agent-card-actions button.primary { border-color: #3370ff; color: #fff; background: #3370ff; }
.agent-source { width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 9px; margin-top: 8px; padding: 10px; border: 1px solid #dfe4ec; border-radius: 11px; background: #fff; text-align: left; cursor: pointer; }.agent-source > span { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 9px; color: #3370ff; background: #edf3ff; }.agent-source strong, .agent-source small { display: block; }.agent-source strong { font-size: 10px; }.agent-source small { margin-top: 3px; color: #8f959e; font-size: 8px; }.agent-source b { color: #9ca1a9; }.agent-thinking { display: flex; align-items: center; gap: 4px; padding: 10px 12px; border-radius: 4px 12px 12px 12px; background: #fff; }.agent-thinking i { width: 5px; height: 5px; border-radius: 50%; background: #7f8ba3; animation: agent-pulse 1s infinite alternate; }.agent-thinking i:nth-child(2) { animation-delay: .2s; }.agent-thinking i:nth-child(3) { animation-delay: .4s; }.agent-thinking span { margin-left: 4px; color: #8f959e; font-size: 9px; }
.agent-composer { flex: 0 0 auto; margin: 0 12px 12px; padding: 9px 10px; border: 1px solid #dfe3e9; border-radius: 13px; background: #fff; box-shadow: 0 7px 24px rgba(31,35,41,.06); }.agent-composer textarea { width: 100%; height: 54px; resize: none; border: 0; outline: 0; color: #30343a; background: transparent; font-size: 11px; line-height: 1.5; }.agent-composer > div { display: flex; align-items: center; justify-content: space-between; }.agent-composer span { color: #b1b5bb; font-size: 8px; }.agent-composer button { height: 30px; padding: 0 14px; border: 0; border-radius: 8px; color: #fff; background: #3370ff; font-size: 10px; cursor: pointer; }.agent-composer button:disabled { opacity: .5; cursor: not-allowed; }
@keyframes agent-pulse { to { transform: translateY(-3px); opacity: .45; } }
@media (max-width: 760px) { .agent-launcher { right: 16px; bottom: 16px; }.agent-drawer { inset: 72px 10px 10px auto; width: calc(100vw - 20px); height: auto; min-height: 0; }.agent-shortcuts { grid-template-columns: 1fr; } }
</style>
