<template>
  <div class="chat-page">
    <div class="chat-header">
      <div class="teacher-avatar">🌸</div>
      <div class="teacher-info">
        <h2>Lily 老师</h2>
        <span class="status-dot" />
        <span class="status-text">在线</span>
      </div>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div v-if="loading" class="loading-hint">加载中...</div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        class="msg-row"
        :class="msg.role"
      >
        <div v-if="msg.role === 'assistant'" class="avatar teacher">🌸</div>
        <div class="msg-bubble" :class="msg.role">
          <div class="msg-content" v-html="renderContent(msg.content)" />
          <!-- Quiz card -->
          <div v-if="msg.quiz" class="quiz-card">
            <div class="quiz-stem">{{ msg.quiz.stem }}</div>
            <div v-if="msg.quiz.options?.length" class="quiz-options">
              <button
                v-for="(opt, idx) in msg.quiz.options"
                :key="idx"
                class="quiz-option"
                :class="{
                  selected: msg.selectedAnswer === opt,
                  correct: msg.answered && opt === msg.quiz.answer,
                  wrong: msg.answered && msg.selectedAnswer === opt && opt !== msg.quiz.answer,
                }"
                :disabled="msg.answered"
                @click="selectAnswer(msg, opt)"
              >
                <span class="opt-label">{{ String.fromCharCode(65 + idx) }}</span>
                <span>{{ opt }}</span>
              </button>
            </div>
            <div v-else class="quiz-input-wrap">
              <input
                v-model="msg.inputAnswer"
                placeholder="输入你的答案..."
                :disabled="msg.answered"
                @keydown.enter="submitInputAnswer(msg)"
              />
              <button v-if="!msg.answered" @click="submitInputAnswer(msg)">提交</button>
            </div>
          </div>
          <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        <div v-if="msg.role === 'user'" class="avatar user">
          {{ userInitial }}
        </div>
      </div>

      <div v-if="thinking" class="msg-row assistant">
        <div class="avatar teacher">🌸</div>
        <div class="msg-bubble assistant thinking">
          <span class="dot" /><span class="dot" /><span class="dot" />
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <textarea
        v-model="inputText"
        @keydown.enter.exact.prevent="send"
        placeholder="输入消息..."
        rows="1"
        :disabled="thinking"
        ref="inputRef"
      />
      <button class="send-btn" @click="send" :disabled="!inputText.trim() || thinking">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13" /><path d="M22 2L15 22L11 13L2 9L22 2Z" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { teacherApi, type ChatMessage } from '@/api/teacher'
import { useUserStore } from '@/stores/user'

interface QuizData {
  stem: string
  type: string
  options?: string[]
  answer?: string
}

interface LocalMsg {
  id: number
  role: string
  content: string
  msgType: string
  createdAt: string
  quiz?: QuizData
  selectedAnswer?: string
  inputAnswer?: string
  answered?: boolean
}

function parseQuizData(raw?: string): QuizData | undefined {
  if (!raw) return undefined
  try { return JSON.parse(raw) } catch { return undefined }
}

function parseHistoryMetadata(msg: ChatMessage): LocalMsg {
  const local: LocalMsg = { ...msg }
  if (msg.msgType === 'quiz' && msg.metadata) {
    local.quiz = parseQuizData(msg.metadata)
    local.answered = true // 历史消息视为已答
  }
  return local
}

const userStore = useUserStore()
const messages = ref<LocalMsg[]>([])
const inputText = ref('')
const loading = ref(false)
const thinking = ref(false)
const sessionId = ref<number | null>(null)
const messagesContainer = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()

const userInitial = computed(() => {
  const name = userStore.user?.displayName || userStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

onMounted(async () => {
  loading.value = true
  try {
    // 获取或创建对话
    const res = await teacherApi.getOrCreateSession('chat')
    sessionId.value = res.data.data.id

    // 加载历史消息
    const historyRes = await teacherApi.getHistory(sessionId.value)
    messages.value = (historyRes.data.data || []).map(parseHistoryMetadata)

    // 如果没有历史消息，AI 先打招呼
    if (messages.value.length === 0) {
      thinking.value = true
      await nextTick()
      scrollToBottom()
      const reply = await teacherApi.sendMessage(sessionId.value, '你好', userStore.user?.grade)
      const replyData = reply.data.data
      messages.value.push(
        { id: Date.now() - 1, role: 'user', content: '你好', msgType: 'text', createdAt: new Date().toISOString() } as LocalMsg,
        { id: Date.now(), role: 'assistant', content: replyData.reply, msgType: replyData.msgType || 'text', createdAt: new Date().toISOString(), quiz: parseQuizData(replyData.quizData) } as LocalMsg,
      )
      thinking.value = false
    }
  } catch (e) {
    console.error('加载对话失败', e)
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
})

async function send() {
  const text = inputText.value.trim()
  if (!text || !sessionId.value || thinking.value) return

  inputText.value = ''
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: text,
    msgType: 'text',
    createdAt: new Date().toISOString(),
  } as LocalMsg)

  thinking.value = true
  await nextTick()
  scrollToBottom()

  try {
    const res = await teacherApi.sendMessage(sessionId.value, text, userStore.user?.grade)
    const rd = res.data.data
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: rd.reply,
      msgType: rd.msgType || 'text',
      createdAt: new Date().toISOString(),
      quiz: parseQuizData(rd.quizData),
    } as LocalMsg)
  } catch (e: any) {
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，Lily 老师遇到了一点问题，请稍后再试～ 🌸',
      msgType: 'text',
      createdAt: new Date().toISOString(),
    } as LocalMsg)
  } finally {
    thinking.value = false
    await nextTick()
    scrollToBottom()
    inputRef.value?.focus()
  }
}

function selectAnswer(msg: LocalMsg, opt: string) {
  if (msg.answered) return
  msg.selectedAnswer = opt
  msg.answered = true
  // 自动将答案发送给 AI 进行反馈
  if (sessionId.value) {
    inputText.value = opt
    send()
  }
}

function submitInputAnswer(msg: LocalMsg) {
  if (msg.answered || !msg.inputAnswer?.trim()) return
  msg.answered = true
  if (sessionId.value) {
    inputText.value = msg.inputAnswer.trim()
    send()
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function renderContent(content: string): string {
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
}

function formatTime(ts: string): string {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  max-width: 800px;
  margin: 0 auto;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);

  .teacher-avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, #fce4ec, #f8bbd0);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
  }

  .teacher-info {
    h2 {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0;
    }

    .status-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #10b981;
      margin-right: 4px;
    }

    .status-text {
      font-size: 12px;
      color: var(--text-muted);
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .loading-hint {
    text-align: center;
    color: var(--text-muted);
    padding: 20px;
  }
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;

  &.user {
    flex-direction: row-reverse;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    flex-shrink: 0;

    &.teacher {
      background: linear-gradient(135deg, #fce4ec, #f8bbd0);
    }

    &.user {
      background: var(--primary);
      color: white;
      font-size: 14px;
      font-weight: 600;
    }
  }
}

.msg-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;

  &.assistant {
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-bottom-left-radius: 4px;
  }

  &.user {
    background: var(--primary);
    color: white;
    border-bottom-right-radius: 4px;
  }

  &.thinking {
    padding: 12px 20px;
    display: flex;
    gap: 4px;
    align-items: center;

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--text-muted);
      animation: bounce 1.4s ease-in-out infinite;

      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }

  .msg-content {
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;

    :deep(code) {
      background: rgba(0, 0, 0, 0.06);
      padding: 1px 6px;
      border-radius: 4px;
      font-family: 'SF Mono', monospace;
      font-size: 13px;
    }

    :deep(strong) {
      font-weight: 600;
    }
  }

  .msg-time {
    font-size: 11px;
    margin-top: 4px;
    opacity: 0.5;
  }

  &.user .msg-time {
    text-align: right;
    color: rgba(255, 255, 255, 0.7);
  }
}

.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
  background: var(--bg-card);

  textarea {
    flex: 1;
    resize: none;
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 10px 16px;
    font-size: 14px;
    line-height: 1.5;
    background: var(--bg-page);
    color: var(--text-primary);
    outline: none;
    max-height: 120px;
    font-family: inherit;

    &:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
    }
  }

  .send-btn {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: none;
    background: var(--primary);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      background: var(--primary-dark, #4338ca);
      transform: scale(1.05);
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-8px); }
}

/* Quiz card */
.quiz-card {
  margin-top: 10px;
  padding: 12px;
  background: var(--bg-page);
  border-radius: 12px;
  border: 1px solid var(--border);

  .quiz-stem {
    font-weight: 600;
    margin-bottom: 10px;
    font-size: 14px;
    line-height: 1.5;
  }

  .quiz-options {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .quiz-option {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 14px;
    border-radius: 10px;
    border: 1px solid var(--border);
    background: var(--bg-card);
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
    text-align: left;
    color: var(--text-primary);

    .opt-label {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--bg-page);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 12px;
      flex-shrink: 0;
    }

    &:hover:not(:disabled) {
      border-color: var(--primary);
      background: rgba(79, 70, 229, 0.04);
    }

    &.selected { border-color: var(--primary); background: rgba(79, 70, 229, 0.08); }
    &.correct { border-color: #10b981; background: rgba(16, 185, 129, 0.08); color: #047857; }
    &.wrong { border-color: #ef4444; background: rgba(239, 68, 68, 0.08); color: #dc2626; }
    &:disabled { cursor: default; }
  }

  .quiz-input-wrap {
    display: flex;
    gap: 8px;

    input {
      flex: 1;
      padding: 8px 12px;
      border: 1px solid var(--border);
      border-radius: 8px;
      font-size: 14px;
      background: var(--bg-card);
      color: var(--text-primary);
    }

    button {
      padding: 8px 16px;
      border: none;
      border-radius: 8px;
      background: var(--primary);
      color: white;
      cursor: pointer;
      font-size: 14px;

      &:hover { opacity: 0.9; }
    }
  }
}
</style>
