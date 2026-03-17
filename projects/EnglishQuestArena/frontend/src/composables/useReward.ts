// 集中化的奖励计算 — 统一 XP / 金币 / 连击加成逻辑
// 所有答题场景 (Quest / Arena / Camp / Boss) 共用
import { useUserStore } from '@/stores/user'

export interface RewardResult {
  xpEarned: number
  coinsEarned: number
  comboBonus: boolean
}

const BASE_XP = 10
const COMBO_THRESHOLD = 5
const COMBO_BONUS_XP = 5
const COMBO_BONUS_COINS = 2

/**
 * 计算答题奖励并写入 userStore
 * @param correct 是否答对
 * @param combo 当前连击数 (答对后已+1)
 * @param context 场景标识，预留给后端统计
 * @param options 可选的自定义基础奖励值
 */
export function grantReward(
  correct: boolean,
  combo: number,
  _context: 'quest' | 'arena' | 'camp' | 'boss' = 'quest',
  options?: { baseXp?: number; baseCoins?: number },
): RewardResult {
  if (!correct) return { xpEarned: 0, coinsEarned: 0, comboBonus: false }

  const userStore = useUserStore()
  const comboBonus = combo >= COMBO_THRESHOLD
  const xp = options?.baseXp ?? BASE_XP
  const coins = options?.baseCoins ?? 1
  const xpEarned = xp + (comboBonus ? COMBO_BONUS_XP : 0)
  const coinsEarned = coins + (comboBonus ? COMBO_BONUS_COINS : 0)

  userStore.addXp(xpEarned)
  userStore.addCoins(coinsEarned)

  return { xpEarned, coinsEarned, comboBonus }
}
