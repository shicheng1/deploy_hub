import request from './index'
import type { ApiResult } from '@/types'

export interface ScriptVariable {
  name: string
  description: string
  group?: string
}

export function getScriptVariableList() {
  return request.get<any, ApiResult<ScriptVariable[]>>('/script-variable/list')
}
