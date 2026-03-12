// 从 ContentItem[] 自动生成营地 MapEncounter[]
import type { ContentItem } from '@/types'
import type { MapEncounter, MonsterDifficulty } from '@/game/config/mapData'

// 地图上预设的怪物坐标（与 tiles 中 type=8 的位置对应）
// 每章地图最多支持的怪物数量取决于地图中 8 的数量
interface MonsterSlot {
  tileX: number
  tileY: number
}

const KEYS = ['A', 'B', 'C'] as const

/**
 * 从章节词表中筛出 WORD 类型项，为每个生成一个 monster encounter
 * @param words 章节全部 ContentItem
 * @param slots 地图上怪物坐标位置
 * @param chapterPrefix encounter id 前缀，如 'ch1'
 */
export function generateMonsterEncounters(
  words: ContentItem[],
  slots: MonsterSlot[],
  chapterPrefix: string,
): MapEncounter[] {
  const wordItems = words.filter(w => w.type === 'WORD')

  return wordItems.map((word, i) => {
    const slot = slots[i % slots.length]!

    // 构建选项：正确答案 + 干扰项
    const distractorWords = word.distractors
      .map(code => words.find(w => w.code === code))
      .filter((w): w is ContentItem => !!w)
      .slice(0, 2)

    // 如果干扰项不够，从同章其他词补充
    while (distractorWords.length < 2) {
      const other = wordItems.find(
        w => w.code !== word.code && !distractorWords.includes(w) &&
          !word.distractors.includes(w.code)
      )
      if (other) distractorWords.push(other)
      else break
    }

    // 随机打乱选项顺序（用确定性种子以保持一致）
    const allOptions = [word, ...distractorWords]
    const seed = word.code.length + i
    const shuffled = allOptions
      .map((item, idx) => ({ item, sort: (seed * (idx + 1) * 31) % 97 }))
      .sort((a, b) => a.sort - b.sort)
      .map(x => x.item)

    const correctIdx = shuffled.findIndex(w => w.code === word.code)
    const correctKey = KEYS[correctIdx] ?? 'A'

    const options = shuffled.map((w, idx) => ({
      key: KEYS[idx] ?? String.fromCharCode(65 + idx),
      textEn: w.zh,
      textZh: w.zh,
    }))

    // 根据单词复杂度分配难度
    const wordLen = word.en.replace(/\s/g, '').length
    const difficulty: MonsterDifficulty = wordLen <= 4 ? 'easy' : wordLen <= 6 ? 'medium' : 'hard'
    const rewardXp = difficulty === 'easy' ? 8 : difficulty === 'medium' ? 12 : 18
    const rewardCoins = difficulty === 'easy' ? 3 : difficulty === 'medium' ? 6 : 10

    return {
      id: `${chapterPrefix}_mon_${word.code.toLowerCase()}`,
      type: 'monster' as const,
      tileX: slot.tileX,
      tileY: slot.tileY,
      wordEn: word.en,
      wordZh: word.zh,
      phonetic: word.phonetic,
      sentence: word.sentence,
      sentenceZh: word.sentenceZh,
      defeated: false,
      difficulty,
      options,
      correctKey,
      reward: { xp: rewardXp, coins: rewardCoins },
    }
  })
}

/**
 * 从地图 tiles 数据中提取所有 type=8（怪物）的坐标
 */
export function extractMonsterSlots(tiles: number[][]): MonsterSlot[] {
  const slots: MonsterSlot[] = []
  for (let y = 0; y < tiles.length; y++) {
    const row = tiles[y]!
    for (let x = 0; x < row.length; x++) {
      if (row[x] === 8) {
        slots.push({ tileX: x, tileY: y })
      }
    }
  }
  return slots
}
