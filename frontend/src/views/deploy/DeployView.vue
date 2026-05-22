<template>
  <div>
    <div class="page-header">
      <h2>部署中心</h2>
    </div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600;">项目 / 应用</span>
          </template>
          <el-input
            v-model="treeFilter"
            placeholder="搜索项目或应用"
            clearable
            style="margin-bottom: 12px;"
          />
          <div v-loading="treeLoading">
            <el-tree
              ref="treeRef"
              :data="treeData"
              :props="treeProps"
              :filter-node-method="filterNode"
              node-key="nodeKey"
              highlight-current
              default-expand-all
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <!-- 项目节点 -->
                  <template v-if="data.nodeType === 'project'">
                    <el-icon style="margin-right: 4px;"><Folder /></el-icon>
                    <span class="tree-node-label">{{ data.name }}</span>
                    <el-tag size="small" type="info" style="margin-left: 6px;">{{ data.appCount }} 个应用</el-tag>
                    <el-button
                      type="primary"
                      link
                      size="small"
                      style="margin-left: 8px;"
                      @click.stop="openBatchDeploy(data)"
                    >
                      同时部署前后端
                    </el-button>
                  </template>
                  <!-- 应用节点 -->
                  <template v-else>
                    <el-icon style="margin-right: 4px;"><Monitor /></el-icon>
                    <span class="tree-node-label">{{ data.name }}</span>
                    <el-tag size="small" :type="appTypeTagType(data.appType)" style="margin-left: 6px;">
                      {{ typeMap[data.appType] || data.appType }}
                    </el-tag>
                  </template>
                </div>
              </template>
            </el-tree>
            <el-empty v-if="treeData.length === 0 && !treeLoading" description="暂无项目" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <DeployPanel
          :key="deployPanelKey"
          ref="deployPanelRef"
          :app-id="selectedAppId"
          :app-type="selectedAppType"
          :servers="serverList"
          :prefill-script="prefillScript"
          @deploy-triggered="handleDeploy"
          @view-log="viewLog"
        />
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 20px;">
      <template #header>
        <span style="font-weight: 600;">部署状态看板</span>
      </template>
      <el-table :data="deployStore.records" stripe v-loading="deployStore.loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="appName" label="应用" width="140" />
        <el-table-column prop="serverName" label="服务器" width="140" />
        <el-table-column prop="version" label="版本" width="140" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" min-width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewLog(row.id)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="logDialogVisible" title="部署日志" width="70%" destroy-on-close>
      <LogTerminal :record-id="currentRecordId" />
    </el-dialog>

    <!-- 批量部署弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="同时部署前后端" width="520px" destroy-on-close>
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="项目">
          <el-input :model-value="batchProjectName" disabled />
        </el-form-item>
        <el-form-item label="目标服务器" prop="serverIds">
          <el-select
            v-model="batchForm.serverIds"
            multiple
            placeholder="请选择目标服务器"
            style="width: 100%;"
          >
            <el-option
              v-for="s in serverList"
              :key="s.id"
              :label="`${s.name} (${s.host})`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="batchForm.version" placeholder="请输入版本号，如: v1.0.0" />
        </el-form-item>
        <el-form-item label="部署模式">
          <el-radio-group v-model="batchForm.deployMode">
            <el-radio value="FULL">一键部署</el-radio>
            <el-radio value="DEPLOY_ONLY">仅部署</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchDeploying" @click="handleBatchDeploy">确认部署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Folder, Monitor } from '@element-plus/icons-vue'
import { getProjectList } from '@/api/project'
import { getAppList } from '@/api/app'
import { batchDeploy } from '@/api/deploy'
import { getServerList } from '@/api/server'
import { useDeployStore } from '@/stores/deploy'
import DeployPanel from '@/components/DeployPanel.vue'
import StatusTag from '@/components/StatusTag.vue'
import LogTerminal from '@/components/LogTerminal.vue'
import type { App, Project, Server, DeployRequest, BatchDeployRequest } from '@/types'

const deployStore = useDeployStore()

// --- 树形结构相关 ---
interface TreeNode {
  nodeKey: string
  name: string
  nodeType: 'project' | 'app'
  projectId?: number
  appCount?: number
  appId?: number
  appType?: string
  isLeaf?: boolean
  children?: TreeNode[]
}

const treeRef = ref<any>()
const treeFilter = ref('')
const treeLoading = ref(false)
const treeData = ref<TreeNode[]>([])

const treeProps = {
  label: 'name',
  children: 'children',
  isLeaf: 'isLeaf'
}

const typeMap: Record<string, string> = {
  FULL_STACK: '前后端',
  BACKEND_ONLY: '纯后端',
  FRONTEND_ONLY: '纯前端'
}

function appTypeTagType(type?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (type === 'FULL_STACK') return ''
  if (type === 'BACKEND_ONLY') return 'warning'
  if (type === 'FRONTEND_ONLY') return 'success'
  return 'info'
}

// 搜索过滤
function filterNode(value: string, data: TreeNode): boolean {
  if (!value) return true
  const keyword = value.toLowerCase()
  return data.name.toLowerCase().includes(keyword)
}

watch(treeFilter, (val) => {
  treeRef.value?.filter(val)
})

// 构建树形数据
function buildTree(projects: Project[], apps: App[]): TreeNode[] {
  const appMap = new Map<number, App[]>()
  apps.forEach(app => {
    const list = appMap.get(app.projectId) || []
    list.push(app)
    appMap.set(app.projectId, list)
  })

  return projects.map(project => {
    const projectApps = appMap.get(project.id) || []
    return {
      nodeKey: `project-${project.id}`,
      name: project.name,
      nodeType: 'project' as const,
      projectId: project.id,
      appCount: projectApps.length,
      isLeaf: false,
      children: projectApps.map(app => ({
        nodeKey: `app-${app.id}`,
        name: app.name,
        nodeType: 'app' as const,
        appId: app.id,
        appType: app.type,
        isLeaf: true
      }))
    }
  })
}

async function loadTreeData() {
  treeLoading.value = true
  try {
    const [projectRes, appRes] = await Promise.all([
      getProjectList({ page: 1, size: 100 }),
      getAppList({ page: 1, size: 100 })
    ])
    treeData.value = buildTree(projectRes.data.records, appRes.data.records)
  } finally {
    treeLoading.value = false
  }
}

// --- 应用选择相关 ---
const selectedAppId = ref<number | undefined>()
const selectedAppType = ref<string | undefined>()
const deployPanelKey = ref(0)
const deployPanelRef = ref<InstanceType<typeof DeployPanel> | null>(null)
const prefillScript = ref('')

const serverList = ref<Server[]>([])

function handleNodeClick(data: TreeNode) {
  if (data.nodeType === 'app' && data.appId) {
    // 切换应用时更新 key，强制 DeployPanel 完全重置
    selectedAppId.value = data.appId
    selectedAppType.value = data.appType
    deployPanelKey.value++
  }
  // 项目节点点击仅展开/折叠，el-tree 默认处理
}

async function loadServers() {
  try {
    const res = await getServerList({ page: 1, size: 100 })
    serverList.value = res.data.records
  } catch { /* ignore */ }
}

async function handleDeploy(data: DeployRequest) {
  try {
    const res = await deployStore.deploy(data)
    if (res?.data) {
      deployPanelRef.value?.startTracking(res.data)
    }
    ElMessage.success('部署已触发')
    deployStore.fetchList({ page: 1, size: 10 })
  } catch (err: any) {
    deployPanelRef.value?.handleDeployError(err)
  }
}

// --- 日志弹窗 ---
const logDialogVisible = ref(false)
const currentRecordId = ref(0)

function viewLog(id: number) {
  currentRecordId.value = id
  logDialogVisible.value = true
}

// --- 批量部署相关 ---
const batchDialogVisible = ref(false)
const batchDeploying = ref(false)
const batchProjectId = ref<number>(0)
const batchProjectName = ref('')
const batchFormRef = ref<FormInstance>()

const batchForm = ref<{
  serverIds: number[]
  version: string
  deployMode: string
}>({
  serverIds: [],
  version: '',
  deployMode: 'FULL'
})

const batchRules: FormRules = {
  serverIds: [{ required: true, message: '请选择目标服务器', trigger: 'change', type: 'array' }]
}

function openBatchDeploy(projectNode: TreeNode) {
  batchProjectId.value = projectNode.projectId!
  batchProjectName.value = projectNode.name
  batchForm.value = {
    serverIds: [],
    version: '',
    deployMode: 'FULL'
  }
  batchDialogVisible.value = true
}

async function handleBatchDeploy() {
  if (!batchFormRef.value) return
  await batchFormRef.value.validate()
  batchDeploying.value = true
  try {
    const data: BatchDeployRequest = {
      projectId: batchProjectId.value,
      serverIds: batchForm.value.serverIds,
      version: batchForm.value.version || undefined,
      deployMode: batchForm.value.deployMode
    }
    await batchDeploy(data)
    ElMessage.success('批量部署已触发')
    batchDialogVisible.value = false
    deployStore.fetchList({ page: 1, size: 10 })
  } catch (err: any) {
    ElMessage.error(err?.message || '批量部署失败')
  } finally {
    batchDeploying.value = false
  }
}

// --- 初始化 ---
onMounted(() => {
  loadTreeData()
  loadServers()
  deployStore.fetchList({ page: 1, size: 10 })
  const script = sessionStorage.getItem('deployScript')
  if (script) {
    prefillScript.value = script
    sessionStorage.removeItem('deployScript')
  }
})
</script>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  font-size: 14px;
  overflow: hidden;
}

.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
