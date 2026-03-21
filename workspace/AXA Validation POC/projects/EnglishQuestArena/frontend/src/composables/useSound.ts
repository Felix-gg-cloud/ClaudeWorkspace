// 8-bit Sound Effects using Web Audio API
// Zero audio files — all synthesized at runtime

let audioCtx: AudioContext | null = null

function getCtx(): AudioContext {
  if (!audioCtx) audioCtx = new AudioContext()
  if (audioCtx.state === 'suspended') audioCtx.resume()
  return audioCtx
}

function playTone(freq: number, duration: number, type: OscillatorType = 'square', volume = 0.15) {
  const ctx = getCtx()
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = type
  osc.frequency.value = freq
  gain.gain.setValueAtTime(volume, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start(ctx.currentTime)
  osc.stop(ctx.currentTime + duration)
}

function playNoise(duration: number, volume = 0.1) {
  const ctx = getCtx()
  const bufferSize = ctx.sampleRate * duration
  const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate)
  const data = buffer.getChannelData(0)
  for (let i = 0; i < bufferSize; i++) {
    data[i] = Math.random() * 2 - 1
  }
  const source = ctx.createBufferSource()
  source.buffer = buffer
  const gain = ctx.createGain()
  gain.gain.setValueAtTime(volume, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration)
  source.connect(gain)
  gain.connect(ctx.destination)
  source.start()
}

export function useSound() {
  // Answer correct: ascending 3-note chime
  function correct() {
    playTone(523, 0.12, 'square', 0.12)  // C5
    setTimeout(() => playTone(659, 0.12, 'square', 0.12), 80)   // E5
    setTimeout(() => playTone(784, 0.18, 'square', 0.12), 160)  // G5
  }

  // Answer wrong: descending buzz
  function wrong() {
    playTone(220, 0.15, 'sawtooth', 0.1)  // A3
    setTimeout(() => playTone(175, 0.2, 'sawtooth', 0.1), 150)  // F3
  }

  // Combo increment
  function combo() {
    const ctx = getCtx()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'square'
    osc.frequency.setValueAtTime(523, ctx.currentTime)     // C5
    osc.frequency.linearRampToValueAtTime(1047, ctx.currentTime + 0.1) // C6
    gain.gain.setValueAtTime(0.1, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start(ctx.currentTime)
    osc.stop(ctx.currentTime + 0.15)
  }

  // Boss takes damage: heavy hit
  function bossHit() {
    playNoise(0.08, 0.15)
    playTone(110, 0.25, 'sine', 0.2)  // A2 deep thud
  }

  // Player takes damage: slash
  function playerHit() {
    playNoise(0.06, 0.12)
    playTone(165, 0.15, 'sawtooth', 0.1) // E3
  }

  // Level up: victory fanfare
  function levelUp() {
    const notes = [262, 330, 392, 523, 659] // C4 E4 G4 C5 E5
    notes.forEach((freq, i) => {
      setTimeout(() => playTone(freq, 0.2, 'square', 0.1), i * 120)
    })
  }

  // Victory: triumphant
  function victory() {
    const notes = [392, 523, 659, 784, 1047] // G4 C5 E5 G5 C6
    notes.forEach((freq, i) => {
      setTimeout(() => playTone(freq, 0.25, 'square', 0.12), i * 140)
    })
    setTimeout(() => {
      playTone(523, 0.4, 'square', 0.08)
      playTone(659, 0.4, 'square', 0.08)
      playTone(784, 0.4, 'square', 0.08)
    }, 750)
  }

  // Defeat: somber descending
  function defeat() {
    playTone(392, 0.3, 'triangle', 0.12) // G4
    setTimeout(() => playTone(330, 0.3, 'triangle', 0.12), 200) // E4
    setTimeout(() => playTone(262, 0.5, 'triangle', 0.12), 400) // C4
  }

  // Button click
  function click() {
    playTone(880, 0.03, 'square', 0.06)  // A5 short click
  }

  // Card flip
  function flip() {
    playNoise(0.04, 0.06)
  }

  // Coin collect
  function coin() {
    playTone(1319, 0.08, 'square', 0.08) // E6
    setTimeout(() => playTone(1568, 0.12, 'square', 0.08), 60) // G6
  }

  return { correct, wrong, combo, bossHit, playerHit, levelUp, victory, defeat, click, flip, coin }
}
