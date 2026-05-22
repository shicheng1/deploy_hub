import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class WebSocketClient {
  private stompClient: Client | null = null
  private connected = false

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.stompClient = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          this.connected = true
          resolve()
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame.headers['message'])
          reject(new Error(frame.headers['message']))
        },
        onDisconnect: () => {
          this.connected = false
        }
      })
      this.stompClient.activate()
    })
  }

  subscribe(destination: string, callback: (message: any) => void) {
    if (!this.stompClient || !this.connected) {
      console.warn('WebSocket未连接，无法订阅:', destination)
      return null
    }
    return this.stompClient.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch {
        callback(message.body)
      }
    })
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate()
      this.stompClient = null
      this.connected = false
    }
  }

  isConnected() {
    return this.connected
  }
}

const wsClient = new WebSocketClient()

export default wsClient
