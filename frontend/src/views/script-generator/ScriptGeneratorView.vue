<template>
  <div>
    <div class="page-header">
      <h2>脚本生成器</h2>
    </div>
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600;">配置参数</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <el-form-item label="项目类型" prop="projectType">
              <el-radio-group v-model="form.projectType" @change="handleTypeChange">
                <el-radio value="JAVA">Java 项目</el-radio>
                <el-radio value="FRONTEND">前端项目</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="应用名称" prop="appName">
              <el-input v-model="form.appName" placeholder="如: my-app" />
            </el-form-item>
            <el-form-item label="远程部署目录" prop="remoteDeployPath">
              <el-input v-model="form.remoteDeployPath" placeholder="如: /opt/my-app">
                <template #append>
                  <el-tooltip content="目标服务器上的部署目录" placement="top">
                    <el-icon><QuestionFilled /></el-icon>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
            <template v-if="form.projectType === 'JAVA'">
              <el-form-item label="配置文件路径">
                <el-input v-model="form.configFilePath" placeholder="如: /opt/my-app/application.yml（可选）">
                  <template #append>
                    <el-tooltip content="目标服务器上的配置文件路径，脚本将自动处理配置文件的备份和恢复" placement="top">
                      <el-icon><QuestionFilled /></el-icon>
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="JVM 参数">
                <el-input v-model="form.jvmParams" placeholder="-Xms512m -Xmx2048m" />
              </el-form-item>
              <el-form-item label="服务端口">
                <el-input v-model="form.serverPort" placeholder="如: 8080（可选）" />
              </el-form-item>
            </template>
            <template v-if="form.projectType === 'FRONTEND'">
              <el-form-item label="重载 Nginx">
                <el-switch v-model="form.reloadNginx" />
              </el-form-item>
            </template>
            <el-form-item>
              <el-button type="primary" @click="handleGenerate" :loading="generating">生成脚本</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: 600;">脚本预览</span>
              <div style="display: flex; gap: 8px;">
                <el-button size="small" @click="handleCopy" :disabled="!generatedScript">
                  <el-icon><CopyDocument /></el-icon>复制
                </el-button>
                <el-button size="small" type="primary" @click="handleSaveAsTemplate" :disabled="!generatedScript">
                  保存为模板
                </el-button>
                <el-button size="small" type="success" @click="handleUseForDeploy" :disabled="!generatedScript">
                  用于部署
                </el-button>
              </div>
            </div>
          </template>
          <div v-if="generatedScript">
            <ScriptEditor v-model="generatedScript" :show-hints="false" />
            <VariableHint :variables="scriptVariables" @insert="handleInsertVariable" />
          </div>
          <el-empty v-else description="配置参数后点击「生成脚本」预览" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="saveDialogVisible" title="保存为脚本模板" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="模板名称">
          <el-input v-model="templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="templateDescription" placeholder="请输入描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSaveTemplate" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { QuestionFilled, CopyDocument } from '@element-plus/icons-vue'
import { generateScript } from '@/api/script-generator'
import { createScript } from '@/api/script'
import ScriptEditor from '@/components/ScriptEditor.vue'
import VariableHint from '@/components/VariableHint.vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const generating = ref(false)
const generatedScript = ref('')

const saveDialogVisible = ref(false)
const templateName = ref('')
const templateDescription = ref('')
const saving = ref(false)

const form = reactive({
  projectType: 'JAVA' as 'JAVA' | 'FRONTEND',
  appName: '',
  remoteDeployPath: '',
  configFilePath: '',
  jvmParams: '-Xms512m -Xmx2048m',
  serverPort: '',
  reloadNginx: true
})

const rules: FormRules = {
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  remoteDeployPath: [{ required: true, message: '请输入远程部署目录', trigger: 'blur' }]
}

const scriptVariables = [
  { name: 'APP_NAME', description: '应用名称', group: '应用变量' },
  { name: 'REMOTE_FILE_PATH', description: '远程文件路径', group: '应用变量' },
  { name: 'REMOTE_DEPLOY_PATH', description: '远程部署路径', group: '应用变量' },
  { name: 'SERVER_HOST', description: '服务器地址', group: '服务器变量' },
  { name: 'SERVER_PORT', description: '服务器端口', group: '服务器变量' },
  { name: 'SERVER_USER', description: '服务器用户', group: '服务器变量' },
  { name: 'VERSION', description: '版本号', group: '系统变量' },
  { name: 'TIMESTAMP', description: '时间戳', group: '系统变量' },
  { name: 'DATE', description: '日期', group: '系统变量' }
]

function handleTypeChange() {
  if (form.projectType === 'JAVA') {
    form.jvmParams = '-Xms512m -Xmx2048m'
    form.reloadNginx = true
  } else {
    form.configFilePath = ''
    form.jvmParams = ''
    form.serverPort = ''
  }
}

async function handleGenerate() {
  if (!formRef.value) return
  await formRef.value.validate()
  generating.value = true
  try {
    const res = await generateScript({
      projectType: form.projectType,
      appName: form.appName,
      remoteDeployPath: form.remoteDeployPath,
      configFilePath: form.configFilePath || undefined,
      jvmParams: form.jvmParams || undefined,
      serverPort: form.serverPort || undefined,
      reloadNginx: form.reloadNginx
    })
    generatedScript.value = res.data
    ElMessage.success('脚本生成成功')
  } catch {
    // error handled by interceptor
  } finally {
    generating.value = false
  }
}

function handleCopy() {
  if (!generatedScript.value) return
  navigator.clipboard.writeText(generatedScript.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function handleSaveAsTemplate() {
  if (!generatedScript.value) return
  templateName.value = `${form.appName || '脚本'}部署模板`
  templateDescription.value = `${form.projectType === 'JAVA' ? 'Java' : '前端'}项目部署脚本`
  saveDialogVisible.value = true
}

async function confirmSaveTemplate() {
  if (!templateName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  saving.value = true
  try {
    await createScript({
      name: templateName.value.trim(),
      content: generatedScript.value,
      description: templateDescription.value
    })
    ElMessage.success('保存成功')
    saveDialogVisible.value = false
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

function handleUseForDeploy() {
  if (!generatedScript.value) return
  sessionStorage.setItem('deployScript', generatedScript.value)
  router.push('/deploy')
}

function handleInsertVariable(varName: string) {
  generatedScript.value += varName
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}
</style>
