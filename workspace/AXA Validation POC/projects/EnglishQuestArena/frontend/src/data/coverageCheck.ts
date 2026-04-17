// 覆盖率校验 & 自动补题（Auto-Drill）
import type { ContentItem, Task } from '@/types'

/**
 * 检查哪些 ContentItem 未被任何 Task 引用
 */
export function findUncoveredItems(
  words: ContentItem[],
  tasks: Task[],
): ContentItem[] {
  const coveredCodes = new Set(
    tasks.flatMap(t => t.links?.contentItemCodes ?? [])
  )
  return words.filter(w => !coveredCodes.has(w.code))
}

/**
 * 为未覆盖的词汇自动生成 FLASHCARD + MCQ 补充题
 */
export function generateAutoDrill(
  uncovered: ContentItem[],
  allWords: ContentItem[],
  lessonCode: string,
  startOrder: number,
): Task[] {
  const tasks: Task[] = []
  let order = startOrder

  for (const word of uncovered) {
    if (word.type !== 'WORD') continue

    // 1. 生成 FLASHCARD
    tasks.push({
      code: `${lessonCode}_AD_FC_${word.code}`,
      lessonCode,
      orderIndex: order++,
      type: 'FLASHCARD',
      promptEn: word.en,
      promptZhHint: word.zh,
      answer: { action: 'GOT_IT' },
      explanationEn: `${word.en} = ${word.zh}`,
      explanationZh: `${word.en} 的意思是"${word.zh}"`,
      xpReward: 2,
      goldReward: 0,
      tts: { enabled: true, ttsTextEn: word.en },
      links: { contentItemCodes: [word.code] },
    })

    // 2. 生成 MCQ（英→中）
    const distractors = getDistractors(word, allWords, 2)
    const options = shuffle([word, ...distractors], word.code.length)
    const correctKey = ['A', 'B', 'C'][options.findIndex(o => o.code === word.code)] ?? 'A'

    tasks.push({
      code: `${lessonCode}_AD_MCQ_${word.code}`,
      lessonCode,
      orderIndex: order++,
      type: 'MCQ',
      promptEn: `Choose the meaning of "${word.en}"`,
      promptZhHint: `选择 ${word.en} 的中文意思`,
      options: options.map((o, i) => ({
        key: ['A', 'B', 'C'][i] ?? String.fromCharCode(65 + i),
        textEn: o.en,
        textZh: o.zh,
      })),
      answer: { correctOptionKey: correctKey },
      explanationEn: `${word.en} = ${word.zh}`,
      explanationZh: `${word.en} 的意思是"${word.zh}"`,
      xpReward: 5,
      goldReward: 0,
      tts: { enabled: true, ttsTextEn: word.en },
      links: { contentItemCodes: [word.code] },
    })
  }

  return tasks
}

/**
 * 打印覆盖率报告（开发阶段使用）
 */
export function logCoverageReport(
  chapterCode: string,
  words: ContentItem[],
  campMonsterCount: number,
  questTasks: Task[],
  bossTasks: Task[],
) {
  const wordItems = words.filter(w => w.type === 'WORD')
  const allTasks = [...questTasks, ...bossTasks]
  const coveredCodes = new Set(allTasks.flatMap(t => t.links?.contentItemCodes ?? []))
  const uncovered = wordItems.filter(w => !coveredCodes.has(w.code))

  console.group(`📊 ${chapterCode} 覆盖率报告`)
  console.log(`总词汇: ${wordItems.length}`)
  console.log(`营地怪物: ${campMonsterCount}`)
  console.log(`Quest+Boss 题目: ${allTasks.length}`)
  console.log(`被题目覆盖: ${wordItems.length - uncovered.length}/${wordItems.length}`)
  if (uncovered.length > 0) {
    console.warn(`⚠️ 未覆盖: ${uncovered.map(w => w.code).join(', ')}`)
  } else {
    console.log(`✅ 全部覆盖`)
  }
  console.groupEnd()
}

// ---- 内部工具 ----

function getDistractors(word: ContentItem, allWords: ContentItem[], count: number): ContentItem[] {
  // 优先使用预设的干扰项
  const result: ContentItem[] = word.distractors
    .map(code => allWords.find(w => w.code === code))
    .filter((w): w is ContentItem => !!w)
    .slice(0, count)

  // 不够则从同章其他词补充
  if (result.length < count) {
    const used = new Set([word.code, ...result.map(r => r.code)])
    for (const w of allWords) {
      if (result.length >= count) break
      if (w.type === 'WORD' && !used.has(w.code)) {
        result.push(w)
        used.add(w.code)
      }
    }
  }
  return result
}

function shuffle<T>(arr: T[], seed: number): T[] {
  const result = [...arr]
  for (let i = result.length - 1; i > 0; i--) {
    const j = ((seed * (i + 1) * 31) % 97) % (i + 1)
    const tmp = result[i]!
    result[i] = result[j]!
    result[j] = tmp
  }
  return result
}
