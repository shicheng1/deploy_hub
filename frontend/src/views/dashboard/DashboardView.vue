<template>
  <div>
    <el-row :gutter="20" style="margin-bottom: 24px;">
      <el-col :span="6">
        <el-tooltip content="已注册的服务器总数（含在线和离线）" placement="top">
          <div class="stat-card stat-server">
            <div class="stat-icon">
              <el-icon :size="32"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalServers }}</div>
              <div class="stat-label">服务器总数</div>
            </div>
          </div>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip content="已注册的应用总数" placement="top">
          <div class="stat-card stat-app">
            <div class="stat-icon">
              <el-icon :size="32"><Box /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalApps }}</div>
              <div class="stat-label">应用总数</div>
            </div>
          </div>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip content="今日 0 点至今触发的部署次数" placement="top">
          <div class="stat-card stat-deploy">
            <div class="stat-icon">
              <el-icon :size="32"><Promotion /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayDeploys }}</div>
              <div class="stat-label">今日部署</div>
            </div>
          </div>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip content="全量部署记录中成功部署的占比" placement="top">
          <div class="stat-card stat-rate">
            <div class="stat-icon">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.successRate }}%</div>
              <div class="stat-label">部署成功率</div>
            </div>
          </div>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600;">最近部署记录</span>
          <el-button :icon="Refresh" circle size="small" @click="loadDashboard" :loading="loading" />
        </div>
      </template>
      <el-table :data="recentRecords" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="appName" label="应用名" min-width="140" />
        <el-table-column prop="serverName" label="服务器" min-width="140" />
        <el-table-column prop="version" label="版本" width="140" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="时间" min-width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewLog(row.id)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="logDialogVisible" title="部署日志" width="70%" destroy-on-close>
      <LogTerminal :record-id="currentRecordId" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Monitor, Box, Promotion, CircleCheck, Refresh } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/dashboard'
import { getDeployList } from '@/api/deploy'
import StatusTag from '@/components/StatusTag.vue'
import LogTerminal from '@/components/LogTerminal.vue'
import type { DeployRecord, DashboardStats } from '@/types'

const loading = ref(false)
const recentRecords = ref<DeployRecord[]>([])
const logDialogVisible = ref(false)
const currentRecordId = ref(0)

const stats = ref<DashboardStats>({
  totalServers: 0,
  totalApps: 0,
  todayDeploys: 0,
  successRate: 0
})

async function loadDashboard() {
  loading.value = true
  try {
    const [statsRes, deployRes] = await Promise.all([
      getDashboardStats(),
      getDeployList({ page: 1, size: 10 })
    ])
    stats.value = statsRes.data
    recentRecords.value = deployRes.data.records
  } catch {
  } finally {
    loading.value = false
  }
}

function viewLog(id: number) {
  currentRecordId.value = id
  logDialogVisible.value = true
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  padding: 24px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}
.stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}
.stat-server::before {
  background: #409eff;
}
.stat-app::before {
  background: #67c23a;
}
.stat-deploy::before {
  background: #e6a23c;
}
.stat-rate::before {
  background: #f56c6c;
}
.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  margin-right: 16px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}
.stat-card:hover .stat-icon {
  transform: scale(1.05);
}
.stat-server .stat-icon {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}
.stat-app .stat-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}
.stat-deploy .stat-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}
.stat-rate .stat-icon {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}
.stat-info {
  flex: 1;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.1;
  letter-spacing: -0.02em;
}
.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 6px;
  font-weight: 500;
}
</style>
