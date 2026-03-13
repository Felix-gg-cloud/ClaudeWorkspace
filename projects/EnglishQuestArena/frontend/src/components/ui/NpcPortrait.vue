<template>
  <canvas ref="canvas" :width="W" :height="H" class="npc-portrait-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const props = defineProps<{
  type: 'sage' | 'guide' | 'knight' | 'merchant'
  /** 是否播放入场动画 */
  animate?: boolean
}>()

const W = 200
const H = 320
const canvas = ref<HTMLCanvasElement>()

function draw() {
  const el = canvas.value
  if (!el) return
  const ctx = el.getContext('2d')
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)

  switch (props.type) {
    case 'sage': drawSage(ctx); break
    case 'guide': drawGuide(ctx); break
    case 'knight': drawKnight(ctx); break
    case 'merchant': drawMerchant(ctx); break
  }
}

// =================== 贤者 (紫袍法师) ===================
function drawSage(ctx: CanvasRenderingContext2D) {
  // 魔法光圈
  const auraGrad = ctx.createRadialGradient(100, 180, 20, 100, 180, 120)
  auraGrad.addColorStop(0, 'rgba(120, 80, 220, 0.15)')
  auraGrad.addColorStop(0.6, 'rgba(120, 80, 220, 0.05)')
  auraGrad.addColorStop(1, 'transparent')
  ctx.fillStyle = auraGrad
  ctx.fillRect(0, 40, W, 280)

  // 阴影
  ctx.fillStyle = 'rgba(0,0,0,0.25)'
  ctx.beginPath()
  ctx.ellipse(100, 310, 50, 10, 0, 0, Math.PI * 2)
  ctx.fill()

  // 长袍
  const robeGrad = ctx.createLinearGradient(100, 110, 100, 310)
  robeGrad.addColorStop(0, '#7755cc')
  robeGrad.addColorStop(0.5, '#5533aa')
  robeGrad.addColorStop(1, '#332277')
  ctx.fillStyle = robeGrad
  ctx.beginPath()
  ctx.moveTo(60, 140)
  ctx.quadraticCurveTo(40, 250, 35, 310)
  ctx.lineTo(165, 310)
  ctx.quadraticCurveTo(160, 250, 140, 140)
  ctx.closePath()
  ctx.fill()

  // 袍子高光
  ctx.fillStyle = 'rgba(200, 170, 255, 0.08)'
  ctx.beginPath()
  ctx.moveTo(75, 140)
  ctx.quadraticCurveTo(60, 230, 55, 310)
  ctx.lineTo(85, 310)
  ctx.quadraticCurveTo(80, 230, 85, 140)
  ctx.closePath()
  ctx.fill()

  // 袍子暗面
  ctx.fillStyle = 'rgba(0, 0, 0, 0.1)'
  ctx.beginPath()
  ctx.moveTo(120, 140)
  ctx.quadraticCurveTo(140, 230, 145, 310)
  ctx.lineTo(165, 310)
  ctx.quadraticCurveTo(155, 250, 140, 140)
  ctx.closePath()
  ctx.fill()

  // 腰带
  ctx.fillStyle = '#daa520'
  ctx.beginPath()
  ctx.moveTo(55, 190)
  ctx.quadraticCurveTo(100, 198, 145, 190)
  ctx.quadraticCurveTo(100, 202, 55, 194)
  ctx.closePath()
  ctx.fill()
  // 腰带扣
  ctx.fillStyle = '#ffd700'
  ctx.beginPath()
  ctx.arc(100, 194, 6, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#b8860b'
  ctx.beginPath()
  ctx.arc(100, 194, 3, 0, Math.PI * 2)
  ctx.fill()

  // 领口V字
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.moveTo(85, 130)
  ctx.lineTo(100, 155)
  ctx.lineTo(115, 130)
  ctx.closePath()
  ctx.fill()

  // 头部
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.ellipse(100, 100, 32, 36, 0, 0, Math.PI * 2)
  ctx.fill()
  // 头部高光
  ctx.fillStyle = 'rgba(255, 230, 200, 0.3)'
  ctx.beginPath()
  ctx.ellipse(90, 88, 16, 18, -0.2, 0, Math.PI * 2)
  ctx.fill()

  // 胡子
  ctx.fillStyle = '#cccccc'
  ctx.beginPath()
  ctx.moveTo(80, 115)
  ctx.quadraticCurveTo(100, 170, 100, 180)
  ctx.quadraticCurveTo(100, 170, 120, 115)
  ctx.closePath()
  ctx.fill()
  ctx.fillStyle = 'rgba(255,255,255,0.2)'
  ctx.beginPath()
  ctx.moveTo(88, 120)
  ctx.quadraticCurveTo(100, 160, 100, 168)
  ctx.quadraticCurveTo(100, 155, 105, 118)
  ctx.closePath()
  ctx.fill()

  // 帽子
  const hatGrad = ctx.createLinearGradient(100, 10, 100, 80)
  hatGrad.addColorStop(0, '#6644bb')
  hatGrad.addColorStop(1, '#4422aa')
  ctx.fillStyle = hatGrad
  ctx.beginPath()
  ctx.moveTo(100, 8)
  ctx.quadraticCurveTo(55, 40, 56, 80)
  ctx.quadraticCurveTo(100, 72, 144, 80)
  ctx.quadraticCurveTo(145, 40, 100, 8)
  ctx.fill()
  // 帽檐
  ctx.fillStyle = '#553399'
  ctx.beginPath()
  ctx.ellipse(100, 78, 52, 12, 0, 0, Math.PI * 2)
  ctx.fill()
  // 帽子星星
  drawStar(ctx, 105, 40, 5, '#ffd700', 0.7)
  drawStar(ctx, 90, 55, 3.5, '#ffc855', 0.5)

  // 眼睛
  drawEyes(ctx, 86, 96, 96, 96, '#4422aa')

  // 眉毛
  ctx.strokeStyle = '#887766'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.arc(86, 90, 8, Math.PI + 0.3, Math.PI * 2 - 0.3)
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(114, 90, 8, Math.PI + 0.3, Math.PI * 2 - 0.3)
  ctx.stroke()

  // 微笑
  ctx.strokeStyle = '#aa7755'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.arc(100, 108, 8, 0.15, Math.PI - 0.15)
  ctx.stroke()

  // 法杖 (右手)
  ctx.strokeStyle = '#8B4513'
  ctx.lineWidth = 5
  ctx.beginPath()
  ctx.moveTo(155, 150)
  ctx.lineTo(165, 310)
  ctx.stroke()
  // 法杖顶端宝珠
  const orbGrad = ctx.createRadialGradient(155, 140, 2, 155, 140, 14)
  orbGrad.addColorStop(0, '#fff')
  orbGrad.addColorStop(0.3, '#aa88ff')
  orbGrad.addColorStop(0.7, '#7744dd')
  orbGrad.addColorStop(1, '#4422aa')
  ctx.fillStyle = orbGrad
  ctx.beginPath()
  ctx.arc(155, 140, 14, 0, Math.PI * 2)
  ctx.fill()
  // 宝珠光晕
  ctx.fillStyle = 'rgba(170, 130, 255, 0.3)'
  ctx.beginPath()
  ctx.arc(155, 140, 22, 0, Math.PI * 2)
  ctx.fill()

  // 魔法粒子
  drawSparkles(ctx, [
    { x: 155, y: 120, r: 2, color: '#ddbbff' },
    { x: 145, y: 130, r: 1.5, color: '#ccaaff' },
    { x: 168, y: 135, r: 1.5, color: '#eeccff' },
    { x: 148, y: 148, r: 1, color: '#ddbbff' },
  ])
}

// =================== 向导 (绿衣冒险者) ===================
function drawGuide(ctx: CanvasRenderingContext2D) {
  // 暖光
  const auraGrad = ctx.createRadialGradient(100, 180, 20, 100, 180, 120)
  auraGrad.addColorStop(0, 'rgba(60, 180, 80, 0.12)')
  auraGrad.addColorStop(1, 'transparent')
  ctx.fillStyle = auraGrad
  ctx.fillRect(0, 40, W, 280)

  // 阴影
  ctx.fillStyle = 'rgba(0,0,0,0.25)'
  ctx.beginPath()
  ctx.ellipse(100, 310, 45, 10, 0, 0, Math.PI * 2)
  ctx.fill()

  // 斗篷
  const cloakGrad = ctx.createLinearGradient(100, 100, 100, 310)
  cloakGrad.addColorStop(0, '#2d7d46')
  cloakGrad.addColorStop(1, '#1a5530')
  ctx.fillStyle = cloakGrad
  ctx.beginPath()
  ctx.moveTo(50, 130)
  ctx.quadraticCurveTo(30, 220, 40, 300)
  ctx.lineTo(160, 300)
  ctx.quadraticCurveTo(170, 220, 150, 130)
  ctx.closePath()
  ctx.fill()

  // 内衬 (棕色皮甲)
  ctx.fillStyle = '#8b6914'
  ctx.beginPath()
  ctx.moveTo(70, 140)
  ctx.lineTo(65, 260)
  ctx.lineTo(135, 260)
  ctx.lineTo(130, 140)
  ctx.closePath()
  ctx.fill()
  // 皮甲纹路
  ctx.strokeStyle = 'rgba(0,0,0,0.15)'
  ctx.lineWidth = 1
  for (let y = 160; y < 250; y += 20) {
    ctx.beginPath()
    ctx.moveTo(70, y)
    ctx.lineTo(130, y)
    ctx.stroke()
  }

  // 腰带
  ctx.fillStyle = '#5a3a1a'
  ctx.fillRect(62, 200, 76, 8)
  ctx.fillStyle = '#daa520'
  ctx.beginPath()
  ctx.arc(100, 204, 5, 0, Math.PI * 2)
  ctx.fill()

  // 领口
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.moveTo(80, 128)
  ctx.lineTo(100, 150)
  ctx.lineTo(120, 128)
  ctx.closePath()
  ctx.fill()

  // 头部
  ctx.fillStyle = '#f5c78a'
  ctx.beginPath()
  ctx.ellipse(100, 96, 30, 34, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = 'rgba(255, 225, 195, 0.3)'
  ctx.beginPath()
  ctx.ellipse(92, 86, 14, 16, -0.2, 0, Math.PI * 2)
  ctx.fill()

  // 短发
  ctx.fillStyle = '#664422'
  ctx.beginPath()
  ctx.ellipse(100, 78, 32, 22, 0, Math.PI, Math.PI * 2)
  ctx.fill()
  // 刘海
  ctx.beginPath()
  ctx.moveTo(68, 86)
  ctx.quadraticCurveTo(75, 74, 90, 76)
  ctx.lineTo(80, 90)
  ctx.closePath()
  ctx.fill()

  // 帽子
  ctx.fillStyle = '#2a6e3a'
  ctx.beginPath()
  ctx.moveTo(100, 52)
  ctx.quadraticCurveTo(60, 70, 66, 86)
  ctx.quadraticCurveTo(100, 78, 134, 86)
  ctx.quadraticCurveTo(140, 70, 100, 52)
  ctx.fill()
  // 帽子羽毛
  ctx.strokeStyle = '#ff6644'
  ctx.lineWidth = 2.5
  ctx.beginPath()
  ctx.moveTo(125, 70)
  ctx.quadraticCurveTo(145, 40, 135, 25)
  ctx.stroke()
  ctx.strokeStyle = '#ff8844'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(125, 70)
  ctx.quadraticCurveTo(148, 45, 140, 32)
  ctx.stroke()

  // 眼睛
  drawEyes(ctx, 88, 94, 112, 94, '#2a5520')

  // 微笑（开朗的笑容）
  ctx.strokeStyle = '#aa7755'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.arc(100, 105, 10, 0.1, Math.PI - 0.1)
  ctx.stroke()

  // 弓 (左肩)
  ctx.strokeStyle = '#8B4513'
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.moveTo(38, 130)
  ctx.quadraticCurveTo(22, 200, 38, 270)
  ctx.stroke()
  // 弓弦
  ctx.strokeStyle = '#ccc'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(38, 130)
  ctx.lineTo(38, 270)
  ctx.stroke()

  // 指引手势
  ctx.fillStyle = '#f5c78a'
  ctx.beginPath()
  ctx.ellipse(155, 190, 8, 10, 0.3, 0, Math.PI * 2)
  ctx.fill()
  // 手指
  ctx.fillStyle = '#f5c78a'
  ctx.save()
  ctx.translate(165, 182)
  ctx.rotate(-0.5)
  ctx.fillRect(-3, -14, 6, 14)
  ctx.restore()
}

// =================== 骑士 (铁甲 + 红披风) ===================
function drawKnight(ctx: CanvasRenderingContext2D) {
  // 光效
  const auraGrad = ctx.createRadialGradient(100, 180, 20, 100, 180, 120)
  auraGrad.addColorStop(0, 'rgba(200, 100, 50, 0.12)')
  auraGrad.addColorStop(1, 'transparent')
  ctx.fillStyle = auraGrad
  ctx.fillRect(0, 40, W, 280)

  // 阴影
  ctx.fillStyle = 'rgba(0,0,0,0.3)'
  ctx.beginPath()
  ctx.ellipse(100, 310, 50, 10, 0, 0, Math.PI * 2)
  ctx.fill()

  // 红披风
  ctx.fillStyle = '#8b2222'
  ctx.beginPath()
  ctx.moveTo(55, 135)
  ctx.quadraticCurveTo(25, 230, 35, 310)
  ctx.lineTo(165, 310)
  ctx.quadraticCurveTo(175, 230, 145, 135)
  ctx.closePath()
  ctx.fill()
  ctx.fillStyle = 'rgba(180, 40, 40, 0.15)'
  ctx.beginPath()
  ctx.moveTo(65, 135)
  ctx.quadraticCurveTo(45, 230, 50, 310)
  ctx.lineTo(75, 310)
  ctx.quadraticCurveTo(70, 230, 80, 135)
  ctx.closePath()
  ctx.fill()

  // 铁甲身体
  const armorGrad = ctx.createLinearGradient(60, 130, 140, 130)
  armorGrad.addColorStop(0, '#888')
  armorGrad.addColorStop(0.3, '#bbb')
  armorGrad.addColorStop(0.5, '#ddd')
  armorGrad.addColorStop(0.7, '#bbb')
  armorGrad.addColorStop(1, '#777')
  ctx.fillStyle = armorGrad
  ctx.beginPath()
  ctx.moveTo(65, 135)
  ctx.lineTo(60, 250)
  ctx.lineTo(140, 250)
  ctx.lineTo(135, 135)
  ctx.closePath()
  ctx.fill()

  // 盔甲中线
  ctx.strokeStyle = 'rgba(0,0,0,0.15)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(100, 140)
  ctx.lineTo(100, 250)
  ctx.stroke()

  // 肩甲
  ctx.fillStyle = '#aaa'
  ctx.beginPath()
  ctx.ellipse(60, 140, 18, 12, 0.3, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(140, 140, 18, 12, -0.3, 0, Math.PI * 2)
  ctx.fill()

  // 腰带
  ctx.fillStyle = '#8b6914'
  ctx.fillRect(58, 215, 84, 8)
  ctx.fillStyle = '#ffd700'
  ctx.beginPath()
  ctx.arc(100, 219, 6, 0, Math.PI * 2)
  ctx.fill()

  // 领口
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.moveTo(80, 128)
  ctx.lineTo(100, 142)
  ctx.lineTo(120, 128)
  ctx.closePath()
  ctx.fill()

  // 头部
  ctx.fillStyle = '#eebb88'
  ctx.beginPath()
  ctx.ellipse(100, 96, 30, 34, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = 'rgba(255, 220, 190, 0.3)'
  ctx.beginPath()
  ctx.ellipse(92, 86, 14, 16, -0.2, 0, Math.PI * 2)
  ctx.fill()

  // 短发 (深色)
  ctx.fillStyle = '#333'
  ctx.beginPath()
  ctx.ellipse(100, 76, 33, 22, 0, Math.PI, Math.PI * 2)
  ctx.fill()

  // 疤痕
  ctx.strokeStyle = 'rgba(180, 100, 80, 0.5)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(118, 88)
  ctx.lineTo(125, 105)
  ctx.stroke()

  // 眼睛（锐利）
  drawEyes(ctx, 86, 94, 112, 94, '#884422', true)

  // 坚定表情
  ctx.strokeStyle = '#996644'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(90, 110)
  ctx.lineTo(110, 110)
  ctx.stroke()

  // 大剑 (右手)
  ctx.strokeStyle = '#999'
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.moveTo(160, 100)
  ctx.lineTo(170, 300)
  ctx.stroke()
  // 剑刃高光
  ctx.strokeStyle = 'rgba(255,255,255,0.4)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(160, 105)
  ctx.lineTo(168, 280)
  ctx.stroke()
  // 剑柄
  ctx.fillStyle = '#5a3a1a'
  ctx.fillRect(150, 95, 20, 10)
  // 剑格
  ctx.fillStyle = '#daa520'
  ctx.fillRect(146, 102, 28, 5)
}

// =================== 商人 (书生/学者) ===================
function drawMerchant(ctx: CanvasRenderingContext2D) {
  // 暖光
  const auraGrad = ctx.createRadialGradient(100, 180, 20, 100, 180, 120)
  auraGrad.addColorStop(0, 'rgba(200, 160, 80, 0.12)')
  auraGrad.addColorStop(1, 'transparent')
  ctx.fillStyle = auraGrad
  ctx.fillRect(0, 40, W, 280)

  // 阴影
  ctx.fillStyle = 'rgba(0,0,0,0.25)'
  ctx.beginPath()
  ctx.ellipse(100, 310, 45, 10, 0, 0, Math.PI * 2)
  ctx.fill()

  // 长大衣
  const coatGrad = ctx.createLinearGradient(100, 130, 100, 310)
  coatGrad.addColorStop(0, '#8b6914')
  coatGrad.addColorStop(0.5, '#6b5210')
  coatGrad.addColorStop(1, '#4a380a')
  ctx.fillStyle = coatGrad
  ctx.beginPath()
  ctx.moveTo(55, 140)
  ctx.quadraticCurveTo(40, 240, 42, 310)
  ctx.lineTo(158, 310)
  ctx.quadraticCurveTo(160, 240, 145, 140)
  ctx.closePath()
  ctx.fill()

  // 内衬
  ctx.fillStyle = '#eed8a0'
  ctx.beginPath()
  ctx.moveTo(80, 140)
  ctx.lineTo(78, 260)
  ctx.lineTo(122, 260)
  ctx.lineTo(120, 140)
  ctx.closePath()
  ctx.fill()

  // 领口
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.moveTo(82, 128)
  ctx.lineTo(100, 148)
  ctx.lineTo(118, 128)
  ctx.closePath()
  ctx.fill()

  // 多个纽扣
  ctx.fillStyle = '#daa520'
  for (let y = 160; y <= 240; y += 22) {
    ctx.beginPath()
    ctx.arc(100, y, 3, 0, Math.PI * 2)
    ctx.fill()
  }

  // 头部
  ctx.fillStyle = '#ffcc88'
  ctx.beginPath()
  ctx.ellipse(100, 96, 30, 34, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = 'rgba(255, 230, 200, 0.3)'
  ctx.beginPath()
  ctx.ellipse(92, 86, 14, 16, -0.2, 0, Math.PI * 2)
  ctx.fill()

  // 整齐的头发
  ctx.fillStyle = '#554430'
  ctx.beginPath()
  ctx.ellipse(100, 76, 33, 22, 0, Math.PI, Math.PI * 2)
  ctx.fill()

  // 圆框眼镜
  ctx.strokeStyle = '#b8860b'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.arc(86, 94, 10, 0, Math.PI * 2)
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(114, 94, 10, 0, Math.PI * 2)
  ctx.stroke()
  // 镜桥
  ctx.beginPath()
  ctx.moveTo(96, 94)
  ctx.lineTo(104, 94)
  ctx.stroke()

  // 眼睛
  ctx.fillStyle = '#554430'
  ctx.beginPath()
  ctx.arc(86, 94, 2.5, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(114, 94, 2.5, 0, Math.PI * 2)
  ctx.fill()
  // 眼睛高光
  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.arc(84, 92, 1, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(112, 92, 1, 0, Math.PI * 2)
  ctx.fill()

  // 温和微笑
  ctx.strokeStyle = '#aa7755'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.arc(100, 106, 7, 0.15, Math.PI - 0.15)
  ctx.stroke()

  // 书本 (左手)
  ctx.fillStyle = '#8b1a1a'
  ctx.save()
  ctx.translate(35, 190)
  ctx.rotate(-0.15)
  ctx.fillRect(0, 0, 30, 40)
  // 书页
  ctx.fillStyle = '#ffe8c0'
  ctx.fillRect(3, 2, 24, 36)
  // 书页线
  ctx.strokeStyle = 'rgba(0,0,0,0.1)'
  ctx.lineWidth = 0.5
  for (let y = 8; y < 34; y += 5) {
    ctx.beginPath()
    ctx.moveTo(6, y)
    ctx.lineTo(24, y)
    ctx.stroke()
  }
  ctx.restore()

  // 羽毛笔 (右手)
  ctx.strokeStyle = '#daa520'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(155, 170)
  ctx.quadraticCurveTo(160, 150, 155, 130)
  ctx.stroke()
  // 羽毛
  ctx.fillStyle = '#fff8e0'
  ctx.beginPath()
  ctx.moveTo(155, 130)
  ctx.quadraticCurveTo(140, 120, 145, 105)
  ctx.quadraticCurveTo(155, 115, 165, 105)
  ctx.quadraticCurveTo(160, 120, 155, 130)
  ctx.fill()
}

// =================== 辅助函数 ===================
function drawEyes(ctx: CanvasRenderingContext2D, lx: number, ly: number, rx: number, ry: number, color: string, sharp = false) {
  // 眼白
  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.ellipse(lx, ly, sharp ? 7 : 6, sharp ? 5 : 6, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(rx, ry, sharp ? 7 : 6, sharp ? 5 : 6, 0, 0, Math.PI * 2)
  ctx.fill()

  // 虹膜
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.arc(lx + 1, ly, 3.5, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(rx + 1, ry, 3.5, 0, Math.PI * 2)
  ctx.fill()

  // 瞳孔
  ctx.fillStyle = '#111'
  ctx.beginPath()
  ctx.arc(lx + 1, ly, 1.8, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(rx + 1, ry, 1.8, 0, Math.PI * 2)
  ctx.fill()

  // 高光
  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.arc(lx - 1, ly - 2, 1.5, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(rx - 1, ry - 2, 1.5, 0, Math.PI * 2)
  ctx.fill()
}

function drawStar(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, color: string, alpha: number) {
  ctx.save()
  ctx.globalAlpha = alpha
  ctx.fillStyle = color
  ctx.beginPath()
  for (let i = 0; i < 5; i++) {
    const angle = (i * 4 * Math.PI) / 5 - Math.PI / 2
    const method = i === 0 ? 'moveTo' : 'lineTo'
    ctx[method](x + r * Math.cos(angle), y + r * Math.sin(angle))
  }
  ctx.closePath()
  ctx.fill()
  ctx.restore()
}

function drawSparkles(ctx: CanvasRenderingContext2D, particles: Array<{ x: number; y: number; r: number; color: string }>) {
  for (const p of particles) {
    ctx.fillStyle = p.color
    ctx.globalAlpha = 0.7
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1
}

onMounted(draw)
watch(() => props.type, draw)
</script>

<style scoped>
.npc-portrait-canvas {
  display: block;
  image-rendering: auto;
}
</style>
