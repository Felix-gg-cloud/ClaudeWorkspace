// 纹理生成器 — 用 Canvas 2D 生成高质量瓦片纹理
// 风格: 现代手绘 / Stardew Valley 类

export function generateTextures(scene: Phaser.Scene) {
  const T = 64 // tile size

  // ===== 草地纹理 =====
  createCanvasTexture(scene, 'tile_grass', T, T, (ctx) => {
    // 基础渐变
    const grad = ctx.createRadialGradient(T / 2, T / 2, 0, T / 2, T / 2, T * 0.7)
    grad.addColorStop(0, '#4a8c3f')
    grad.addColorStop(1, '#3d7a34')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, T, T)

    // 草丛细节
    ctx.strokeStyle = 'rgba(80, 160, 60, 0.35)'
    ctx.lineWidth = 1.5
    for (let i = 0; i < 12; i++) {
      const x = Math.random() * T
      const y = Math.random() * T
      const h = 4 + Math.random() * 6
      ctx.beginPath()
      ctx.moveTo(x, y)
      ctx.quadraticCurveTo(x + 2, y - h, x + (Math.random() - 0.5) * 4, y - h)
      ctx.stroke()
    }

    // 小花点缀
    for (let i = 0; i < 3; i++) {
      const x = 8 + Math.random() * (T - 16)
      const y = 8 + Math.random() * (T - 16)
      const colors = ['#ffee88', '#ee88cc', '#88ccff', '#ffffff']
      ctx.fillStyle = colors[Math.floor(Math.random() * colors.length)]!
      ctx.beginPath()
      ctx.arc(x, y, 1.5, 0, Math.PI * 2)
      ctx.fill()
    }
  })

  // ===== 草地变体 =====
  createCanvasTexture(scene, 'tile_grass2', T, T, (ctx) => {
    ctx.fillStyle = '#3e7832'
    ctx.fillRect(0, 0, T, T)
    const grad = ctx.createLinearGradient(0, 0, T, T)
    grad.addColorStop(0, 'rgba(70, 150, 55, 0.4)')
    grad.addColorStop(1, 'rgba(50, 110, 40, 0.4)')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, T, T)

    ctx.fillStyle = 'rgba(60, 140, 50, 0.3)'
    for (let i = 0; i < 6; i++) {
      const x = Math.random() * T
      const y = Math.random() * T
      ctx.beginPath()
      ctx.ellipse(x, y, 3, 2, Math.random() * Math.PI, 0, Math.PI * 2)
      ctx.fill()
    }
  })

  // ===== 路径纹理 =====
  createCanvasTexture(scene, 'tile_path', T, T, (ctx) => {
    // 泥土基底
    ctx.fillStyle = '#b09060'
    ctx.fillRect(0, 0, T, T)

    // 深色变化
    ctx.fillStyle = 'rgba(140, 110, 70, 0.5)'
    ctx.fillRect(2, 2, T - 4, T - 4)

    // 小石子
    for (let i = 0; i < 8; i++) {
      const x = 4 + Math.random() * (T - 8)
      const y = 4 + Math.random() * (T - 8)
      const r = 1 + Math.random() * 2.5
      const shade = 140 + Math.floor(Math.random() * 50)
      ctx.fillStyle = `rgb(${shade}, ${shade - 10}, ${shade - 30})`
      ctx.beginPath()
      ctx.ellipse(x, y, r, r * 0.7, Math.random() * Math.PI, 0, Math.PI * 2)
      ctx.fill()
    }

    // 边缘渐变
    ctx.fillStyle = 'rgba(80, 60, 40, 0.15)'
    ctx.fillRect(0, 0, T, 3)
    ctx.fillRect(0, T - 3, T, 3)
    ctx.fillRect(0, 0, 3, T)
    ctx.fillRect(T - 3, 0, 3, T)
  })

  // ===== 水纹理 =====
  createCanvasTexture(scene, 'tile_water', T, T, (ctx) => {
    const grad = ctx.createLinearGradient(0, 0, T, T)
    grad.addColorStop(0, '#2a5c8a')
    grad.addColorStop(0.5, '#3070a0')
    grad.addColorStop(1, '#266088')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, T, T)

    // 波纹
    ctx.strokeStyle = 'rgba(100, 180, 255, 0.25)'
    ctx.lineWidth = 1
    for (let i = 0; i < 4; i++) {
      const y = 10 + i * 14
      ctx.beginPath()
      ctx.moveTo(0, y)
      ctx.bezierCurveTo(T * 0.25, y - 4, T * 0.75, y + 4, T, y)
      ctx.stroke()
    }

    // 高光
    ctx.fillStyle = 'rgba(150, 220, 255, 0.12)'
    ctx.beginPath()
    ctx.ellipse(T * 0.3, T * 0.4, 8, 4, -0.3, 0, Math.PI * 2)
    ctx.fill()
  })

  // ===== 树木纹理 (64x80 — 比瓦片高) =====
  createCanvasTexture(scene, 'sprite_tree', 64, 80, (ctx) => {
    // 树干
    ctx.fillStyle = '#6b4423'
    roundRect(ctx, 26, 45, 12, 30, 3)

    // 树干纹理
    ctx.strokeStyle = 'rgba(80, 50, 20, 0.5)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(30, 50)
    ctx.lineTo(31, 70)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(34, 52)
    ctx.lineTo(35, 68)
    ctx.stroke()

    // 阴影在底部
    ctx.fillStyle = 'rgba(0, 0, 0, 0.15)'
    ctx.beginPath()
    ctx.ellipse(32, 75, 16, 6, 0, 0, Math.PI * 2)
    ctx.fill()

    // 树冠 (多层)
    drawTreeCanopy(ctx, 32, 30, 28, '#2d724a')
    drawTreeCanopy(ctx, 28, 22, 22, '#35855a')
    drawTreeCanopy(ctx, 36, 18, 18, '#3d9965')
    drawTreeCanopy(ctx, 32, 12, 14, '#45aa70')

    // 高光
    ctx.fillStyle = 'rgba(120, 220, 140, 0.2)'
    ctx.beginPath()
    ctx.ellipse(26, 16, 6, 8, -0.3, 0, Math.PI * 2)
    ctx.fill()
  })

  // ===== 帐篷纹理 =====
  createCanvasTexture(scene, 'sprite_tent', 64, 64, (ctx) => {
    // 阴影
    ctx.fillStyle = 'rgba(0, 0, 0, 0.12)'
    ctx.beginPath()
    ctx.ellipse(32, 56, 28, 8, 0, 0, Math.PI * 2)
    ctx.fill()

    // 帐篷主体
    ctx.fillStyle = '#8b6547'
    ctx.beginPath()
    ctx.moveTo(32, 8)
    ctx.lineTo(4, 52)
    ctx.lineTo(60, 52)
    ctx.closePath()
    ctx.fill()

    // 帐篷门
    ctx.fillStyle = '#5a3d28'
    ctx.beginPath()
    ctx.moveTo(32, 30)
    ctx.lineTo(22, 52)
    ctx.lineTo(42, 52)
    ctx.closePath()
    ctx.fill()

    // 帐篷条纹
    ctx.strokeStyle = 'rgba(200, 160, 120, 0.3)'
    ctx.lineWidth = 1.5
    ctx.beginPath()
    ctx.moveTo(32, 10)
    ctx.lineTo(12, 48)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(32, 10)
    ctx.lineTo(52, 48)
    ctx.stroke()

    // 顶部旗帜
    ctx.fillStyle = '#cc4444'
    ctx.beginPath()
    ctx.moveTo(32, 4)
    ctx.lineTo(40, 10)
    ctx.lineTo(32, 14)
    ctx.closePath()
    ctx.fill()
  })

  // ===== 篝火纹理 =====
  createCanvasTexture(scene, 'sprite_campfire', 48, 48, (ctx) => {
    // 光圈
    const grad = ctx.createRadialGradient(24, 30, 4, 24, 30, 24)
    grad.addColorStop(0, 'rgba(255, 150, 50, 0.15)')
    grad.addColorStop(1, 'rgba(255, 100, 20, 0)')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, 48, 48)

    // 木柴
    ctx.fillStyle = '#5c3a1e'
    ctx.save()
    ctx.translate(24, 38)
    ctx.rotate(-0.3)
    roundRect(ctx, -12, -3, 24, 6, 2)
    ctx.restore()
    ctx.save()
    ctx.translate(24, 38)
    ctx.rotate(0.3)
    roundRect(ctx, -12, -3, 24, 6, 2)
    ctx.restore()

    // 火焰
    ctx.fillStyle = '#ff6622'
    ctx.beginPath()
    ctx.moveTo(24, 12)
    ctx.quadraticCurveTo(12, 28, 16, 36)
    ctx.lineTo(32, 36)
    ctx.quadraticCurveTo(36, 28, 24, 12)
    ctx.fill()

    ctx.fillStyle = '#ffaa22'
    ctx.beginPath()
    ctx.moveTo(24, 18)
    ctx.quadraticCurveTo(16, 30, 20, 34)
    ctx.lineTo(28, 34)
    ctx.quadraticCurveTo(32, 30, 24, 18)
    ctx.fill()

    ctx.fillStyle = '#ffee66'
    ctx.beginPath()
    ctx.moveTo(24, 24)
    ctx.quadraticCurveTo(20, 32, 22, 34)
    ctx.lineTo(26, 34)
    ctx.quadraticCurveTo(28, 32, 24, 24)
    ctx.fill()
  })

  // ===== 石头纹理 =====
  createCanvasTexture(scene, 'sprite_rock', 48, 40, (ctx) => {
    // 阴影
    ctx.fillStyle = 'rgba(0, 0, 0, 0.1)'
    ctx.beginPath()
    ctx.ellipse(24, 34, 20, 6, 0, 0, Math.PI * 2)
    ctx.fill()

    // 主石体
    ctx.fillStyle = '#777'
    ctx.beginPath()
    ctx.moveTo(8, 30)
    ctx.quadraticCurveTo(4, 18, 16, 10)
    ctx.quadraticCurveTo(24, 6, 36, 12)
    ctx.quadraticCurveTo(44, 20, 40, 30)
    ctx.closePath()
    ctx.fill()

    // 高光
    ctx.fillStyle = 'rgba(180, 180, 190, 0.4)'
    ctx.beginPath()
    ctx.ellipse(20, 16, 8, 5, -0.4, 0, Math.PI * 2)
    ctx.fill()

    // 裂纹
    ctx.strokeStyle = 'rgba(50, 50, 50, 0.3)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(22, 15)
    ctx.lineTo(28, 25)
    ctx.lineTo(32, 22)
    ctx.stroke()
  })

  // ===== 怪物精灵辅助 =====
  function drawMonsterShadow(ctx: CanvasRenderingContext2D, x: number, y: number, rx: number) {
    ctx.fillStyle = 'rgba(0, 0, 0, 0.18)'
    ctx.beginPath()
    ctx.ellipse(x, y, rx, 4, 0, 0, Math.PI * 2)
    ctx.fill()
  }

  function drawMonsterEye(ctx: CanvasRenderingContext2D, x: number, y: number, size: number, pupilColor: string, angry?: boolean) {
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.ellipse(x, y, size, size * 1.2, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.strokeStyle = 'rgba(0,0,0,0.3)'
    ctx.lineWidth = 0.5
    ctx.stroke()
    const irisGrad = ctx.createRadialGradient(x, y + 0.5, 0, x, y + 0.5, size * 0.7)
    irisGrad.addColorStop(0, pupilColor)
    irisGrad.addColorStop(1, 'rgba(0,0,0,0.8)')
    ctx.fillStyle = irisGrad
    ctx.beginPath()
    ctx.arc(x + 0.5, y + 0.5, size * 0.6, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#000'
    ctx.beginPath()
    ctx.arc(x + 0.5, y + 0.5, size * 0.25, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(x - size * 0.2, y - size * 0.3, size * 0.2, 0, Math.PI * 2)
    ctx.fill()
    if (angry) {
      ctx.strokeStyle = 'rgba(0,0,0,0.7)'
      ctx.lineWidth = 1.5
      ctx.beginPath()
      ctx.moveTo(x - size, y - size * 1.3)
      ctx.lineTo(x + size * 0.5, y - size * 0.8)
      ctx.stroke()
    }
  }

  // ===== 简单怪物 (绿色小蘑菇精灵) =====
  createCanvasTexture(scene, 'sprite_monster_easy', 48, 48, (ctx) => {
    drawMonsterShadow(ctx, 24, 44, 12)
    // 蘑菇伞盖
    const capGrad = ctx.createRadialGradient(24, 18, 2, 24, 22, 16)
    capGrad.addColorStop(0, '#55dd55')
    capGrad.addColorStop(0.6, '#33aa33')
    capGrad.addColorStop(1, '#228822')
    ctx.fillStyle = capGrad
    ctx.beginPath()
    ctx.ellipse(24, 20, 18, 14, 0, Math.PI, Math.PI * 2)
    ctx.fill()
    // 伞盖底边
    ctx.fillStyle = '#228822'
    ctx.fillRect(6, 19, 36, 3)
    // 伞盖斑点
    ctx.fillStyle = 'rgba(255, 255, 200, 0.5)'
    ctx.beginPath()
    ctx.arc(16, 12, 3, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(28, 10, 2.5, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(34, 15, 2, 0, Math.PI * 2)
    ctx.fill()
    // 蘑菇柄 (身体)
    ctx.fillStyle = '#ffe8cc'
    ctx.beginPath()
    ctx.moveTo(16, 22)
    ctx.lineTo(14, 40)
    ctx.quadraticCurveTo(24, 44, 34, 40)
    ctx.lineTo(32, 22)
    ctx.closePath()
    ctx.fill()
    // 柄高光
    ctx.fillStyle = 'rgba(255, 255, 255, 0.3)'
    ctx.beginPath()
    ctx.ellipse(22, 30, 4, 8, -0.1, 0, Math.PI * 2)
    ctx.fill()
    // 大眼睛
    drawMonsterEye(ctx, 20, 30, 4, '#22aa22')
    drawMonsterEye(ctx, 30, 30, 4, '#22aa22')
    // 微笑
    ctx.strokeStyle = '#886644'
    ctx.lineWidth = 1.5
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.arc(25, 35, 4, 0.2, Math.PI - 0.2)
    ctx.stroke()
    // 小脚
    ctx.fillStyle = '#ddbb88'
    ctx.beginPath()
    ctx.ellipse(18, 42, 4, 2, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.ellipse(30, 42, 4, 2, 0, 0, Math.PI * 2)
    ctx.fill()
  })

  // ===== 简单怪物变体2 (黄色仙人掌小精灵) =====
  createCanvasTexture(scene, 'sprite_monster_easy_2', 48, 48, (ctx) => {
    drawMonsterShadow(ctx, 24, 44, 10)
    // 身体 — 仙人掌柱
    const bodyG = ctx.createLinearGradient(16, 10, 32, 40)
    bodyG.addColorStop(0, '#66cc44')
    bodyG.addColorStop(0.5, '#44aa33')
    bodyG.addColorStop(1, '#338822')
    ctx.fillStyle = bodyG
    ctx.beginPath()
    ctx.moveTo(17, 42)
    ctx.quadraticCurveTo(16, 12, 24, 8)
    ctx.quadraticCurveTo(32, 12, 31, 42)
    ctx.closePath()
    ctx.fill()
    // 左臂
    ctx.beginPath()
    ctx.moveTo(17, 24)
    ctx.quadraticCurveTo(8, 22, 8, 16)
    ctx.quadraticCurveTo(8, 12, 12, 14)
    ctx.quadraticCurveTo(14, 16, 17, 20)
    ctx.closePath()
    ctx.fill()
    // 右臂
    ctx.beginPath()
    ctx.moveTo(31, 22)
    ctx.quadraticCurveTo(40, 20, 40, 14)
    ctx.quadraticCurveTo(40, 10, 36, 12)
    ctx.quadraticCurveTo(34, 14, 31, 18)
    ctx.closePath()
    ctx.fill()
    // 高光
    ctx.fillStyle = 'rgba(150, 255, 100, 0.25)'
    ctx.beginPath()
    ctx.ellipse(21, 22, 3, 10, 0, 0, Math.PI * 2)
    ctx.fill()
    // 小花
    ctx.fillStyle = '#ffdd44'
    ctx.beginPath()
    ctx.arc(26, 10, 4, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#ff9922'
    ctx.beginPath()
    ctx.arc(26, 10, 2, 0, Math.PI * 2)
    ctx.fill()
    // 眼睛
    drawMonsterEye(ctx, 21, 24, 3, '#228800')
    drawMonsterEye(ctx, 28, 24, 3, '#228800')
    // 微笑
    ctx.strokeStyle = '#226622'
    ctx.lineWidth = 1.5
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.arc(24.5, 31, 3, 0.3, Math.PI - 0.3)
    ctx.stroke()
  })

  // ===== 简单怪物变体3 (粉色果冻虫) =====
  createCanvasTexture(scene, 'sprite_monster_easy_3', 48, 48, (ctx) => {
    drawMonsterShadow(ctx, 24, 44, 12)
    // 身体 — 毛毛虫
    const segs: [number, number, number, string][] = [
      [14, 38, 7, '#ff88aa'],
      [22, 34, 8, '#ff6699'],
      [30, 36, 7.5, '#ff88aa'],
      [36, 40, 6.5, '#ff6699'],
    ]
    for (const [sx, sy, sr, sc] of segs) {
      const sg = ctx.createRadialGradient(sx - 1, sy - 1, 1, sx, sy, sr)
      sg.addColorStop(0, '#ffaacc')
      sg.addColorStop(0.6, sc)
      sg.addColorStop(1, '#cc4477')
      ctx.fillStyle = sg
      ctx.beginPath()
      ctx.arc(sx, sy, sr, 0, Math.PI * 2)
      ctx.fill()
    }
    // 头部
    const hg = ctx.createRadialGradient(12, 28, 2, 14, 30, 10)
    hg.addColorStop(0, '#ffbbdd')
    hg.addColorStop(0.6, '#ff6699')
    hg.addColorStop(1, '#cc3366')
    ctx.fillStyle = hg
    ctx.beginPath()
    ctx.arc(14, 30, 10, 0, Math.PI * 2)
    ctx.fill()
    // 触角
    ctx.strokeStyle = '#cc4477'
    ctx.lineWidth = 1.5
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(10, 22)
    ctx.quadraticCurveTo(6, 14, 4, 16)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(16, 22)
    ctx.quadraticCurveTo(18, 14, 22, 16)
    ctx.stroke()
    // 触角球
    ctx.fillStyle = '#ffaacc'
    ctx.beginPath()
    ctx.arc(4, 16, 2, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(22, 16, 2, 0, Math.PI * 2)
    ctx.fill()
    // 眼睛
    drawMonsterEye(ctx, 10, 28, 3.5, '#cc2266')
    drawMonsterEye(ctx, 18, 28, 3.5, '#cc2266')
    // 微笑
    ctx.strokeStyle = '#882244'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(14, 33, 3, 0.2, Math.PI - 0.2)
    ctx.stroke()
    // 小脚
    ctx.fillStyle = '#cc4477'
    for (const [sx, sy] of [[12, 39], [16, 38], [21, 37], [26, 40], [30, 43], [35, 45]] as const) {
      ctx.beginPath()
      ctx.ellipse(sx, sy + 2, 1.5, 1, 0, 0, Math.PI * 2)
      ctx.fill()
    }
  })

  // ===== 中等怪物 (蓝色独眼蝙蝠) =====
  createCanvasTexture(scene, 'sprite_monster_medium', 48, 48, (ctx) => {
    drawMonsterShadow(ctx, 24, 44, 10)
    // 翅膀 — 左
    const wingGrad = ctx.createLinearGradient(0, 20, 20, 20)
    wingGrad.addColorStop(0, '#3355bb')
    wingGrad.addColorStop(1, '#5577dd')
    ctx.fillStyle = wingGrad
    ctx.beginPath()
    ctx.moveTo(14, 20)
    ctx.quadraticCurveTo(2, 10, 4, 22)
    ctx.quadraticCurveTo(2, 32, 6, 28)
    ctx.quadraticCurveTo(4, 36, 10, 32)
    ctx.lineTo(14, 28)
    ctx.closePath()
    ctx.fill()
    // 翅膀 — 右
    const wingGrad2 = ctx.createLinearGradient(28, 20, 48, 20)
    wingGrad2.addColorStop(0, '#5577dd')
    wingGrad2.addColorStop(1, '#3355bb')
    ctx.fillStyle = wingGrad2
    ctx.beginPath()
    ctx.moveTo(34, 20)
    ctx.quadraticCurveTo(46, 10, 44, 22)
    ctx.quadraticCurveTo(46, 32, 42, 28)
    ctx.quadraticCurveTo(44, 36, 38, 32)
    ctx.lineTo(34, 28)
    ctx.closePath()
    ctx.fill()
    // 身体
    const bg = ctx.createRadialGradient(24, 24, 2, 24, 26, 12)
    bg.addColorStop(0, '#88aaff')
    bg.addColorStop(0.6, '#5577dd')
    bg.addColorStop(1, '#3355bb')
    ctx.fillStyle = bg
    ctx.beginPath()
    ctx.ellipse(24, 26, 12, 14, 0, 0, Math.PI * 2)
    ctx.fill()
    // 耳朵
    ctx.fillStyle = '#4466cc'
    ctx.beginPath()
    ctx.moveTo(16, 16)
    ctx.lineTo(12, 6)
    ctx.lineTo(20, 14)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(32, 16)
    ctx.lineTo(36, 6)
    ctx.lineTo(28, 14)
    ctx.fill()
    // 大独眼
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.ellipse(24, 22, 7, 8, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.strokeStyle = 'rgba(0,0,0,0.2)'
    ctx.lineWidth = 0.5
    ctx.stroke()
    // 虹膜
    const irisG = ctx.createRadialGradient(24, 23, 0, 24, 23, 5)
    irisG.addColorStop(0, '#3399ff')
    irisG.addColorStop(1, '#1144aa')
    ctx.fillStyle = irisG
    ctx.beginPath()
    ctx.arc(24, 23, 4.5, 0, Math.PI * 2)
    ctx.fill()
    // 瞳孔
    ctx.fillStyle = '#000'
    ctx.beginPath()
    ctx.arc(24, 23, 2, 0, Math.PI * 2)
    ctx.fill()
    // 高光
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(22, 21, 1.5, 0, Math.PI * 2)
    ctx.fill()
    // 小嘴
    ctx.fillStyle = '#223366'
    ctx.beginPath()
    ctx.ellipse(24, 32, 3, 2, 0, 0, Math.PI * 2)
    ctx.fill()
    // 尖齿
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.moveTo(22, 31)
    ctx.lineTo(23, 34)
    ctx.lineTo(24, 31)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(24, 31)
    ctx.lineTo(25, 34)
    ctx.lineTo(26, 31)
    ctx.fill()
  })

  // ===== 困难怪物 (紫色暗影恶魔) =====
  createCanvasTexture(scene, 'sprite_monster_hard', 48, 48, (ctx) => {
    drawMonsterShadow(ctx, 24, 44, 15)
    // 身体 — 倒三角
    const bg = ctx.createRadialGradient(22, 24, 2, 24, 26, 18)
    bg.addColorStop(0, '#cc66dd')
    bg.addColorStop(0.5, '#9933aa')
    bg.addColorStop(1, '#662277')
    ctx.fillStyle = bg
    ctx.beginPath()
    ctx.moveTo(8, 42)
    ctx.lineTo(4, 28)
    ctx.quadraticCurveTo(6, 16, 16, 12)
    ctx.quadraticCurveTo(24, 10, 32, 12)
    ctx.quadraticCurveTo(42, 16, 44, 28)
    ctx.lineTo(40, 42)
    ctx.closePath()
    ctx.fill()
    // 暗纹理
    ctx.fillStyle = 'rgba(40, 0, 60, 0.15)'
    for (let i = 0; i < 3; i++) {
      ctx.beginPath()
      ctx.moveTo(10 + i * 10, 18 + i * 4)
      ctx.lineTo(14 + i * 10, 38 - i * 2)
      ctx.lineTo(18 + i * 10, 18 + i * 4)
      ctx.fill()
    }
    // 犄角
    ctx.fillStyle = '#553366'
    ctx.beginPath()
    ctx.moveTo(14, 14)
    ctx.lineTo(8, 2)
    ctx.lineTo(18, 12)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(34, 14)
    ctx.lineTo(40, 2)
    ctx.lineTo(30, 12)
    ctx.fill()
    // 角尖高光
    ctx.fillStyle = '#ddaaee'
    ctx.beginPath()
    ctx.arc(8, 3, 1.5, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(40, 3, 1.5, 0, Math.PI * 2)
    ctx.fill()
    // 高光
    ctx.fillStyle = 'rgba(220, 180, 255, 0.25)'
    ctx.beginPath()
    ctx.ellipse(18, 20, 5, 7, -0.3, 0, Math.PI * 2)
    ctx.fill()
    // 眼睛 — 愤怒
    drawMonsterEye(ctx, 17, 24, 4.5, '#cc33ff', true)
    drawMonsterEye(ctx, 31, 24, 4.5, '#cc33ff', true)
    // 尖牙嘴巴
    ctx.fillStyle = '#330033'
    ctx.beginPath()
    ctx.moveTo(16, 32)
    ctx.quadraticCurveTo(24, 38, 32, 32)
    ctx.quadraticCurveTo(24, 34, 16, 32)
    ctx.fill()
    // 尖牙
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.moveTo(18, 32)
    ctx.lineTo(20, 36)
    ctx.lineTo(22, 32)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(26, 32)
    ctx.lineTo(28, 36)
    ctx.lineTo(30, 32)
    ctx.fill()
    // 暗能量粒子
    ctx.fillStyle = 'rgba(200, 100, 255, 0.5)'
    const pts: number[][] = [[5, 20], [43, 22], [6, 36], [42, 38], [24, 6]]
    for (const p of pts) {
      ctx.beginPath()
      ctx.arc(p[0]!, p[1]!, 1, 0, Math.PI * 2)
      ctx.fill()
    }
  })

  // ===== 默认怪物 (橙色小狐狸 - 向后兼容) =====
  createCanvasTexture(scene, 'sprite_monster', 48, 48, (ctx) => {
    drawMonsterShadow(ctx, 24, 44, 12)
    // 身体
    const bg = ctx.createRadialGradient(24, 28, 3, 24, 30, 14)
    bg.addColorStop(0, '#ff9955')
    bg.addColorStop(0.6, '#ee7733')
    bg.addColorStop(1, '#cc5522')
    ctx.fillStyle = bg
    ctx.beginPath()
    ctx.ellipse(24, 30, 12, 14, 0, 0, Math.PI * 2)
    ctx.fill()
    // 耳朵
    ctx.fillStyle = '#ee7733'
    ctx.beginPath()
    ctx.moveTo(14, 20)
    ctx.lineTo(10, 8)
    ctx.lineTo(20, 18)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(34, 20)
    ctx.lineTo(38, 8)
    ctx.lineTo(28, 18)
    ctx.fill()
    // 内耳
    ctx.fillStyle = '#ffccaa'
    ctx.beginPath()
    ctx.moveTo(15, 19)
    ctx.lineTo(12, 11)
    ctx.lineTo(19, 18)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(33, 19)
    ctx.lineTo(36, 11)
    ctx.lineTo(29, 18)
    ctx.fill()
    // 眼睛
    drawMonsterEye(ctx, 19, 26, 3.5, '#cc5500')
    drawMonsterEye(ctx, 29, 26, 3.5, '#cc5500')
    // 鼻子
    ctx.fillStyle = '#332211'
    ctx.beginPath()
    ctx.ellipse(24, 31, 2, 1.5, 0, 0, Math.PI * 2)
    ctx.fill()
    // 嘴
    ctx.strokeStyle = '#663311'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(24, 33, 3, 0.2, Math.PI - 0.2)
    ctx.stroke()
    // 尾巴
    ctx.fillStyle = '#ee7733'
    ctx.beginPath()
    ctx.moveTo(36, 36)
    ctx.quadraticCurveTo(46, 30, 44, 24)
    ctx.quadraticCurveTo(42, 28, 38, 34)
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.moveTo(42, 26)
    ctx.quadraticCurveTo(44, 24, 43, 28)
    ctx.fill()
  })

  // ===== NPC 精灵 — 4 种类型 =====
  // 通用 NPC 辅助
  function drawNpcBase(ctx: CanvasRenderingContext2D, headColor: string, robeColor1: string, robeColor2: string) {
    // 阴影
    ctx.fillStyle = 'rgba(0, 0, 0, 0.15)'
    ctx.beginPath()
    ctx.ellipse(24, 60, 14, 4, 0, 0, Math.PI * 2)
    ctx.fill()
    // 长袍
    const rg = ctx.createLinearGradient(24, 24, 24, 58)
    rg.addColorStop(0, robeColor1)
    rg.addColorStop(1, robeColor2)
    ctx.fillStyle = rg
    ctx.beginPath()
    ctx.moveTo(14, 28)
    ctx.lineTo(10, 58)
    ctx.lineTo(38, 58)
    ctx.lineTo(34, 28)
    ctx.closePath()
    ctx.fill()
    // 头部
    ctx.fillStyle = headColor
    ctx.beginPath()
    ctx.arc(24, 20, 10, 0, Math.PI * 2)
    ctx.fill()
    // 眼睛
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.ellipse(20, 19, 3, 2.5, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.ellipse(28, 19, 3, 2.5, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#222'
    ctx.beginPath()
    ctx.arc(20.5, 19, 1.5, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(28.5, 19, 1.5, 0, Math.PI * 2)
    ctx.fill()
    // 高光
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(19.5, 17.5, 0.7, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(27.5, 17.5, 0.7, 0, Math.PI * 2)
    ctx.fill()
  }

  // 贤者 (紫袍 + 尖帽 + 法杖)
  createCanvasTexture(scene, 'sprite_npc_sage', 48, 64, (ctx) => {
    drawNpcBase(ctx, '#ffcc88', '#6655aa', '#443388')
    // 腰带
    ctx.fillStyle = '#daa520'
    ctx.fillRect(14, 36, 20, 3)
    // 帽子
    ctx.fillStyle = '#5544aa'
    ctx.beginPath()
    ctx.moveTo(24, 0)
    ctx.quadraticCurveTo(10, 8, 12, 18)
    ctx.lineTo(36, 18)
    ctx.quadraticCurveTo(38, 8, 24, 0)
    ctx.fill()
    ctx.fillStyle = '#4433aa'
    ctx.beginPath()
    ctx.ellipse(24, 16, 16, 4, 0, 0, Math.PI * 2)
    ctx.fill()
    // 帽子星星
    ctx.fillStyle = '#ffd700'
    ctx.globalAlpha = 0.8
    ctx.beginPath()
    ctx.arc(26, 8, 2, 0, Math.PI * 2)
    ctx.fill()
    ctx.globalAlpha = 1
    // 法杖
    ctx.strokeStyle = '#8B4513'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(40, 16)
    ctx.lineTo(42, 58)
    ctx.stroke()
    ctx.fillStyle = '#aa77ff'
    ctx.beginPath()
    ctx.arc(40, 14, 4, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = 'rgba(170,120,255,0.4)'
    ctx.beginPath()
    ctx.arc(40, 14, 7, 0, Math.PI * 2)
    ctx.fill()
    // 胡子
    ctx.fillStyle = '#ccc'
    ctx.beginPath()
    ctx.moveTo(20, 24)
    ctx.quadraticCurveTo(24, 38, 24, 40)
    ctx.quadraticCurveTo(24, 38, 28, 24)
    ctx.closePath()
    ctx.fill()
    // 微笑
    ctx.strokeStyle = '#884444'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(24, 23, 4, 0.1, Math.PI - 0.1)
    ctx.stroke()
  })

  // 向导 (绿衣 + 弓 + 羽毛帽)
  createCanvasTexture(scene, 'sprite_npc_guide', 48, 64, (ctx) => {
    drawNpcBase(ctx, '#f5c78a', '#2d7d46', '#1a5530')
    // 皮甲内衬
    ctx.fillStyle = '#8b6914'
    ctx.fillRect(17, 28, 14, 18)
    // 腰带
    ctx.fillStyle = '#5a3a1a'
    ctx.fillRect(14, 38, 20, 3)
    ctx.fillStyle = '#daa520'
    ctx.beginPath()
    ctx.arc(24, 39, 2, 0, Math.PI * 2)
    ctx.fill()
    // 帽子
    ctx.fillStyle = '#2a6e3a'
    ctx.beginPath()
    ctx.moveTo(24, 4)
    ctx.quadraticCurveTo(12, 12, 14, 18)
    ctx.lineTo(34, 18)
    ctx.quadraticCurveTo(36, 12, 24, 4)
    ctx.fill()
    // 羽毛
    ctx.strokeStyle = '#ff5533'
    ctx.lineWidth = 1.5
    ctx.beginPath()
    ctx.moveTo(32, 12)
    ctx.quadraticCurveTo(38, 2, 35, -2)
    ctx.stroke()
    // 短发
    ctx.fillStyle = '#664422'
    ctx.beginPath()
    ctx.ellipse(24, 14, 11, 6, 0, Math.PI, Math.PI * 2)
    ctx.fill()
    // 弓
    ctx.strokeStyle = '#8B4513'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(6, 22)
    ctx.quadraticCurveTo(0, 40, 6, 56)
    ctx.stroke()
    ctx.strokeStyle = '#aaa'
    ctx.lineWidth = 0.5
    ctx.beginPath()
    ctx.moveTo(6, 22)
    ctx.lineTo(6, 56)
    ctx.stroke()
    // 微笑
    ctx.strokeStyle = '#995544'
    ctx.lineWidth = 1.2
    ctx.beginPath()
    ctx.arc(24, 22, 5, 0.1, Math.PI - 0.1)
    ctx.stroke()
  })

  // 骑士 (铁甲 + 红披风 + 剑)
  createCanvasTexture(scene, 'sprite_npc_knight', 48, 64, (ctx) => {
    // 红披风
    ctx.fillStyle = '#8b2222'
    ctx.beginPath()
    ctx.moveTo(12, 26)
    ctx.quadraticCurveTo(6, 42, 8, 58)
    ctx.lineTo(40, 58)
    ctx.quadraticCurveTo(42, 42, 36, 26)
    ctx.closePath()
    ctx.fill()
    drawNpcBase(ctx, '#eebb88', '#999', '#777')
    // 铁甲高光
    ctx.fillStyle = 'rgba(255,255,255,0.15)'
    ctx.fillRect(18, 28, 4, 20)
    // 肩甲
    ctx.fillStyle = '#aaa'
    ctx.beginPath()
    ctx.ellipse(14, 28, 6, 4, 0.3, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.ellipse(34, 28, 6, 4, -0.3, 0, Math.PI * 2)
    ctx.fill()
    // 短发
    ctx.fillStyle = '#333'
    ctx.beginPath()
    ctx.ellipse(24, 14, 11, 6, 0, Math.PI, Math.PI * 2)
    ctx.fill()
    // 疤痕
    ctx.strokeStyle = 'rgba(180,80,60,0.6)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(28, 17)
    ctx.lineTo(30, 23)
    ctx.stroke()
    // 剑
    ctx.strokeStyle = '#bbb'
    ctx.lineWidth = 2.5
    ctx.beginPath()
    ctx.moveTo(42, 12)
    ctx.lineTo(44, 56)
    ctx.stroke()
    ctx.strokeStyle = 'rgba(255,255,255,0.4)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(42, 14)
    ctx.lineTo(43, 50)
    ctx.stroke()
    ctx.fillStyle = '#daa520'
    ctx.fillRect(38, 15, 8, 3)
    // 嘴
    ctx.strokeStyle = '#996644'
    ctx.lineWidth = 1.2
    ctx.beginPath()
    ctx.moveTo(21, 23)
    ctx.lineTo(27, 23)
    ctx.stroke()
  })

  // 商人/学者 (棕大衣 + 眼镜 + 书)
  createCanvasTexture(scene, 'sprite_npc_merchant', 48, 64, (ctx) => {
    drawNpcBase(ctx, '#ffcc88', '#8b6914', '#5a4510')
    // 内衬
    ctx.fillStyle = '#eed8a0'
    ctx.fillRect(18, 28, 12, 18)
    // 纽扣
    ctx.fillStyle = '#daa520'
    for (let y = 32; y <= 42; y += 5) {
      ctx.beginPath()
      ctx.arc(24, y, 1.2, 0, Math.PI * 2)
      ctx.fill()
    }
    // 整齐头发
    ctx.fillStyle = '#554430'
    ctx.beginPath()
    ctx.ellipse(24, 14, 12, 6, 0, Math.PI, Math.PI * 2)
    ctx.fill()
    // 圆框眼镜
    ctx.strokeStyle = '#b8860b'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(20, 19, 4.5, 0, Math.PI * 2)
    ctx.stroke()
    ctx.beginPath()
    ctx.arc(28, 19, 4.5, 0, Math.PI * 2)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(24.5, 19)
    ctx.lineTo(23.5, 19)
    ctx.stroke()
    // 书
    ctx.fillStyle = '#8b1a1a'
    ctx.save()
    ctx.translate(4, 36)
    ctx.rotate(-0.15)
    ctx.fillRect(0, 0, 10, 14)
    ctx.fillStyle = '#ffe8c0'
    ctx.fillRect(1, 1, 8, 12)
    ctx.restore()
    // 微笑
    ctx.strokeStyle = '#aa7755'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(24, 23, 4, 0.15, Math.PI - 0.15)
    ctx.stroke()
  })

  // 兼容旧引用：sprite_npc → sage
  createCanvasTexture(scene, 'sprite_npc', 48, 64, (ctx) => {
    // 复用 sage 的绘制
    drawNpcBase(ctx, '#ffcc88', '#6655aa', '#443388')
    ctx.fillStyle = '#daa520'
    ctx.fillRect(14, 36, 20, 3)
    ctx.fillStyle = '#5544aa'
    ctx.beginPath()
    ctx.moveTo(24, 0)
    ctx.quadraticCurveTo(10, 8, 12, 18)
    ctx.lineTo(36, 18)
    ctx.quadraticCurveTo(38, 8, 24, 0)
    ctx.fill()
    ctx.fillStyle = '#4433aa'
    ctx.beginPath()
    ctx.ellipse(24, 16, 16, 4, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.strokeStyle = '#884444'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(24, 23, 4, 0.1, Math.PI - 0.1)
    ctx.stroke()
  })

  // ===== 宝箱精灵 =====
  createCanvasTexture(scene, 'sprite_chest', 40, 40, (ctx) => {
    // 阴影
    ctx.fillStyle = 'rgba(0, 0, 0, 0.12)'
    ctx.beginPath()
    ctx.ellipse(20, 36, 16, 4, 0, 0, Math.PI * 2)
    ctx.fill()

    // 箱体
    const boxGrad = ctx.createLinearGradient(4, 18, 4, 36)
    boxGrad.addColorStop(0, '#c49040')
    boxGrad.addColorStop(1, '#8a6020')
    ctx.fillStyle = boxGrad
    roundRect(ctx, 4, 18, 32, 18, 3)

    // 箱盖
    ctx.fillStyle = '#d4a050'
    roundRect(ctx, 2, 12, 36, 10, 3)

    // 金属条
    ctx.fillStyle = '#ffd700'
    roundRect(ctx, 2, 20, 36, 3, 0)

    // 锁
    ctx.fillStyle = '#ffd700'
    ctx.beginPath()
    ctx.arc(20, 22, 4, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#8a6020'
    ctx.beginPath()
    ctx.arc(20, 22, 2, 0, Math.PI * 2)
    ctx.fill()
  })

  // ===== 宠物精灵 (小精灵猫 - 跟随玩家) =====
  createCanvasTexture(scene, 'sprite_pet', 24, 24, (ctx) => {
    // 阴影
    ctx.fillStyle = 'rgba(0, 0, 0, 0.1)'
    ctx.beginPath()
    ctx.ellipse(12, 21, 8, 3, 0, 0, Math.PI * 2)
    ctx.fill()

    // 身体 (橘色小猫球)
    const bodyGrad = ctx.createRadialGradient(12, 14, 1, 12, 15, 9)
    bodyGrad.addColorStop(0, '#ffcc66')
    bodyGrad.addColorStop(0.7, '#ff9933')
    bodyGrad.addColorStop(1, '#cc7722')
    ctx.fillStyle = bodyGrad
    ctx.beginPath()
    ctx.ellipse(12, 14, 8, 7, 0, 0, Math.PI * 2)
    ctx.fill()

    // 耳朵
    ctx.fillStyle = '#ff9933'
    ctx.beginPath()
    ctx.moveTo(6, 9)
    ctx.lineTo(4, 3)
    ctx.lineTo(9, 7)
    ctx.closePath()
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(18, 9)
    ctx.lineTo(20, 3)
    ctx.lineTo(15, 7)
    ctx.closePath()
    ctx.fill()

    // 内耳
    ctx.fillStyle = '#ffbbaa'
    ctx.beginPath()
    ctx.moveTo(6.5, 8)
    ctx.lineTo(5, 4.5)
    ctx.lineTo(8.5, 7)
    ctx.closePath()
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(17.5, 8)
    ctx.lineTo(19, 4.5)
    ctx.lineTo(15.5, 7)
    ctx.closePath()
    ctx.fill()

    // 眼睛
    ctx.fillStyle = '#222'
    ctx.beginPath()
    ctx.ellipse(9, 12, 1.8, 2, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.ellipse(15, 12, 1.8, 2, 0, 0, Math.PI * 2)
    ctx.fill()

    // 瞳孔高光
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(8.5, 11.3, 0.7, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(14.5, 11.3, 0.7, 0, Math.PI * 2)
    ctx.fill()

    // 鼻子
    ctx.fillStyle = '#ff6688'
    ctx.beginPath()
    ctx.ellipse(12, 14, 1, 0.7, 0, 0, Math.PI * 2)
    ctx.fill()

    // 嘴
    ctx.strokeStyle = '#aa5533'
    ctx.lineWidth = 0.6
    ctx.beginPath()
    ctx.arc(11, 15.5, 1.5, 0.1, Math.PI - 0.1)
    ctx.stroke()
    ctx.beginPath()
    ctx.arc(13, 15.5, 1.5, 0.1, Math.PI - 0.1)
    ctx.stroke()

    // 尾巴
    ctx.strokeStyle = '#ff9933'
    ctx.lineWidth = 2
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(19, 17)
    ctx.quadraticCurveTo(23, 12, 21, 7)
    ctx.stroke()
  })

  // ===== 玩家角色 (Q版大头 Chibi — 4方向各3帧) =====
  const dirs = ['down', 'up', 'left', 'right'] as const
  for (const dir of dirs) {
    for (let frame = 0; frame < 3; frame++) {
      createCanvasTexture(scene, `player_${dir}_${frame}`, 48, 56, (ctx) => {
        drawPlayerChibi(ctx, dir, frame)
      })
    }
  }
}

// 辅助: 画圆角矩形
function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
  ctx.fill()
}

// 辅助: 画树冠
function drawTreeCanopy(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number, color: string) {
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.ellipse(cx, cy, r, r * 0.8, 0, 0, Math.PI * 2)
  ctx.fill()
}

// 辅助: 画 Q 版大头角色精灵 (Chibi style)
function drawPlayerChibi(ctx: CanvasRenderingContext2D, dir: 'down' | 'up' | 'left' | 'right', frame: number) {
  const cx = 24
  // 走路弹跳: frame 0 = 静止, 1 = 左脚, 2 = 右脚
  const bounce = frame === 0 ? 0 : (frame === 1 ? -2 : -1)
  const legSwing = frame === 0 ? 0 : (frame === 1 ? 3 : -3)

  // ---- 阴影 ----
  ctx.fillStyle = 'rgba(0, 0, 0, 0.18)'
  ctx.beginPath()
  ctx.ellipse(cx, 54, 10, 3, 0, 0, Math.PI * 2)
  ctx.fill()

  // ---- 小短腿 ----
  const legY = 46 + bounce
  ctx.fillStyle = '#4a7aaf'
  // 左腿
  roundRect(ctx, cx - 7, legY + legSwing * 0.3, 5, 7, 2)
  // 右腿
  roundRect(ctx, cx + 2, legY - legSwing * 0.3, 5, 7, 2)

  // 小靴子
  ctx.fillStyle = '#6b4423'
  roundRect(ctx, cx - 8, legY + 5 + legSwing * 0.3, 6, 4, 2)
  roundRect(ctx, cx + 2, legY + 5 - legSwing * 0.3, 6, 4, 2)

  // ---- 身体 (很小的身体) ----
  const bodyY = 34 + bounce
  const bodyGrad = ctx.createLinearGradient(cx, bodyY, cx, bodyY + 14)
  bodyGrad.addColorStop(0, '#5b9bd5')
  bodyGrad.addColorStop(1, '#4a7ab5')
  ctx.fillStyle = bodyGrad
  roundRect(ctx, cx - 9, bodyY, 18, 14, 4)

  // 黑色轮廓
  ctx.strokeStyle = 'rgba(0, 0, 0, 0.3)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.roundRect(cx - 9, bodyY, 18, 14, 4)
  ctx.stroke()

  // 腰带
  ctx.fillStyle = '#e8a838'
  ctx.fillRect(cx - 9, bodyY + 10, 18, 3)
  ctx.fillStyle = '#ffd700'
  roundRect(ctx, cx - 2, bodyY + 9, 4, 5, 1)

  // 小手臂
  ctx.fillStyle = '#ffcc88'
  if (dir === 'down' || dir === 'up') {
    // 两只小手在两侧
    ctx.beginPath()
    ctx.arc(cx - 11, bodyY + 6 + legSwing * 0.4, 3, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(cx + 11, bodyY + 6 - legSwing * 0.4, 3, 0, Math.PI * 2)
    ctx.fill()
  } else if (dir === 'left') {
    ctx.beginPath()
    ctx.arc(cx - 10, bodyY + 5 + legSwing * 0.5, 3, 0, Math.PI * 2)
    ctx.fill()
  } else {
    ctx.beginPath()
    ctx.arc(cx + 10, bodyY + 5 + legSwing * 0.5, 3, 0, Math.PI * 2)
    ctx.fill()
  }

  // ---- 大头 (占角色 50%) ----
  const headY = 18 + bounce
  const headR = 15

  // 头部阴影 (脖子处)
  ctx.fillStyle = 'rgba(0, 0, 0, 0.08)'
  ctx.beginPath()
  ctx.ellipse(cx, headY + headR - 2, 10, 3, 0, 0, Math.PI * 2)
  ctx.fill()

  // 头部主体
  ctx.fillStyle = '#ffe0b4'
  ctx.beginPath()
  ctx.arc(cx, headY, headR, 0, Math.PI * 2)
  ctx.fill()

  // 头部轮廓
  ctx.strokeStyle = 'rgba(180, 120, 60, 0.3)'
  ctx.lineWidth = 1.2
  ctx.beginPath()
  ctx.arc(cx, headY, headR, 0, Math.PI * 2)
  ctx.stroke()

  // 腮红
  if (dir !== 'up') {
    ctx.fillStyle = 'rgba(255, 150, 140, 0.3)'
    if (dir === 'down' || dir === 'right') {
      ctx.beginPath()
      ctx.ellipse(cx + 9, headY + 4, 4, 3, 0, 0, Math.PI * 2)
      ctx.fill()
    }
    if (dir === 'down' || dir === 'left') {
      ctx.beginPath()
      ctx.ellipse(cx - 9, headY + 4, 4, 3, 0, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // ---- 头发 ----
  if (dir === 'up') {
    // 背面: 完整头发
    ctx.fillStyle = '#8b5e3c'
    ctx.beginPath()
    ctx.arc(cx, headY - 1, headR + 1, 0, Math.PI * 2)
    ctx.fill()
    // 发尾
    ctx.beginPath()
    ctx.moveTo(cx - 10, headY + 6)
    ctx.quadraticCurveTo(cx - 14, headY + 16, cx - 8, headY + 12)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(cx + 10, headY + 6)
    ctx.quadraticCurveTo(cx + 14, headY + 16, cx + 8, headY + 12)
    ctx.fill()
  } else {
    // 前面/侧面: 刘海 + 头顶
    ctx.fillStyle = '#8b5e3c'
    ctx.beginPath()
    ctx.arc(cx, headY - 2, headR + 1, Math.PI + 0.3, -0.3)
    ctx.fill()

    // 蓬松刘海
    ctx.fillStyle = '#9b6e4c'
    ctx.beginPath()
    if (dir === 'down') {
      ctx.ellipse(cx - 5, headY - 10, 7, 5, -0.15, 0, Math.PI * 2)
    } else if (dir === 'left') {
      ctx.ellipse(cx - 3, headY - 10, 8, 5, -0.2, 0, Math.PI * 2)
    } else {
      ctx.ellipse(cx + 3, headY - 10, 8, 5, 0.2, 0, Math.PI * 2)
    }
    ctx.fill()

    // 呆毛 (ahoge)
    ctx.strokeStyle = '#9b6e4c'
    ctx.lineWidth = 2
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(cx + 2, headY - headR + 1)
    ctx.quadraticCurveTo(cx + 8, headY - headR - 8, cx + 4, headY - headR - 4)
    ctx.stroke()
    ctx.lineCap = 'butt'
  }

  // ---- 五官 (大眼睛 + 小嘴) ----
  if (dir === 'down') {
    // 大眼睛
    const eyeY = headY + 1
    // 白色眼白
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.ellipse(cx - 5, eyeY, 4.5, 5, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.ellipse(cx + 5, eyeY, 4.5, 5, 0, 0, Math.PI * 2)
    ctx.fill()

    // 瞳孔
    ctx.fillStyle = '#2d5a8a'
    ctx.beginPath()
    ctx.arc(cx - 5, eyeY + 0.5, 3, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(cx + 5, eyeY + 0.5, 3, 0, Math.PI * 2)
    ctx.fill()

    // 大高光
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(cx - 6.5, eyeY - 1.5, 1.8, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(cx + 3.5, eyeY - 1.5, 1.8, 0, Math.PI * 2)
    ctx.fill()
    // 小高光
    ctx.beginPath()
    ctx.arc(cx - 3.5, eyeY + 1.5, 0.8, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(cx + 6.5, eyeY + 1.5, 0.8, 0, Math.PI * 2)
    ctx.fill()

    // 小嘴
    ctx.fillStyle = '#e87070'
    ctx.beginPath()
    ctx.arc(cx, headY + 7, 2, 0, Math.PI)
    ctx.fill()

  } else if (dir === 'left') {
    // 左看 — 单侧大眼
    const eyeY = headY + 1
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.ellipse(cx - 5, eyeY, 4.5, 5, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#2d5a8a'
    ctx.beginPath()
    ctx.arc(cx - 6, eyeY + 0.5, 3, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(cx - 7, eyeY - 1.5, 1.5, 0, Math.PI * 2)
    ctx.fill()

    // 小嘴
    ctx.fillStyle = '#e87070'
    ctx.beginPath()
    ctx.arc(cx - 5, headY + 7, 1.5, 0, Math.PI)
    ctx.fill()

  } else if (dir === 'right') {
    // 右看 — 单侧大眼
    const eyeY = headY + 1
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.ellipse(cx + 5, eyeY, 4.5, 5, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#2d5a8a'
    ctx.beginPath()
    ctx.arc(cx + 6, eyeY + 0.5, 3, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(cx + 7, eyeY - 1.5, 1.5, 0, Math.PI * 2)
    ctx.fill()

    // 小嘴
    ctx.fillStyle = '#e87070'
    ctx.beginPath()
    ctx.arc(cx + 5, headY + 7, 1.5, 0, Math.PI)
    ctx.fill()
  }
  // dir === 'up' — 只显示头发，无五官

  // ---- 头巾/发带 ----
  if (dir !== 'up') {
    ctx.fillStyle = '#ff6b6b'
    ctx.fillRect(cx - headR + 2, headY - 6, headR * 2 - 4, 3)
    // 蝴蝶结
    ctx.beginPath()
    ctx.moveTo(cx + headR - 4, headY - 6)
    ctx.lineTo(cx + headR + 2, headY - 10)
    ctx.lineTo(cx + headR - 2, headY - 5)
    ctx.lineTo(cx + headR + 2, headY - 1)
    ctx.closePath()
    ctx.fill()
  } else {
    ctx.fillStyle = '#ff6b6b'
    ctx.fillRect(cx - headR + 2, headY - 5, headR * 2 - 4, 3)
    // 蝴蝶结在后面
    ctx.beginPath()
    ctx.moveTo(cx, headY - 5)
    ctx.lineTo(cx - 6, headY - 12)
    ctx.lineTo(cx, headY - 4)
    ctx.lineTo(cx + 6, headY - 12)
    ctx.closePath()
    ctx.fill()
  }

  // ---- 披风标记 (背面时显示小披风) ----
  if (dir === 'up') {
    ctx.fillStyle = 'rgba(90, 150, 220, 0.5)'
    ctx.beginPath()
    ctx.moveTo(cx - 8, bodyY + 2)
    ctx.quadraticCurveTo(cx - 10 + legSwing * 0.3, bodyY + 16, cx - 6, bodyY + 18)
    ctx.lineTo(cx + 6, bodyY + 18)
    ctx.quadraticCurveTo(cx + 10 - legSwing * 0.3, bodyY + 16, cx + 8, bodyY + 2)
    ctx.closePath()
    ctx.fill()
  }
}

// 辅助: 从 canvas 2D context 创建 Phaser 纹理
function createCanvasTexture(
  scene: Phaser.Scene,
  key: string,
  width: number,
  height: number,
  draw: (ctx: CanvasRenderingContext2D) => void
) {
  if (scene.textures.exists(key)) return

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')!
  draw(ctx)
  scene.textures.addCanvas(key, canvas)
}
