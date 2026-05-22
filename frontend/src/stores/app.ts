import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { App } from '@/types'
import { getAppList, createApp, updateApp, deleteApp } from '@/api/app'

export const useAppStore = defineStore('app', () => {
  const apps = ref<App[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchList(params: { page: number; size: number; name?: string }) {
    loading.value = true
    try {
      const res = await getAppList(params)
      apps.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function addApp(data: Partial<App>) {
    return await createApp(data)
  }

  async function editApp(id: number, data: Partial<App>) {
    return await updateApp(id, data)
  }

  async function removeApp(id: number) {
    return await deleteApp(id)
  }

  return { apps, total, loading, fetchList, addApp, editApp, removeApp }
})
