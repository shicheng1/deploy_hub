<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapse ? '64px' : '220px'">
      <div class="sidebar-container">
        <div class="sidebar-logo">
          <span v-show="!isCollapse">DeployHub</span>
          <span v-show="isCollapse">DH</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          class="sidebar-menu"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><Monitor /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          <el-menu-item index="/server">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>服务器管理</template>
          </el-menu-item>
          <el-menu-item index="/app">
            <el-icon><Box /></el-icon>
            <template #title>应用管理</template>
          </el-menu-item>
          <el-menu-item index="/deploy">
            <el-icon><Promotion /></el-icon>
            <template #title>部署中心</template>
          </el-menu-item>
          <el-menu-item index="/script">
            <el-icon><Document /></el-icon>
            <template #title>脚本管理</template>
          </el-menu-item>
          <el-menu-item index="/script-generator">
            <el-icon><MagicStick /></el-icon>
            <template #title>脚本生成器</template>
          </el-menu-item>
          <el-menu-item index="/script-execution">
            <el-icon><Cpu /></el-icon>
            <template #title>脚本执行</template>
          </el-menu-item>
          <el-menu-item index="/script-execution-history">
            <el-icon><List /></el-icon>
            <template #title>执行历史</template>
          </el-menu-item>
          <el-menu-item index="/file-deploy">
            <el-icon><Upload /></el-icon>
            <template #title>文件部署</template>
          </el-menu-item>
          <el-menu-item index="/nginx">
            <el-icon><Connection /></el-icon>
            <template #title>Nginx管理</template>
          </el-menu-item>
          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon>
            <template #title>部署历史</template>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <template #title>系统设置</template>
          </el-menu-item>
        </el-menu>
      </div>
    </el-aside>
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 1px 4px rgba(0,21,41,.08); padding: 0 20px;">
        <div style="display: flex; align-items: center;">
          <el-icon
            :size="20"
            style="cursor: pointer; margin-right: 16px;"
            @click="isCollapse = !isCollapse"
          >
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="font-size: 14px; color: #303133; font-weight: 500;">Admin</span>
        </div>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Monitor, OfficeBuilding, Box, Promotion, Document, Clock, Setting, Fold, Expand, Cpu, Upload, List, MagicStick, Connection } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || 'DeployHub')
</script>
