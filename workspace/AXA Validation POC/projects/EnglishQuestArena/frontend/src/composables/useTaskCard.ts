// 统一的题卡组件映射 — 消除 QuestTodayView / ArenaView / BossView 的重复导入
import { computed, type Component } from 'vue'
import type { Task } from '@/types'
import McqCard from '@/components/ui/McqCard.vue'
import McqReverseCard from '@/components/ui/McqReverseCard.vue'
import FlashCard from '@/components/ui/FlashCard.vue'
import SpellingCard from '@/components/ui/SpellingCard.vue'
import WordOrderCard from '@/components/ui/WordOrderCard.vue'
import FillBlankCard from '@/components/ui/FillBlankCard.vue'
import ListenPickCard from '@/components/ui/ListenPickCard.vue'
import ListenFillCard from '@/components/ui/ListenFillCard.vue'
import DialogueCard from '@/components/ui/DialogueCard.vue'
import SituationPickCard from '@/components/ui/SituationPickCard.vue'
import ErrorFixCard from '@/components/ui/ErrorFixCard.vue'

const taskComponentMap: Record<string, Component> = {
  MCQ: McqCard,
  MCQ_REVERSE: McqReverseCard,
  FLASHCARD: FlashCard,
  SPELLING: SpellingCard,
  WORD_ORDER: WordOrderCard,
  FILL_BLANK: FillBlankCard,
  LISTEN_PICK: ListenPickCard,
  LISTEN_FILL: ListenFillCard,
  DIALOGUE_REVIEW: DialogueCard,
  SITUATION_PICK: SituationPickCard,
  ERROR_FIX: ErrorFixCard,
}

export function resolveTaskComponent(task: Task | null | undefined): Component | null {
  if (!task) return null
  return taskComponentMap[task.type] ?? McqCard
}

export function useTaskCard(taskRef: () => Task | null | undefined) {
  const taskComponent = computed(() => resolveTaskComponent(taskRef()))
  return { taskComponent }
}
