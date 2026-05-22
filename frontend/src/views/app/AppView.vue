<template>
  <div>
    <div class="page-header">
      <h2>应用管理</h2>
    </div>
    <div class="filter-container">
      <el-input
        v-model="searchName"
        placeholder="搜索应用名称"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增应用
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="appStore.apps" stripe v-loading="appStore.loading" style="width: 100%">
        <template #empty>
          <el-empty description="暂无应用，点击右上角「新增应用」开始使用" :image-size="80" />
        </template>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="projectName" label="所属项目" min-width="120" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ typeMap[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectPath" label="项目路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="buildCommand" label="构建命令" min-width="200" show-overflow-tooltip />
        <el-table-column prop="outputPath" label="产物路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remoteDeployPath" label="部署目录" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleBuild(row.id, cmd)">
              <el-button type="success" link size="small">
                构建<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="frontend">构建前端</el-dropdown-item>
                  <el-dropdown-item command="backend">构建后端</el-dropdown-item>
                  <el-dropdown-item command="all">全部构建</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popconfirm title="确定删除该应用?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="appStore.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <AppFormDialog
      v-model:visible="dialogVisible"
      :form-data="currentApp"
      :project-list="projectList"
      @submit="handleSubmit"
    />

    <el-dialog v-model="buildLogVisible" title="构建日志" width="70%" destroy-on-close>
      <LogTerminal :record-id="buildAppId" topic="/topic/build" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, ArrowDown } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import AppFormDialog from '@/components/AppFormDialog.vue'
import LogTerminal from '@/components/LogTerminal.vue'
import type { App, Project } from '@/types'
import { buildFrontend, buildBackend, buildAll } from '@/api/app'
import { getProjectList } from '@/api/project'

const appStore = useAppStore()
const searchName = ref('')
const page = ref(1)
const size = ref(10)
const dialogVisible = ref(false)
const currentApp = ref<Partial<App> | null>(null)
const buildLogVisible = ref(false)
const buildAppId = ref(0)
const projectList = ref<Project[]>([])

const typeMap: Record<string, string> = {
  FULL_STACK: '前后端',
  BACKEND_ONLY: '纯后端',
  FRONTEND_ONLY: '纯前端'
}

function loadData() {
  appStore.fetchList({ page: page.value, size: size.value, name: searchName.value || undefined })
}

function handleSearch() {
  page.value = 1
  loadData()
}

function openDialog(row?: App) {
  currentApp.value = row ? { ...row } : null
  dialogVisible.value = true
}

async function handleSubmit(data: Partial<App>) {
  if (data.id) {
    await appStore.editApp(data.id, data)
    ElMessage.success('更新成功')
  } else {
    await appStore.addApp(data)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  loadData()
}

async function handleDelete(id: number) {
  await appStore.removeApp(id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleBuild(appId: number, command: string) {
  buildAppId.value = appId
  buildLogVisible.value = true
  try {
    if (command === 'frontend') {
      await buildFrontend(appId)
    } else if (command === 'backend') {
      await buildBackend(appId)
    } else {
      await buildAll(appId)
    }
  } catch (e: any) {
    ElMessage.error('构建请求失败: ' + (e.message || '未知错误'))
  }
}

async function loadProjectList() {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectList.value = res.data.records
  } catch {
    projectList.value = []
  }
}

onMounted(() => {
  loadData()
  loadProjectList()
})
</script>
