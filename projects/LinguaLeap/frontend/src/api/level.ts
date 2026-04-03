import http from './http'

export interface KnowledgeLevel {
  id: number
  code: string
  name: string
  description: string
  gradeGroup: string
  sortOrder: number
  unitCount: number
  totalKps: number
  masteredKps: number
  learningKps: number
  progress: number
}

export interface KnowledgeUnit {
  id: number
  name: string
  description: string
  topic: string
  sortOrder: number
  kpCount: number
  totalProgress: number
  masteredCount: number
  progress: number
}

export interface LevelDetail {
  id: number
  code: string
  name: string
  description: string
  gradeGroup: string
  units: KnowledgeUnit[]
}

export interface LearningCard {
  id: number
  type: string
  content: string
  phonetic: string | null
  meaningZh: string
  exampleSentence: string
  exampleZh: string
  difficulty: number
  status: string
  reviewCount: number
}

export interface UnitCards {
  unitId: number
  unitName: string
  topic: string
  cards: LearningCard[]
  totalCount: number
}

export const levelApi = {
  list() {
    return http.get<{ data: KnowledgeLevel[] }>('/content/levels')
  },
  getDetail(levelId: number) {
    return http.get<{ data: LevelDetail }>(`/content/levels/${levelId}`)
  },
  getUnitCards(unitId: number) {
    return http.get<{ data: UnitCards }>(`/content/levels/units/${unitId}/cards`)
  },
  markProgress(kpId: number, status: string) {
    return http.post('/content/levels/progress', { kpId, status })
  },
  completeUnit(unitId: number) {
    return http.post(`/content/levels/units/${unitId}/complete`)
  },
  importKps(unitId: number, kpList: any[]) {
    return http.post<{ data: { imported: number } }>(`/content/levels/units/${unitId}/import-kps`, kpList)
  },
}

export const aiKnowledgeApi = {
  generateUnitContent(params: {
    levelCode: string
    levelName: string
    levelDesc: string
    topic: string
    unitName: string
    count: number
  }) {
    return http.post<{ data: any[] }>('/ai/generate/unit-content', params)
  },
}
