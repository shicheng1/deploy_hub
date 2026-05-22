import request from './index'
import type { DeployRecord, PageResult, PageParams, ApiResult, DeployRequest, BatchDeployRequest } from '@/types'

export function getDeployList(params: PageParams) {
  return request.get<any, ApiResult<PageResult<DeployRecord>>>('/deploy/list', { params })
}

export function triggerDeploy(data: DeployRequest, deployMode?: string): Promise<any> {
  return request.post('/deploy/trigger', data, { params: { deployMode } })
}

export function rollbackDeploy(id: number) {
  return request.post<any, ApiResult<DeployRecord>>(`/deploy/${id}/rollback`)
}

export function getDeployLog(id: number) {
  return request.get<any, ApiResult<string>>(`/deploy/${id}/log`)
}

export function batchDeploy(data: BatchDeployRequest) {
  return request.post<any, ApiResult<number[]>>('/deploy/batch-trigger', data)
}
