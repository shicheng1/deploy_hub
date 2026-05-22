import request from './index'
import type { ApiResult, DashboardStats } from '@/types'

export function getDashboardStats() {
  return request.get<any, ApiResult<DashboardStats>>('/dashboard/stats')
}
