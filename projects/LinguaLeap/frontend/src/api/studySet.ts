import http from './http'

export interface StudySet {
  id: number
  userId: number
  title: string
  description: string
  sourceType: string
  sourceText: string
  grade: string
  status: 'processing' | 'ready' | 'failed'
  aiSummary: string
  aiStrategy: string
  itemCount: number
  questionCount: number
  createdAt: string
  updatedAt: string
}

export interface LearningItem {
  id: number
  studySetId: number
  category: 'vocabulary' | 'grammar' | 'sentence_pattern' | 'passage'
  content: string
  meaningZh: string
  phonetic: string
  exampleSentence: string
  exampleZh: string
  extraData: string
  difficulty: number
  aiNote: string
}

export interface StudySetDetail {
  studySet: StudySet
  items: LearningItem[]
  groupedItems: Record<string, LearningItem[]>
  categoryCounts: Record<string, number>
}

export const studySetApi = {
  create(data: { title: string; text: string; sourceType?: string; userNote?: string; grade?: string }) {
    return http.post<{ data: StudySet }>('/content/study-sets', data)
  },
  list() {
    return http.get<{ data: StudySet[] }>('/content/study-sets')
  },
  detail(id: number) {
    return http.get<{ data: StudySetDetail }>(`/content/study-sets/${id}`)
  },
  delete(id: number) {
    return http.delete(`/content/study-sets/${id}`)
  },
  upload(file: File, title: string, userNote?: string, grade?: string) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', title)
    if (userNote) formData.append('userNote', userNote)
    if (grade) formData.append('grade', grade)
    return http.post<{ data: StudySet }>('/content/study-sets/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
