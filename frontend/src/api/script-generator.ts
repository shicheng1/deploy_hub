import request from './index'
import type { ApiResult } from '@/types'

export interface ScriptGeneratorRequest {
  projectType: 'JAVA' | 'FRONTEND'
  appName: string
  remoteDeployPath: string
  configFilePath?: string
  jvmParams?: string
  serverPort?: string
  reloadNginx?: boolean
}

export function generateScript(data: ScriptGeneratorRequest) {
  return request.post<any, ApiResult<string>>('/script-generator/generate', data)
}
