import { ref } from 'vue'

const THEME_KEY = 'll_theme'

type Theme = 'light' | 'dark'

const current = ref<Theme>((localStorage.getItem(THEME_KEY) as Theme) || 'light')

function apply(theme: Theme) {
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem(THEME_KEY, theme)
  current.value = theme
}

function toggle() {
  apply(current.value === 'light' ? 'dark' : 'light')
}

// Apply on load
apply(current.value)

export function useTheme() {
  return { current, toggle }
}
