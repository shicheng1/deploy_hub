import request from './index'
import type { NginxConfig, NginxTemplate, NginxPackage, ServiceStatus, DeployStepResult, NginxServerConfig, PageResult, ApiResult } from '@/types'

// Nginx Config
export function getNginxConfigList(params: { page: number; size: number; serverId?: number; name?: string }) {
  return request.get<any, ApiResult<PageResult<NginxConfig>>>('/nginx-config/list', {
    params: { pageNum: params.page, pageSize: params.size, serverId: params.serverId, name: params.name }
  })
}

export function getNginxConfigById(id: number) {
  return request.get<any, ApiResult<NginxConfig>>(`/nginx-config/${id}`)
}

export function createNginxConfig(data: Partial<NginxConfig>) {
  return request.post<any, ApiResult<void>>('/nginx-config', data)
}

export function updateNginxConfig(id: number, data: Partial<NginxConfig>) {
  return request.put<any, ApiResult<void>>(`/nginx-config/${id}`, data)
}

export function deleteNginxConfig(id: number) {
  return request.delete<any, ApiResult<void>>(`/nginx-config/${id}`)
}

export function testNginxConfig(id: number) {
  return request.post<any, ApiResult<string>>(`/nginx-config/${id}/test`)
}

export function deployNginxConfig(id: number) {
  return request.post<any, ApiResult<void>>(`/nginx-config/${id}/deploy`)
}

export function saveAsTemplate(id: number, templateName: string, templateDesc: string) {
  return request.post<any, ApiResult<NginxTemplate>>(`/nginx-config/${id}/save-as-template`, null, { params: { templateName, templateDesc } })
}

export function getServiceStatus(id: number) {
  return request.get<any, ApiResult<ServiceStatus[]>>(`/nginx-config/${id}/service-status`)
}

export function oneClickDeploy(data: { templateId: number; serverId: number; packageId?: number; configName: string; configPath?: string }) {
  return request.post<any, ApiResult<DeployStepResult>>('/nginx-config/one-click-deploy', data)
}

export function getServerNginxConfig(serverId: number) {
  return request.get<any, ApiResult<NginxServerConfig>>('/nginx-config/server-config', { params: { serverId } })
}

// Nginx Template
export function getNginxTemplateList(params: { page: number; size: number; category?: string; name?: string }) {
  return request.get<any, ApiResult<PageResult<NginxTemplate>>>('/nginx-template/list', {
    params: { pageNum: params.page, pageSize: params.size, category: params.category, name: params.name }
  })
}

export function getNginxTemplateById(id: number) {
  return request.get<any, ApiResult<NginxTemplate>>(`/nginx-template/${id}`)
}

export function createNginxTemplate(data: Partial<NginxTemplate>) {
  return request.post<any, ApiResult<void>>('/nginx-template', data)
}

export function updateNginxTemplate(id: number, data: Partial<NginxTemplate>) {
  return request.put<any, ApiResult<void>>(`/nginx-template/${id}`, data)
}

export function deleteNginxTemplate(id: number) {
  return request.delete<any, ApiResult<void>>(`/nginx-template/${id}`)
}

export function createConfigFromTemplate(templateId: number, serverId: number, configName: string) {
  return request.post<any, ApiResult<NginxConfig>>(`/nginx-template/${templateId}/create-config`, null, {
    params: { serverId, configName }
  })
}

// Nginx Package
export function getNginxPackageList(params: { page: number; size: number; name?: string }) {
  return request.get<any, ApiResult<PageResult<NginxPackage>>>('/nginx-package/list', {
    params: { pageNum: params.page, pageSize: params.size, name: params.name }
  })
}

export function getNginxPackageById(id: number) {
  return request.get<any, ApiResult<NginxPackage>>(`/nginx-package/${id}`)
}

export function uploadNginxPackage(formData: FormData) {
  return request.post<any, ApiResult<void>>('/nginx-package/upload', formData)
}

export function deleteNginxPackage(id: number) {
  return request.delete<any, ApiResult<void>>(`/nginx-package/${id}`)
}

export function installNginxPackage(packageId: number, serverId: number) {
  return request.post<any, ApiResult<string>>(`/nginx-package/${packageId}/install`, null, {
    params: { serverId }
  })
}
