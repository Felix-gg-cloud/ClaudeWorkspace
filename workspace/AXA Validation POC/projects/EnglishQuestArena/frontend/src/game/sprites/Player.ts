// 玩家精灵类 — 使用 TextureGen 生成的角色纹理
import Phaser from 'phaser'

export class Player extends Phaser.Physics.Arcade.Sprite {
  private speed = 160
  private facing: 'down' | 'up' | 'left' | 'right' = 'down'
  private stepTimer = 0
  private animFrame = 0
  private animClock = 0

  constructor(scene: Phaser.Scene, x: number, y: number) {
    super(scene, x, y, 'player_down_0')
    scene.add.existing(this)
    scene.physics.add.existing(this)

    const physBody = this.body as Phaser.Physics.Arcade.Body
    physBody.setSize(20, 20)
    physBody.setOffset(14, 32)
    physBody.setCollideWorldBounds(true)
  }

  updateMovement(cursors: Phaser.Types.Input.Keyboard.CursorKeys, wasd: Record<string, Phaser.Input.Keyboard.Key>) {
    const physBody = this.body as Phaser.Physics.Arcade.Body
    physBody.setVelocity(0)

    let moving = false

    if (cursors.left.isDown || wasd['A']?.isDown) {
      physBody.setVelocityX(-this.speed)
      this.facing = 'left'
      moving = true
    } else if (cursors.right.isDown || wasd['D']?.isDown) {
      physBody.setVelocityX(this.speed)
      this.facing = 'right'
      moving = true
    }

    if (cursors.up.isDown || wasd['W']?.isDown) {
      physBody.setVelocityY(-this.speed)
      this.facing = 'up'
      moving = true
    } else if (cursors.down.isDown || wasd['S']?.isDown) {
      physBody.setVelocityY(this.speed)
      this.facing = 'down'
      moving = true
    }

    // 行走动画
    if (moving) {
      this.stepTimer++
      this.animClock++
      if (this.animClock >= 8) {
        this.animClock = 0
        this.animFrame = (this.animFrame + 1) % 3
      }
    } else {
      this.stepTimer = 0
      this.animFrame = 0
      this.animClock = 0
    }

    // 更新纹理
    const texKey = `player_${this.facing}_${this.animFrame}`
    if (this.texture.key !== texKey) {
      this.setTexture(texKey)
    }

    // 深度随 Y 排序
    this.setDepth(this.y / 10 + 5)
  }

  getFacing() {
    return this.facing
  }
}
