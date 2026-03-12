import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ChapterProgress, ChapterPhase, Lesson, BossConfig } from '@/types'
import { chapterConfigs, chapterLessons, chapterCampMaps, chapterBossTasks } from '@/mock/chapters'
import type { CampMapData } from '@/game/config/mapData'
import type { Task } from '@/types'
import { generateQuestTasks } from '@/data/questGenerator'

const STORAGE_KEY = 'eqa_chapter_progress'

function loadFromStorage(): Record<string, ChapterProgress> | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return null
}

function saveToStorage(data: Record<string, ChapterProgress>) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

// Default initial progress: CH1 is in 'camp' phase, rest locked
function defaultProgress(): Record<string, ChapterProgress> {
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
  return map
}

export const useChapterStore = defineStore('chapter', () => {
  const currentChapterCode = ref('PRE_A1_CH1')
  const progressMap = ref<Record<string, ChapterProgress>>(
    loadFromStorage() ?? defaultProgress()
  )

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
    chapterLessons[currentChapterCode.value] ?? []
  )

  const currentCampMap = computed<CampMapData>(() =>
    chapterCampMaps[currentChapterCode.value]
  )

  const currentBossConfig = computed<BossConfig>(() =>
    currentChapter.value.bossConfig
  )

  const currentBossTasks = computed<Task[]>(() =>
    chapterBossTasks[currentChapterCode.value] ?? []
  )

  // 当前应进行的 Quest 天数 (已完成天数的下一天)
  const currentQuestDayIndex = computed<number>(() => {
    const completed = currentProgress.value?.questDaysCompleted ?? []
    const lessons = currentLessons.value
    const nextLesson = lessons.find(l => !completed.includes(l.dayIndex))
    return nextLesson?.dayIndex ?? 1
  })

  // 当前 Quest 天的自动生成任务
  const currentQuestTasks = computed<Task[]>(() => {
    const chapter = currentChapter.value
    if (!chapter) return []
    return generateQuestTasks(chapter.words, currentQuestDayIndex.value, chapter.code)
  })

  // Camp completion percentage
  const campCompletionRate = computed(() => {
    const map = currentCampMap.value
    if (!map) return 0
    const monsters = map.encounters.filter(e => e.type === 'monster')
    if (monsters.length === 0) return 1
    const prog = currentProgress.value
    const defeated = prog ? prog.campDefeated.length : 0
    return defeated / monsters.length
  })

  // Is quest unlocked? Camp >= threshold OR already past camp
  const isQuestUnlocked = computed(() => {
    const phase = currentPhase.value
    if (phase === 'quest' || phase === 'boss' || phase === 'completed') return true
    return campCompletionRate.value >= (currentChapter.value?.campUnlockRate ?? 0.8)
  })

  // Is boss unlocked? All quest days completed
  const isBossUnlocked = computed(() => {
    const phase = currentPhase.value
    if (phase === 'boss' || phase === 'completed') return true
    if (phase !== 'quest') return false
    const lessons = currentLessons.value
    const completed = currentProgress.value?.questDaysCompleted ?? []
    return lessons.length > 0 && completed.length >= lessons.length
  })

  // All learned words across completed chapters (for Arena SRS pool)
  const allLearnedWords = computed(() => {
    const words: typeof chapterConfigs[0]['words'] = []
    for (const ch of chapterConfigs) {
      const prog = progressMap.value[ch.code]
      if (!prog) continue
      // Include words from chapters where camp has been at least partially explored
      if (prog.phase !== 'locked') {
        words.push(...ch.words)
      }
    }
    return words
  })

  // ---- Actions ----

  function persist() {
    saveToStorage(progressMap.value)
  }

  function setCurrentChapter(code: string) {
    currentChapterCode.value = code
  }

  function defeatCampMonster(encounterId: string) {
    const prog = progressMap.value[currentChapterCode.value]
    if (!prog || prog.campDefeated.includes(encounterId)) return
    prog.campDefeated.push(encounterId)
    // Auto-advance phase if threshold met
    if (prog.phase === 'camp' && campCompletionRate.value >= (currentChapter.value?.campUnlockRate ?? 0.8)) {
      prog.phase = 'quest'
    }
    persist()
  }

  function openCampChest(encounterId: string) {
    const prog = progressMap.value[currentChapterCode.value]
    if (!prog || prog.campOpened.includes(encounterId)) return
    prog.campOpened.push(encounterId)
    persist()
  }

  function completeQuestDay(dayIndex: number) {
    const prog = progressMap.value[currentChapterCode.value]
    if (!prog || prog.questDaysCompleted.includes(dayIndex)) return
    prog.questDaysCompleted.push(dayIndex)
    // Auto-advance to boss phase if all days done
    const lessons = currentLessons.value
    if (prog.questDaysCompleted.length >= lessons.length) {
      prog.phase = 'boss'
    }
    persist()
  }

  function defeatBoss() {
    const prog = progressMap.value[currentChapterCode.value]
    if (!prog) return
    prog.bossDefeated = true
    prog.phase = 'completed'
    // Unlock next chapter
    const currentOrder = currentChapter.value.orderIndex
    const next = chapterConfigs.find(c => c.orderIndex === currentOrder + 1)
    if (next) {
      const nextProg = progressMap.value[next.code]
      if (nextProg && nextProg.phase === 'locked') {
        nextProg.phase = 'camp'
      }
    }
    persist()
  }

  // Advance to quest phase manually (if already eligible)
  function advanceToQuest() {
    const prog = progressMap.value[currentChapterCode.value]
    if (prog && prog.phase === 'camp' && isQuestUnlocked.value) {
      prog.phase = 'quest'
      persist()
    }
  }

  function resetProgress() {
    progressMap.value = defaultProgress()
    currentChapterCode.value = 'PRE_A1_CH1'
    persist()
  }

  return {
    currentChapterCode,
    progressMap,
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
    setCurrentChapter,
    defeatCampMonster,
    openCampChest,
    completeQuestDay,
    defeatBoss,
    advanceToQuest,
    resetProgress,
  }
})
