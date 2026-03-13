// 营地探索场景 — 现代手绘风格
import Phaser from 'phaser'
import { Player } from '../sprites/Player'
import { defaultCampMap, TILE_WALKABLE, type MapEncounter, type CampMapData } from '../config/mapData'
import { generateTextures } from '../rendering/TextureGen'

const T = 64 // 瓦片尺寸

export class CampScene extends Phaser.Scene {
  private player!: Player
  private cursors!: Phaser.Types.Input.Keyboard.CursorKeys
  private wasd!: Record<string, Phaser.Input.Keyboard.Key>
  private mapData: CampMapData = defaultCampMap
  private wallLayer!: Phaser.Physics.Arcade.StaticGroup
  private encounterZones: Array<{ zone: Phaser.GameObjects.Zone; encounter: MapEncounter; sprites: Phaser.GameObjects.GameObject[] }> = []
  private activeEncounter: MapEncounter | null = null
  private paused = false
  private waterTiles: Phaser.GameObjects.Image[] = []
  private fireParticles: Phaser.GameObjects.Arc[] = []
  private fogGraphics!: Phaser.GameObjects.Graphics
  private revealedTiles: Set<string> = new Set()
  private pet!: Phaser.GameObjects.Image
  private petHistory: Array<{ x: number; y: number }> = []
  private petDelay = 12 // 玩家位置记录延迟帧数
  private lastFogTileX = -1
  private lastFogTileY = -1

  public onEncounter?: (encounter: MapEncounter) => void
  public onRandomEvent?: (event: { type: string; title: string; message: string; reward?: { xp?: number; coins?: number } }) => void
  private stepCount = 0
  private lastRandomEventStep = 0

  constructor() {
    super({ key: 'CampScene' })
  }

  /** Allow setting map data from Vue before scene starts */
  setMapData(data: CampMapData) {
    this.mapData = data
  }

  create() {
    // 生成所有纹理
    generateTextures(this)

    const worldW = this.mapData.width * T
    const worldH = this.mapData.height * T

    this.physics.world.setBounds(0, 0, worldW, worldH)

    // 画地图
    this.drawTilemap()

    // 碰撞墙
    this.createCollisionWalls()

    // 遭遇区
    this.createEncounterZones()

    // 玩家
    const startX = this.mapData.playerStart.x * T + T / 2
    const startY = this.mapData.playerStart.y * T + T / 2
    this.player = new Player(this, startX, startY)
    this.player.setDepth(10)

    // 宠物伴侣
    this.pet = this.add.image(startX + 20, startY + 16, 'sprite_pet')
    this.pet.setDepth(9)
    this.petHistory = Array.from({ length: this.petDelay }, () => ({ x: startX, y: startY }))

    this.physics.add.collider(this.player, this.wallLayer)

    for (const ez of this.encounterZones) {
      this.physics.add.overlap(this.player, ez.zone, () => {
        this.triggerEncounter(ez.encounter)
      })
    }

    // 摄像机
    this.cameras.main.setBounds(0, 0, worldW, worldH)
    this.cameras.main.startFollow(this.player, true, 0.08, 0.08)
    this.cameras.main.setZoom(1)
    this.cameras.main.setBackgroundColor(0x1a2a1a)

    // 输入
    this.cursors = this.input.keyboard!.createCursorKeys()
    this.wasd = {
      W: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.W, false),
      A: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.A, false),
      S: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.S, false),
      D: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.D, false),
    }

    // 环境特效
    this.addAmbientEffects()

    // 迷雾系统
    this.fogGraphics = this.add.graphics()
    this.fogGraphics.setDepth(250)
    this.updateFog()
  }

  private drawTilemap() {
    const { tiles } = this.mapData
    for (let y = 0; y < tiles.length; y++) {
      const row = tiles[y]
      if (!row) continue
      for (let x = 0; x < row.length; x++) {
        const tileType = row[x] ?? 0
        const px = x * T + T / 2
        const py = y * T + T / 2

        // 基础瓦片
        let textureKey = 'tile_grass'
        if (tileType === 1) textureKey = 'tile_path'
        else if (tileType === 2) textureKey = 'tile_water'
        else if (tileType === 0) {
          textureKey = ((x + y) % 3 === 0) ? 'tile_grass2' : 'tile_grass'
        } else {
          // 树/帐篷/篝火/NPC/宝箱/怪物/石头 — 底下铺草
          textureKey = 'tile_grass'
        }

        const tile = this.add.image(px, py, textureKey)
        tile.setDisplaySize(T, T)
        tile.setDepth(0)

        if (tileType === 2) {
          this.waterTiles.push(tile)
        }

        // 装饰物 (树/帐篷/篝火/石头)
        if (tileType === 3) {
          const tree = this.add.image(px, py - 12, 'sprite_tree')
          tree.setDepth(py / 10 + 1)
        } else if (tileType === 4) {
          const tent = this.add.image(px, py, 'sprite_tent')
          tent.setDepth(py / 10 + 1)
        } else if (tileType === 5) {
          const fire = this.add.image(px, py, 'sprite_campfire')
          fire.setDepth(py / 10 + 1)

          // 火焰动态光效
          const glow = this.add.circle(px, py, 48, 0xff8844, 0.06)
          glow.setDepth(0.5)
          this.tweens.add({
            targets: glow,
            alpha: { from: 0.03, to: 0.1 },
            scaleX: { from: 0.9, to: 1.15 },
            scaleY: { from: 0.9, to: 1.15 },
            duration: 700,
            yoyo: true,
            repeat: -1,
            ease: 'Sine.easeInOut',
          })

          // 火星粒子
          for (let i = 0; i < 4; i++) {
            const spark = this.add.circle(
              px + Phaser.Math.Between(-8, 8),
              py - 10,
              1.5,
              0xffaa44,
              0.7
            )
            spark.setDepth(100)
            this.fireParticles.push(spark)
            this.tweens.add({
              targets: spark,
              y: py - 30 - Math.random() * 20,
              x: px + Phaser.Math.Between(-15, 15),
              alpha: 0,
              duration: 1000 + Math.random() * 800,
              repeat: -1,
              delay: Math.random() * 1000,
              onRepeat: () => {
                spark.setPosition(px + Phaser.Math.Between(-8, 8), py - 10)
                spark.setAlpha(0.7)
              },
            })
          }
        } else if (tileType === 9) {
          const rock = this.add.image(px, py + 6, 'sprite_rock')
          rock.setDepth(py / 10 + 1)
        }
      }
    }
  }

  private createCollisionWalls() {
    this.wallLayer = this.physics.add.staticGroup()
    const { tiles } = this.mapData
    for (let y = 0; y < tiles.length; y++) {
      const row = tiles[y]
      if (!row) continue
      for (let x = 0; x < row.length; x++) {
        const tileType = row[x] ?? 0
        if (!TILE_WALKABLE[tileType]) {
          const wall = this.add.zone(x * T + T / 2, y * T + T / 2, T, T)
          this.wallLayer.add(wall)
        }
      }
    }
  }

  private createEncounterZones() {
    for (const encounter of this.mapData.encounters) {
      if (encounter.defeated) continue

      const px = encounter.tileX * T + T / 2
      const py = encounter.tileY * T + T / 2

      const zone = this.add.zone(px, py, T * 0.6, T * 0.6)
      this.physics.add.existing(zone, true)

      const sprites: Phaser.GameObjects.GameObject[] = []

      if (encounter.type === 'monster') {
        const diffKey = encounter.difficulty
          ? `sprite_monster_${encounter.difficulty}`
          : 'sprite_monster'
        const mon = this.add.image(px, py, diffKey)
        mon.setDepth(py / 10 + 1)
        sprites.push(mon)

        // 呼吸动画
        this.tweens.add({
          targets: mon,
          scaleY: { from: 1, to: 1.06 },
          scaleX: { from: 1, to: 0.96 },
          duration: 1200,
          yoyo: true,
          repeat: -1,
          ease: 'Sine.easeInOut',
        })

        // 危险光环
        const aura = this.add.circle(px, py + 4, 22, 0xff3333, 0.08)
        aura.setDepth(py / 10)
        sprites.push(aura)
        this.tweens.add({
          targets: aura,
          scaleX: 1.6,
          scaleY: 1.6,
          alpha: 0,
          duration: 1500,
          repeat: -1,
        })

        // 单词标签
        const label = this.add.text(px, py - 30, encounter.wordEn, {
          fontSize: '13px',
          color: '#ffdddd',
          fontFamily: 'Arial, sans-serif',
          backgroundColor: 'rgba(80, 20, 20, 0.7)',
          padding: { x: 6, y: 3 },
        }).setOrigin(0.5)
        label.setDepth(200)
        sprites.push(label)

        this.tweens.add({
          targets: label,
          y: py - 34,
          duration: 1500,
          yoyo: true,
          repeat: -1,
          ease: 'Sine.easeInOut',
        })

      } else if (encounter.type === 'npc') {
        const npcKey = encounter.npcAvatar ? `sprite_npc_${encounter.npcAvatar}` : 'sprite_npc'
        const npc = this.add.image(px, py - 8, npcKey)
        npc.setDepth(py / 10 + 1)
        sprites.push(npc)

        // 感叹号气泡
        const bubbleBg = this.add.circle(px + 14, py - 38, 10, 0xffffff, 0.9)
        bubbleBg.setDepth(201)
        sprites.push(bubbleBg)

        const excl = this.add.text(px + 14, py - 38, '!', {
          fontSize: '14px',
          color: '#cc6600',
          fontStyle: 'bold',
          fontFamily: 'Arial, sans-serif',
        }).setOrigin(0.5)
        excl.setDepth(202)
        sprites.push(excl)

        this.tweens.add({
          targets: [bubbleBg, excl],
          y: '-=3',
          duration: 800,
          yoyo: true,
          repeat: -1,
          ease: 'Sine.easeInOut',
        })

      } else if (encounter.type === 'treasure') {
        const chest = this.add.image(px, py + 2, 'sprite_chest')
        chest.setDepth(py / 10 + 1)
        sprites.push(chest)

        // 闪光
        const sparkle = this.add.circle(px, py - 6, 3, 0xffd700, 0.6)
        sparkle.setDepth(200)
        sprites.push(sparkle)
        this.tweens.add({
          targets: sparkle,
          alpha: { from: 0.2, to: 0.8 },
          scaleX: { from: 0.6, to: 1.2 },
          scaleY: { from: 0.6, to: 1.2 },
          duration: 900,
          yoyo: true,
          repeat: -1,
        })

        // 第二个闪光
        const sparkle2 = this.add.circle(px + 8, py - 2, 2, 0xffee88, 0.4)
        sparkle2.setDepth(200)
        sprites.push(sparkle2)
        this.tweens.add({
          targets: sparkle2,
          alpha: { from: 0.1, to: 0.6 },
          duration: 1200,
          yoyo: true,
          repeat: -1,
          delay: 400,
        })
      }

      this.encounterZones.push({ zone, encounter, sprites })
    }
  }

  private addAmbientEffects() {
    const worldW = this.mapData.width * T
    const worldH = this.mapData.height * T

    // 萤火虫
    for (let i = 0; i < 20; i++) {
      const x = Phaser.Math.Between(0, worldW)
      const y = Phaser.Math.Between(0, worldH)
      const size = 1.5 + Math.random() * 1.5
      const particle = this.add.circle(x, y, size, 0xccffaa, 0.3)
      particle.setDepth(150)

      this.tweens.add({
        targets: particle,
        x: x + Phaser.Math.Between(-80, 80),
        y: y + Phaser.Math.Between(-60, 60),
        alpha: { from: 0.05, to: 0.5 },
        duration: Phaser.Math.Between(4000, 8000),
        yoyo: true,
        repeat: -1,
        delay: Phaser.Math.Between(0, 4000),
        ease: 'Sine.easeInOut',
      })
    }

    // 暗角 vignette — 适配 RESIZE 模式
    this.vignetteGfx = this.add.graphics()
    this.vignetteGfx.setScrollFactor(0)
    this.vignetteGfx.setDepth(300)
    this.drawVignette()

    // 监听 resize 重绘暗角
    this.scale.on('resize', () => this.drawVignette())
  }

  private vignetteGfx!: Phaser.GameObjects.Graphics

  private drawVignette() {
    const vig = this.vignetteGfx
    vig.clear()
    const w = this.cameras.main.width
    const h = this.cameras.main.height
    const band = 30
    for (let i = 0; i < band; i++) {
      const alpha = 0.25 * (1 - i / band)
      vig.fillStyle(0x0a1a0a, alpha)
      vig.fillRect(0, i, w, 1)
      vig.fillRect(0, h - i, w, 1)
      vig.fillRect(i, 0, 1, h)
      vig.fillRect(w - i, 0, 1, h)
    }
  }

  private _waterTimer = 0

  private updateFog() {
    const fog = this.fogGraphics
    fog.clear()

    const playerTileX = Math.floor(this.player.x / T)
    const playerTileY = Math.floor(this.player.y / T)
    const revealRadius = 5

    // 记录玩家周围已揭露的瓦片
    for (let dy = -revealRadius; dy <= revealRadius; dy++) {
      for (let dx = -revealRadius; dx <= revealRadius; dx++) {
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist <= revealRadius) {
          this.revealedTiles.add(`${playerTileX + dx},${playerTileY + dy}`)
        }
      }
    }

    // 绘制迷雾 — 未揭露的瓦片覆盖暗色
    const { width, height } = this.mapData
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        const key = `${x},${y}`
        if (this.revealedTiles.has(key)) continue

        // 已揭露区域边缘的瓦片半透明
        const dx = x - playerTileX
        const dy = y - playerTileY
        const dist = Math.sqrt(dx * dx + dy * dy)
        const edgeAlpha = dist <= revealRadius + 2 ? 0.35 : 0.55

        fog.fillStyle(0x0a0a0a, edgeAlpha)
        fog.fillRect(x * T, y * T, T, T)
      }
    }
  }

  private triggerEncounter(encounter: MapEncounter) {
    if (this.paused || encounter.defeated) return
    this.paused = true
    this.activeEncounter = encounter

    const physBody = this.player.body as Phaser.Physics.Arcade.Body
    physBody.setVelocity(0)

    if (this.onEncounter) {
      this.onEncounter(encounter)
    }
  }

  public resolveEncounter(encounterId: string, success: boolean) {
    if (this.activeEncounter && this.activeEncounter.id === encounterId) {
      this.activeEncounter.defeated = true

      const idx = this.encounterZones.findIndex(ez => ez.encounter.id === encounterId)
      if (idx >= 0) {
        const found = this.encounterZones[idx]!
        found.zone.destroy()
        // 移除所有关联精灵
        for (const s of found.sprites) {
          if (s && 'destroy' in s) (s as Phaser.GameObjects.GameObject).destroy()
        }
        this.encounterZones.splice(idx, 1)
      }

      if (success) {
        const fx = this.activeEncounter
        const fxX = fx.tileX * T + T / 2
        const fxY = fx.tileY * T + T / 2

        // 胜利粒子爆炸
        for (let i = 0; i < 12; i++) {
          const colors = [0xffd700, 0xff8844, 0xffee88, 0xffffff]
          const p = this.add.circle(fxX, fxY, 2 + Math.random() * 3, colors[i % 4]!, 0.8)
          p.setDepth(250)
          this.tweens.add({
            targets: p,
            x: fxX + Phaser.Math.Between(-50, 50),
            y: fxY + Phaser.Math.Between(-50, 50),
            alpha: 0,
            scaleX: 0.2,
            scaleY: 0.2,
            duration: 600 + Math.random() * 400,
            ease: 'Power2',
            onComplete: () => p.destroy(),
          })
        }

        // "+XP" 文字
        if (fx.reward) {
          const xpText = this.add.text(fxX, fxY - 10, `+${fx.reward.xp} XP`, {
            fontSize: '16px',
            color: '#ffd700',
            fontStyle: 'bold',
            fontFamily: 'Arial, sans-serif',
            stroke: '#000',
            strokeThickness: 2,
          }).setOrigin(0.5).setDepth(260)

          this.tweens.add({
            targets: xpText,
            y: fxY - 50,
            alpha: 0,
            duration: 1200,
            ease: 'Power2',
            onComplete: () => xpText.destroy(),
          })
        }
      }
    }

    this.activeEncounter = null
    this.paused = false
  }

  update(_time: number, _delta: number) {
    if (this.paused) return
    this.player.updateMovement(this.cursors, this.wasd)

    // 步数计数（有移动时累加）
    const body = this.player.body as Phaser.Physics.Arcade.Body
    if (body && (Math.abs(body.velocity.x) > 10 || Math.abs(body.velocity.y) > 10)) {
      this.stepCount++
      // 每走约 200 步，有概率触发随机事件
      if (this.stepCount - this.lastRandomEventStep > 200 && Math.random() < 0.02) {
        this.triggerRandomEvent()
      }
    }

    // 宠物跟随 (延迟追踪玩家位置)
    this.petHistory.push({ x: this.player.x, y: this.player.y })
    if (this.petHistory.length > this.petDelay) {
      const target = this.petHistory.shift()!
      const lerpFactor = 0.15
      this.pet.x += (target.x + 18 - this.pet.x) * lerpFactor
      const baseY = target.y + 12
      this.pet.y += (baseY - this.pet.y) * lerpFactor
    }
    // 漂浮偏移
    this.pet.y += Math.sin(_time * 0.004) * 0.3
    // 深度跟随玩家
    this.pet.setDepth(this.player.depth - 1)

    // 水面轻微抖动
    this._waterTimer += _delta * 0.001
    for (const wt of this.waterTiles) {
      wt.setAlpha(0.85 + Math.sin(this._waterTimer * 2 + wt.x * 0.02) * 0.1)
    }

    // 迷雾更新 — 仅在玩家跨越新 tile 时重绘
    const fogTileX = Math.floor(this.player.x / T)
    const fogTileY = Math.floor(this.player.y / T)
    if (fogTileX !== this.lastFogTileX || fogTileY !== this.lastFogTileY) {
      this.lastFogTileX = fogTileX
      this.lastFogTileY = fogTileY
      this.updateFog()
    }
  }

  private triggerRandomEvent() {
    this.lastRandomEventStep = this.stepCount
    const events = [
      { type: 'coin', title: '✨ 发现金币！', message: '你在草丛中发现了几枚闪闪发光的金币！', reward: { coins: 5 } },
      { type: 'xp', title: '📚 灵感闪现！', message: '你突然回忆起之前学过的单词，感到充满力量！', reward: { xp: 8 } },
      { type: 'tip', title: '🗺️ 旅行者的提示', message: '一位路过的旅行者告诉你：多听发音能更好地记住单词哦！', reward: {} },
      { type: 'heal', title: '🌟 神秘泉水', message: '你发现了一处散发淡光的泉水，喝了一口感觉精力充沛！', reward: { xp: 5, coins: 3 } },
      { type: 'pet', title: '🐾 小猫咪很开心', message: '你的宠物伴侣发现了一朵有趣的花，开心地跳来跳去！', reward: { coins: 2 } },
      { type: 'star', title: '⭐ 幸运星', message: '天空中一颗流星划过，感觉今天运气特别好！', reward: { xp: 10 } },
    ]
    const event = events[Math.floor(Math.random() * events.length)]
    if (!event || !this.onRandomEvent) return
    this.paused = true
    const physBody = this.player.body as Phaser.Physics.Arcade.Body
    physBody.setVelocity(0)
    this.onRandomEvent(event)
  }

  public resumeAfterEvent() {
    this.paused = false
  }

  public getDefeatedMonsters(): MapEncounter[] {
    return this.mapData.encounters.filter(e => e.type === 'monster' && e.defeated)
  }
}
