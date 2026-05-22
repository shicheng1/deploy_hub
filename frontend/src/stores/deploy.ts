import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DeployRecord, DeployRequest } from '@/types'
import { getDeployList, triggerDeploy, rollbackDeploy, getDeployLog } from '@/api/deploy'

export const useDeployStore = defineStore('deploy', () => {
  const records = ref<DeployRecord[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchList(params: { page: number; size: number; appId?: number; serverId?: number; status?: string; startTime?: string; endTime?: string }) {
    loading.value = true
    try {
      const res = await getDeployList(params)
      records.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function deploy(data: DeployRequest) {
    return await triggerDeploy(data, data.deployMode)
  }

  async function rollback(id: number) {
    return await rollbackDeploy(id)
  }

  async function fetchLog(id: number) {
    return await getDeployLog(id)
  }

  return { records, total, loading, fetchList, deploy, rollback, fetchLog }
})
