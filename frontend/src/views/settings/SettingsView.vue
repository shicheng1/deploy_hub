<template>
  <div class="settings-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <template #header>
            <span>配置分组</span>
          </template>
          <el-menu :default-active="activeGroup" @select="handleGroupSelect">
            <el-menu-item index="">全部</el-menu-item>
            <el-menu-item index="SSH">SSH配置</el-menu-item>
            <el-menu-item index="DEPLOY">部署配置</el-menu-item>
            <el-menu-item index="NOTIFY">通知配置</el-menu-item>
            <el-menu-item index="OTHER">其他配置</el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>配置项</span>
              <el-button type="primary" @click="handleAdd">新增配置</el-button>
            </div>
          </template>
          <el-table :data="configList" border stripe>
            <el-table-column prop="configKey" label="配置键" width="200" />
            <el-table-column prop="configValue" label="配置值" show-overflow-tooltip />
            <el-table-column prop="configGroup" label="分组" width="120" />
            <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="500">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="配置键" required>
          <el-input v-model="formData.configKey" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input v-model="formData.configValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="formData.configGroup">
            <el-option label="SSH配置" value="SSH" />
            <el-option label="部署配置" value="DEPLOY" />
            <el-option label="通知配置" value="NOTIFY" />
            <el-option label="其他配置" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, createConfig, updateConfig, deleteConfig } from '../../api/config'
import type { Config } from '../../api/config'

const activeGroup = ref('')
const configList = ref<Config[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formData = ref<Partial<Config>>({})

const loadConfig = async () => {
  const res = await getConfigList(activeGroup.value || undefined)
  configList.value = res.data || []
}

const handleGroupSelect = (index: string) => {
  activeGroup.value = index
  loadConfig()
}

const handleAdd = () => {
  isEdit.value = false
  formData.value = { configGroup: activeGroup.value || 'OTHER' }
  dialogVisible.value = true
}

const handleEdit = (row: Config) => {
  isEdit.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (isEdit.value) {
    await updateConfig(formData.value.id!, formData.value)
  } else {
    await createConfig(formData.value)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadConfig()
}

const handleDelete = async (row: Config) => {
  await ElMessageBox.confirm('确定删除此配置项？', '提示', { type: 'warning' })
  await deleteConfig(row.id)
  ElMessage.success('删除成功')
  loadConfig()
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.settings-container {
  padding: 20px;
}
</style>
