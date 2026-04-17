// 从 ContentItem[] 自动生成 Quest 每日任务列表
// 根据当前章节和天数，筛选对应 dayIndex 的词汇，按题型分配生成 Task[]
import type { ContentItem, Task, TaskOption } from '@/types'

const KEYS = ['A', 'B', 'C', 'D'] as const

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j]!, copy[i]!]
  }
  return copy
}

function buildMcqOptions(word: ContentItem, allWords: ContentItem[]): { options: TaskOption[]; correctKey: string } {
  const distractors = allWords
    .filter(w => w.code !== word.code)
    .sort(() => Math.random() - 0.5)
    .slice(0, 2)

  const all = shuffle([word, ...distractors])
  const correctIdx = all.findIndex(w => w.code === word.code)
  const correctKey = KEYS[correctIdx] ?? 'A'

  const options = all.map((w, i) => ({
    key: KEYS[i] ?? 'A',
    textEn: w.en,
    textZh: w.zh,
  }))

  return { options, correctKey }
}

/**
 * 为指定章节 + dayIndex 生成每日 Quest 任务列表
 * 题型分配: MCQ → FLASHCARD → SPELLING → MCQ_REVERSE → LISTEN_PICK → FILL_BLANK
 * 每个词至少 2 道题; 总量约 targetCount
 */
export function generateQuestTasks(
  allWords: ContentItem[],
  dayIndex: number,
  chapterCode: string,
  targetCount = 30,
): Task[] {
  // 当天词汇
  const dayWords = allWords.filter(w => w.dayIndex === dayIndex && w.type === 'WORD')
  if (dayWords.length === 0) return []

  const tasks: Task[] = []
  let order = 1
  const prefix = `${chapterCode}_D${String(dayIndex).padStart(2, '0')}`

  // 第一轮: 每词先出一张 FLASHCARD (学习)
  for (const word of dayWords) {
    tasks.push({
      code: `${prefix}_FC_${word.code}`,
      lessonCode: `${chapterCode}_D${String(dayIndex).padStart(2, '0')}`,
      orderIndex: order++,
      type: 'FLASHCARD',
      promptEn: word.en,
      promptZhHint: word.zh,
      answer: { action: 'GOT_IT' },
      explanationEn: word.sentence,
      explanationZh: word.sentenceZh,
      xpReward: 2,
      goldReward: 0,
      tts: { enabled: true, ttsTextEn: word.en },
      links: { contentItemCodes: [word.code] },
    })
  }

  // 第二轮: MCQ 选择题
  for (const word of dayWords) {
    const { options, correctKey } = buildMcqOptions(word, allWords)
    tasks.push({
      code: `${prefix}_MCQ_${word.code}`,
      lessonCode: `${chapterCode}_D${String(dayIndex).padStart(2, '0')}`,
      orderIndex: order++,
      type: 'MCQ',
      promptEn: `What does "${word.en}" mean?`,
      promptZhHint: `选择 ${word.en} 的意思`,
      options,
      answer: { correctOptionKey: correctKey },
      explanationEn: `${word.en} = ${word.zh}`,
      explanationZh: `${word.en} 的意思是"${word.zh}"`,
      xpReward: 5,
      goldReward: 0,
      tts: { enabled: true, ttsTextEn: word.en },
      links: { contentItemCodes: [word.code] },
    })
  }

  // 第三轮: SPELLING 拼写
  for (const word of dayWords) {
    tasks.push({
      code: `${prefix}_SP_${word.code}`,
      lessonCode: `${chapterCode}_D${String(dayIndex).padStart(2, '0')}`,
      orderIndex: order++,
      type: 'SPELLING',
      promptEn: 'Type the word you hear.',
      promptZhHint: `听音拼写：${word.zh}`,
      answer: { acceptedTexts: [word.en.toLowerCase()] },
      explanationEn: `The word is "${word.en}"`,
      explanationZh: `这个单词是 ${word.en}（${word.zh}）`,
      xpReward: 8,
      goldReward: 1,
      tts: { enabled: true, ttsTextEn: word.en },
      links: { contentItemCodes: [word.code] },
    })
  }

  // 如果题目不够 targetCount，补充 MCQ_REVERSE 和 LISTEN_PICK
  const remaining = targetCount - tasks.length
  if (remaining > 0) {
    const extraWords = shuffle(dayWords)
    for (let i = 0; i < remaining && i < extraWords.length * 2; i++) {
      const word = extraWords[i % extraWords.length]!
      const isReverse = i % 2 === 0

      if (isReverse) {
        const { options: revOpts, correctKey: revKey } = buildMcqOptions(word, allWords)
        // MCQ_REVERSE: 给中文选英文
        const revOptions = revOpts.map(o => ({ ...o, textEn: o.textEn, textZh: o.textZh }))
        tasks.push({
          code: `${prefix}_MR_${word.code}_${i}`,
          lessonCode: `${chapterCode}_D${String(dayIndex).padStart(2, '0')}`,
          orderIndex: order++,
          type: 'MCQ_REVERSE',
          promptEn: word.zh,
          promptZhHint: word.zh,
          options: revOptions,
          answer: { correctOptionKey: revKey },
          explanationEn: `"${word.en}" means ${word.zh}`,
          explanationZh: `${word.zh} 对应的英文是 ${word.en}`,
          xpReward: 5,
          goldReward: 0,
          tts: { enabled: true, ttsTextEn: word.en },
          links: { contentItemCodes: [word.code] },
        })
      } else {
        // LISTEN_PICK
        const others = allWords
          .filter(w => w.code !== word.code && w.type === 'WORD')
          .sort(() => Math.random() - 0.5)
          .slice(0, 2)
        const allOpts = shuffle([word, ...others])
        const cKey = KEYS[allOpts.findIndex(w => w.code === word.code)] ?? 'A'

        tasks.push({
          code: `${prefix}_LP_${word.code}_${i}`,
          lessonCode: `${chapterCode}_D${String(dayIndex).padStart(2, '0')}`,
          orderIndex: order++,
          type: 'LISTEN_PICK',
          promptEn: 'Listen and pick the correct word.',
          promptZhHint: '听发音，选择正确的单词',
          options: allOpts.map((w, idx) => ({ key: KEYS[idx] ?? 'A', textEn: w.en, textZh: w.zh })),
          answer: { correctOptionKey: cKey },
          explanationEn: `The word is "${word.en}"`,
          explanationZh: `发音是 ${word.en}（${word.zh}）`,
          xpReward: 6,
          goldReward: 0,
          tts: { enabled: true, ttsTextEn: word.en },
          links: { contentItemCodes: [word.code] },
        })
      }
    }
  }

  return tasks
}
