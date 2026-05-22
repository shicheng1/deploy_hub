import request from './index'
import type { ScriptTemplate, PageResult, PageParams, ApiResult } from '@/types'

export function getScriptList(params: PageParams & { category?: string }) {
  return request.get<any, ApiResult<PageResult<ScriptTemplate>>>('/script/list', { params })
}

export function getScriptById(id: number) {
  return request.get<any, ApiResult<ScriptTemplate>>(`/script/${id}`)
}

export function createScript(data: Partial<ScriptTemplate>) {
  return request.post<any, ApiResult<ScriptTemplate>>('/script', data)
}

export function updateScript(id: number, data: Partial<ScriptTemplate>) {
  return request.put<any, ApiResult<ScriptTemplate>>(`/script/${id}`, data)
}

export function deleteScript(id: number) {
  return request.delete<any, ApiResult<void>>(`/script/${id}`)
}

export function uploadScript(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResult<void>>('/script/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}