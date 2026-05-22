<template>
  <div>
    <div class="page-header">
      <h2>执行历史</h2>
    </div>
    <el-card shadow="never" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="服务器">
          <el-select v-model="filterForm.serverId" placeholder="全部" clearable filterable style="width: 180px;">
            <el-option v-for="s in serverList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px;">
            <el-option label="等待中" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="records" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="脚本摘要" min-width="200">
          <template #default="{ row }">
            {{ truncate(row.scriptContent, 50) }}
          </template>
        </el-table-column>
        <el-table-column prop="serverName" label="服务器名称" width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="180" />
        <el-table-column prop="finishedAt" label="结束时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewLog(row.id)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="logDialogVisible" title="执行日志" width="70%" destroy-on-close>
      <LogTerminal :record-id="currentRecordId" topic="/topic/script-execution" :fallback-log-api="getScriptExecutionLog" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getServerList } from '@/api/server'
import { getScriptExecutionList, getScriptExecutionLog } from '@/api/scriptExecution'
import type { Server } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import LogTerminal from '@/components/LogTerminal.vue'

const page = ref(1)
const size = ref(10)
const total = ref(0)
const records = ref<any[]>([])
const loading = ref(false)
const serverList = ref<Server[]>([])
const logDialogVisible = ref(false)
const currentRecordId = ref(0)

const filterForm = reactive({
  serverId: undefined as number | undefined,
  status: undefined as string | undefined
})

async function loadServers() {
  try {
    const res = await getServerList({ page: 1, size: 100 })
    serverList.value = res.data.records
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: size.value,
      serverId: filterForm.serverId,
      status: filterForm.status
    }
    const res = await getScriptExecutionList(params)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  filterForm.serverId = undefined
  filterForm.status = undefined
  page.value = 1
  loadData()
}

function viewLog(id: number) {
  currentRecordId.value = id
  logDialogVisible.value = true
}

function truncate(str: string | null | undefined, len: number): string {
  if (!str) return '-'
  return str.length > len ? str.substring(0, len) + '...' : str
}

onMounted(() => {
  loadServers()
  loadData()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
</style>
