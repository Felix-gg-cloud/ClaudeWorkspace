# LingoBuddy 项目 Copilot 指令

## 技术栈
- **前端框架**: Vue 3 + Vite + TypeScript
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **UI 组件库**: Element Plus
- **HTTP**: axios（`withCredentials: true`）
- **图标**: Element Plus Icons / iconify
- **后端框架**: Spring Boot 3.x (Maven)
- **安全**: Spring Security（Session + Cookie，非 JWT）
- **ORM**: Spring Data JPA
- **数据库**: H2（dev）/ PostgreSQL（prod）
- **文档**: springdoc-openapi（Swagger）

## 架构：前后端分离
- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- 认证方式：Cookie Session（前端 axios 需 `withCredentials: true`）
- CORS：后端允许 `http://localhost:5173` + `allowCredentials=true`

## 设计美学（Design System）

### 风格基调
- 参考 Linear.app / Notion 的极简现代风格
- 干净、留白充足、信息层级清晰
- 克制使用装饰元素，功能优先

### 配色方案
```
Primary:    blue-600  (#2563eb)  — 主操作、链接、强调
Accent:     green-500 (#22c55e)  — 成功、正确反馈
Warning:    amber-500 (#f59e0b)  — 警告提示
Error:      red-500   (#ef4444)  — 错误、删除操作
Neutral:    gray-50 ~ gray-900   — 背景、文字、边框

背景色:     gray-50   (#f9fafb)
卡片背景:   white     (#ffffff)
主文字:     gray-900  (#111827)
次文字:     gray-500  (#6b7280)
边框:       gray-200  (#e5e7eb)
```

### 排版体系
```
Hero 标题:    text-4xl sm:text-5xl  font-bold    tracking-tight
页面标题:     text-2xl sm:text-3xl  font-bold    tracking-tight
区块标题:     text-xl              font-semibold
卡片标题:     text-base            font-semibold
正文:         text-sm / text-base  font-normal   text-gray-700
辅助文字:     text-sm              font-normal   text-gray-500
标签/小字:    text-xs              font-medium   text-gray-400
行高:         leading-relaxed（正文）/ leading-tight（标题）
```

### 间距节奏（Spacing）
```
页面外边距:    px-6 (移动端) / px-8 (桌面端)
最大宽度:      max-w-5xl mx-auto
Section 间:   py-16 sm:py-24
组件间:        gap-6
卡片内边距:    p-6
按钮内边距:    px-4 py-2 (sm) / px-6 py-3 (md)
元素微间距:    gap-2 / gap-3
```

### 圆角 & 阴影
```
按钮:         rounded-lg
输入框:       rounded-lg
卡片:         rounded-xl
大容器/模态:  rounded-2xl
头像:         rounded-full

阴影:         shadow-sm（默认）/ shadow-md（hover 提升）
避免使用:     shadow-xl / shadow-2xl（过重）
```

### 边框
```
卡片边框:     border border-gray-200
hover 高亮:   hover:border-primary-200
分割线:       border-t border-gray-100
输入框聚焦:   focus:ring-2 focus:ring-primary-500 focus:border-primary-500
```

### 交互反馈
```
过渡:         transition-all duration-200
按钮 hover:   hover:bg-primary-700（填充按钮）/ hover:bg-gray-50（线框按钮）
卡片 hover:   hover:shadow-md hover:border-primary-200
链接 hover:   hover:text-primary-600
禁用态:       opacity-50 cursor-not-allowed
加载态:       animate-pulse（骨架屏）/ animate-spin（加载器）
```

### 响应式（Mobile First）
```
移动端:       默认样式
sm (640px):   两列网格、增大字号
md (768px):   侧边栏显示
lg (1024px):  完整桌面布局
```

## 组件编写规范

### 按钮（Button）
```tsx
// Primary
className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition-all hover:bg-primary-700 hover:shadow-md disabled:opacity-50"

// Secondary / Outline
className="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm transition-all hover:bg-gray-50"

// Ghost
className="inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium text-gray-600 transition-all hover:bg-gray-100 hover:text-gray-900"
```

### 卡片（Card）
```tsx
className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md hover:border-primary-200"
```

### 输入框（Input）
```tsx
className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm transition-all placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
```

### 徽章（Badge）
```tsx
// 成功
className="inline-flex items-center rounded-full bg-green-50 px-2.5 py-0.5 text-xs font-medium text-green-700"
// 警告
className="inline-flex items-center rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-700"
```

## 代码规范
- 使用函数组件 + Hooks，不用 class 组件
- 导出 Props 类型定义
- 组件文件名 PascalCase，工具函数 camelCase
- 一个文件只导出一个主组件
- 图标统一用 lucide-react，大小默认 size={16}（小）/ size={20}（中）/ size={24}（大）

## 禁止
- 不要使用 inline style（style={{ }}）
- 不要使用 !important
- 不要用 float 布局，全部用 flex / grid
- 不要用 CSS Modules 或 Styled Components
- 配色不超过 3 种主色
- 不要用 alert()，用 Toast 组件
- 不要在组件内硬编码文案，抽到常量或 data 文件
