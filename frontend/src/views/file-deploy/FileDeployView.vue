<template>
  <div>
    <div class="page-header">
      <h2>文件部署</h2>
    </div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600;">选择目标服务器</span>
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

        <el-card shadow="never" style="margin-top: 20px;">
          <template #header>
            <span style="font-weight: 600;">部署选项</span>
          </template>
          <el-form :model="deployForm" label-width="120px">
            <el-form-item label="远程路径">
              <el-input v-model="deployForm.remotePath" placeholder="如 /tmp 或 /home/user/deploy" />
            </el-form-item>
            <el-form-item label="自定义文件名">
              <el-input v-model="deployForm.fileName" placeholder="保持原文件名留空" />
            </el-form-item>
            <el-form-item label="安装脚本">
              <el-switch v-model="deployForm.enableScript" />
            </el-form-item>
            <el-form-item v-if="deployForm.enableScript">
              <el-textarea
                ref="scriptTextareaRef"
                v-model="deployForm.installScript"
                :rows="6"
                placeholder="输入安装脚本，可用 ${FILE_PATH} 表示上传文件的路径"
              />
              <VariableHint :variables="fileDeployVariables" @insert="handleInsertVariable" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: 600;">文件上传</span>
              <el-button 
                type="primary" 
                :loading="deploying" 
                :disabled="selectedServerIds.length === 0 || !selectedFile" 
                @click="handleDeploy"
              >
                <el-icon><Upload /></el-icon>开始部署
              </el-button>
            </div>
          </template>
          <div v-if="!selectedFile" class="upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="handleDrop">
            <div style="text-align: center; padding: 40px;">
              <el-icon size="48" style="color: #409EFF; margin-bottom: 16px;"><Upload /></el-icon>
              <p style="color: #606266;">点击或拖拽文件到此处上传</p>
              <p style="color: #909399; font-size: 12px; margin-top: 8px;">支持所有文件格式</p>
            </div>
          </div>
          <div v-else class="file-preview">
            <el-row :gutter="12" align="middle">
              <el-col :span="4">
                <div class="file-icon">
                  <el-icon size="48" style="color: #409EFF;"><Document /></el-icon>
                </div>
              </el-col>
              <el-col :span="16">
                <p style="font-weight: 500;">{{ selectedFile.name }}</p>
                <p style="color: #909399; font-size: 12px;">{{ formatFileSize(selectedFile.size) }}</p>
              </el-col>
              <el-col :span="4">
                <el-button type="text" @click="clearFile">更换文件</el-button>
              </el-col>
            </el-row>
          </div>
          <input ref="fileInput" type="file" class="hidden-input" @change="handleFileChange" />
        </el-card>

        <el-card shadow="never" style="margin-top: 20px;">
          <template #header>
            <span style="font-weight: 600;">部署结果</span>
          </template>
          <div v-if="deployResults.length > 0">
            <div
              v-for="result in deployResults"
              :key="result.serverId"
              class="result-item"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                  <span style="font-weight: 500;">{{ result.serverName }}</span>
                  <span style="font-size: 12px; color: #909399; margin-left: 8px;">{{ result.serverHost }}</span>
                </div>
                <div>
                  <span v-if="result.success" class="success-tag">成功</span>
                  <span v-else class="fail-tag">失败</span>
                </div>
              </div>
              <p style="color: #606266; font-size: 13px; margin-top: 8px;">{{ result.message }}</p>
            </div>
            <div style="margin-top: 16px; padding-top: 16px; border-top: 1px solid #e8e8e8; display: flex; justify-content: space-between;">
              <span>总计: {{ deployResults.length }} 台服务器</span>
              <span>成功: <span style="color: #67c23a;">{{ successCount }}</span> | 失败: <span style="color: #f56c6c;">{{ failCount }}</span></span>
            </div>
          </div>
          <el-empty v-else description="暂无部署记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Document } from '@element-plus/icons-vue'
import { getServerList } from '@/api/server'
import { uploadFile, deployWithScript } from '@/api/fileDeploy'
import VariableHint from '@/components/VariableHint.vue'
import type { Server } from '@/types'

const serverSearch = ref('')
const serverLoading = ref(false)
const serverList = ref<Server[]>([])
const selectedServerIds = ref<number[]>([])

const selectedFile = ref<File | null>(null) as { value: globalThis.File | null }
const fileInput = ref<HTMLInputElement | null>(null)
const scriptTextareaRef = ref<any>()

const deploying = ref(false)

const fileDeployVariables = [
  { name: 'FILE_PATH', description: '上传文件的完整路径' }
]

function handleInsertVariable(varName: string) {
  const textarea = scriptTextareaRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | undefined
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const before = deployForm.value.installScript.substring(0, start)
    const after = deployForm.value.installScript.substring(end)
    deployForm.value.installScript = before + varName + after
    nextTick(() => {
      textarea.focus()
      const newPos = start + varName.length
      textarea.setSelectionRange(newPos, newPos)
    })
  } else {
    deployForm.value.installScript += varName
  }
}

const deployForm = ref({
  remotePath: '',
  fileName: '',
  enableScript: false,
  installScript: ''
})

interface DeployResult {
  serverId: number
  serverName: string
  serverHost: string
  success: boolean
  message: string
}
const deployResults = ref<DeployResult[]>([])

const filteredServers = computed(() => {
  if (!serverSearch.value) return serverList.value
  const keyword = serverSearch.value.toLowerCase()
  return serverList.value.filter(s =>
    s.name.toLowerCase().includes(keyword) || s.host.toLowerCase().includes(keyword)
  )
})

const successCount = computed(() => deployResults.value.filter(r => r.success).length)
const failCount = computed(() => deployResults.value.filter(r => !r.success).length)

async function loadServers() {
  serverLoading.value = true
  try {
    const res = await getServerList({ page: 1, size: 100 })
    serverList.value = res.data.records
  } finally {
    serverLoading.value = false
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0]
  }
}

function handleDrop(event: DragEvent) {
  if (event.dataTransfer?.files.length) {
    selectedFile.value = event.dataTransfer.files[0]
  }
}

function clearFile() {
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

async function handleDeploy() {
  if (selectedServerIds.value.length === 0) {
    ElMessage.warning('请选择至少一台服务器')
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  deploying.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    selectedServerIds.value.forEach(id => {
      formData.append('serverIds', String(id))
    })
    formData.append('remotePath', deployForm.value.remotePath)
    if (deployForm.value.fileName) {
      formData.append('fileName', deployForm.value.fileName)
    }

    let res
    if (deployForm.value.enableScript && deployForm.value.installScript) {
      formData.append('installScript', deployForm.value.installScript)
      res = await deployWithScript(formData)
    } else {
      res = await uploadFile(formData)
    }

    deployResults.value = res.data.results.map((item: any) => ({
      serverId: item.serverId,
      serverName: item.serverName,
      serverHost: item.serverHost,
      success: item.success,
      message: item.message
    }))

    ElMessage.success(`部署完成！成功: ${res.data.successCount}, 失败: ${res.data.failCount}`)
  } catch (err) {
    ElMessage.error('部署失败')
  } finally {
    deploying.value = false
  }
}

onMounted(() => {
  loadServers()
})
</script>

<style scoped>
.server-item {
  padding: 6px 0;
}

.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-area:hover {
  border-color: #409EFF;
}

.file-preview {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.file-icon {
  display: flex;
  justify-content: center;
  align-items: center;
}

.result-item {
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 12px;
}

.success-tag {
  background: #f0f9eb;
  color: #67c23a;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.fail-tag {
  background: #fef0f0;
  color: #f56c6c;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.hidden-input {
  display: none;
}
</style>