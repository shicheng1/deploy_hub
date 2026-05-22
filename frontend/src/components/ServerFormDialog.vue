<template>
  <el-dialog
    :model-value="visible"
    :title="formData?.id ? '编辑服务器' : '新增服务器'"
    width="560px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
    @close="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入服务器名称" />
      </el-form-item>
      <el-form-item label="IP地址" prop="host">
        <el-input v-model="form.host" placeholder="请输入IP地址" />
      </el-form-item>
      <el-form-item label="端口" prop="port">
        <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="认证方式" prop="authType">
        <el-radio-group v-model="form.authType">
          <el-radio value="PASSWORD">密码</el-radio>
          <el-radio value="KEY">密钥</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.authType === 'PASSWORD'" label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item v-if="form.authType === 'KEY'" label="密钥内容" prop="privateKey">
        <el-input v-model="form.privateKey" type="textarea" :rows="4" placeholder="请输入密钥内容" />
      </el-form-item>
      <el-form-item label="分组" prop="groupName">
        <el-input v-model="form.groupName" placeholder="请输入分组名称" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Server } from '@/types'

const props = defineProps<{
  visible: boolean
  formData: Partial<Server> | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'submit', data: Partial<Server>): void
}>()

const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  host: '',
  port: 22,
  username: '',
  authType: 'PASSWORD' as 'PASSWORD' | 'KEY',
  password: '',
  privateKey: '',
  groupName: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

watch(() => props.formData, (val) => {
  if (val) {
    form.id = val.id
    form.name = val.name || ''
    form.host = val.host || ''
    form.port = val.port || 22
    form.username = val.username || ''
    form.authType = val.authType || 'PASSWORD'
    form.groupName = val.groupName || ''
    form.description = val.description || ''
    form.password = ''
    form.privateKey = ''
  }
}, { immediate: true })

function resetForm() {
  form.id = undefined
  form.name = ''
  form.host = ''
  form.port = 22
  form.username = ''
  form.authType = 'PASSWORD'
  form.password = ''
  form.privateKey = ''
  form.groupName = ''
  form.description = ''
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  const data: Partial<Server> & { password?: string; privateKey?: string } = {
    id: form.id,
    name: form.name,
    host: form.host,
    port: form.port,
    username: form.username,
    authType: form.authType,
    groupName: form.groupName,
    description: form.description
  }
  if (form.authType === 'PASSWORD') {
    data.password = form.password
  } else {
    data.privateKey = form.privateKey
  }
  emit('submit', data)
}
</script>
