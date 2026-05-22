<template>
  <div class="variable-hint">
    <template v-if="!compact">
      <div v-for="group in groupedVariables" :key="group.label" class="variable-group">
        <span class="group-label">{{ group.label }}</span>
        <div class="group-tags">
          <el-tooltip
            v-for="v in group.items"
            :key="v.name"
            :content="v.description"
            placement="top"
          >
            <el-tag
              size="small"
              type="info"
              class="variable-tag"
              @click="emit('insert', `\${${v.name}}`)"
            >
              \${{ v.name }}
            </el-tag>
          </el-tooltip>
        </div>
      </div>
    </template>
    <template v-else>
      <div class="group-tags compact-tags">
        <el-tooltip
          v-for="v in variables"
          :key="v.name"
          :content="v.description"
          placement="top"
        >
          <el-tag
            size="small"
            type="info"
            class="variable-tag"
            @click="emit('insert', `\${${v.name}}`)"
          >
            \${{ v.name }}
          </el-tag>
        </el-tooltip>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface VariableItem {
  name: string
  description: string
  group?: string
}

const props = withDefaults(defineProps<{
  variables: VariableItem[]
  compact?: boolean
}>(), {
  compact: false
})

const emit = defineEmits<{
  (e: 'insert', variableName: string): void
}>()

const groupedVariables = computed(() => {
  const map = new Map<string, VariableItem[]>()
  props.variables.forEach(v => {
    const key = v.group || '其他'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(v)
  })
  return Array.from(map.entries()).map(([label, items]) => ({ label, items }))
})
</script>

<style scoped>
.variable-hint {
  padding: 8px 0;
}

.variable-group {
  margin-bottom: 8px;
}

.group-label {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
  white-space: nowrap;
}

.group-tags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
}

.compact-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.variable-tag {
  cursor: pointer;
  font-family: 'Courier New', Courier, monospace;
  transition: all 0.2s;
}

.variable-tag:hover {
  color: #409EFF;
  border-color: #b3d8ff;
  background-color: #ecf5ff;
}
</style>
