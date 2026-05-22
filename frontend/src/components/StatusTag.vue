<template>
  <el-tag :type="tagType" size="small" effect="dark">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DeployStatus } from '@/types'

const props = defineProps<{
  status: string
}>()

const statusMap: Record<string, { type: 'info' | 'warning' | 'success' | 'danger'; label: string }> = {
  [DeployStatus.PENDING]: { type: 'info', label: '等待中' },
  [DeployStatus.DEPLOYING]: { type: 'warning', label: '部署中' },
  [DeployStatus.SUCCESS]: { type: 'success', label: '成功' },
  [DeployStatus.FAILED]: { type: 'danger', label: '失败' },
  [DeployStatus.ROLLING_BACK]: { type: 'warning', label: '回滚中' },
  [DeployStatus.ROLLED_BACK]: { type: 'info', label: '已回滚' },
  RUNNING: { type: 'warning', label: '执行中' },
  CANCELLED: { type: 'info', label: '已取消' }
}

const tagType = computed(() => statusMap[props.status]?.type || 'info')
const label = computed(() => statusMap[props.status]?.label || props.status)
</script>
