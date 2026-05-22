import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'server',
        name: 'Server',
        component: () => import('@/views/server/ServerView.vue'),
        meta: { title: '服务器管理' }
      },
      {
        path: 'app',
        name: 'App',
        component: () => import('@/views/app/AppView.vue'),
        meta: { title: '应用管理' }
      },
      {
        path: 'deploy',
        name: 'Deploy',
        component: () => import('@/views/deploy/DeployView.vue'),
        meta: { title: '部署中心' }
      },
      {
        path: 'script',
        name: 'Script',
        component: () => import('@/views/script/ScriptView.vue'),
        meta: { title: '脚本管理' }
      },
      {
        path: 'script/new',
        name: 'ScriptNew',
        component: () => import('@/views/script/ScriptDetailView.vue'),
        meta: { title: '新建脚本' }
      },
      {
        path: 'script/:id',
        name: 'ScriptDetail',
        component: () => import('@/views/script/ScriptDetailView.vue'),
        meta: { title: '脚本详情' }
      },
      {
        path: 'script-generator',
        name: 'ScriptGenerator',
        component: () => import('@/views/script-generator/ScriptGeneratorView.vue'),
        meta: { title: '脚本生成器' }
      },
      {
        path: 'script-execution',
        name: 'ScriptExecution',
        component: () => import('@/views/script-execution/ScriptExecutionView.vue'),
        meta: { title: '脚本执行' }
      },
      {
        path: 'script-execution-history',
        name: 'ScriptExecutionHistory',
        component: () => import('@/views/script-execution-history/ScriptExecutionHistoryView.vue'),
        meta: { title: '执行历史' }
      },
      {
        path: 'file-deploy',
        name: 'FileDeploy',
        component: () => import('@/views/file-deploy/FileDeployView.vue'),
        meta: { title: '文件部署' }
      },
      {
        path: 'nginx',
        name: 'Nginx',
        component: () => import('@/views/nginx/NginxView.vue'),
        meta: { title: 'Nginx管理' }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('@/views/history/HistoryView.vue'),
        meta: { title: '部署历史' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '系统设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
