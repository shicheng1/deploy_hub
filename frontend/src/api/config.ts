import request from './index'
import type { ApiResult } from '@/types'

export interface Config {
  id: number
  configKey: string
  configValue: string
  configGroup: string
  description: string
  createTime: string
  updateTime: string
}

export function getConfigList(configGroup?: string) {
  return request.get<any, ApiResult<Config[]>>('/config/list', { params: { configGroup } })
}

export function getConfigByKey(configKey: string) {
  return request.get<any, ApiResult<Config>>(`/config/key/${configKey}`)
}

export function createConfig(data: Partial<Config>) {
  return request.post<any, ApiResult<Config>>('/config', data)
}

export function updateConfig(id: number, data: Partial<Config>) {
  return request.put<any, ApiResult<Config>>(`/config/${id}`, data)
}

export function deleteConfig(id: number) {
  return request.delete<any, ApiResult<void>>(`/config/${id}`)
}
