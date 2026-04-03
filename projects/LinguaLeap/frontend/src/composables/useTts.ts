import { ref } from 'vue'

const speaking = ref(false)
const supported = ref(typeof window !== 'undefined' && 'speechSynthesis' in window)

/**
 * Web Speech API TTS composable
 * 零成本，浏览器原生支持
 */
export function useTts() {
  function speak(text: string, lang = 'en-US') {
    if (!supported.value || !text) return

    // 停止之前的朗读
    window.speechSynthesis.cancel()

    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = lang
    utterance.rate = 0.9
    utterance.pitch = 1

    // 尝试选择英文语音
    const voices = window.speechSynthesis.getVoices()
    const enVoice = voices.find(v => v.lang.startsWith('en') && v.localService)
      || voices.find(v => v.lang.startsWith('en'))
    if (enVoice) utterance.voice = enVoice

    utterance.onstart = () => { speaking.value = true }
    utterance.onend = () => { speaking.value = false }
    utterance.onerror = () => { speaking.value = false }

    window.speechSynthesis.speak(utterance)
  }

  function stop() {
    window.speechSynthesis.cancel()
    speaking.value = false
  }

  return { speak, stop, speaking, supported }
}
