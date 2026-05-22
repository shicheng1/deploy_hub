<template>
  <div>
    <div class="page-header">
      <h2>Nginx管理</h2>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: Nginx配置 -->
      <el-tab-pane label="Nginx配置" name="config">
        <div class="filter-container">
          <el-select v-model="configFilterServerId" placeholder="按服务器筛选" clearable style="width: 200px;" @change="loadConfigs">
            <el-option v-for="s in serverList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-button type="primary" @click="openConfigDialog()">
            <el-icon><Plus /></el-icon>新增配置
          </el-button>
        </div>

        <el-table :data="configList" stripe v-loading="configLoading" style="width: 100%">
          <template #empty>
            <el-empty description="暂无Nginx配置" :image-size="80" />
          </template>
          <el-table-column prop="name" label="配置名称" min-width="120" />
          <el-table-column prop="serverName" label="服务器" min-width="120" />
          <el-table-column prop="serverHost" label="服务器地址" min-width="140" />
          <el-table-column prop="configPath" label="配置路径" min-width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openConfigDialog(row)">编辑</el-button>
              <el-button type="warning" link size="small" @click="handleTestConfig(row.id)">语法检测</el-button>
              <el-button type="success" link size="small" @click="handleDeployConfig(row)">部署</el-button>
              <el-button type="info" link size="small" @click="handleServiceStatus(row.id)">服务状态</el-button>
              <el-button type="primary" link size="small" @click="openSaveAsTemplateDialog(row)">保存为模板</el-button>
              <el-popconfirm title="确定删除该配置?" @confirm="handleDeleteConfig(row.id)">
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            v-model:current-page="configPage"
            v-model:page-size="configSize"
            :total="configTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadConfigs"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: 配置模板 -->
      <el-tab-pane label="配置模板" name="template">
        <div class="filter-container">
          <el-input v-model="templateCategory" placeholder="按分类筛选" clearable style="width: 200px;" @clear="loadTemplates" @keyup.enter="loadTemplates" />
          <el-button type="primary" @click="loadTemplates">搜索</el-button>
          <el-button type="primary" @click="openTemplateDialog()">
            <el-icon><Plus /></el-icon>新增模板
          </el-button>
        </div>

        <el-table :data="templateList" stripe v-loading="templateLoading" style="width: 100%">
          <template #empty>
            <el-empty description="暂无配置模板" :image-size="80" />
          </template>
          <el-table-column prop="name" label="模板名称" min-width="140" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openTemplateDialog(row)">编辑</el-button>
              <el-button type="success" link size="small" @click="openCreateConfigFromTemplateDialog(row)">创建配置</el-button>
              <el-popconfirm title="确定删除该模板?" @confirm="handleDeleteTemplate(row.id)">
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            v-model:current-page="templatePage"
            v-model:page-size="templateSize"
            :total="templateTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadTemplates"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 3: 安装包管理 -->
      <el-tab-pane label="安装包管理" name="package">
        <div class="filter-container">
          <el-button type="primary" @click="openUploadPackageDialog">
            <el-icon><Upload /></el-icon>上传安装包
          </el-button>
        </div>

        <el-table :data="packageList" stripe v-loading="packageLoading" style="width: 100%">
          <template #empty>
            <el-empty description="暂无安装包" :image-size="80" />
          </template>
          <el-table-column prop="name" label="包名称" min-width="140" />
          <el-table-column prop="version" label="版本" width="120" />
          <el-table-column prop="fileSize" label="文件大小" width="120">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="上传时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="success" link size="small" @click="openInstallPackageDialog(row)">安装到服务器</el-button>
              <el-popconfirm title="确定删除该安装包?" @confirm="handleDeletePackage(row.id)">
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            v-model:current-page="packagePage"
            v-model:page-size="packageSize"
            :total="packageTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadPackages"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 4: 一键部署 -->
      <el-tab-pane label="一键部署" name="deploy">
        <el-card shadow="never" style="max-width: 800px;">
          <el-steps :active="deployStep" finish-status="success" align-center style="margin-bottom: 30px;">
            <el-step title="选择模板" />
            <el-step title="选择服务器" />
            <el-step title="选择安装包" />
            <el-step title="配置信息" />
          </el-steps>

          <el-form :model="deployForm" label-width="120px">
            <!-- Step 1 -->
            <div v-show="deployStep === 0">
              <el-form-item label="配置模板">
                <el-select v-model="deployForm.templateId" placeholder="请选择配置模板" style="width: 100%;" filterable>
                  <el-option v-for="t in allTemplates" :key="t.id" :label="t.name" :value="t.id">
                    <span>{{ t.name }}</span>
                    <span style="float: right; color: #8492a6; font-size: 12px;">{{ t.category }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <div style="text-align: center;">
                <el-button type="primary" :disabled="!deployForm.templateId" @click="deployStep = 1">下一步</el-button>
              </div>
            </div>

            <!-- Step 2 -->
            <div v-show="deployStep === 1">
              <el-form-item label="目标服务器">
                <el-select v-model="deployForm.serverId" placeholder="请选择目标服务器" style="width: 100%;" filterable>
                  <el-option v-for="s in serverList" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
                </el-select>
              </el-form-item>
              <div style="text-align: center;">
                <el-button @click="deployStep = 0">上一步</el-button>
                <el-button type="primary" :disabled="!deployForm.serverId" @click="deployStep = 2">下一步</el-button>
              </div>
            </div>

            <!-- Step 3 -->
            <div v-show="deployStep === 2">
              <el-form-item label="安装包">
                <el-select v-model="deployForm.packageId" placeholder="可选，选择Nginx安装包" style="width: 100%;" clearable filterable>
                  <el-option v-for="p in allPackages" :key="p.id" :label="`${p.name} (${p.version})`" :value="p.id" />
                </el-select>
              </el-form-item>
              <div style="text-align: center;">
                <el-button @click="deployStep = 1">上一步</el-button>
                <el-button type="primary" @click="deployStep = 3">下一步</el-button>
              </div>
            </div>

            <!-- Step 4 -->
            <div v-show="deployStep === 3">
              <el-form-item label="配置名称">
                <el-input v-model="deployForm.configName" placeholder="请输入配置名称" />
              </el-form-item>
              <el-form-item label="配置路径">
                <el-input v-model="deployForm.configPath" placeholder="如 /usr/local/nginx/conf/nginx.conf 或 /etc/nginx/conf.d/app.conf" />
              </el-form-item>
              <div style="text-align: center;">
                <el-button @click="deployStep = 2">上一步</el-button>
                <el-button type="primary" :loading="deploying" :disabled="!deployForm.configName || !deployForm.configPath" @click="handleOneClickDeploy">
                  执行部署
                </el-button>
              </div>
            </div>
          </el-form>

          <!-- Deploy Result -->
          <div v-if="deployResult" style="margin-top: 24px;">
            <el-divider>部署结果</el-divider>
            <el-result :icon="deployResult.success ? 'success' : 'error'" :title="deployResult.success ? '部署成功' : '部署失败'" :sub-title="deployResult.message">
              <template #extra>
                <el-steps :active="deployResult.steps.length" finish-status="success" direction="vertical" style="text-align: left;">
                  <el-step
                    v-for="(step, idx) in deployResult.steps"
                    :key="idx"
                    :title="step.stepName"
                    :description="`${step.success ? '成功' : '失败'} - ${step.message} (${step.duration}ms)`"
                    :status="step.success ? 'success' : 'error'"
                  />
                </el-steps>
              </template>
            </el-result>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 5: 服务器配置 -->
      <el-tab-pane label="服务器配置" name="server-config">
        <div class="filter-container">
          <el-select v-model="serverConfigServerId" placeholder="请选择服务器" style="width: 250px;" filterable>
            <el-option v-for="s in serverList" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
          <el-button type="primary" :loading="serverConfigLoading" :disabled="!serverConfigServerId" @click="handleLoadServerConfig">
            查看配置
          </el-button>
        </div>

        <div v-if="serverConfigData" style="margin-top: 16px;">
          <!-- Nginx 安装状态 -->
          <el-descriptions :column="3" border style="margin-bottom: 20px;">
            <el-descriptions-item label="安装状态">
              <el-tag :type="serverConfigData.installed ? 'success' : 'danger'" size="small">
                {{ serverConfigData.installed ? '已安装' : '未安装' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="版本号">
              {{ serverConfigData.version || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="主配置路径">
              {{ serverConfigData.configFilePath || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <template v-if="serverConfigData.installed">
            <!-- 主配置文件 -->
            <el-collapse v-model="serverConfigExpanded">
              <el-collapse-item title="主配置文件" :name="serverConfigData.configFilePath">
                <template #title>
                  <span style="font-weight: 600;">主配置文件</span>
                  <span style="margin-left: 8px; color: #909399; font-size: 12px;">{{ serverConfigData.configFilePath }}</span>
                </template>
                <div class="config-code-block">
                  <pre>{{ serverConfigData.mainConfigContent || '# 文件为空或无法读取' }}</pre>
                </div>
              </el-collapse-item>

              <!-- conf.d 配置文件 -->
              <el-collapse-item v-for="file in serverConfigData.confFiles" :key="file.filePath" :name="file.filePath">
                <template #title>
                  <span style="font-weight: 600;">{{ file.fileName }}</span>
                  <span style="margin-left: 8px; color: #909399; font-size: 12px;">{{ file.filePath }}</span>
                </template>
                <div class="config-code-block">
                  <pre>{{ file.content || '# 文件为空或无法读取' }}</pre>
                </div>
              </el-collapse-item>
            </el-collapse>

            <el-empty v-if="!serverConfigData.mainConfigContent && (!serverConfigData.confFiles || serverConfigData.confFiles.length === 0)"
              description="未读取到配置文件内容" :image-size="60" />
          </template>
        </div>

        <el-empty v-else-if="!serverConfigLoading" description="请选择服务器并点击查看配置" :image-size="80" />
      </el-tab-pane>
    </el-tabs>

    <!-- Config Dialog -->
    <el-dialog v-model="configDialogVisible" :title="currentConfig?.id ? '编辑配置' : '新增配置'" width="700px" destroy-on-close>
      <el-form :model="configForm" label-width="100px" :rules="configRules" ref="configFormRef">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="configForm.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="目标服务器" prop="serverId">
          <el-select v-model="configForm.serverId" placeholder="请选择服务器" style="width: 100%;" filterable :disabled="!!currentConfig?.id">
            <el-option v-for="s in serverList" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置路径" prop="configPath">
          <el-input v-model="configForm.configPath" placeholder="如 /etc/nginx/conf.d/app.conf" />
        </el-form-item>
        <el-form-item label="配置内容" prop="configContent">
          <el-input v-model="configForm.configContent" type="textarea" :rows="12" placeholder="请输入Nginx配置内容" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="configForm.description" type="textarea" :rows="2" placeholder="配置描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSubmitting" @click="handleConfigSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Save As Template Dialog -->
    <el-dialog v-model="saveAsTemplateVisible" title="保存为模板" width="500px" destroy-on-close>
      <el-form :model="saveAsTemplateForm" label-width="100px" :rules="saveAsTemplateRules" ref="saveAsTemplateFormRef">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="saveAsTemplateForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板描述" prop="templateDesc">
          <el-input v-model="saveAsTemplateForm.templateDesc" type="textarea" :rows="3" placeholder="请输入模板描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveAsTemplateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveAsTemplateSubmitting" @click="handleSaveAsTemplate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Service Status Dialog -->
    <el-dialog v-model="serviceStatusVisible" title="服务状态" width="600px" destroy-on-close>
      <div v-loading="serviceStatusLoading">
        <el-table :data="serviceStatusList" stripe style="width: 100%">
          <template #empty>
            <el-empty description="暂无服务状态数据" :image-size="60" />
          </template>
          <el-table-column prop="name" label="服务名称" min-width="140" />
          <el-table-column prop="address" label="地址" min-width="160" />
          <el-table-column prop="type" label="类型" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'UPSTREAM' ? '' : 'warning'">
                {{ row.type === 'UPSTREAM' ? 'Upstream' : 'Server Block' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small">
                {{ row.status === 'ONLINE' ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responseTime" label="响应时间" width="110">
            <template #default="{ row }">{{ row.responseTime }}ms</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- Template Dialog -->
    <el-dialog v-model="templateDialogVisible" :title="currentTemplate?.id ? '编辑模板' : '新增模板'" width="700px" destroy-on-close>
      <el-form :model="templateForm" label-width="100px" :rules="templateRules" ref="templateFormRef">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="templateForm.category" placeholder="如：负载均衡、反向代理、静态资源" />
        </el-form-item>
        <el-form-item label="配置内容" prop="configContent">
          <el-input v-model="templateForm.configContent" type="textarea" :rows="12" placeholder="请输入Nginx配置模板内容" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="2" placeholder="模板描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateSubmitting" @click="handleTemplateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Create Config From Template Dialog -->
    <el-dialog v-model="createConfigFromTemplateVisible" title="从模板创建配置" width="500px" destroy-on-close>
      <el-form :model="createConfigForm" label-width="100px" :rules="createConfigRules" ref="createConfigFormRef">
        <el-form-item label="模板">
          <el-input :model-value="createConfigForm.templateName" disabled />
        </el-form-item>
        <el-form-item label="目标服务器" prop="serverId">
          <el-select v-model="createConfigForm.serverId" placeholder="请选择服务器" style="width: 100%;" filterable>
            <el-option v-for="s in serverList" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="createConfigForm.configName" placeholder="请输入配置名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createConfigFromTemplateVisible = false">取消</el-button>
        <el-button type="primary" :loading="createConfigSubmitting" @click="handleCreateConfigFromTemplate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Install Package Dialog -->
    <el-dialog v-model="installPackageVisible" title="安装到服务器" width="500px" destroy-on-close>
      <el-form :model="installPackageForm" label-width="100px" :rules="installPackageRules" ref="installPackageFormRef">
        <el-form-item label="安装包">
          <el-input :model-value="installPackageForm.packageName" disabled />
        </el-form-item>
        <el-form-item label="目标服务器" prop="serverId">
          <el-select v-model="installPackageForm.serverId" placeholder="请选择服务器" style="width: 100%;" filterable>
            <el-option v-for="s in serverList" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="installPackageVisible = false">取消</el-button>
        <el-button type="primary" :loading="installPackageSubmitting" @click="handleInstallPackage">安装</el-button>
      </template>
    </el-dialog>

    <!-- Upload Package Dialog -->
    <el-dialog v-model="uploadPackageVisible" title="上传Nginx安装包" width="500px" destroy-on-close>
      <el-form :model="uploadPackageForm" label-width="100px" :rules="uploadPackageRules" ref="uploadPackageFormRef">
        <el-form-item label="安装包" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".tar.gz,.tar,.zip,.rpm,.deb"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 .tar.gz, .tar, .zip, .rpm, .deb 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="uploadPackageForm.name" placeholder="请输入安装包名称" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="uploadPackageForm.version" placeholder="如：1.24.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadPackageForm.description" type="textarea" :rows="2" placeholder="描述信息（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadPackageVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadPackageSubmitting" :disabled="!uploadPackageForm.file" @click="handleUploadPackage">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { getServerList } from '@/api/server'
import {
  getNginxConfigList, createNginxConfig, updateNginxConfig, deleteNginxConfig,
  testNginxConfig, deployNginxConfig, saveAsTemplate, getServiceStatus, oneClickDeploy,
  getNginxTemplateList, createNginxTemplate, updateNginxTemplate, deleteNginxTemplate,
  createConfigFromTemplate,
  getNginxPackageList, uploadNginxPackage, deleteNginxPackage, installNginxPackage,
  getServerNginxConfig
} from '@/api/nginx'
import type { NginxConfig, NginxTemplate, NginxPackage, ServiceStatus, DeployStepResult, NginxServerConfig, Server } from '@/types'
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'

// ==================== 公共数据 ====================
const serverList = ref<Server[]>([])

async function loadServers() {
  try {
    const res = await getServerList({ page: 1, size: 100 })
    serverList.value = res.data.records
  } catch {
    // error handled by interceptor
  }
}

// ==================== Tab 控制 ====================
const activeTab = ref('config')

// ==================== Tab 1: Nginx配置 ====================
const configLoading = ref(false)
const configList = ref<NginxConfig[]>([])
const configTotal = ref(0)
const configPage = ref(1)
const configSize = ref(10)
const configFilterServerId = ref<number | undefined>()

const configDialogVisible = ref(false)
const currentConfig = ref<Partial<NginxConfig> | null>(null)
const configSubmitting = ref(false)
const configFormRef = ref<FormInstance>()
const configForm = ref<Partial<NginxConfig>>({
  name: '', serverId: undefined, configPath: '', configContent: '', description: ''
})
const configRules: FormRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  serverId: [{ required: true, message: '请选择服务器', trigger: 'change' }],
  configPath: [{ required: true, message: '请输入配置路径', trigger: 'blur' }],
  configContent: [{ required: true, message: '请输入配置内容', trigger: 'blur' }]
}

async function loadConfigs() {
  configLoading.value = true
  try {
    const res = await getNginxConfigList({
      page: configPage.value,
      size: configSize.value,
      serverId: configFilterServerId.value
    })
    configList.value = res.data.records
    configTotal.value = res.data.total
  } finally {
    configLoading.value = false
  }
}

function openConfigDialog(row?: NginxConfig) {
  if (row) {
    currentConfig.value = { ...row }
    configForm.value = { ...row }
  } else {
    currentConfig.value = null
    configForm.value = { name: '', serverId: undefined, configPath: '', configContent: '', description: '' }
  }
  configDialogVisible.value = true
}

async function handleConfigSubmit() {
  const valid = await configFormRef.value?.validate().catch(() => false)
  if (!valid) return

  configSubmitting.value = true
  try {
    if (currentConfig.value?.id) {
      await updateNginxConfig(currentConfig.value.id, configForm.value)
      ElMessage.success('更新成功')
    } else {
      await createNginxConfig(configForm.value)
      ElMessage.success('创建成功')
    }
    configDialogVisible.value = false
    loadConfigs()
  } finally {
    configSubmitting.value = false
  }
}

async function handleDeleteConfig(id: number) {
  try {
    await deleteNginxConfig(id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch {
    // error handled by interceptor
  }
}

async function handleTestConfig(id: number) {
  try {
    const res = await testNginxConfig(id)
    ElMessageBox.alert(res.data || '语法检测通过', '语法检测结果', { type: 'success' })
  } catch (e: any) {
    ElMessageBox.alert(e.message || '语法检测失败', '语法检测结果', { type: 'error' })
  }
}

async function handleDeployConfig(row: NginxConfig) {
  try {
    await ElMessageBox.confirm(
      `确定要部署配置「${row.name}」到服务器 ${row.serverName} 吗？`,
      '确认部署',
      { type: 'warning' }
    )
    await deployNginxConfig(row.id)
    ElMessage.success('部署成功')
    loadConfigs()
  } catch {
    // cancelled or error handled by interceptor
  }
}

// 保存为模板
const saveAsTemplateVisible = ref(false)
const saveAsTemplateSubmitting = ref(false)
const saveAsTemplateFormRef = ref<FormInstance>()
const saveAsTemplateConfigId = ref(0)
const saveAsTemplateForm = ref({ templateName: '', templateDesc: '' })
const saveAsTemplateRules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

function openSaveAsTemplateDialog(row: NginxConfig) {
  saveAsTemplateConfigId.value = row.id
  saveAsTemplateForm.value = { templateName: row.name, templateDesc: '' }
  saveAsTemplateVisible.value = true
}

async function handleSaveAsTemplate() {
  const valid = await saveAsTemplateFormRef.value?.validate().catch(() => false)
  if (!valid) return

  saveAsTemplateSubmitting.value = true
  try {
    await saveAsTemplate(
      saveAsTemplateConfigId.value,
      saveAsTemplateForm.value.templateName,
      saveAsTemplateForm.value.templateDesc
    )
    ElMessage.success('保存为模板成功')
    saveAsTemplateVisible.value = false
    loadTemplates()
  } finally {
    saveAsTemplateSubmitting.value = false
  }
}

// 服务状态
const serviceStatusVisible = ref(false)
const serviceStatusLoading = ref(false)
const serviceStatusList = ref<ServiceStatus[]>([])

async function handleServiceStatus(id: number) {
  serviceStatusVisible.value = true
  serviceStatusLoading.value = true
  try {
    const res = await getServiceStatus(id)
    serviceStatusList.value = res.data
  } finally {
    serviceStatusLoading.value = false
  }
}

// ==================== Tab 2: 配置模板 ====================
const templateLoading = ref(false)
const templateList = ref<NginxTemplate[]>([])
const templateTotal = ref(0)
const templatePage = ref(1)
const templateSize = ref(10)
const templateCategory = ref('')

const templateDialogVisible = ref(false)
const currentTemplate = ref<Partial<NginxTemplate> | null>(null)
const templateSubmitting = ref(false)
const templateFormRef = ref<FormInstance>()
const templateForm = ref<Partial<NginxTemplate>>({
  name: '', category: '', configContent: '', description: ''
})
const templateRules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  configContent: [{ required: true, message: '请输入配置内容', trigger: 'blur' }]
}

async function loadTemplates() {
  templateLoading.value = true
  try {
    const res = await getNginxTemplateList({
      page: templatePage.value,
      size: templateSize.value,
      category: templateCategory.value || undefined
    })
    templateList.value = res.data.records
    templateTotal.value = res.data.total
  } finally {
    templateLoading.value = false
  }
}

function openTemplateDialog(row?: NginxTemplate) {
  if (row) {
    currentTemplate.value = { ...row }
    templateForm.value = { ...row }
  } else {
    currentTemplate.value = null
    templateForm.value = { name: '', category: '', configContent: '', description: '' }
  }
  templateDialogVisible.value = true
}

async function handleTemplateSubmit() {
  const valid = await templateFormRef.value?.validate().catch(() => false)
  if (!valid) return

  templateSubmitting.value = true
  try {
    if (currentTemplate.value?.id) {
      await updateNginxTemplate(currentTemplate.value.id, templateForm.value)
      ElMessage.success('更新成功')
    } else {
      await createNginxTemplate(templateForm.value)
      ElMessage.success('创建成功')
    }
    templateDialogVisible.value = false
    loadTemplates()
  } finally {
    templateSubmitting.value = false
  }
}

async function handleDeleteTemplate(id: number) {
  try {
    await deleteNginxTemplate(id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch {
    // error handled by interceptor
  }
}

// 从模板创建配置
const createConfigFromTemplateVisible = ref(false)
const createConfigSubmitting = ref(false)
const createConfigFormRef = ref<FormInstance>()
const createConfigForm = ref({ templateId: 0, templateName: '', serverId: undefined as number | undefined, configName: '' })
const createConfigRules: FormRules = {
  serverId: [{ required: true, message: '请选择服务器', trigger: 'change' }],
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }]
}

function openCreateConfigFromTemplateDialog(row: NginxTemplate) {
  createConfigForm.value = { templateId: row.id, templateName: row.name, serverId: undefined, configName: '' }
  createConfigFromTemplateVisible.value = true
}

async function handleCreateConfigFromTemplate() {
  const valid = await createConfigFormRef.value?.validate().catch(() => false)
  if (!valid) return

  createConfigSubmitting.value = true
  try {
    await createConfigFromTemplate(createConfigForm.value.templateId, createConfigForm.value.serverId!, createConfigForm.value.configName)
    ElMessage.success('创建配置成功')
    createConfigFromTemplateVisible.value = false
    loadConfigs()
  } finally {
    createConfigSubmitting.value = false
  }
}

// ==================== Tab 3: 安装包管理 ====================
const packageLoading = ref(false)
const packageList = ref<NginxPackage[]>([])
const packageTotal = ref(0)
const packagePage = ref(1)
const packageSize = ref(10)

async function loadPackages() {
  packageLoading.value = true
  try {
    const res = await getNginxPackageList({ page: packagePage.value, size: packageSize.value })
    packageList.value = res.data.records
    packageTotal.value = res.data.total
  } finally {
    packageLoading.value = false
  }
}

// 上传安装包
const uploadPackageVisible = ref(false)
const uploadPackageSubmitting = ref(false)
const uploadPackageFormRef = ref<FormInstance>()
const uploadPackageForm = ref({
  file: null as UploadRawFile | null,
  name: '',
  version: '',
  description: ''
})
const uploadPackageRules: FormRules = {
  name: [{ required: true, message: '请输入安装包名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
}

function openUploadPackageDialog() {
  uploadPackageForm.value = { file: null, name: '', version: '', description: '' }
  uploadPackageVisible.value = true
}

function handleFileChange(file: any) {
  uploadPackageForm.value.file = file.raw
  // 自动从文件名提取名称和版本
  const fileName = file.name
  const nameWithoutExt = fileName.replace(/\.(tar\.gz|tar|zip|rpm|deb)$/i, '')
  if (!uploadPackageForm.value.name) {
    uploadPackageForm.value.name = nameWithoutExt
  }
  // 尝试从文件名提取版本号
  const versionMatch = nameWithoutExt.match(/(\d+\.\d+\.\d+)/)
  if (versionMatch && !uploadPackageForm.value.version) {
    uploadPackageForm.value.version = versionMatch[1]
  }
}

function handleFileRemove() {
  uploadPackageForm.value.file = null
}

async function handleUploadPackage() {
  const valid = await uploadPackageFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!uploadPackageForm.value.file) {
    ElMessage.warning('请选择安装包文件')
    return
  }

  uploadPackageSubmitting.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadPackageForm.value.file)
    formData.append('name', uploadPackageForm.value.name)
    formData.append('version', uploadPackageForm.value.version)
    formData.append('description', uploadPackageForm.value.description || '')
    await uploadNginxPackage(formData)
    ElMessage.success('上传成功')
    uploadPackageVisible.value = false
    loadPackages()
  } finally {
    uploadPackageSubmitting.value = false
  }
}

async function handleDeletePackage(id: number) {
  try {
    await deleteNginxPackage(id)
    ElMessage.success('删除成功')
    loadPackages()
  } catch {
    // error handled by interceptor
  }
}

// 安装到服务器
const installPackageVisible = ref(false)
const installPackageSubmitting = ref(false)
const installPackageFormRef = ref<FormInstance>()
const installPackageForm = ref({ packageId: 0, packageName: '', serverId: undefined as number | undefined })
const installPackageRules: FormRules = {
  serverId: [{ required: true, message: '请选择服务器', trigger: 'change' }]
}

function openInstallPackageDialog(row: NginxPackage) {
  installPackageForm.value = { packageId: row.id, packageName: `${row.name} (${row.version})`, serverId: undefined }
  installPackageVisible.value = true
}

async function handleInstallPackage() {
  const valid = await installPackageFormRef.value?.validate().catch(() => false)
  if (!valid) return

  installPackageSubmitting.value = true
  try {
    await installNginxPackage(installPackageForm.value.packageId, installPackageForm.value.serverId!)
    ElMessage.success('安装成功')
    installPackageVisible.value = false
  } finally {
    installPackageSubmitting.value = false
  }
}

// ==================== Tab 4: 一键部署 ====================
const deployStep = ref(0)
const deploying = ref(false)
const deployResult = ref<DeployStepResult | null>(null)
const allTemplates = ref<NginxTemplate[]>([])
const allPackages = ref<NginxPackage[]>([])
const deployForm = ref({
  templateId: undefined as number | undefined,
  serverId: undefined as number | undefined,
  packageId: undefined as number | undefined,
  configName: '',
  configPath: '/usr/local/nginx/conf/nginx.conf'
})

async function loadAllTemplates() {
  try {
    const res = await getNginxTemplateList({ page: 1, size: 200 })
    allTemplates.value = res.data.records
  } catch {
    // error handled by interceptor
  }
}

async function loadAllPackages() {
  try {
    const res = await getNginxPackageList({ page: 1, size: 200 })
    allPackages.value = res.data.records
  } catch {
    // error handled by interceptor
  }
}

async function handleOneClickDeploy() {
  deploying.value = true
  deployResult.value = null
  try {
    const res = await oneClickDeploy({
      templateId: deployForm.value.templateId!,
      serverId: deployForm.value.serverId!,
      packageId: deployForm.value.packageId,
      configName: deployForm.value.configName,
      configPath: deployForm.value.configPath
    })
    deployResult.value = res.data
    if (res.data.success) {
      ElMessage.success('一键部署成功')
    } else {
      ElMessage.error('一键部署失败')
    }
  } catch {
    // error handled by interceptor
  } finally {
    deploying.value = false
  }
}

// ==================== Tab 5: 服务器配置 ====================
const serverConfigServerId = ref<number | undefined>()
const serverConfigLoading = ref(false)
const serverConfigData = ref<NginxServerConfig | null>(null)
const serverConfigExpanded = ref<string[]>([])

async function handleLoadServerConfig() {
  if (!serverConfigServerId.value) return
  serverConfigLoading.value = true
  serverConfigData.value = null
  serverConfigExpanded.value = []
  try {
    const res = await getServerNginxConfig(serverConfigServerId.value)
    serverConfigData.value = res.data
    // 默认展开主配置文件
    if (res.data.installed && res.data.configFilePath) {
      serverConfigExpanded.value = [res.data.configFilePath]
    }
  } finally {
    serverConfigLoading.value = false
  }
}

// ==================== 工具函数 ====================
function statusTagType(status: string) {
  const map: Record<string, string> = { DRAFT: 'info', DEPLOYED: 'success', ERROR: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', DEPLOYED: '已部署', ERROR: '异常' }
  return map[status] || status
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// ==================== 初始化 ====================
onMounted(() => {
  loadServers()
  loadConfigs()
  loadTemplates()
  loadPackages()
  loadAllTemplates()
  loadAllPackages()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.filter-container {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.config-code-block {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  max-height: 500px;
  overflow: auto;
}

.config-code-block pre {
  margin: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #303133;
}
</style>
