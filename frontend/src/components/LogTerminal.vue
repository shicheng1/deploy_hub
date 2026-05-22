<template>
  <div class="terminal-container" ref="terminalContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'
import wsClient from '@/utils/websocket'
import type { ApiResult } from '@/types'

const props = withDefaults(defineProps<{
  recordId: number
  topic?: string
  fallbackLogApi?: (id: number) => Promise<ApiResult<string>>
}>(), {
  topic: '/topic/deploy'
})

const emit = defineEmits<{
  (e: 'status-change', status: string): void
}>()

const terminalContainer = ref<HTMLDivElement>()
let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null
let subscription: any = null

function initTerminal() {
  if (!terminalContainer.value) return
  terminal = new Terminal({
    theme: {
      background: '#1e1e1e',
      foreground: '#d4d4d4',
      cursor: '#d4d4d4'
    },
    fontSize: 13,
    fontFamily: "'Courier New', Courier, monospace",
    cursorBlink: true,
    disableStdin: true,
    convertEol: true
  })
  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)
  terminal.open(terminalContainer.value)
  fitAddon.fit()
  terminal.writeln('Waiting for deploy log...')
}

async function subscribeLog() {
  if (!props.recordId || !terminal) return
  try {
    if (!wsClient.isConnected()) {
      await wsClient.connect()
    }
    subscription = wsClient.subscribe(`${props.topic}/${props.recordId}`, (data: any) => {
      if (!terminal) return
      if (typeof data === 'object' && data !== null && data.status) {
        emit('status-change', data.status)
        const statusText = data.error ? `[Status] ${data.status}: ${data.error}` : `[Status] ${data.status}`
        terminal.writeln(`\x1b[33m${statusText}\x1b[0m`)
      } else {
        const message = typeof data === 'string' ? data : data.message || data.log || data.line || JSON.stringify(data)
        terminal.writeln(message)
      }
      terminal.scrollToBottom()
    })
    // 订阅成功后，也通过 HTTP 获取历史日志，避免错过已推送的消息
    loadLogFallback()
  } catch {
    if (terminal) {
      terminal.writeln('\x1b[33mWebSocket connection failed, falling back to HTTP log...\x1b[0m')
    }
    loadLogFallback()
  }
}

async function loadLogFallback() {
  if (!props.recordId || !terminal) return
  try {
    if (props.fallbackLogApi) {
      const res = await props.fallbackLogApi(props.recordId)
      if (terminal && res.data) {
        // 只在有历史日志时才替换内容
        const logContent = res.data.trim()
        if (logContent) {
          terminal.clear()
          terminal.writeln(logContent)
        }
      }
    } else {
      const { getDeployLog } = await import('@/api/deploy')
      const res = await getDeployLog(props.recordId)
      if (terminal && res.data) {
        const logContent = res.data.trim()
        if (logContent) {
          terminal.clear()
          terminal.writeln(logContent)
        }
      }
    }
  } catch {
    // fallback 失败不显示错误，因为可能只是还没有日志
  }
}

watch(() => props.recordId, () => {
  if (terminal) {
    terminal.clear()
    terminal.writeln('Waiting for deploy log...')
  }
  if (subscription) {
    subscription.unsubscribe()
    subscription = null
  }
  subscribeLog()
})

onMounted(() => {
  initTerminal()
  subscribeLog()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (subscription) {
    subscription.unsubscribe()
  }
  if (terminal) {
    terminal.dispose()
    terminal = null
  }
})

function handleResize() {
  if (fitAddon) {
    fitAddon.fit()
  }
}
</script>

<style scoped>
.terminal-container {
  width: 100%;
  height: 400px;
  background: #1e1e1e;
  border-radius: 6px;
  padding: 8px;
  box-sizing: border-box;
}
</style>
