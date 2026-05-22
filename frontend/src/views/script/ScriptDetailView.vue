<template>
  <div>
    <div class="page-header">
      <div class="page-header-left">
        <el-button text @click="router.push('/script')">
          <el-icon><ArrowLeft /></el-icon>返回列表
        </el-button>
        <h2>{{ isCreate ? '新建脚本' : '编辑脚本' }}</h2>
      </div>
    </div>

    <div v-loading="pageLoading">
      <el-card shadow="never">
        <el-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-width="100px"
          style="max-width: 600px; margin-bottom: 20px;"
        >
          <el-form-item label="脚本名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入脚本名称" />
          </el-form-item>
          <el-form-item label="脚本分类" prop="category">
            <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
              <el-option label="前端脚本" value="FRONTEND" />
              <el-option label="后端脚本" value="BACKEND" />
              <el-option label="通用脚本" value="GENERAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属项目" prop="appId">
            <el-select
              v-model="form.appId"
              placeholder="选择关联项目（可选）"
              clearable
              style="width: 100%;"
            >
              <el-option label="全局脚本" :value="null" />
              <el-option
                v-for="project in projectList"
                :key="project.id"
                :label="project.name"
                :value="project.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="2"
              placeholder="请输入描述"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <span style="font-weight: 600;">脚本内容</span>
        </template>
        <ScriptEditor v-model="form.content" />
      </el-card>

      <div class="action-bar">
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isCreate ? '创建' : '保存' }}
        </el-button>
        <el-popconfirm
          v-if="!isCreate"
          title="确定删除该脚本?"
          @confirm="handleDelete"
        >
          <template #reference>
            <el-button type="danger" :loading="deleting">删除</el-button>
          </template>
        </el-popconfirm>
        <el-button @click="router.push('/script')">取消</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getScriptById, createScript, updateScript, deleteScript } from '@/api/script'
import { getProjectList } from '@/api/project'
import ScriptEditor from '@/components/ScriptEditor.vue'
import type { Project } from '@/types'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const pageLoading = ref(false)
const saving = ref(false)
const deleting = ref(false)

const projectList = ref<Project[]>([])

/** 判断是否为新建模式 */
const isCreate = computed(() => route.params.id === 'new')

/** 表单数据 */
const form = reactive({
  name: '',
  category: 'GENERAL',
  appId: null as number | null,
  description: '',
  content: ''
})

/** 表单校验规则 */
const formRules: FormRules = {
  name: [{ required: true, message: '请输入脚本名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择脚本分类', trigger: 'change' }]
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

/** 加载脚本详情（编辑模式） */
async function loadScriptDetail(id: number) {
  pageLoading.value = true
  try {
    const res = await getScriptById(id)
    const script = res.data
    form.name = script.name
    form.category = script.category || 'GENERAL'
    form.appId = script.appId ?? null
    form.description = script.description
    form.content = script.content
  } catch {
    ElMessage.error('加载脚本失败')
    router.push('/script')
  } finally {
    pageLoading.value = false
  }
}

/** 保存脚本 */
async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isCreate.value) {
      await createScript({ ...form })
      ElMessage.success('创建成功')
    } else {
      const id = Number(route.params.id)
      await updateScript(id, { ...form })
      ElMessage.success('保存成功')
    }
    router.push('/script')
  } catch {
    ElMessage.error(isCreate.value ? '创建失败' : '保存失败')
  } finally {
    saving.value = false
  }
}

/** 删除脚本 */
async function handleDelete() {
  const id = Number(route.params.id)
  if (!id) return

  deleting.value = true
  try {
    await deleteScript(id)
    ElMessage.success('删除成功')
    router.push('/script')
  } catch {
    ElMessage.error('删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(async () => {
  await loadProjects()
  if (!isCreate.value) {
    const id = Number(route.params.id)
    if (id) {
      loadScriptDetail(id)
    } else {
      ElMessage.error('无效的脚本ID')
      router.push('/script')
    }
  }
})
</script>

<style scoped>
.page-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-header-left h2 {
  margin: 0;
}

.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
