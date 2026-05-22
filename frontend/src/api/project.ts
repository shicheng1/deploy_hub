import request from './index'
import type { Project, PageParams, ApiResult, PageResult } from '@/types'

export function getProjectList(params: PageParams) {
  return request.get<any, ApiResult<PageResult<Project>>>('/project/list', { params })
}

export function getProject(id: number) {
  return request.get<any, ApiResult<Project>>(`/project/${id}`)
}

export function createProject(data: Partial<Project>) {
  return request.post<any, ApiResult<void>>('/project', data)
}

export function updateProject(id: number, data: Partial<Project>) {
  return request.put<any, ApiResult<void>>(`/project/${id}`, data)
}

export function deleteProject(id: number) {
  return request.delete<any, ApiResult<void>>(`/project/${id}`)
}
