import request from './index'
import type { App, PageResult, PageParams, ApiResult, BuildResult } from '@/types'

export function getAppList(params: PageParams & { projectId?: number }) {
  return request.get<any, ApiResult<PageResult<App>>>('/app/list', { params })
}

export function getAppById(id: number) {
  return request.get<any, ApiResult<App>>(`/app/${id}`)
}

export function createApp(data: Partial<App>) {
  return request.post<any, ApiResult<App>>('/app', data)
}

export function updateApp(id: number, data: Partial<App>) {
  return request.put<any, ApiResult<App>>(`/app/${id}`, data)
}

export function deleteApp(id: number) {
  return request.delete<any, ApiResult<void>>(`/app/${id}`)
}

export function buildBackend(appId: number) {
  return request.post<any, ApiResult<BuildResult>>(`/build/backend/${appId}`)
}

export function buildFrontend(appId: number) {
  return request.post<any, ApiResult<BuildResult>>(`/build/frontend/${appId}`)
}

export function buildAll(appId: number) {
  return request.post<any, ApiResult<BuildResult>>(`/build/all/${appId}`)
}
