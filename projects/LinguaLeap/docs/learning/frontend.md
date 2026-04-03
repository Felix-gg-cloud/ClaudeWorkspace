# 前端知识详解

> Vue 3 + TypeScript + Pinia + SCSS 核心知识点。
> 复盘时逐项展开，结合项目代码讲解。

---

## Vue 3 核心

### 1. Composition API

**项目中的体现**：
```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const count = ref(0)                              // 响应式数据
const doubled = computed(() => count.value * 2)   // 计算属性
onMounted(() => { fetchData() })                  // 生命周期
</script>
```

**复盘要点**：
- [ ] `ref` vs `reactive` 的区别和选择
- [ ] 为什么 `ref` 需要 `.value` 而模板里不需要
- [ ] `computed` vs 普通函数，什么时候用哪个
- [ ] `watch` vs `watchEffect` 的区别
- [ ] `<script setup>` 是什么语法糖

### 2. 组件化

**项目中的体现**：
- `AppIcon.vue` — props 驱动的图标组件
- `AppLayout.vue` — 布局组件 + `<router-view>`
- Phase 1：`ChoiceQuestion.vue`, `FillQuestion.vue` — 题型组件

**复盘要点**：
- [ ] Props 定义（`defineProps<T>()`）和类型校验
- [ ] Emit 事件（`defineEmits<T>()`）
- [ ] 父子组件通信 vs Pinia 全局状态
- [ ] 插槽 `<slot>` 的用法
- [ ] `v-for` 的 key 为什么重要

### 3. 生命周期

```
setup()              ← <script setup> 在这里执行
    ↓
onBeforeMount        ← DOM 还没渲染
    ↓
onMounted            ← DOM 已渲染，可以发请求
    ↓
onBeforeUpdate       ← 数据变了，DOM 还没更新
    ↓
onUpdated            ← DOM 已更新
    ↓
onBeforeUnmount      ← 即将销毁，清理定时器/监听器
    ↓
onUnmounted          ← 已销毁
```

---

## TypeScript

### 项目中的用法

```typescript
// 接口定义
interface User {
  id: number
  username: string
  displayName?: string    // 可选属性
  grade: 'primary' | 'junior' | 'senior'  // 字面量联合类型
}

// 泛型
interface ApiResponse<T> {
  code: number
  data: T
  message?: string
}

// 类型推断
const user = ref<User | null>(null)  // 明确告诉 TS 类型
```

**复盘要点**：
- [ ] `interface` vs `type` 的区别
- [ ] 泛型 `<T>` 解决什么问题
- [ ] 可选属性 `?` 和非空断言 `!`
- [ ] `as` 类型断言什么时候用
- [ ] `unknown` vs `any` 的区别

---

## Pinia 状态管理

**项目中的体现（stores/user.ts）**：
```typescript
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('ll_token'))
  const user = ref<User | null>(null)

  async function login(username: string, password: string) {
    const res = await http.post('/auth/login', { username, password })
    token.value = res.data.data.token
    localStorage.setItem('ll_token', token.value)
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('ll_token')
  }

  return { token, user, login, logout }
})
```

**复盘要点**：
- [ ] 为什么用 Pinia 而不是组件内 state
- [ ] Setup Store vs Options Store 的区别
- [ ] `storeToRefs()` 是做什么的
- [ ] 状态持久化（localStorage）策略
- [ ] 多个 Store 之间如何交互

---

## Vue Router

**项目中的体现**：
```typescript
// 路由守卫
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return '/login'   // 没 token → 跳登录
  }
})

// 懒加载
{
  path: '/banks',
  component: () => import('@/views/BankListView.vue')  // 按需加载
}
```

**复盘要点**：
- [ ] 路由守卫（beforeEach / beforeEnter / beforeRouteLeave）
- [ ] 动态路由参数 `/banks/:id` 怎么取
- [ ] 懒加载原理（动态 import → 独立 chunk）
- [ ] 嵌套路由和 `<router-view>` 的关系
- [ ] 路由 meta 信息的用法

---

## Axios HTTP 封装

**项目中的体现（api/http.ts）**：
```typescript
const http = axios.create({ baseURL: '/api' })

// 请求拦截器：自动加 Token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('ll_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：统一处理 401
http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      // Token 过期 → 登出 → 跳登录
      useUserStore().logout()
      router.push('/login')
    }
    return Promise.reject(err)
  }
)
```

**复盘要点**：
- [ ] 拦截器的执行顺序
- [ ] 为什么要统一封装而不是每次直接 axios.get
- [ ] 错误处理策略（全局 vs 局部）
- [ ] 前端怎么配代理解决跨域（vite.config.ts proxy）

---

## SCSS

**项目中的用法**：
```scss
// variables.scss — 设计令牌
$radius-md: 12px;
$transition: 0.2s ease;

// 嵌套
.card {
  &:hover { transform: translateY(-2px); }
  &__title { font-size: 16px; }      // BEM 命名
  &__desc { color: var(--text-muted); }
}

// CSS 变量（支持主题切换）
:root {
  --bg-card: #ffffff;
  --text-primary: #1f2937;
}
[data-theme="dark"] {
  --bg-card: #1f2937;
  --text-primary: #f3f4f6;
}
```

**复盘要点**：
- [ ] SCSS 变量 `$var` vs CSS 变量 `--var` 的区别
- [ ] BEM 命名规范（Block__Element--Modifier）
- [ ] `@use` vs `@import` 的区别
- [ ] 主题切换怎么实现的
- [ ] `scoped` 样式的原理（data 属性选择器）

---

## 待复盘文件清单

| 文件 | 涉及知识点 |
|------|-----------|
| `src/api/http.ts` | Axios 封装、拦截器、错误处理 |
| `src/stores/user.ts` | Pinia、响应式、localStorage |
| `src/stores/theme.ts` | 主题切换、CSS 变量 |
| `src/router/index.ts` | 路由守卫、懒加载、嵌套路由 |
| `src/layouts/AppLayout.vue` | 布局组件、Composition API |
| `src/views/LoginView.vue` | 表单处理、双向绑定、Emit |
| `src/components/AppIcon.vue` | Props、SVG 渲染 |
| `src/styles/variables.scss` | 设计令牌、SCSS 模块化 |
| `src/styles/global.scss` | 主题、CSS 变量、全局样式 |
| `vite.config.ts` | 构建配置、代理、别名 |
