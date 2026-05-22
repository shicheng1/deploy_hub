<template>
  <div>
    <div class="page-header">
      <h2>脚本管理</h2>
    </div>

    <!-- 顶部筛选栏 -->
    <el-card shadow="never" style="margin-bottom: 20px;">
      <div class="filter-bar">
        <div class="filter-left">
          <el-select
            v-model="selectedAppId"
            placeholder="筛选项目"
            style="width: 200px;"
            @change="loadScripts"
          >
            <el-option label="全部脚本" :value="null" />
            <el-option label="全局脚本" :value="0" />
            <el-option
              v-for="project in projectList"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
          <el-select
            v-model="selectedCategory"
            placeholder="筛选分类"
            clearable
            style="width: 160px; margin-left: 12px;"
            @change="loadScripts"
          >
            <el-option label="全部分类" :value="null" />
            <el-option label="前端脚本" value="FRONTEND" />
            <el-option label="后端脚本" value="BACKEND" />
            <el-option label="通用脚本" value="GENERAL" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" @click="router.push('/script/new')">
            <el-icon><Plus /></el-icon>新增
          </el-button>
          <input
            type="file"
            id="script-upload"
            accept=".sh,.py,.txt,.bat,.cmd,.ps1,.js"
            style="display: none;"
            @change="handleFileUpload"
          />
          <el-button type="success" @click="triggerFileUpload">
            <el-icon><Upload /></el-icon>上传
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 脚本卡片列表 -->
    <div v-loading="loading" class="script-card-grid">
      <el-card
        v-for="script in scriptList"
        :key="script.id"
        class="script-card"
        shadow="hover"
        @click="router.push(`/script/${script.id}`)"
      >
        <div class="script-card-header">
          <span class="script-card-name">{{ script.name }}</span>
          <el-tag :type="categoryTagType(script.category)" size="small">
            {{ categoryLabel(script.category) }}
          </el-tag>
        </div>
        <div class="script-card-meta">
          <el-tag v-if="script.appName" type="info" size="small">{{ script.appName }}</el-tag>
          <el-tag v-else type="warning" size="small">全局</el-tag>
        </div>
        <div class="script-card-desc">{{ script.description || '无描述' }}</div>
      </el-card>
      <el-empty
        v-if="scriptList.length === 0 && !loading"
        description="暂无脚本，点击上方「新增」或「上传」创建脚本"
        :image-size="80"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, Plus } from '@element-plus/icons-vue'
import { getScriptList, uploadScript } from '@/api/script'
import { getProjectList } from '@/api/project'
import type { ScriptTemplate, Project } from '@/types'

const router = useRouter()

const projectList = ref<Project[]>([])
const selectedAppId = ref<number | null>(null)
const selectedCategory = ref<string | null>(null)

const loading = ref(false)
const scriptList = ref<ScriptTemplate[]>([])

/** 分类标签文本 */
function categoryLabel(category: string): string {
  const map: Record<string, string> = {
    FRONTEND: '前端脚本',
    BACKEND: '后端脚本',
    GENERAL: '通用脚本'
  }
  return map[category] || category || '通用脚本'
}

/** 分类标签颜色 */
function categoryTagType(category: string): '' | 'success' | 'info' {
  const map: Record<string, '' | 'success' | 'info'> = {
    FRONTEND: '',
    BACKEND: 'success',
    GENERAL: 'info'
  }
  return map[category] || 'info'
}

/** 加载项目列表 */
async function loadProjects() {
  try {
    const res = await getProjectList({ page: 1, size: 100 })
    projectList.value = res.data.records
  } catch {
    /* ignore */
  }
}

/** 加载脚本列表 */
async function loadScripts() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 100 }
    if (selectedAppId.value !== null) {
      params.appId = selectedAppId.value
    }
    if (selectedCategory.value) {
      params.category = selectedCategory.value
    }
    const res = await getScriptList(params)
    scriptList.value = res.data.records
  } finally {
    loading.value = false
  }
}

/** 触发文件上传 */
function triggerFileUpload() {
  const input = document.getElementById('script-upload') as HTMLInputElement
  input.click()
}

/** 处理文件上传 */
async function handleFileUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  try {
    await uploadScript(file)
    ElMessage.success('上传成功')
    loadScripts()
    input.value = ''
  } catch {
    ElMessage.error('上传失败')
    input.value = ''
  }
}

onMounted(() => {
  loadProjects()
  loadScripts()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.script-card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  min-height: 200px;
}

.script-card {
  width: 320px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.script-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.script-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.script-card-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.script-card-meta {
  margin-bottom: 8px;
}

.script-card-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 768px) {
  .script-card {
    width: 100%;
  }
}
</style>
