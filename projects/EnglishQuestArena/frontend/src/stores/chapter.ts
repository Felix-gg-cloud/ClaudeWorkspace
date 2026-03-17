import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ChapterProgress, ChapterPhase, Lesson, BossConfig, Task } from '@/types'
import { chapterConfigs, chapterCampMaps } from '@/mock/chapters'
import type { CampMapData } from '@/game/config/mapData'
import http from '@/api/http'

const OPENED_KEY_PREFIX = 'eqa_camp_opened_'
let currentUserId = 0

function openedKey() {
  return OPENED_KEY_PREFIX + currentUserId
}

function loadOpened(): Record<string, string[]> {
  try {
    const raw = localStorage.getItem(openedKey())
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return {}
}

export const useChapterStore = defineStore('chapter', () => {
  const currentChapterCode = ref('PRE_A1_CH1')
  const progressMap = ref<Record<string, ChapterProgress>>({})
  const lessonsCache = ref<Record<string, Lesson[]>>({})
  const questTasksCache = ref<Task[]>([])
  const bossDataCache = ref<{ config: BossConfig; tasks: Task[] } | null>(null)
  const campOpened = ref<Record<string, string[]>>(loadOpened())
  const loading = ref(false)

  // ---- Getters ----

  const allChapters = computed(() => chapterConfigs)

  const currentChapter = computed(() =>
    chapterConfigs.find(c => c.code === currentChapterCode.value)!
  )

  const currentProgress = computed(() =>
    progressMap.value[currentChapterCode.value]
  )

  const currentPhase = computed<ChapterPhase>(() =>
    currentProgress.value?.phase ?? 'locked'
  )

  const currentLessons = computed<Lesson[]>(() =>
    lessonsCache.value[currentChapterCode.value] ?? []
  )

  const currentCampMap = computed<CampMapData>(() =>
    chapterCampMaps[currentChapterCode.value]
  )

  const currentBossConfig = computed<BossConfig>(() =>
    bossDataCache.value?.config ?? currentChapter.value.bossConfig
  )

  const currentBossTasks = computed<Task[]>(() =>
    bossDataCache.value?.tasks ?? []
  )

  const currentQuestDayIndex = computed<number>(() => {
    const completed = currentProgress.value?.questDaysCompleted ?? []
    const lessons = currentLessons.value
    const nextLesson = lessons.find(l => !completed.includes(l.dayIndex))
    return nextLesson?.dayIndex ?? 1
  })

  const currentQuestTasks = computed<Task[]>(() => questTasksCache.value)

  const campCompletionRate = computed(() => {
    const map = currentCampMap.value
    if (!map) return 0
    const monsters = map.encounters.filter(e => e.type === 'monster')
    if (monsters.length === 0) return 1
    const prog = currentProgress.value
    const defeated = prog ? prog.campDefeated.length : 0
    return defeated / monsters.length
  })

  const isQuestUnlocked = computed(() => {
    const phase = currentPhase.value
    if (phase === 'quest' || phase === 'boss' || phase === 'completed') return true
    return campCompletionRate.value >= (currentChapter.value?.campUnlockRate ?? 0.8)
  })

  const isBossUnlocked = computed(() => {
    const phase = currentPhase.value
    if (phase === 'boss' || phase === 'completed') return true
    if (phase !== 'quest') return false
    const lessons = currentLessons.value
    const completed = currentProgress.value?.questDaysCompleted ?? []
    return lessons.length > 0 && completed.length >= lessons.length
  })

  const allLearnedWords = computed(() => {
    const words: typeof chapterConfigs[0]['words'] = []
    for (const ch of chapterConfigs) {
      const prog = progressMap.value[ch.code]
      if (!prog) continue
      if (prog.phase !== 'locked') {
        words.push(...ch.words)
      }
    }
    return words
  })

  // ---- API Actions ----

  /** 从后端加载全部章节进度 */
  async function loadChapters() {
    try {
      const { data } = await http.get('/chapters')
      const map: Record<string, ChapterProgress> = {}
      for (const ch of data as Record<string, unknown>[]) {
        map[ch.code as string] = {
          chapterCode: ch.code as string,
          phase: (ch.phase as ChapterPhase) || 'locked',
          campDefeated: (ch.campDefeated as string[]) || [],
          campOpened: campOpened.value[ch.code as string] || [],
          questDaysCompleted: (ch.questDaysCompleted as number[]) || [],
          bossDefeated: (ch.bossDefeated as boolean) || false,
        }
      }
      progressMap.value = map
    } catch {
      // 离线时保留本地默认
      if (Object.keys(progressMap.value).length === 0) {
        for (const ch of chapterConfigs) {
          progressMap.value[ch.code] = {
            chapterCode: ch.code,
            phase: ch.orderIndex === 1 ? 'camp' : 'locked',
            campDefeated: [],
            campOpened: [],
            questDaysCompleted: [],
            bossDefeated: false,
          }
        }
      }
    }
  }

  /** 加载某章节课时列表 */
  async function loadLessons(chapterCode: string) {
    try {
      const { data } = await http.get(`/quest/${chapterCode}/lessons`)
      lessonsCache.value[chapterCode] = (data as Record<string, unknown>[]).map(l => ({
        code: l.code as string,
        chapterCode: l.chapterCode as string,
        dayIndex: l.dayIndex as number,
        titleEn: l.titleEn as string,
        titleZh: l.titleZh as string,
        estimatedMinutes: l.estimatedMinutes as number,
        targetTaskCount: 30,
        autoDrillEnabled: true,
        completed: l.completed as boolean,
        current: false,
      }))
    } catch { /* ignore */ }
  }

  /** 加载某天的 Quest 任务 */
  async function loadQuestTasks(chapterCode: string, dayIndex: number) {
    loading.value = true
    try {
      const { data } = await http.get(`/quest/${chapterCode}/day/${dayIndex}/tasks`)
      questTasksCache.value = (data as Record<string, unknown>[]).map(mapBackendTask)
    } catch {
      questTasksCache.value = []
    } finally {
      loading.value = false
    }
  }

  /** 加载 Boss 数据 */
  async function loadBossData(chapterCode: string) {
    loading.value = true
    try {
      const { data } = await http.get(`/boss/${chapterCode}`)
      const d = data as Record<string, unknown>
      bossDataCache.value = {
        config: {
          code: d.code as string,
          chapterCode: d.chapterCode as string,
          bossName: d.bossName as string,
          bossType: (d.bossTitle as string) || 'gatekeeper',
          bossHp: d.bossHp as number,
          playerHp: d.playerHp as number,
          comboThreshold: 3,
          comboBonusDamage: ((d.damageCorrect as number) || 1) * 2,
          bossDamage: (d.damageWrong as number) || 1,
          dailyRetryLimit: 3,
          tier: 1,
        },
        tasks: ((d.tasks as Record<string, unknown>[]) || []).map(mapBossTask),
      }
    } catch {
      bossDataCache.value = null
    } finally {
      loading.value = false
    }
  }

  // ---- Mutation Actions ----

  function setCurrentChapter(code: string) {
    currentChapterCode.value = code
  }

  async function defeatCampMonster(encounterId: string) {
    const code = currentChapterCode.value
    const prog = progressMap.value[code]
    if (!prog || prog.campDefeated.includes(encounterId)) return

    const map = currentCampMap.value
    const totalMonsters = map ? map.encounters.filter(e => e.type === 'monster').length : 0

    // 使用整体赋值确保 Vue 响应式追踪
    progressMap.value = {
      ...progressMap.value,
      [code]: {
        ...prog,
        campDefeated: [...prog.campDefeated, encounterId],
      },
    }

    try {
      const { data } = await http.post('/progress/camp', {
        chapterCode: code,
        encounterId,
        totalMonsters,
      })
      // 根据后端返回的击败数重新判断阶段
      const defeated = (data as Record<string, unknown>).defeated as number
      if (defeated >= Math.ceil(totalMonsters * (currentChapter.value?.campUnlockRate ?? 0.8))) {
        progressMap.value = {
          ...progressMap.value,
          [code]: { ...progressMap.value[code]!, phase: 'quest' },
        }
      }
    } catch {
      // 离线回退：本地判断阶段
      const newProg = progressMap.value[code]!
      const rate = totalMonsters > 0 ? newProg.campDefeated.length / totalMonsters : 0
      if (newProg.phase === 'camp' && rate >= (currentChapter.value?.campUnlockRate ?? 0.8)) {
        progressMap.value = {
          ...progressMap.value,
          [code]: { ...newProg, phase: 'quest' },
        }
      }
    }
  }

  function openCampChest(encounterId: string) {
    const code = currentChapterCode.value
    const prog = progressMap.value[code]
    if (!prog || prog.campOpened.includes(encounterId)) return
    progressMap.value = {
      ...progressMap.value,
      [code]: { ...prog, campOpened: [...prog.campOpened, encounterId] },
    }
    if (!campOpened.value[code]) campOpened.value[code] = []
    campOpened.value[code].push(encounterId)
    localStorage.setItem(openedKey(), JSON.stringify(campOpened.value))
  }

  async function completeQuestDay(dayIndex: number) {
    const code = currentChapterCode.value
    const prog = progressMap.value[code]
    if (!prog || prog.questDaysCompleted.includes(dayIndex)) return
    progressMap.value = {
      ...progressMap.value,
      [code]: { ...prog, questDaysCompleted: [...prog.questDaysCompleted, dayIndex] },
    }

    const lessonCode = `${code}_D${String(dayIndex).padStart(2, '0')}`
    try {
      const { data } = await http.post('/progress/quest-day', { lessonCode, xpEarned: 0 })
      const d = data as Record<string, unknown>
      if (d.allDaysCompleted) {
        progressMap.value = {
          ...progressMap.value,
          [code]: { ...progressMap.value[code]!, phase: 'boss' },
        }
      }
    } catch {
      const updatedProg = progressMap.value[code]!
      const lessons = currentLessons.value
      if (updatedProg.questDaysCompleted.length >= lessons.length) {
        progressMap.value = {
          ...progressMap.value,
          [code]: { ...updatedProg, phase: 'boss' },
        }
      }
    }
  }

  async function defeatBoss() {
    const code = currentChapterCode.value
    const prog = progressMap.value[code]
    if (!prog) return

    const updates: Record<string, ChapterProgress> = {
      ...progressMap.value,
      [code]: { ...prog, bossDefeated: true, phase: 'completed' },
    }

    // 解锁下一章
    const currentOrder = currentChapter.value.orderIndex
    const next = chapterConfigs.find(c => c.orderIndex === currentOrder + 1)
    if (next) {
      const nextProg = updates[next.code]
      if (nextProg && nextProg.phase === 'locked') {
        updates[next.code] = { ...nextProg, phase: 'camp' }
      }
    }
    progressMap.value = updates

    try {
      await http.post('/progress/boss', {
        bossCode: currentBossConfig.value.code,
        chapterCode: code,
        victory: true,
        bossHpRemaining: 0,
        playerHpRemaining: 0,
        maxCombo: 0,
        xpEarned: 100,
      })
    } catch { /* ignore */ }
  }

  function advanceToQuest() {
    const code = currentChapterCode.value
    const prog = progressMap.value[code]
    if (prog && prog.phase === 'camp' && isQuestUnlocked.value) {
      progressMap.value = {
        ...progressMap.value,
        [code]: { ...prog, phase: 'quest' },
      }
    }
  }

  function resetProgress() {
    const map: Record<string, ChapterProgress> = {}
    for (const ch of chapterConfigs) {
      map[ch.code] = {
        chapterCode: ch.code,
        phase: ch.orderIndex === 1 ? 'camp' : 'locked',
        campDefeated: [],
        campOpened: [],
        questDaysCompleted: [],
        bossDefeated: false,
      }
    }
    progressMap.value = map
    currentChapterCode.value = 'PRE_A1_CH1'
    campOpened.value = {}
    localStorage.removeItem(openedKey())
  }

  /** 清除当前用户的所有营地 localStorage 数据（位置/迷雾/宝箱） */
  function clearCampLocalData() {
    localStorage.removeItem(openedKey())
    // 清除 eqa_camp_state_{userId}_{chapterCode} 条目
    const prefix = 'eqa_camp_state_' + currentUserId + '_'
    const toRemove: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(prefix)) toRemove.push(key)
    }
    for (const key of toRemove) localStorage.removeItem(key)
    resetProgress()
  }

  function reload(userId: number) {
    currentUserId = userId
    campOpened.value = loadOpened()
    // 同步到已加载的 progressMap
    for (const [code, prog] of Object.entries(progressMap.value)) {
      progressMap.value[code] = {
        ...prog,
        campOpened: campOpened.value[code] || [],
      }
    }
  }

  return {
    currentChapterCode,
    progressMap,
    loading,
    allChapters,
    currentChapter,
    currentProgress,
    currentPhase,
    currentLessons,
    currentCampMap,
    currentBossConfig,
    currentBossTasks,
    currentQuestDayIndex,
    currentQuestTasks,
    campCompletionRate,
    isQuestUnlocked,
    isBossUnlocked,
    allLearnedWords,
    loadChapters,
    loadLessons,
    loadQuestTasks,
    loadBossData,
    setCurrentChapter,
    defeatCampMonster,
    openCampChest,
    completeQuestDay,
    defeatBoss,
    advanceToQuest,
    resetProgress,
    clearCampLocalData,
    reload,
  }
})

/** 将后端 Quest 任务映射为前端 Task 类型 */
function mapBackendTask(t: Record<string, unknown>): Task {
  const type = t.type as string
  return {
    code: t.code as string,
    lessonCode: (t.lessonCode as string) || '',
    orderIndex: (t.orderIndex as number) || 0,
    type: type as Task['type'],
    promptEn: (t.promptEn as string) || '',
    promptZhHint: (t.promptZhHint as string) || '',
    options: t.options as Task['options'],
    answer: (t.answer as Record<string, unknown>) || {},
    explanationEn: (t.explanationEn as string) || '',
    explanationZh: (t.explanationZh as string) || '',
    xpReward: (t.xpReward as number) || 5,
    goldReward: 0,
    tts: { enabled: (t.ttsEnabled as boolean) ?? true, ttsTextEn: (t.ttsTextEn as string) || (t.promptEn as string) || '' },
    links: { contentItemCodes: (t.contentItemCodes as string[]) || [] },
  }
}

/** 将后端 Boss 题池任务映射为前端 Task 类型 */
function mapBossTask(t: Record<string, unknown>): Task {
  let options: Task['options']
  try {
    options = typeof t.options === 'string' ? JSON.parse(t.options) : t.options as Task['options']
  } catch { options = undefined }

  let answer: Record<string, unknown>
  try {
    const raw = t.answer
    if (typeof raw === 'string') {
      // 可能是 JSON 对象字符串或纯 key 字符串
      if (raw.startsWith('{') || raw.startsWith('[')) {
        answer = JSON.parse(raw)
      } else {
        answer = { correctOptionKey: raw }
      }
    } else {
      answer = (raw as Record<string, unknown>) || {}
    }
  } catch {
    answer = { correctOptionKey: t.answer as string }
  }

  let contentItemCodes: string[] = []
  try {
    const raw = t.contentItemCodes
    if (typeof raw === 'string' && raw) {
      contentItemCodes = raw.startsWith('[') ? JSON.parse(raw) : raw.split(',')
    } else if (Array.isArray(raw)) {
      contentItemCodes = raw as string[]
    }
  } catch { /* ignore */ }

  return {
    code: (t.taskCode as string) || '',
    lessonCode: '',
    orderIndex: 0,
    type: (t.taskType as Task['type']) || 'MCQ',
    promptEn: (t.promptEn as string) || '',
    promptZhHint: (t.promptZhHint as string) || '',
    options,
    answer,
    explanationEn: (t.explanationEn as string) || '',
    explanationZh: (t.explanationZh as string) || '',
    xpReward: 5,
    goldReward: 0,
    tts: { enabled: (t.ttsEnabled as boolean) ?? true, ttsTextEn: (t.ttsTextEn as string) || '' },
    links: { contentItemCodes },
  }
}
