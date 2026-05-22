<template>
  <div>
    <div class="page-header">
      <h2>服务器管理</h2>
    </div>
    <div class="filter-container">
      <el-input
        v-model="searchName"
        placeholder="搜索服务器名称"
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
        <el-icon><Plus /></el-icon>新增服务器
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="serverStore.servers" stripe v-loading="serverStore.loading" style="width: 100%">
        <template #empty>
          <el-empty description="暂无服务器，点击右上角「新增服务器」开始使用" :image-size="80" />
        </template>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="host" label="IP地址" min-width="140" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="分组" width="120" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handleTestConnection(row.id)">测试连接</el-button>
            <el-popconfirm title="确定删除该服务器?" @confirm="handleDelete(row.id)">
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
          :total="serverStore.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <ServerFormDialog
      v-model:visible="dialogVisible"
      :form-data="currentServer"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useServerStore } from '@/stores/server'
import ServerFormDialog from '@/components/ServerFormDialog.vue'
import type { Server } from '@/types'

const serverStore = useServerStore()
const searchName = ref('')
const page = ref(1)
const size = ref(10)
const dialogVisible = ref(false)
const currentServer = ref<Partial<Server> | null>(null)

function loadData() {
  serverStore.fetchList({ page: page.value, size: size.value, name: searchName.value || undefined })
}

function handleSearch() {
  page.value = 1
  loadData()
}

function openDialog(row?: Server) {
  currentServer.value = row ? { ...row } : null
  dialogVisible.value = true
}

async function handleSubmit(data: Partial<Server>) {
  if (data.id) {
    await serverStore.editServer(data.id, data)
    ElMessage.success('更新成功')
  } else {
    await serverStore.addServer(data)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  loadData()
}

async function handleDelete(id: number) {
  await serverStore.removeServer(id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleTestConnection(id: number) {
  try {
    await serverStore.testConn(id)
    ElMessage.success('连接成功')
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  loadData()
})
</script>
