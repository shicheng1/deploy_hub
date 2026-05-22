import request from './index'
import type { ApiResult, PageResult, PageParams } from '@/types'

export interface ScriptExecutionRecord {
  id: number
  scriptContent: string
  serverId: number
  serverName: string
  serverHost: string
  status: string
  log: string
  errorMessage: string
  startedAt: string
  finishedAt: string
  operator: string
  createTime: string
  updateTime: string
}

export interface ScriptExecutionRequest {
  scriptContent: string
  serverIds: number[]
  operator?: string
}

export interface BatchScriptExecutionRequest {
  scriptContents: string[]
  serverIds: number[]
  operator?: string
}

export interface BatchScriptExecutionResult {
  executionIds: number[]
  successCount: number
  failCount: number
}

export function getScriptExecutionList(params: PageParams & { serverId?: number; status?: string }) {
  return request.get<any, ApiResult<PageResult<ScriptExecutionRecord>>>('/script-execution/list', { params })
}

export function executeScript(data: ScriptExecutionRequest) {
  return request.post<any, ApiResult<void>>('/script-execution/execute', data)
}

export function batchExecuteScript(data: BatchScriptExecutionRequest) {
  return request.post<any, ApiResult<BatchScriptExecutionResult>>('/script-execution/batch-execute', data)
}

export function getScriptExecutionLog(id: number) {
  return request.get<any, ApiResult<string>>(`/script-execution/${id}/log`)
}

export function cancelScriptExecution(id: number) {
  return request.post<any, ApiResult<void>>(`/script-execution/${id}/cancel`)
}