<template>
  <div>
    <div class="page-header">
      <h2>部署历史</h2>
    </div>
    <el-card shadow="never" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="应用名称">
          <el-select v-model="filterForm.appId" placeholder="全部" clearable filterable style="width: 180px;">
            <el-option v-for="app in appList" :key="app.id" :label="app.name" :value="app.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务器名称">
          <el-select v-model="filterForm.serverId" placeholder="全部" clearable filterable style="width: 180px;">
            <el-option v-for="s in serverList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px;">
            <el-option label="等待中" value="PENDING" />
            <el-option label="部署中" value="DEPLOYING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="回滚中" value="ROLLING_BACK" />
            <el-option label="已回滚" value="ROLLED_BACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="deployStore.records" stripe v-loading="deployStore.loading" style="width: 100%">
        <el-table-column prop="appName" label="应用名" min-width="140" />
        <el-table-column prop="serverName" label="服务器名" min-width="140" />
        <el-table-column prop="version" label="版本" width="140" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="180" />
        <el-table-column prop="finishedAt" label="结束时间" width="180" />
        <el-table-column label="耗时" width="120">
          <template #default="{ row }">
            {{ formatDuration(row.startedAt, row.finishedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewLog(row.id)">查看日志</el-button>
            <el-button
              v-if="row.status === 'FAILED'"
              type="warning"
              link
              size="small"
              @click="handleRollback(row.id)"
            >回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="deployStore.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="logDialogVisible" title="部署日志" width="70%" destroy-on-close>
      <LogTerminal :record-id="currentRecordId" @status-change="onStatusChange" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAppList } from '@/api/app'
import { getServerList } from '@/api/server'
import { useDeployStore } from '@/stores/deploy'
import StatusTag from '@/components/StatusTag.vue'
import LogTerminal from '@/components/LogTerminal.vue'
import type { App, Server } from '@/types'

const deployStore = useDeployStore()

const page = ref(1)
const size = ref(10)
const appList = ref<App[]>([])
const serverList = ref<Server[]>([])
const logDialogVisible = ref(false)
const currentRecordId = ref(0)

const filterForm = reactive({
  appId: undefined as number | undefined,
  serverId: undefined as number | undefined,
  status: undefined as string | undefined,
  timeRange: null as string[] | null
})

async function loadDictionaries() {
  try {
    const [appRes, serverRes] = await Promise.all([
      getAppList({ page: 1, size: 100 }),
      getServerList({ page: 1, size: 100 })
    ])
    appList.value = appRes.data.records
    serverList.value = serverRes.data.records
  } catch { /* ignore */ }
}

function loadData() {
  const params: any = {
    page: page.value,
    size: size.value,
    appId: filterForm.appId,
    serverId: filterForm.serverId,
    status: filterForm.status
  }
  if (filterForm.timeRange && filterForm.timeRange.length === 2) {
    params.startTime = filterForm.timeRange[0] + ' 00:00:00'
    params.endTime = filterForm.timeRange[1] + ' 23:59:59'
  }
  deployStore.fetchList(params)
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  filterForm.appId = undefined
  filterForm.serverId = undefined
  filterForm.status = undefined
  filterForm.timeRange = null
  page.value = 1
  loadData()
}

function viewLog(id: number) {
  currentRecordId.value = id
  logDialogVisible.value = true
}

function onStatusChange(status: string) {
  if (status === 'SUCCESS' || status === 'FAILED' || status === 'ROLLED_BACK') {
    loadData()
  }
}

async function handleRollback(id: number) {
  try {
    await ElMessageBox.confirm('确定要回滚该部署？此操作不可撤销。', '回滚确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deployStore.rollback(id)
    ElMessage.success('回滚已触发')
    loadData()
  } catch { /* ignore */ }
}

function formatDuration(start: string | null, end: string | null): string {
  if (!start || !end) return '-'
  const startTime = new Date(start).getTime()
  const endTime = new Date(end).getTime()
  const diff = endTime - startTime
  if (diff < 0) return '-'
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const remainSeconds = seconds % 60
  if (minutes < 60) return `${minutes}m${remainSeconds}s`
  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  return `${hours}h${remainMinutes}m`
}

onMounted(() => {
  loadDictionaries()
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
