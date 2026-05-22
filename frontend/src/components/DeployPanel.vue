<template>
  <div class="deploy-panel">
    <div v-if="!appId" class="deploy-guide">
      <el-steps direction="vertical" :active="0" class="guide-steps">
        <el-step title="选择应用" description="在左侧选择要部署的应用" />
        <el-step title="选择服务器" description="选择一个或多个目标服务器" />
        <el-step title="配置脚本" description="编写或从模板选择部署脚本" />
        <el-step title="触发部署" description="点击「触发部署」开始部署" />
      </el-steps>
    </div>
    <template v-else>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="目标服务器" prop="serverIds">
          <el-select
            v-model="form.serverIds"
            multiple
            placeholder="请选择目标服务器"
            style="width: 100%;"
          >
            <el-option
              v-for="s in servers"
              :key="s.id"
              :label="`${s.name} (${s.host})`"
              :value="s.id"
            >
              <span>{{ s.name }}</span>
              <span style="float: right; color: #909399; font-size: 12px;">{{ s.host }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="请输入版本号，如: v1.0.0" />
        </el-form-item>
        <el-form-item v-if="appType !== 'FRONTEND_ONLY'" label="服务名" prop="serviceName">
          <el-input
            v-model="form.serviceName"
            placeholder="服务名（用于systemd服务名和JAR文件名）"
            :disabled="!form.deployScript"
          />
          <span v-if="form.serviceName" style="font-size: 12px; color: #909399;">
            服务文件: /etc/systemd/system/{{ form.serviceName }}.service | JAR文件: {{ form.serviceName }}.jar
          </span>
        </el-form-item>
        <el-form-item label="部署模式">
          <el-radio-group v-model="form.deployMode">
            <el-radio value="FULL">
              一键部署
              <span style="font-size: 12px; color: #909399; margin-left: 4px;">自动执行构建、传输、部署全流程</span>
            </el-radio>
            <el-radio value="DEPLOY_ONLY">
              仅部署
              <span style="font-size: 12px; color: #909399; margin-left: 4px;">跳过构建，直接传输已有产物并执行脚本</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="部署脚本" prop="deployScript">
          <div style="width: 100%; margin-bottom: 8px;">
            <el-select
              v-model="selectedTemplateId"
              placeholder="从模板选择"
              clearable
              style="width: 100%;"
              @change="handleTemplateChange"
            >
              <el-option
                v-for="t in templateList"
                :key="t.id"
                :label="t.name"
                :value="t.id"
              />
            </el-select>
          </div>
          <el-input
            ref="scriptInputRef"
            v-model="form.deployScript"
            type="textarea"
            :rows="6"
            placeholder="请输入部署脚本（可选，也可从上方模板选择）"
            style="width: 100%;"
          />
          <VariableHint :variables="deployVariables" @insert="handleInsertVariable" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="deploying" @click="handleDeploy">
            <el-icon><Promotion /></el-icon>触发部署
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="deployResults.length > 0" class="deploy-results">
        <div style="font-weight: 600; margin-bottom: 12px;">部署状态</div>
        <el-steps :active="deployStepActive" align-center style="margin-bottom: 16px;">
          <el-step title="构建中" :status="deployStepStatus(0)" />
          <el-step title="传输中" :status="deployStepStatus(1)" />
          <el-step title="部署中" :status="deployStepStatus(2)" />
          <el-step title="完成" :status="deployStepStatus(3)" />
        </el-steps>
        <el-alert
          v-if="deploySummary"
          :title="`部署完成：成功 ${deploySummary.successCount} 台，失败 ${deploySummary.failCount} 台`"
          :type="deploySummary.failCount > 0 ? 'warning' : 'success'"
          show-icon
          :closable="false"
          style="margin-bottom: 12px;"
        />
        <el-table :data="deployResults" stripe size="small">
          <el-table-column prop="serverName" label="服务器" width="180" />
          <el-table-column prop="serverHost" label="地址" width="180" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="message" label="信息" min-width="200" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.status === 'FAILED' && row.recordId" type="primary" link size="small" @click="emit('view-log', row.recordId)">查看日志</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import type { Server, DeployRequest, ScriptTemplate } from '@/types'
import { getScriptList } from '@/api/script'
import { getDeployConfig, saveDeployConfig } from '@/api/deployConfig'
import StatusTag from '@/components/StatusTag.vue'
import VariableHint from '@/components/VariableHint.vue'
import wsClient from '@/utils/websocket'

interface DeployResult {
  recordId: number
  serverId: number
  serverName: string
  serverHost: string
  status: string
  step: string
  message: string
}

const props = defineProps<{
  appId?: number
  appType?: 'FULL_STACK' | 'BACKEND_ONLY' | 'FRONTEND_ONLY'
  servers: Server[]
  prefillScript?: string
}>()

const emit = defineEmits<{
  (e: 'deploy-triggered', data: DeployRequest): void
  (e: 'view-log', recordId: number): void
}>()

const formRef = ref<FormInstance>()
const scriptInputRef = ref<any>()
const deploying = ref(false)
const deployResults = ref<DeployResult[]>([])
const subscriptions = ref<any[]>([])

const selectedTemplateId = ref<number | undefined>()
const templateList = ref<ScriptTemplate[]>([])

const form = reactive({
  serverIds: [] as number[],
  version: '',
  serviceName: '',
  deployScript: '',
  deployMode: 'FULL'
})

const rules: FormRules = {
  serverIds: [{ required: true, message: '请选择目标服务器', trigger: 'change', type: 'array' }]
}

const deployVariables = [
  { name: 'APP_NAME', description: '应用名称', group: '应用变量' },
  { name: 'APP_TYPE', description: '应用类型', group: '应用变量' },
  { name: 'PROJECT_PATH', description: '项目路径', group: '应用变量' },
  { name: 'BUILD_COMMAND', description: '构建命令', group: '应用变量' },
  { name: 'OUTPUT_PATH', description: '输出路径', group: '应用变量' },
  { name: 'REMOTE_FILE_PATH', description: '远程文件路径', group: '应用变量' },
  { name: 'REMOTE_DEPLOY_PATH', description: '远程部署路径', group: '应用变量' },
  { name: 'SERVER_HOST', description: '服务器地址', group: '服务器变量' },
  { name: 'SERVER_PORT', description: '服务器端口', group: '服务器变量' },
  { name: 'SERVER_USER', description: '服务器用户', group: '服务器变量' },
  { name: 'SERVER_GROUP', description: '服务器分组', group: '服务器变量' },
  { name: 'VERSION', description: '版本号', group: '系统变量' },
  { name: 'TIMESTAMP', description: '时间戳', group: '系统变量' },
  { name: 'DATE', description: '日期', group: '系统变量' }
]

function handleInsertVariable(varName: string) {
  const textarea = scriptInputRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | undefined
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const before = form.deployScript.substring(0, start)
    const after = form.deployScript.substring(end)
    form.deployScript = before + varName + after
    nextTick(() => {
      textarea.focus()
      const newPos = start + varName.length
      textarea.setSelectionRange(newPos, newPos)
    })
  } else {
    form.deployScript += varName
  }
}

const stepIndexMap: Record<string, number> = {
  BUILDING: 0,
  TRANSFERRING: 1,
  DEPLOYING: 2,
  COMPLETED: 3
}

const deployStepActive = computed(() => {
  if (deployResults.value.length === 0) return 0
  const inProgress = deployResults.value.filter(r => r.status !== 'SUCCESS' && r.status !== 'FAILED')
  if (inProgress.length === 0) return 3
  const steps = inProgress.map(r => {
    if (!r.step) return 0
    return stepIndexMap[r.step] ?? 0
  })
  return Math.min(...steps)
})

const deploySummary = computed(() => {
  const allDone = deployResults.value.length > 0 && deployResults.value.every(r => r.status === 'SUCCESS' || r.status === 'FAILED')
  if (!allDone) return null
  const successCount = deployResults.value.filter(r => r.status === 'SUCCESS').length
  const failCount = deployResults.value.filter(r => r.status === 'FAILED').length
  return { successCount, failCount }
})

async function loadTemplates() {
  try {
    const res = await getScriptList({ page: 1, size: 100 })
    let records = res.data.records
    // 根据 appType 过滤脚本模板分类
    if (props.appType === 'FRONTEND_ONLY') {
      records = records.filter((t: ScriptTemplate) => t.category === 'FRONTEND' || t.category === 'GENERAL')
    } else if (props.appType === 'BACKEND_ONLY' || props.appType === 'FULL_STACK') {
      records = records.filter((t: ScriptTemplate) => t.category === 'BACKEND' || t.category === 'GENERAL')
    }
    templateList.value = records
  } catch { /* ignore */ }
}

function handleTemplateChange(templateId: number) {
  if (!templateId) return
  const template = templateList.value.find(t => t.id === templateId)
  if (template) {
    form.deployScript = template.content
    if (!form.serviceName && props.appId) {
      form.serviceName = `app-${props.appId}`
    }
  }
}

function cleanupSubscriptions() {
  subscriptions.value.forEach(sub => {
    try { sub.unsubscribe() } catch { /* ignore */ }
  })
  subscriptions.value = []
}

async function handleDeploy() {
  if (!formRef.value || !props.appId) return
  await formRef.value.validate()
  deploying.value = true
  cleanupSubscriptions()
  deployResults.value = form.serverIds.map(id => {
    const server = props.servers.find(s => s.id === id)
    return {
      recordId: 0,
      serverId: id,
      serverName: server?.name ?? '',
      serverHost: server?.host ?? '',
      status: 'PENDING',
      step: '',
      message: '等待中'
    }
  })
  
  let deployScript = form.deployScript || ''
  if (form.serviceName) {
    deployScript = deployScript.replace(/\$\{APP_NAME\}/g, form.serviceName)
  }
  
  const data: DeployRequest = {
    appId: props.appId,
    serverIds: form.serverIds,
    version: form.version || undefined,
    deployScript: deployScript || undefined,
    deployMode: form.deployMode
  }
  emit('deploy-triggered', data)

  // 保存部署配置
  try {
    await saveDeployConfig({
      appId: props.appId,
      serverIds: form.serverIds,
      version: form.version,
      serviceName: form.serviceName,
      deployMode: form.deployMode,
      scriptTemplateId: selectedTemplateId.value,
      deployScript: form.deployScript
    })
  } catch { /* 保存配置失败不影响部署流程 */ }
}

async function startTracking(recordIds: number[]) {
  recordIds.forEach((rid, idx) => {
    if (deployResults.value[idx]) {
      deployResults.value[idx].recordId = rid
      deployResults.value[idx].status = 'DEPLOYING'
      deployResults.value[idx].message = '已提交'
    }
  })
  try {
    if (!wsClient.isConnected()) {
      await wsClient.connect()
    }
  } catch { /* ignore */ }
  recordIds.forEach(rid => {
    const sub = wsClient.subscribe(`/topic/deploy/${rid}`, (data: any) => {
      handleWsMessage(rid, data)
    })
    if (sub) {
      subscriptions.value.push(sub)
    }
  })
}

function handleWsMessage(recordId: number, data: any) {
  const result = deployResults.value.find(r => r.recordId === recordId)
  if (!result) return

  if (typeof data === 'object' && data !== null && data.type === 'STEP') {
    const step = data.step
    result.step = step

    if (step === 'BUILDING') {
      result.status = 'DEPLOYING'
      result.message = '构建中'
    } else if (step === 'TRANSFERRING') {
      result.status = 'DEPLOYING'
      result.message = '传输中'
    } else if (step === 'DEPLOYING') {
      result.status = 'DEPLOYING'
      result.message = '部署中'
    } else if (step === 'COMPLETED') {
      result.status = 'SUCCESS'
      result.message = '部署完成'
    } else if (step === 'FAILED') {
      result.status = 'FAILED'
      result.message = data.message || '部署失败'
    }
  } else if (typeof data === 'object' && data !== null && data.status) {
    if (data.status === 'SUCCESS') {
      result.status = 'SUCCESS'
      result.message = '部署完成'
      result.step = 'COMPLETED'
    } else if (data.status === 'FAILED') {
      result.status = 'FAILED'
      result.message = data.errorMessage || '部署失败'
      result.step = 'FAILED'
    }
  }

  const allDone = deployResults.value.every(r => r.status === 'SUCCESS' || r.status === 'FAILED')
  if (allDone) {
    deploying.value = false
  }
}

function handleDeployError(err: any) {
  deployResults.value.forEach(r => {
    r.status = 'FAILED'
    r.message = err?.message || '部署失败'
  })
  deploying.value = false
}

function deployStepStatus(step: number): '' | 'process' | 'wait' | 'finish' | 'success' | 'error' {
  if (step < deployStepActive.value) return 'finish'
  if (step === deployStepActive.value) {
    const hasFailed = deployResults.value.some(r => r.status === 'FAILED')
    return hasFailed ? 'error' : 'process'
  }
  return 'wait'
}

function resetForm() {
  form.serverIds = []
  form.version = ''
  form.serviceName = ''
  form.deployScript = ''
  form.deployMode = 'FULL'
  selectedTemplateId.value = undefined
  deployResults.value = []
  cleanupSubscriptions()
}

async function loadSavedConfig(appId: number) {
  try {
    const res = await getDeployConfig(appId)
    const config = res.data
    if (config) {
      if (config.serverIds) form.serverIds = config.serverIds
      if (config.version) form.version = config.version
      if (config.serviceName) form.serviceName = config.serviceName
      if (config.deployMode) form.deployMode = config.deployMode
      if (config.scriptTemplateId) selectedTemplateId.value = config.scriptTemplateId
      if (config.deployScript) form.deployScript = config.deployScript
    }
  } catch { /* 无已保存配置，忽略 */ }
}

defineExpose({ startTracking, handleDeployError })

onMounted(() => {
  loadTemplates()
  if (props.appId) {
    loadSavedConfig(props.appId)
  }
})

watch(() => props.appId, (newAppId) => {
  resetForm()
  if (newAppId) {
    loadSavedConfig(newAppId)
  }
})

watch(() => props.appType, () => {
  loadTemplates()
})

watch(() => props.prefillScript, (val) => {
  if (val) {
    form.deployScript = val
  }
}, { immediate: true })

onBeforeUnmount(() => {
  cleanupSubscriptions()
})
</script>

<style scoped>
.deploy-guide {
  display: flex;
  justify-content: center;
  padding: 48px 24px;
}

.guide-steps {
  max-width: 360px;
  width: 100%;
}

.deploy-results {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
