import request from './index'
import type { Server, PageResult, PageParams, ApiResult } from '@/types'

export function getServerList(params: PageParams) {
  return request.get<any, ApiResult<PageResult<Server>>>('/server/list', { params })
}

export function getServerById(id: number) {
  return request.get<any, ApiResult<Server>>(`/server/${id}`)
}

export function createServer(data: Partial<Server>) {
  return request.post<any, ApiResult<Server>>('/server', data)
}

export function updateServer(id: number, data: Partial<Server>) {
  return request.put<any, ApiResult<Server>>(`/server/${id}`, data)
}

export function deleteServer(id: number) {
  return request.delete<any, ApiResult<void>>(`/server/${id}`)
}

export function testConnection(id: number) {
  return request.get<any, ApiResult<boolean>>(`/server/${id}/test-connection`)
}
