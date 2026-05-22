import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Server } from '@/types'
import { getServerList, createServer, updateServer, deleteServer, testConnection } from '@/api/server'

export const useServerStore = defineStore('server', () => {
  const servers = ref<Server[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchList(params: { page: number; size: number; name?: string }) {
    loading.value = true
    try {
      const res = await getServerList(params)
      servers.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function addServer(data: Partial<Server>) {
    return await createServer(data)
  }

  async function editServer(id: number, data: Partial<Server>) {
    return await updateServer(id, data)
  }

  async function removeServer(id: number) {
    return await deleteServer(id)
  }

  async function testConn(id: number) {
    return await testConnection(id)
  }

  return { servers, total, loading, fetchList, addServer, editServer, removeServer, testConn }
})
