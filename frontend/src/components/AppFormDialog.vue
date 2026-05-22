<template>
  <el-dialog
    :model-value="visible"
    :title="formData?.id ? '编辑应用' : '新增应用'"
    width="560px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
    @close="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="form.projectId" placeholder="请选择所属项目" style="width: 100%;" filterable>
          <el-option
            v-for="project in projectList"
            :key="project.id"
            :label="project.name"
            :value="project.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入应用名称" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%;">
          <el-option label="前后端" value="FULL_STACK" />
          <el-option label="纯后端" value="BACKEND_ONLY" />
          <el-option label="纯前端" value="FRONTEND_ONLY" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目路径" prop="projectPath">
        <el-input v-model="form.projectPath" placeholder="如 /home/user/my-project 或 D:\project\my-app">
          <template #append>
            <el-tooltip content="DeployHub 服务器上项目根目录的绝对路径，构建命令将在此目录下执行" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="构建命令" prop="buildCommand">
        <el-input v-model="form.buildCommand" placeholder="如: mvn clean package -DskipTests">
          <template #append>
            <el-tooltip content="通用构建命令，在项目路径下执行。如前后端项目可填整体构建命令" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item v-if="form.type === 'FULL_STACK' || form.type === 'FRONTEND_ONLY'" label="前端构建命令" prop="frontendBuildCommand">
        <el-input v-model="form.frontendBuildCommand" placeholder="如: npm run build">
          <template #append>
            <el-tooltip content="前端项目的构建命令，在项目路径下执行，如 npm run build 或 yarn build" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item v-if="form.type === 'FULL_STACK' || form.type === 'BACKEND_ONLY'" label="后端构建命令" prop="backendBuildCommand">
        <el-input v-model="form.backendBuildCommand" placeholder="如: mvn clean package -DskipTests">
          <template #append>
            <el-tooltip content="后端项目的构建命令，如 mvn clean package -DskipTests 或 gradle build" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="产物路径" prop="outputPath">
        <el-input v-model="form.outputPath" placeholder="构建产物相对路径，如: backend/target/app.jar">
          <template #append>
            <el-tooltip content="构建产物相对于项目路径的路径，如 backend/target/app.jar 或 dist/output.zip" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="远程部署目录" prop="remoteDeployPath">
        <el-input v-model="form.remoteDeployPath" placeholder="目标服务器上的部署目录，如 /opt/my-app">
          <template #append>
            <el-tooltip content="应用在目标服务器上的部署目录，脚本生成器将使用此目录生成部署脚本" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="配置文件路径" prop="configFilePath">
        <el-input v-model="form.configFilePath" placeholder="目标服务器上的配置文件路径，如 /opt/my-app/application.yml">
          <template #append>
            <el-tooltip content="目标服务器上的配置文件路径，部署脚本将自动处理配置文件的备份和恢复" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { App, Project } from '@/types'

const props = defineProps<{
  visible: boolean
  formData: Partial<App> | null
  projectList: Project[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'submit', data: Partial<App>): void
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const form = reactive({
  id: undefined as number | undefined,
  projectId: undefined as number | undefined,
  name: '',
  type: 'FULL_STACK' as 'FULL_STACK' | 'BACKEND_ONLY' | 'FRONTEND_ONLY',
  projectPath: '',
  buildCommand: 'mvn clean package -DskipTests',
  frontendBuildCommand: 'npm run build',
  backendBuildCommand: 'mvn clean package -DskipTests',
  outputPath: '',
  remoteDeployPath: '',
  configFilePath: '',
  description: ''
})

const rules: FormRules = {
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  name: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  projectPath: [{ required: true, message: '请输入项目路径', trigger: 'blur' }]
}

function extractAppName(path: string): string {
  return path.split(/[/\\]/).filter(Boolean).pop() || 'app'
}

watch(() => props.formData, (val) => {
  if (val) {
    form.id = val.id
    form.projectId = val.projectId
    form.name = val.name || ''
    form.type = val.type || 'FULL_STACK'
    form.projectPath = val.projectPath || ''
    form.buildCommand = val.buildCommand || 'mvn clean package -DskipTests'
    form.frontendBuildCommand = val.frontendBuildCommand || 'npm run build'
    form.backendBuildCommand = val.backendBuildCommand || 'mvn clean package -DskipTests'
    form.outputPath = val.outputPath || ''
    form.remoteDeployPath = val.remoteDeployPath || ''
    form.configFilePath = val.configFilePath || ''
    form.description = val.description || ''
  }
}, { immediate: true })

watch(() => form.projectPath, (val) => {
  if (val && !form.outputPath) {
    const appName = extractAppName(val)
    if (form.type === 'FRONTEND_ONLY') {
      form.outputPath = 'dist/'
    } else if (form.type === 'BACKEND_ONLY') {
      form.outputPath = `target/${appName}.jar`
    } else {
      form.outputPath = `backend/target/${appName}.jar`
    }
  }
})

watch(() => form.type, () => {
  if (form.projectPath && !form.outputPath) {
    const appName = extractAppName(form.projectPath)
    if (form.type === 'FRONTEND_ONLY') {
      form.outputPath = 'dist/'
    } else if (form.type === 'BACKEND_ONLY') {
      form.outputPath = `target/${appName}.jar`
    } else {
      form.outputPath = `backend/target/${appName}.jar`
    }
  }
})

function resetForm() {
  form.id = undefined
  form.projectId = undefined
  form.name = ''
  form.type = 'FULL_STACK'
  form.projectPath = ''
  form.buildCommand = 'mvn clean package -DskipTests'
  form.frontendBuildCommand = 'npm run build'
  form.backendBuildCommand = 'mvn clean package -DskipTests'
  form.outputPath = ''
  form.remoteDeployPath = ''
  form.configFilePath = ''
  form.description = ''
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    emit('submit', { ...form })
  } finally {
    submitLoading.value = false
  }
}
</script>
