<template>
  <div class="script-editor-container">
    <div class="script-editor-lines">
      <div v-for="n in lineCount" :key="n">{{ n }}</div>
    </div>
    <textarea
      class="script-editor-textarea"
      :value="modelValue"
      @input="handleInput"
      @scroll="syncScroll"
      ref="textareaRef"
      spellcheck="false"
    />
    <VariableHint
      v-if="showHints && variables.length > 0"
      :variables="variables"
      :compact="true"
      @insert="handleInsert"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import VariableHint from '@/components/VariableHint.vue'

interface VariableItem {
  name: string
  description: string
  group?: string
}

const props = withDefaults(defineProps<{
  modelValue: string
  variables?: VariableItem[]
  showHints?: boolean
}>(), {
  variables: () => [],
  showHints: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'insertVariable', varName: string): void
}>()

const textareaRef = ref<HTMLTextAreaElement>()

const lineCount = computed(() => {
  const lines = props.modelValue.split('\n').length
  return Math.max(lines, 1)
})

function handleInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}

function syncScroll(e: Event) {
  const textarea = e.target as HTMLTextAreaElement
  const lines = textarea.parentElement?.querySelector('.script-editor-lines') as HTMLElement
  if (lines) {
    lines.scrollTop = textarea.scrollTop
  }
}

function handleInsert(varName: string) {
  const textarea = textareaRef.value
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const before = props.modelValue.substring(0, start)
    const after = props.modelValue.substring(end)
    const newValue = before + varName + after
    emit('update:modelValue', newValue)
    requestAnimationFrame(() => {
      textarea.focus()
      const newPos = start + varName.length
      textarea.setSelectionRange(newPos, newPos)
    })
  } else {
    emit('update:modelValue', props.modelValue + varName)
  }
  emit('insertVariable', varName)
}
</script>
