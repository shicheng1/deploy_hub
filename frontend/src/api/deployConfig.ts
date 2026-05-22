import request from './index'
import type { DeployConfig, ApiResult } from '@/types'

export function getDeployConfig(appId: number) {
  return request.get<any, ApiResult<DeployConfig>>(`/deploy-config/${appId}`)
}

export function saveDeployConfig(data: Partial<DeployConfig>) {
  return request.post<any, ApiResult<void>>('/deploy-config', data)
}
