# DeployHub 内网部署服务 - 技术文档

## 1. 项目概述

DeployHub 是一套面向内网环境的自动化部署服务平台，支持 Java 项目前后端代码的打包、安装包上传、脚本化部署以及多服务器并行管理。系统采用 Spring Boot + Vue.js 架构，通过 SSH/SFTP 协议直连目标服务器，无需在远程服务器安装额外 Agent。

### 核心能力

| 能力 | 说明 |
|------|------|
| 安装包上传 | 支持 JAR/WAR/ZIP 等安装包上传至指定服务器并自动执行安装流程 |
| 目录设置与打包 | 对 Java 项目前端（npm build）和后端（Maven/Gradle build）分别打包 |
| 脚本化部署 | 通过可配置的 Shell 脚本实现自动化部署，支持变量替换和模板渲染 |
| 多服务器并行 | 支持同时向多台服务器执行部署操作，每台服务器独立跟踪状态 |
| 多应用同时部署 | 支持同时部署多个 Java 应用，每个应用独立管理部署流程 |
| 实时日志 | 通过 WebSocket STOMP 协议实时推送部署日志，前端 xterm.js 终端展示 |
| 部署回滚 | 部署失败后可触发回滚操作，完整的部署状态机管理 |
| 凭据加密 | 服务器密码等敏感信息使用 AES 加密存储 |
| 预置项目 | 内置 6 个项目（皇岗口岸、数据库监控、运维监控、调度系统、数字孪生、业务协同）的部署配置 |
| Docker 测试环境 | 一键启动 MySQL + Ubuntu 测试服务器，支持本地验证部署流程 |

---

## 2. 技术架构

### 2.1 架构图

```
┌─────────────────┐     REST API      ┌──────────────────────┐     SSH/SFTP      ┌──────────────┐
│   Vue 3 前端     │ ◄───────────────► │   Spring Boot 后端    │ ◄───────────────► │  目标服务器    │
│   Element Plus   │   WebSocket       │   JSch SSH Engine     │   SCP 文件传输    │  Java 应用    │
│   xterm.js       │   实时日志         │   MyBatis-Plus        │   Shell 脚本执行  │              │
│   Pinia          │                   │   MySQL               │                   └──────────────┘
└─────────────────┘                    └──────────────────────┘
```

### 2.2 技术栈

#### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行时 |
| Spring Boot | 3.2.5 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.x | 数据库 |
| JSch | 0.2.16 | SSH/SFTP 通信 |
| commons-pool2 | - | SSH 连接池 |
| Hutool | 5.8.27 | 工具库（AES 加密等） |
| Lombok | - | 代码简化 |

#### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.4+ | 类型安全 |
| Vite | 5.2+ | 构建工具 |
| Element Plus | 2.7+ | UI 组件库 |
| Pinia | 2.1+ | 状态管理 |
| Vue Router | 4.3+ | 路由管理 |
| Axios | 1.7+ | HTTP 客户端 |
| xterm.js | 5.3+ | 终端日志展示 |
| @stomp/stompjs | 7.0+ | WebSocket STOMP 客户端 |
| sockjs-client | 1.6+ | SockJS 通信 |

---

## 3. 项目结构

```
deploy_hub/
├── backend/                                # Spring Boot 后端
│   ├── pom.xml                             # Maven 配置
│   └── src/main/
│       ├── java/com/deployhub/
│       │   ├── DeployHubApplication.java   # 启动类
│       │   ├── config/                     # 配置类
│       │   │   ├── CorsConfig.java         # 跨域配置
│       │   │   ├── WebSocketConfig.java    # WebSocket STOMP 配置
│       │   │   ├── MyBatisPlusConfig.java  # MyBatis-Plus 分页插件配置
│       │   │   └── MyBatisPlusMetaObjectHandler.java  # 自动填充处理器
│       │   ├── common/                     # 通用类
│       │   │   ├── Result.java             # 统一响应封装
│       │   │   ├── PageResult.java         # 分页响应封装
│       │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│       │   ├── entity/                     # 数据实体
│       │   │   ├── BaseEntity.java         # 基础实体（id/createTime/updateTime/deleted）
│       │   │   ├── Server.java             # 服务器
│       │   │   ├── App.java                # 应用
│       │   ├── DeployRecord.java       # 部署记录
│       │   ├── DeployConfig.java       # 部署配置（保存和复用）
│       │   ├── ScriptTemplate.java     # 脚本模板
│       │   ├── ScriptExecution.java    # 脚本执行记录
│       │   ├── ScriptVariable.java     # 脚本变量
│       │   └── SystemConfig.java       # 系统配置
│       │   ├── mapper/                     # MyBatis-Plus Mapper
│       │   ├── service/                    # Service 接口
│       │   ├── service/impl/               # Service 实现
│       │   ├── controller/                 # REST 控制器
│       │   ├── dto/                        # 请求传输对象
│       │   ├── vo/                         # 响应视图对象
│       │   ├── ssh/                        # SSH 模块
│       │   │   ├── SshClient.java          # SSH 客户端（命令执行/文件上传）
│       │   │   ├── SshPool.java            # SSH 连接池（多服务器管理）
│       │   │   └── SshExecuteResult.java   # 执行结果
│       │   ├── deploy/                     # 部署模块
│       │   │   ├── DeployEngine.java       # 部署引擎（线程池调度）
│       │   │   ├── DeployTask.java         # 部署任务（支持回滚）
│       │   │   └── DeployStatus.java       # 部署状态枚举
│       │   ├── script/                     # 脚本模块
│       │   │   ├── ScriptEngine.java       # 脚本引擎（变量替换）
│       │   │   └── ScriptVariableResolver.java  # 变量解析器
│       │   ├── util/                       # 工具类
│       │   │   └── PasswordUtil.java       # AES 密码加解密
│       │   └── websocket/                  # WebSocket 模块
│       │       └── DeployWebSocketHandler.java  # 部署日志推送
│       └── resources/
│           ├── application.yml             # 应用配置
│           └── schema.sql                  # 数据库初始化脚本
│
└── frontend/                               # Vue 3 前端
    ├── package.json
    ├── vite.config.ts                      # Vite 配置（API/WS 代理）
    └── src/
        ├── main.ts                         # 入口文件
        ├── App.vue                         # 根组件
        ├── api/                            # API 请求层
        │   ├── index.ts                    # Axios 实例 + 拦截器
        │   ├── server.ts                   # 服务器 API
        │   ├── app.ts                      # 应用 API
        │   ├── deploy.ts                   # 部署 API
        │   ├── script.ts                   # 脚本模板 API
        │   └── transfer.ts                # 文件传输 API
        ├── router/index.ts                 # 路由配置
        ├── stores/                         # Pinia 状态管理
        │   ├── server.ts
        │   ├── app.ts
        │   └── deploy.ts
        ├── views/                          # 页面视图
        │   ├── layout/MainLayout.vue       # 主布局（侧边栏+头部+内容区）
        │   ├── dashboard/DashboardView.vue # 仪表盘
        │   ├── server/ServerView.vue       # 服务器管理
        │   ├── app/AppView.vue             # 应用管理
        │   ├── deploy/DeployView.vue       # 部署中心
        │   ├── script/ScriptView.vue       # 脚本管理
        │   ├── history/HistoryView.vue     # 部署历史
        │   └── settings/SettingsView.vue   # 系统设置
        ├── components/                     # 公共组件
        │   ├── ServerFormDialog.vue        # 服务器表单弹窗
        │   ├── AppFormDialog.vue           # 应用表单弹窗
        │   ├── DeployPanel.vue             # 部署操作面板
        │   ├── ScriptEditor.vue            # 脚本编辑器
        │   ├── LogTerminal.vue             # 实时日志终端（xterm.js）
        │   └── StatusTag.vue               # 部署状态标签
        ├── types/index.ts                  # TypeScript 类型定义
        ├── utils/websocket.ts              # WebSocket STOMP 封装
        └── styles/index.css                # 全局样式
```

---

## 4. 快速开始

### 4.1 环境要求

| 依赖 | 最低版本 |
|------|---------|
| JDK | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |

### 4.2 数据库初始化

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 4.3 后端启动

```bash
cd backend
mvn spring-boot:run
```

后端服务启动在 `http://localhost:8086`

### 4.4 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器启动在 `http://localhost:5173`，API 请求自动代理到后端。

### 4.5 生产构建

```bash
# 后端
cd backend
mvn clean package -DskipTests
java -jar target/deploy-hub-1.0.0.jar

# 前端
cd frontend
npm run build
# 产物在 dist/ 目录，可部署到 Nginx 等 Web 服务器
```

---

## 5. 配置说明

### 5.1 后端配置 (application.yml)

```yaml
server:
  port: 8086                    # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/deploy_hub?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      connection-init-sql: SET NAMES utf8mb4    # 数据库连接初始化 SQL，确保中文不乱码
  sql:
    init:
      encoding: UTF-8                           # SQL 初始化脚本编码

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

deploy:
  ssh:
    pool:
      max-total: 20             # 每台服务器最大 SSH 连接数
      max-idle: 10              # 最大空闲连接数
      min-idle: 2               # 最小空闲连接数
      timeout: 30000            # 连接超时（毫秒）
  upload:
    path: ./uploads             # 上传文件临时存储路径
  script:
    path: ./scripts             # 脚本文件存储路径
```

### 5.2 前端代理配置 (vite.config.ts)

| 代理路径 | 目标 | 用途 |
|---------|------|------|
| `/api` | `http://localhost:8086` | REST API 代理 |
| `/ws` | `ws://localhost:8086/ws` | WebSocket 代理 |

---

## 6. 数据库设计

### 6.1 ER 关系

```
t_server ─────────┐
                   │
t_app ────────────┤
                   ├──► t_deploy_record
t_script_template ─┘
                   
t_system_config（独立）
```

### 6.2 表结构

#### t_server（服务器表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| name | VARCHAR(100) | 服务器名称 |
| host | VARCHAR(255) | IP 地址 |
| port | INT | SSH 端口，默认 22 |
| username | VARCHAR(100) | SSH 用户名 |
| auth_type | VARCHAR(20) | 认证方式：PASSWORD / KEY |
| password | VARCHAR(500) | 加密存储的密码 |
| private_key | TEXT | SSH 私钥 |
| status | VARCHAR(20) | 状态：ONLINE / OFFLINE |
| group_name | VARCHAR(100) | 分组名称 |
| description | VARCHAR(500) | 描述 |
| deleted | INT | 逻辑删除：0/1 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### t_app（应用表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| name | VARCHAR(100) | 应用名称 |
| type | VARCHAR(20) | 类型：FULL_STACK / BACKEND_ONLY / FRONTEND_ONLY |
| repo_url | VARCHAR(500) | 仓库地址 |
| frontend_dir | VARCHAR(255) | 前端代码目录 |
| backend_dir | VARCHAR(255) | 后端代码目录 |
| output_dir | VARCHAR(255) | 构建产物输出目录 |
| build_cmd_frontend | VARCHAR(500) | 前端构建命令 |
| build_cmd_backend | VARCHAR(500) | 后端构建命令 |
| description | VARCHAR(500) | 描述 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### t_deploy_record（部署记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| app_id | BIGINT | 关联应用 ID |
| server_id | BIGINT | 关联服务器 ID |
| status | VARCHAR(30) | 状态：PENDING / DEPLOYING / SUCCESS / FAILED / ROLLING_BACK / ROLLED_BACK |
| version | VARCHAR(100) | 部署版本号 |
| deploy_script | TEXT | 执行的部署脚本 |
| deploy_log | LONGTEXT | 部署日志 |
| error_message | TEXT | 错误信息 |
| started_at | DATETIME | 开始时间 |
| finished_at | DATETIME | 结束时间 |
| operator | VARCHAR(100) | 操作人 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### t_script_template（脚本模板表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| name | VARCHAR(100) | 模板名称 |
| content | TEXT | 脚本内容 |
| variables | JSON | 变量定义（JSON 格式） |
| description | VARCHAR(500) | 描述 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### t_system_config（系统配置表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键自增 |
| config_key | VARCHAR(100) | 配置键（唯一） |
| config_value | TEXT | 配置值 |
| config_group | VARCHAR(50) | 配置分组 |
| description | VARCHAR(500) | 描述 |
| deleted | INT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 7. API 接口文档

### 7.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 7.2 服务器管理 `/server`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/server/list` | 分页查询 | pageNum, pageSize, name(可选), groupName(可选) |
| GET | `/server/{id}` | 查询详情 | id |
| POST | `/server` | 新增 | ServerDTO |
| PUT | `/server` | 修改 | ServerDTO |
| DELETE | `/server/{id}` | 删除 | id |
| GET | `/server/{id}/test-connection` | 连通性测试 | id |

### 7.3 应用管理 `/app`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/app/list` | 分页查询 | pageNum, pageSize, name(可选), type(可选) |
| GET | `/app/{id}` | 查询详情 | id |
| POST | `/app` | 新增 | AppDTO |
| PUT | `/app` | 修改 | AppDTO |
| DELETE | `/app/{id}` | 删除 | id |

### 7.4 构建管理 `/build`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| POST | `/build/backend/{appId}` | 后端构建 | appId |
| POST | `/build/frontend/{appId}` | 前端构建 | appId |
| POST | `/build/all/{appId}` | 全量构建 | appId |

### 7.5 文件传输 `/transfer`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| POST | `/transfer/upload` | 上传安装包 | serverId, file(MultipartFile), remotePath |
| POST | `/transfer/push` | 推送本地文件 | serverId, localFilePath, remotePath |

### 7.6 部署管理 `/deploy`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/deploy/list` | 分页查询 | pageNum, pageSize, appId(可选), serverId(可选), status(可选) |
| GET | `/deploy/{id}` | 查询详情 | id |
| POST | `/deploy/trigger` | 触发部署 | DeployRequestDTO |
| POST | `/deploy/{id}/rollback` | 回滚部署 | id |
| GET | `/deploy/{id}/log` | 获取日志 | id |

**DeployRequestDTO:**

```json
{
  "appId": 1,
  "serverIds": [1, 2, 3],
  "version": "v1.0.0",
  "deployScript": "#!/bin/bash\ncp ${APP_NAME}.jar /opt/apps/",
  "operator": "admin"
}
```

### 7.7 脚本模板 `/script-template`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/script-template/list` | 分页查询 | pageNum, pageSize, name(可选) |
| GET | `/script-template/{id}` | 查询详情 | id |
| POST | `/script-template` | 新增 | ScriptTemplateDTO |
| PUT | `/script-template` | 修改 | ScriptTemplateDTO |
| DELETE | `/script-template/{id}` | 删除 | id |

### 脚本执行 `/script-execution`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| POST | `/script-execution/execute` | 执行脚本 | ScriptExecutionDTO（返回 executionIds 列表） |
| POST | `/script-execution/batch-execute` | 批量执行 | BatchScriptExecutionDTO |
| GET | `/script-execution/{id}/log` | 获取日志 | id |
| POST | `/script-execution/{id}/cancel` | 取消执行 | id |

### 7.8 系统配置 `/config`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/config/list` | 查询列表 | configGroup(可选) |
| GET | `/config/{configKey}` | 按 Key 查询 | configKey |
| POST | `/config` | 新增 | SystemConfig |
| PUT | `/config/{id}` | 修改 | id, SystemConfig |
| DELETE | `/config/{id}` | 删除 | id |

### 7.9 WebSocket 实时日志

**连接地址:** `ws://localhost:8086/ws`

**订阅主题:** `/topic/deploy/{recordId}`

**消息格式:**

日志消息（纯文本）：
```
SSH连接建立成功, 服务器: 192.168.1.100
```

状态变更消息（JSON）：
```json
{
  "recordId": 1,
  "status": "DEPLOYING",
  "timestamp": 1715923200000
}
```

---

## 8. 核心模块设计

### 8.1 SSH 连接池

`SshPool` 使用 `ConcurrentHashMap<host:port, GenericObjectPool<Session>>` 管理多台服务器的 SSH 连接池，每台服务器独立维护连接池，支持并行部署。

```
SshPool
├── "192.168.1.100:22" → GenericObjectPool<Session> (max=20, idle=10)
├── "192.168.1.101:22" → GenericObjectPool<Session> (max=20, idle=10)
└── "192.168.1.102:22" → GenericObjectPool<Session> (max=20, idle=10)
```

### 8.2 部署状态机

```
PENDING ──► DEPLOYING ──► SUCCESS
    │            │
    │            ▼
    │          FAILED ──► ROLLING_BACK ──► ROLLED_BACK
    │            │
    └────────────┘ (重新部署)
```

### 8.3 脚本变量替换

脚本模板中使用 `${变量名}` 语法定义变量，部署时自动替换：

| 变量 | 来源 | 示例值 |
|------|------|--------|
| `${APP_NAME}` | 应用名称 | my-project |
| `${APP_TYPE}` | 应用类型 | FULL_STACK |
| `${APP_OUTPUT_DIR}` | 产物目录 | /opt/build/output |
| `${FRONTEND_DIR}` | 前端目录 | /opt/project/frontend |
| `${BACKEND_DIR}` | 后端目录 | /opt/project/backend |
| `${SERVER_HOST}` | 服务器地址 | 192.168.1.100 |
| `${SERVER_PORT}` | SSH 端口 | 22 |
| `${SERVER_USER}` | 服务器用户 | deploy |
| `${SERVER_GROUP}` | 服务器分组 | production |
| `${VERSION}` | 部署版本 | v1.0.0 |
| `${TIMESTAMP}` | 时间戳 | 1715923200000 |
| `${DATE}` | 日期 | 2026-05-17 |

### 8.4 密码安全

- 服务器密码使用 AES 算法加密后存入数据库
- 加解密工具类 `PasswordUtil`，密钥为 `DeployHub2024Key`
- 连接 SSH 时自动解密，前端不返回密码字段

### 8.5 部署流程

```
用户触发部署
    │
    ▼
DeployRecordServiceImpl.trigger()
    │ 遍历 serverIds，为每台服务器创建 DeployTask
    ▼
DeployEngine.submitDeploy()
    │ 创建 DeployRecord（PENDING），提交到线程池
    ▼
DeployTask.run()
    ├── 1. 更新状态为 DEPLOYING
    ├── 2. 解析脚本变量（ScriptEngine.render）
    ├── 3. 从 SshPool 借用 Session
    ├── 4. 执行部署脚本（executeCommandWithCallback）
    │      └── 实时推送日志到 WebSocket
    ├── 5. 判断执行结果（exitCode）
    │      ├── 成功 → 更新状态为 SUCCESS
    │      └── 失败 → 更新状态为 FAILED
    └── 6. 归还 Session 到 SshPool
```

---

## 9. 前端页面说明

| 页面 | 路径 | 功能 |
|------|------|------|
| 仪表盘 | `/dashboard` | 统计卡片（服务器数/应用数/今日部署/成功率）+ 最近部署记录 |
| 服务器管理 | `/server` | 服务器 CRUD + 连通性测试 + 分组管理 |
| 应用管理 | `/app` | 应用 CRUD + 目录配置 + 构建操作（前端/后端/全量） |
| 部署中心 | `/deploy` | 选择应用+多选服务器+配置脚本+触发部署+安装包上传+状态看板 |
| 脚本管理 | `/script` | 脚本模板 CRUD + 变量插入 + 编辑器 |
| 部署历史 | `/history` | 筛选查询 + 状态标签 + 日志查看 + 回滚操作 |
| 系统设置 | `/settings` | 配置分组管理 + 配置项 CRUD |

---

## 10. 部署运维

### 10.1 Nginx 前端部署参考

```nginx
server {
    listen 80;
    server_name deploy-hub.example.com;

    location / {
        root /opt/deploy-hub/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8086/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8086/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 10.3 后端 JAR 部署

```bash
# 构建
cd backend && mvn clean package -DskipTests

# 启动
nohup java -jar target/deploy-hub-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://db-host:3306/deploy_hub \
  --spring.datasource.password=your_password \
  > deploy-hub.log 2>&1 &
```
