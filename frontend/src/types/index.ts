export enum DeployStatus {
  PENDING = 'PENDING',
  DEPLOYING = 'DEPLOYING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  ROLLING_BACK = 'ROLLING_BACK',
  ROLLED_BACK = 'ROLLED_BACK'
}

export interface Server {
  id: number
  name: string
  host: string
  port: number
  username: string
  authType: 'PASSWORD' | 'KEY'
  status: 'ONLINE' | 'OFFLINE'
  groupName: string
  description: string
  createTime: string
  updateTime: string
}

export interface App {
  id: number
  name: string
  projectId: number
  projectName: string
  type: 'FULL_STACK' | 'BACKEND_ONLY' | 'FRONTEND_ONLY'
  projectPath: string
  buildCommand: string
  frontendBuildCommand: string
  backendBuildCommand: string
  outputPath: string
  remoteDeployPath: string
  configFilePath: string
  description: string
  createTime: string
  updateTime: string
}

export interface DeployRecord {
  id: number
  appId: number
  serverId: number
  appName: string
  serverName: string
  status: DeployStatus
  version: string
  deployScript: string
  deployLog: string
  errorMessage: string
  startedAt: string
  finishedAt: string
  operator: string
  createTime: string
  updateTime: string
}

export interface ScriptTemplate {
  id: number
  name: string
  appId: number | null
  category: string
  appName: string
  content: string
  description: string
  createTime: string
  updateTime: string
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface DeployRequest {
  appId: number
  serverIds: number[]
  version?: string
  deployScript?: string
  operator?: string
  deployMode?: string
}

export interface PageParams {
  page: number
  size: number
  [key: string]: any
}

export interface BuildResult {
  success: boolean
  output: string
  message: string
}

export interface DashboardStats {
  totalServers: number
  totalApps: number
  todayDeploys: number
  successRate: number
}

export interface Project {
  id: number
  name: string
  description: string
  appCount: number
  createTime: string
  updateTime: string
}

export interface DeployConfig {
  id: number
  appId: number
  serverIds: number[]
  version: string
  serviceName: string
  deployMode: string
  scriptTemplateId: number
  deployScript: string
  createTime: string
  updateTime: string
}

export interface BatchDeployRequest {
  projectId: number
  serverIds: number[]
  version?: string
  deployMode?: string
}

export interface NginxConfig {
  id: number
  serverId: number
  serverName: string
  serverHost: string
  name: string
  configPath: string
  configContent: string
  status: 'DRAFT' | 'DEPLOYED' | 'ERROR'
  description: string
  createTime: string
  updateTime: string
}

export interface NginxTemplate {
  id: number
  name: string
  description: string
  category: string
  configContent: string
  sourceServerIds: string
  createTime: string
  updateTime: string
}

export interface NginxPackage {
  id: number
  name: string
  version: string
  filePath: string
  fileSize: number
  description: string
  createTime: string
  updateTime: string
}

export interface ServiceStatus {
  name: string
  address: string
  status: 'ONLINE' | 'OFFLINE'
  responseTime: number
  type: 'UPSTREAM' | 'SERVER_BLOCK'
}

export interface DeployStepResult {
  success: boolean
  currentStep: string
  message: string
  steps: {
    stepName: string
    success: boolean
    message: string
    duration: number
  }[]
}

export interface NginxConfigFileInfo {
  fileName: string
  filePath: string
  content: string
}

export interface NginxServerConfig {
  installed: boolean
  version: string
  configFilePath: string
  mainConfigContent: string
  confFiles: NginxConfigFileInfo[]
}