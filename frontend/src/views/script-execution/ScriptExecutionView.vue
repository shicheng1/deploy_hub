<template>
  <div>
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2>脚本执行</h2>
        <el-radio-group v-model="executionMode" size="default">
          <el-radio-button value="single">单脚本执行</el-radio-button>
          <el-radio-button value="batch">批量执行</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600;">选择服务器</span>
          </template>
          <el-input
            v-model="serverSearch"
            placeholder="搜索服务器"
            clearable
            style="margin-bottom: 12px;"
          />
          <div v-loading="serverLoading">
            <el-checkbox-group v-model="selectedServerIds">
              <div
                v-for="server in filteredServers"
                :key="server.id"
                class="server-item"
              >
                <el-checkbox :value="server.id">
                  <span style="font-weight: 500;">{{ server.name }}</span>
                  <span style="font-size: 12px; color: #909399; margin-left: 8px;">{{ server.host }}</span>
                </el-checkbox>
              </div>
            </el-checkbox-group>
            <el-empty v-if="filteredServers.length === 0" description="暂无服务器" :image-size="60" />
          </div>
        </el-card>

        <el-card v-if="executionMode === 'batch'" shadow="never" style="margin-top: 20px;">
          <template #header>
            <span style="font-weight: 600;">选择脚本模板</span>
          </template>
          <el-input
            v-model="scriptSearch"
            placeholder="搜索脚本"
            clearable
            style="margin-bottom: 12px;"
          />
          <div v-loading="scriptLoading">
            <el-checkbox-group v-model="selectedScriptIds">
              <div
                v-for="script in filteredScripts"
                :key="script.id"
                class="script-item"
              >
                <el-checkbox :value="script.id">
                  <span style="font-weight: 500;">{{ script.name }}</span>
                  <el-tag v-if="script.appName" size="small" type="info" style="margin-left: 8px;">{{ script.appName }}</el-tag>
                  <el-tag v-else size="small" type="warning" style="margin-left: 8px;">全局</el-tag>
                </el-checkbox>
              </div>
            </el-checkbox-group>
            <el-empty v-if="filteredScripts.length === 0" description="暂无脚本" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <template v-if="executionMode === 'single'">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 600;">脚本内容</span>
                <div style="display: flex; gap: 12px;">
                  <input
                    type="file"
                    id="execution-script-upload"
                    accept=".sh,.py,.txt,.bat,.cmd,.ps1,.js"
                    style="display: none;"
                    @change="handleExecutionFileUpload"
                  />
                  <el-button type="info" size="small" @click="triggerExecutionFileUpload">
                    <el-icon><Upload /></el-icon>上传脚本
                  </el-button>
                  <el-select
                    v-model="selectedTemplateId"
                    placeholder="从模板选择"
                    clearable
                    style="width: 200px;"
                    @change="handleTemplateChange"
                  >
                    <el-option
                      v-for="t in templateList"
                      :key="t.id"
                      :label="t.name"
                      :value="t.id"
                    />
                  </el-select>
                  <el-button type="primary" :loading="executing" :disabled="selectedServerIds.length === 0 || !scriptContent" @click="handleExecute">
                    <el-icon><Promotion /></el-icon>执行脚本
                  </el-button>
                </div>
              </div>
            </template>
            <ScriptEditor v-model="scriptContent" />
          </el-card>

          <!-- 单脚本执行实时日志 -->
          <el-card v-if="executionTasks.length > 0" shadow="never" style="margin-top: 20px;">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 600;">执行日志</span>
                <el-button type="info" size="small" @click="executionTasks = []">清除</el-button>
              </div>
            </template>
            <div
              v-for="task in executionTasks"
              :key="task.id"
              class="task-item"
            >
              <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
                <div>
                  <span style="font-weight: 500;">{{ task.scriptName }}</span>
                  <span style="font-size: 12px; color: #909399; margin-left: 8px;">@ {{ task.serverName }}</span>
                </div>
                <StatusTag :status="task.status" />
              </div>
              <div class="log-container">
                <LogTerminal
                  :record-id="task.id"
                  topic="/topic/script-execution"
                  :fallback-log-api="getScriptExecutionLog"
                  :auto-scroll="true"
                />
              </div>
            </div>
          </el-card>

          <el-card shadow="never" style="margin-top: 20px;">
            <template #header>
              <span style="font-weight: 600;">执行记录</span>
            </template>
            <el-table :data="executionRecords" stripe v-loading="recordsLoading" style="width: 100%">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="serverName" label="服务器" width="140" />
              <el-table-column prop="serverHost" label="地址" width="160" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :status="row.status" />
                </template>
              </el-table-column>
              <el-table-column prop="startedAt" label="开始时间" min-width="180" />
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="viewLog(row.id)">查看日志</el-button>
                  <el-button
                    v-if="row.status === 'RUNNING'"
                    type="danger"
                    link
                    size="small"
                    @click="handleCancel(row.id)"
                  >暂停</el-button>
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
                @change="loadRecords"
              />
            </div>
          </el-card>
        </template>

        <template v-if="executionMode === 'batch'">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 600;">脚本内容预览</span>
                <el-button
                  type="primary"
                  :loading="executing"
                  :disabled="selectedServerIds.length === 0 || selectedScriptIds.length === 0"
                  @click="handleBatchExecute"
                >
                  <el-icon><VideoPlay /></el-icon>批量执行
                </el-button>
              </div>
            </template>
            <div v-if="selectedScripts.length > 0">
              <el-tabs type="border-card" v-model="activeScriptTab">
                <el-tab-pane
                  v-for="script in selectedScripts"
                  :key="script.id"
                  :label="script.name"
                  :name="String(script.id)"
                >
                  <pre class="script-preview">{{ script.content }}</pre>
                </el-tab-pane>
              </el-tabs>
            </div>
            <el-empty v-else description="请选择脚本查看内容" :image-size="80" />
          </el-card>

          <el-card shadow="never" style="margin-top: 20px;">
            <template #header>
              <span style="font-weight: 600;">执行任务列表</span>
            </template>
            <div v-if="executionTasks.length > 0">
              <div
                v-for="task in executionTasks"
                :key="task.id"
                class="task-item"
              >
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
                  <div>
                    <span style="font-weight: 500;">{{ task.scriptName }}</span>
                    <span style="font-size: 12px; color: #909399; margin-left: 8px;">@ {{ task.serverName }}</span>
                  </div>
                  <StatusTag :status="task.status" />
                </div>
                <div class="log-container">
                  <LogTerminal
                    :record-id="task.id"
                    topic="/topic/script-execution"
                    :fallback-log-api="getScriptExecutionLog"
                    :auto-scroll="true"
                  />
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无执行任务" :image-size="60" />
          </el-card>
        </template>
      </el-col>
    </el-row>

    <el-dialog v-model="logDialogVisible" title="执行日志" width="70%" destroy-on-close>
      <LogTerminal :record-id="currentLogId" topic="/topic/script-execution" :fallback-log-api="getScriptExecutionLog" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Upload, VideoPlay } from '@element-plus/icons-vue'
import { getServerList } from '@/api/server'
import { getScriptList } from '@/api/script'
import { getScriptExecutionList, executeScript, batchExecuteScript, getScriptExecutionLog, cancelScriptExecution } from '@/api/scriptExecution'
import type { Server, ScriptTemplate } from '@/types'
import wsClient from '@/utils/websocket'
import ScriptEditor from '@/components/ScriptEditor.vue'
import StatusTag from '@/components/StatusTag.vue'
import LogTerminal from '@/components/LogTerminal.vue'

const executionMode = ref<'single' | 'batch'>('single')

const serverSearch = ref('')
const serverLoading = ref(false)
const serverList = ref<Server[]>([])
const selectedServerIds = ref<number[]>([])

const scriptContent = ref('')
const selectedTemplateId = ref<number | undefined>()
const templateList = ref<ScriptTemplate[]>([])
const executing = ref(false)

const scriptSearch = ref('')
const scriptLoading = ref(false)
const scriptList = ref<ScriptTemplate[]>([])
const selectedScriptIds = ref<number[]>([])
const activeScriptTab = ref('')

interface ExecutionTask {
  id: number
  scriptName: string
  serverName: string
  status: string
}
const executionTasks = ref<ExecutionTask[]>([])

const executionRecords = ref<any[]>([])
const recordsLoading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const logDialogVisible = ref(false)
const currentLogId = ref(0)

const wsSubscriptions = ref<any[]>([])

const filteredServers = computed(() => {
  if (!serverSearch.value) return serverList.value
  const keyword = serverSearch.value.toLowerCase()
  return serverList.value.filter(s =>
    s.name.toLowerCase().includes(keyword) || s.host.toLowerCase().includes(keyword)
  )
})

const filteredScripts = computed(() => {
  if (!scriptSearch.value) return scriptList.value
  const keyword = scriptSearch.value.toLowerCase()
  return scriptList.value.filter(s =>
    s.name.toLowerCase().includes(keyword)
  )
})

const selectedScripts = computed(() => {
  return scriptList.value.filter(s => selectedScriptIds.value.includes(s.id))
})

watch(executionMode, (mode) => {
  if (mode === 'single') {
    unsubscribeAll()
    executionTasks.value = []
  }
})

async function loadServers() {
  serverLoading.value = true
  try {
    const res = await getServerList({ page: 1, size: 100 })
    serverList.value = res.data.records
  } finally {
    serverLoading.value = false
  }
}

async function loadTemplates() {
  try {
    const res = await getScriptList({ page: 1, size: 100 })
    templateList.value = res.data.records
  } catch { /* ignore */ }
}

async function loadScripts() {
  scriptLoading.value = true
  try {
    const res = await getScriptList({ page: 1, size: 100 })
    scriptList.value = res.data.records
  } finally {
    scriptLoading.value = false
  }
}

function handleTemplateChange(templateId: number) {
  if (!templateId) return
  const template = templateList.value.find(t => t.id === templateId)
  if (template) {
    scriptContent.value = template.content
  }
}

function triggerExecutionFileUpload() {
  const input = document.getElementById('execution-script-upload') as HTMLInputElement
  input.click()
}

async function handleExecutionFileUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    scriptContent.value = text
    ElMessage.success('文件内容已加载')
    input.value = ''
  } catch {
    ElMessage.error('读取文件失败')
    input.value = ''
  }
}

async function handleExecute() {
  if (selectedServerIds.value.length === 0) {
    ElMessage.warning('请选择至少一台服务器')
    return
  }
  if (!scriptContent.value) {
    ElMessage.warning('请输入脚本内容')
    return
  }
  executing.value = true
  try {
    const res = await executeScript({
      scriptContent: scriptContent.value,
      serverIds: selectedServerIds.value
    })
    const executionIds = res.data.executionIds as number[]
    const serversMap = new Map(serverList.value.map(s => [s.id, s.name]))

    executionTasks.value = []
    let index = 0
    for (const serverId of selectedServerIds.value) {
      if (index < executionIds.length) {
        executionTasks.value.push({
          id: executionIds[index],
          scriptName: '自定义脚本',
          serverName: serversMap.get(serverId) || '未知',
          status: 'PENDING'
        })
        index++
      }
    }

    subscribeTaskStatuses(executionIds)
    ElMessage.success(`已触发 ${executionTasks.value.length} 个执行任务`)
    loadRecords()
  } catch { /* ignore */ } finally {
    executing.value = false
  }
}

async function handleBatchExecute() {
  if (selectedServerIds.value.length === 0) {
    ElMessage.warning('请选择至少一台服务器')
    return
  }
  if (selectedScriptIds.value.length === 0) {
    ElMessage.warning('请选择至少一个脚本')
    return
  }

  const scripts = selectedScripts.value
  const scriptContents = scripts.map(s => s.content)

  executing.value = true
  try {
    const res = await batchExecuteScript({
      scriptContents,
      serverIds: selectedServerIds.value
    })

    const executionIds = res.data.executionIds as number[]

    const serversMap = new Map(serverList.value.map(s => [s.id, s.name]))

    executionTasks.value = []
    let index = 0
    for (const script of scripts) {
      for (const serverId of selectedServerIds.value) {
        if (index < executionIds.length) {
          executionTasks.value.push({
            id: executionIds[index],
            scriptName: script.name,
            serverName: serversMap.get(serverId) || '未知',
            status: 'PENDING'
          })
          index++
        }
      }
    }

    subscribeTaskStatuses(executionIds)
    ElMessage.success(`已触发 ${executionTasks.value.length} 个执行任务`)
  } catch (err) {
    ElMessage.error('批量执行失败')
  } finally {
    executing.value = false
  }
}

function subscribeTaskStatuses(ids: number[]) {
  unsubscribeAll()

  if (!wsClient.isConnected()) {
    wsClient.connect().then(() => {
      doSubscribe(ids)
    }).catch(() => {})
  } else {
    doSubscribe(ids)
  }
}

function doSubscribe(ids: number[]) {
  for (const id of ids) {
    const sub = wsClient.subscribe(`/topic/script-execution/${id}`, (data: any) => {
      const task = executionTasks.value.find(t => t.id === id)
      if (task && data.status) {
        task.status = data.status
      }
    })
    if (sub) {
      wsSubscriptions.value.push(sub)
    }
  }
}

function unsubscribeAll() {
  for (const sub of wsSubscriptions.value) {
    try { sub.unsubscribe() } catch { /* ignore */ }
  }
  wsSubscriptions.value = []
}

async function handleCancel(id: number) {
  try {
    await cancelScriptExecution(id)
    ElMessage.success('已发送暂停请求')
    loadRecords()
  } catch (err) {
    ElMessage.error('暂停失败')
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await getScriptExecutionList({ page: page.value, size: size.value })
    executionRecords.value = res.data.records
    total.value = res.data.total
  } finally {
    recordsLoading.value = false
  }
}

function viewLog(id: number) {
  currentLogId.value = id
  logDialogVisible.value = true
}

onMounted(() => {
  loadServers()
  loadTemplates()
  loadScripts()
  loadRecords()
})

onUnmounted(() => {
  unsubscribeAll()
})
</script>

<style scoped>
.server-item {
  padding: 6px 0;
}

.script-item {
  padding: 8px 0;
}

.task-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.log-container {
  background: #fafafa;
  border-radius: 4px;
  overflow: hidden;
  max-height: 200px;
}

.script-preview {
  white-space: pre-wrap;
  word-break: break-all;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
  margin: 0;
}
</style>
