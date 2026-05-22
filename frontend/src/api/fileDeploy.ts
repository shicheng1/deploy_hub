import request from './index'
import type { ApiResult } from '@/types'

export interface FileUploadResult {
  results: UploadResultItem[]
  successCount: number
  failCount: number
}

export interface UploadResultItem {
  serverId: number
  serverName: string
  serverHost: string
  success: boolean
  message: string
}

export function uploadFile(formData: FormData) {
  return request.post<any, ApiResult<FileUploadResult>>('/file-deploy/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deployWithScript(formData: FormData) {
  return request.post<any, ApiResult<FileUploadResult>>('/file-deploy/deploy', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}