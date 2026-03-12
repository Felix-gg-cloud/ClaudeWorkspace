// Phaser 游戏初始化与生命周期管理
import Phaser from 'phaser'
import { CampScene } from './scenes/CampScene'
import type { CampMapData } from './config/mapData'

let gameInstance: Phaser.Game | null = null
let pendingMapData: CampMapData | null = null

export function createPhaserGame(container: HTMLElement, mapData?: CampMapData): Phaser.Game {
  // 销毁旧实例
  if (gameInstance) {
    gameInstance.destroy(true)
    gameInstance = null
  }

  pendingMapData = mapData ?? null

  const config: Phaser.Types.Core.GameConfig = {
    type: Phaser.AUTO,
    parent: container,
    backgroundColor: '#111111',
    pixelArt: true,
    roundPixels: true,
    physics: {
      default: 'arcade',
      arcade: {
        gravity: { x: 0, y: 0 },
        debug: false,
      },
    },
    scale: {
      mode: Phaser.Scale.RESIZE,
      autoCenter: Phaser.Scale.CENTER_BOTH,
    },
    scene: [CampScene],
    callbacks: {
      preBoot: (game) => {
        // Inject map data into scene before create()
        game.events.once('boot', () => {
          if (pendingMapData) {
            const scene = game.scene.getScene('CampScene') as CampScene | null
            if (scene) scene.setMapData(pendingMapData)
          }
        })
      },
    },
  }

  gameInstance = new Phaser.Game(config)
  return gameInstance
}

export function destroyPhaserGame() {
  if (gameInstance) {
    gameInstance.destroy(true)
    gameInstance = null
  }
}

export function getCampScene(): CampScene | null {
  if (!gameInstance) return null
  return gameInstance.scene.getScene('CampScene') as CampScene | null
}
