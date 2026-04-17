// 营地地图数据
// 瓦片类型: 0=草地 1=路径 2=水 3=树 4=帐篷 5=篝火 6=NPC 7=宝箱 8=怪物 9=石头

export type MonsterDifficulty = 'easy' | 'medium' | 'hard'

export interface MapEncounter {
  id: string
  type: 'monster' | 'npc' | 'treasure'
  tileX: number
  tileY: number
  wordEn: string
  wordZh: string
  phonetic: string
  sentence: string
  sentenceZh: string
  defeated: boolean
  difficulty?: MonsterDifficulty
  options?: Array<{ key: string; textEn: string; textZh: string }>
  correctKey?: string
  npcName?: string
  npcAvatar?: 'sage' | 'guide' | 'knight' | 'merchant'
  npcLines?: string[]
  reward?: { xp: number; coins: number }
}

export interface CampMapData {
  width: number
  height: number
  tileSize: number
  tiles: number[][]
  encounters: MapEncounter[]
  playerStart: { x: number; y: number }
}

// 瓦片颜色映射
export const TILE_COLORS: Record<number, number> = {
  0: 0x2d5a1e, // 草地 - 深绿
  1: 0x8b7355, // 路径 - 土色
  2: 0x1a3a5c, // 水 - 深蓝
  3: 0x1a3d1a, // 树 - 更深绿
  4: 0x6b4226, // 帐篷 - 棕色
  5: 0xcc6600, // 篝火 - 橘色
  6: 0x4a4a8a, // NPC - 紫蓝
  7: 0xdaa520, // 宝箱 - 金色
  8: 0x8b0000, // 怪物 - 暗红
  9: 0x555555, // 石头 - 灰色
}

// 瓦片是否可行走
export const TILE_WALKABLE: Record<number, boolean> = {
  0: true,  // 草地
  1: true,  // 路径
  2: false, // 水
  3: false, // 树
  4: false, // 帐篷（碰触触发）
  5: true,  // 篝火（可经过）
  6: true,  // NPC（碰触触发）
  7: true,  // 宝箱（碰触触发）
  8: true,  // 怪物（碰触触发）
  9: false, // 石头
}

// 瓦片装饰符号
export const TILE_SYMBOLS: Record<number, string> = {
  3: '🌲',
  4: '⛺',
  5: '🔥',
  6: '👤',
  7: '💎',
  8: '🐺',
  9: '🪨',
}

// 默认营地地图 20x15
export const defaultCampMap: CampMapData = {
  width: 20,
  height: 15,
  tileSize: 64,
  playerStart: { x: 2, y: 7 },
  tiles: [
    // y=0 (top row)
    [3, 3, 9, 3, 3, 0, 0, 2, 2, 2, 2, 0, 0, 3, 3, 9, 3, 3, 3, 3],
    [3, 0, 0, 0, 3, 0, 0, 2, 2, 2, 0, 0, 0, 0, 3, 0, 0, 0, 3, 3],
    [9, 0, 4, 0, 0, 1, 1, 0, 2, 0, 0, 1, 1, 0, 0, 0, 4, 0, 0, 3],
    [3, 0, 0, 0, 0, 1, 3, 0, 0, 0, 3, 1, 3, 0, 8, 0, 0, 0, 0, 9],
    [3, 0, 0, 5, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 7, 0, 3],
    [0, 0, 0, 0, 0, 1, 0, 8, 0, 0, 0, 1, 0, 0, 3, 0, 0, 0, 0, 0],
    [0, 0, 6, 0, 0, 1, 0, 0, 0, 5, 0, 1, 0, 0, 0, 8, 0, 0, 0, 0],
    [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0],
    [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 6, 0, 0],
    [0, 0, 8, 0, 0, 1, 0, 0, 7, 0, 0, 1, 0, 8, 0, 0, 0, 0, 0, 0],
    [3, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0, 5, 0, 0, 3],
    [3, 0, 7, 0, 0, 1, 0, 0, 0, 0, 3, 1, 3, 0, 0, 0, 0, 0, 0, 3],
    [9, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 4, 0, 0, 0, 9],
    [3, 0, 0, 3, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 8, 3, 3],
    [3, 3, 9, 3, 3, 0, 0, 2, 2, 2, 2, 0, 0, 3, 9, 3, 3, 3, 3, 3],
  ],
  encounters: [
    // 单词怪物
    {
      id: 'mon_hello', type: 'monster', tileX: 7, tileY: 5,
      wordEn: 'hello', wordZh: '你好', phonetic: '/həˈloʊ/',
      sentence: 'Hello, my name is Tom.', sentenceZh: '你好，我叫汤姆。',
      defeated: false,
      options: [
        { key: 'A', textEn: '你好', textZh: '你好' },
        { key: 'B', textEn: '再见', textZh: '再见' },
        { key: 'C', textEn: '谢谢', textZh: '谢谢' },
      ],
      correctKey: 'A',
      reward: { xp: 10, coins: 5 }
    },
    {
      id: 'mon_goodbye', type: 'monster', tileX: 14, tileY: 3,
      wordEn: 'goodbye', wordZh: '再见', phonetic: '/ɡʊdˈbaɪ/',
      sentence: 'Goodbye! See you tomorrow.', sentenceZh: '再见！明天见。',
      defeated: false,
      options: [
        { key: 'A', textEn: '你好', textZh: '你好' },
        { key: 'B', textEn: '再见', textZh: '再见' },
        { key: 'C', textEn: '对不起', textZh: '对不起' },
      ],
      correctKey: 'B',
      reward: { xp: 10, coins: 5 }
    },
    {
      id: 'mon_thank', type: 'monster', tileX: 15, tileY: 6,
      wordEn: 'thank you', wordZh: '谢谢你', phonetic: '/θæŋk juː/',
      sentence: 'Thank you very much!', sentenceZh: '非常感谢！',
      defeated: false,
      options: [
        { key: 'A', textEn: '对不起', textZh: '对不起' },
        { key: 'B', textEn: '没关系', textZh: '没关系' },
        { key: 'C', textEn: '谢谢你', textZh: '谢谢你' },
      ],
      correctKey: 'C',
      reward: { xp: 12, coins: 6 }
    },
    {
      id: 'mon_sorry', type: 'monster', tileX: 2, tileY: 9,
      wordEn: 'sorry', wordZh: '对不起', phonetic: '/ˈsɒri/',
      sentence: "I'm sorry about that.", sentenceZh: '对此我很抱歉。',
      defeated: false,
      options: [
        { key: 'A', textEn: '高兴', textZh: '高兴' },
        { key: 'B', textEn: '对不起', textZh: '对不起' },
        { key: 'C', textEn: '好的', textZh: '好的' },
      ],
      correctKey: 'B',
      reward: { xp: 10, coins: 5 }
    },
    {
      id: 'mon_please', type: 'monster', tileX: 13, tileY: 9,
      wordEn: 'please', wordZh: '请', phonetic: '/pliːz/',
      sentence: 'Please sit down.', sentenceZh: '请坐下。',
      defeated: false,
      options: [
        { key: 'A', textEn: '请', textZh: '请' },
        { key: 'B', textEn: '不要', textZh: '不要' },
        { key: 'C', textEn: '必须', textZh: '必须' },
      ],
      correctKey: 'A',
      reward: { xp: 10, coins: 5 }
    },
    {
      id: 'mon_name', type: 'monster', tileX: 17, tileY: 13,
      wordEn: 'name', wordZh: '名字', phonetic: '/neɪm/',
      sentence: 'What is your name?', sentenceZh: '你叫什么名字？',
      defeated: false,
      options: [
        { key: 'A', textEn: '年龄', textZh: '年龄' },
        { key: 'B', textEn: '名字', textZh: '名字' },
        { key: 'C', textEn: '家庭', textZh: '家庭' },
      ],
      correctKey: 'B',
      reward: { xp: 12, coins: 6 }
    },
    // NPC
    {
      id: 'npc_guide', type: 'npc', tileX: 2, tileY: 6,
      wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '',
      defeated: false,
      npcName: '冒险向导',
      npcAvatar: 'guide',
      npcLines: [
        '欢迎来到营地，年轻的冒险者！',
        '在这片土地上，你会遇到许多单词怪物。',
        '击败它们来学习新单词吧！',
        '提示：用方向键或 WASD 移动。',
      ],
    },
    {
      id: 'npc_sage', type: 'npc', tileX: 17, tileY: 8,
      wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '',
      defeated: false,
      npcName: '语法贤者',
      npcAvatar: 'sage',
      npcLines: [
        '我是语法贤者。让我教你一个小窍门。',
        '"I am" 可以缩写为 "I\'m"。',
        '"You are" 可以缩写为 "You\'re"。',
        '记住：am 只跟 I 搭配！',
      ],
    },
    // 宝箱
    {
      id: 'chest_1', type: 'treasure', tileX: 17, tileY: 4,
      wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '',
      defeated: false,
      reward: { xp: 20, coins: 15 }
    },
    {
      id: 'chest_2', type: 'treasure', tileX: 8, tileY: 9,
      wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '',
      defeated: false,
      reward: { xp: 15, coins: 10 }
    },
    {
      id: 'chest_3', type: 'treasure', tileX: 2, tileY: 11,
      wordEn: '', wordZh: '', phonetic: '', sentence: '', sentenceZh: '',
      defeated: false,
      reward: { xp: 15, coins: 10 }
    },
  ]
}
