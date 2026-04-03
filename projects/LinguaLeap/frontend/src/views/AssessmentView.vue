<template>
  <div class="assessment-page">
    <!-- 已完成评估 -->
    <div v-if="completed" class="completed-card">
      <div class="completed-icon">🎉</div>
      <h2>入学评估已完成</h2>
      <p>Lily 老师已经了解你的水平啦！</p>
      <div class="profile-card" v-if="profile">
        <div class="profile-row">
          <span class="label">词汇水平</span>
          <span class="level-badge">{{ levelLabel(profile.vocabularyLevel) }}</span>
        </div>
        <div class="profile-row">
          <span class="label">语法水平</span>
          <span class="level-badge">{{ levelLabel(profile.grammarLevel) }}</span>
        </div>
        <div class="profile-row" v-if="profile.aiAssessment">
          <p class="assessment-text">{{ profile.aiAssessment }}</p>
        </div>
      </div>
      <button class="btn-primary" @click="goToChat">开始和 Lily 老师聊天 🌸</button>
    </div>

    <!-- 评估进行中 -->
    <div v-else class="assessment-chat">
      <div class="assess-header">
        <div class="teacher-avatar">🌸</div>
        <div>
          <h2>Lily 老师 · 入学评估</h2>
          <p class="phase-hint">{{ phaseHint }}</p>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="loadingInit" class="loading-hint">Lily 老师正在准备中...</div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="msg-row"
          :class="msg.role"
        >
          <div v-if="msg.role === 'assistant'" class="avatar teacher">🌸</div>
          <div class="msg-bubble" :class="msg.role">
            <div class="msg-content" v-html="renderContent(msg.content)" />
            <!-- 题目卡片 -->
            <div v-if="msg.quiz" class="quiz-card">
              <p class="quiz-stem">{{ msg.quiz.stem }}</p>
              <div v-if="msg.quiz.options" class="quiz-options">
                <button
                  v-for="(opt, oi) in msg.quiz.options"
                  :key="oi"
                  class="quiz-option"
                  :class="{
                    selected: msg.selectedAnswer === opt,
                    correct: msg.answered && opt === msg.quiz.answer,
                    wrong: msg.answered && msg.selectedAnswer === opt && opt !== msg.quiz.answer
                  }"
                  :disabled="msg.answered"
                  @click="selectAnswer(idx, opt)"
                >
                  <span class="opt-letter">{{ ['A','B','C','D'][oi] }}</span>
                  <span>{{ opt }}</span>
                </button>
              </div>
              <div v-else class="quiz-input-wrap">
                <input
                  v-model="msg.inputAnswer"
                  placeholder="输入你的答案..."
                  :disabled="msg.answered"
                  @keydown.enter="submitInputAnswer(idx)"
                />
                <button v-if="!msg.answered" @click="submitInputAnswer(idx)">提交</button>
              </div>
            </div>
          </div>
          <div v-if="msg.role === 'user'" class="avatar user">{{ userInitial }}</div>
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
          placeholder="输入你的回答..."
          rows="1"
          :disabled="thinking || completed"
          ref="inputRef"
        />
        <button class="send-btn" @click="send" :disabled="!inputText.trim() || thinking">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 2L11 13" /><path d="M22 2L15 22L11 13L2 9L22 2Z" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { assessmentApi, type StudentProfile } from '@/api/teacher'
import { useUserStore } from '@/stores/user'

interface LocalMsg {
  role: 'user' | 'assistant'
  content: string
  quiz?: { stem: string; options: string[] | null; answer: string; type: string; difficulty: number }
  selectedAnswer?: string
  inputAnswer?: string
  answered?: boolean
}

const router = useRouter()
const userStore = useUserStore()

const messages = ref<LocalMsg[]>([])
const inputText = ref('')
const thinking = ref(false)
const loadingInit = ref(false)
const completed = ref(false)
const profile = ref<StudentProfile | null>(null)
const sessionId = ref<number | null>(null)
const phase = ref<string>('welcome')
const messagesContainer = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()

const userInitial = computed(() => {
  const name = userStore.user?.displayName || userStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const phaseHint = computed(() => {
  switch (phase.value) {
    case 'welcome': return '让我们先互相认识一下 😊'
    case 'quiz': return '做几个小测试，别紧张哦～'
    case 'complete': return '评估完成！'
    default: return ''
  }
})

onMounted(async () => {
  // 检查是否已评估
  try {
    const statusRes = await assessmentApi.status()
    if (statusRes.data.data.assessed) {
      completed.value = true
      const profileRes = await assessmentApi.getProfile()
      profile.value = profileRes.data.data
      return
    }
  } catch (e) {
    // 忽略
  }

  // 开始评估
  loadingInit.value = true
  try {
    const grade = userStore.user?.grade || 'junior'
    const res = await assessmentApi.start(grade)
    sessionId.value = res.data.data.sessionId
    const msg: LocalMsg = { role: 'assistant', content: res.data.data.reply }
    if (res.data.data.quizData) {
      try { msg.quiz = JSON.parse(res.data.data.quizData) } catch (e) { /* ignore */ }
    }
    messages.value.push(msg)
    phase.value = res.data.data.phase
  } catch (e: any) {
    if (e.response?.data?.message === '已完成入学评估') {
      completed.value = true
      const profileRes = await assessmentApi.getProfile()
      profile.value = profileRes.data.data
    } else {
      messages.value.push({ role: 'assistant', content: '抱歉，遇到了一个小问题，请刷新页面重试。' })
    }
  } finally {
    loadingInit.value = false
    await nextTick()
    scrollToBottom()
  }
})

async function send() {
  const text = inputText.value.trim()
  if (!text || !sessionId.value || thinking.value) return

  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  thinking.value = true
  await nextTick()
  scrollToBottom()

  try {
    const grade = userStore.user?.grade || 'junior'
    const res = await assessmentApi.chat(sessionId.value, text, grade)
    const replyMsg: LocalMsg = { role: 'assistant', content: res.data.data.reply }
    if (res.data.data.quizData) {
      try { replyMsg.quiz = JSON.parse(res.data.data.quizData) } catch (e) { /* ignore */ }
    }
    messages.value.push(replyMsg)
    phase.value = res.data.data.phase

    if (res.data.data.phase === 'complete') {
      completed.value = true
      // 加载画像
      try {
        const profileRes = await assessmentApi.getProfile()
        profile.value = profileRes.data.data
      } catch (e) {
        // 画像可能在评估回复后才保存完
      }
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: 'Lily 老师遇到了一点问题，请再试一次～ 🌸' })
  } finally {
    thinking.value = false
    await nextTick()
    scrollToBottom()
    inputRef.value?.focus()
  }
}

function goToChat() {
  router.push('/chat')
}

async function selectAnswer(msgIdx: number, answer: string) {
  const msg = messages.value[msgIdx]
  if (!msg || msg.answered) return
  msg.selectedAnswer = answer
  msg.answered = true
  // 自动发送答案给 AI
  await nextTick()
  inputText.value = answer
  await send()
}

async function submitInputAnswer(msgIdx: number) {
  const msg = messages.value[msgIdx]
  if (!msg || msg.answered || !msg.inputAnswer?.trim()) return
  msg.answered = true
  msg.selectedAnswer = msg.inputAnswer.trim()
  await nextTick()
  inputText.value = msg.inputAnswer.trim()
  await send()
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

function levelLabel(level: string): string {
  const map: Record<string, string> = {
    beginner: '入门',
    elementary: '基础',
    intermediate: '中级',
    upper: '中高级',
    advanced: '高级',
  }
  return map[level] || level || '未评估'
}
</script>

<style scoped lang="scss">
.assessment-page {
  max-width: 800px;
  margin: 0 auto;
  height: calc(100vh - 60px);
}

.completed-card {
  text-align: center;
  padding: 60px 20px;

  .completed-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }

  h2 {
    font-size: 24px;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  > p {
    color: var(--text-secondary);
    margin-bottom: 32px;
  }

  .profile-card {
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 24px;
    margin: 0 auto 32px;
    max-width: 400px;
    text-align: left;

    .profile-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;

      &:not(:last-child) {
        border-bottom: 1px solid var(--border);
      }

      .label {
        color: var(--text-secondary);
        font-size: 14px;
      }

      .level-badge {
        background: var(--primary-light, #eef2ff);
        color: var(--primary);
        padding: 2px 12px;
        border-radius: 12px;
        font-size: 13px;
        font-weight: 500;
      }
    }

    .assessment-text {
      font-size: 14px;
      line-height: 1.6;
      color: var(--text-primary);
      padding: 8px 0;
    }
  }

  .btn-primary {
    background: var(--primary);
    color: white;
    border: none;
    padding: 12px 32px;
    border-radius: 24px;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
    }
  }
}

.assessment-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.assess-header {
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

  h2 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .phase-hint {
    font-size: 13px;
    color: var(--text-muted);
    margin: 2px 0 0;
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
    padding: 40px;
  }
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;

  &.user { flex-direction: row-reverse; }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    flex-shrink: 0;

    &.teacher { background: linear-gradient(135deg, #fce4ec, #f8bbd0); }
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
  }
}

// ---- Quiz card ----
.quiz-card {
  margin-top: 12px;
  padding: 16px;
  background: var(--bg-page);
  border-radius: 12px;
  border: 1px solid var(--border);

  .quiz-stem {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    margin: 0 0 12px;
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
    border: 1.5px solid var(--border);
    border-radius: 10px;
    background: var(--bg-card);
    cursor: pointer;
    font-size: 14px;
    color: var(--text-primary);
    transition: all 0.2s;
    text-align: left;

    .opt-letter {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--bg-page);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      flex-shrink: 0;
    }

    &:hover:not(:disabled) {
      border-color: var(--primary);
      background: rgba(79, 70, 229, 0.04);
    }

    &.selected { border-color: var(--primary); background: rgba(79, 70, 229, 0.08); }
    &.correct { border-color: #10b981; background: rgba(16, 185, 129, 0.08); color: #047857; }
    &.wrong { border-color: #ef4444; background: rgba(239, 68, 68, 0.08); color: #b91c1c; }

    &:disabled { cursor: default; opacity: 0.85; }
  }

  .quiz-input-wrap {
    display: flex;
    gap: 8px;

    input {
      flex: 1;
      padding: 10px 14px;
      border: 1.5px solid var(--border);
      border-radius: 10px;
      font-size: 14px;
      background: var(--bg-card);
      color: var(--text-primary);
      outline: none;

      &:focus { border-color: var(--primary); }
    }

    button {
      padding: 10px 18px;
      border: none;
      border-radius: 10px;
      background: var(--primary);
      color: white;
      font-size: 14px;
      cursor: pointer;

      &:hover { opacity: 0.9; }
    }
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
</style>
