# OA 考勤管理系统 — 交付文档

## 1. 项目概要

| 项目 | 说明 |
|------|------|
| 项目名称 | 企业 OA 考勤管理系统 |
| 技术栈 | Spring Boot 2.7.14 + Vue 3 (Vite) + MySQL 8.0 + Flowable 6.8 |
| 开发语言 | Java 8 (后端) / JavaScript (前端) |
| 构建工具 | Maven (后端) / npm (前端) |
| 交付日期 | 2026-07-20 |
| 当前分支 | main |

### 1.1 目录结构

```
OAV3/
├── oa-attendance-system/            # Spring Boot 后端
│   └── src/main/java/com/oa/attendance/
│       ├── config/                  # SecurityConfig
│       ├── controller/              # REST 控制器（10 个）
│       ├── dto/                     # 请求 DTO
│       ├── entity/                  # MyBatis-Plus 实体（14 个）
│       ├── exception/               # 全局异常处理
│       ├── mapper/                  # MyBatis-Plus Mapper（15 个）
│       ├── repository/              # ES Repository
│       ├── service/                 # 服务接口 + impl（14 个）
│       ├── util/                    # JWT 工具类
│       └── vo/                      # 响应 VO
├── oa-attendance-web/               # Vue 3 前端
│   └── src/
│       ├── api/                     # HTTP 请求模块（11 个）
│       ├── components/              # AppShell、CrudLayout
│       ├── store/                   # auth.js（全局状态）
│       ├── views/                   # 页面组件（16 个）
│       ├── router.js                # 路由配置
│       └── main.js
├── database/                        # 数据库备份
├── db_migration_*.sql               # 增量迁移脚本
└── oa_system_v2.sql                 # 完整建库脚本
```

---

## 2. 已实现模块清单

### 2.1 系统基础

| 模块 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| 登录/登出 | `/login` | 无 | JWT 认证，Redis 黑名单 |
| 仪表盘 | `/dashboard` | 无（登录即可） | 当前用户信息 |
| 个人资料 | `/api/users/profile` | 无（登录即可） | 修改个人资料和密码 |

### 2.2 组织管理

| 模块 | 前端路由 | 权限码 | 说明 |
|------|---------|--------|------|
| 用户管理 | `/system/user` | `user:add/update/delete/query` | CRUD + 部门/角色数据范围 |
| 部门管理 | `/system/dept` | `department:*` | 树形结构 CRUD |
| 职位管理 | `/system/position` | `position:*` | CRUD |
| 角色管理 | `/system/role` | `role:*` | CRUD + 菜单/权限分配 |
| 菜单管理 | `/system/menu` | `menu:*` | 树形结构 CRUD |

### 2.3 考勤管理

| 模块 | 前端路由 | 权限码 | 说明 |
|------|---------|--------|------|
| 考勤规则 | `/attendance/rules` | `attendance:rule:*` | CRUD + WiFi/位置配置 |
| 我的考勤 | `/attendance/my` | `attendance:self` | 签到/签退 + 考勤记录 |
| 考勤统计 | `/attendance/stats` | `attendance:stats` | 部门/全局统计 |

### 2.4 审批流程（Flowable）

| 模块 | 前端路由 | 权限码 | 说明 |
|------|---------|--------|------|
| 我的申请 | `/approvals/my` | `approval:self` | 提交/撤回申请 |
| 审批任务 | `/approvals/tasks` | `approval:handle` | 待处理/已处理 |
| 全部申请 | `/approvals/all` | `approval:all` | ADMIN/MANAGER 查看 |
| 申请类型 | `/api/applications/types` | — | 请假/加班/补卡/外出 |

### 2.5 文档检索

| 模块 | 前端路由 | 权限码 | 说明 |
|------|---------|--------|------|
| 制度文档检索 | `/documents` | `document:query/add/index` | MySQL + ES 双写，全文检索 |

### 2.6 同事检索（新增 2026-07-20）

| 模块 | 前端路由 | 权限码 | 说明 |
|------|---------|--------|------|
| 同事检索 | `/colleagues` | `colleague:query` | 按姓名/工号/手机号检索 |

**权限控制：**

| 角色 | 可见范围 |
|------|---------|
| 员工 | 同部门同事 |
| 主管 | 本部门全员 + 其他部门主管 |
| 管理员 | 全部员工 |

### 2.7 聊天模块（新增 2026-07-20）

| 模块 | 前端路由 | 权限码 | 说明 |
|------|---------|--------|------|
| 聊天 | `/chat` | `chat:query` | 通讯录 + 一对一聊天 |

**权限控制：** 同同事检索模块，后端 `ChatServiceImpl.canChat()` 过滤。

**实现方式：** REST API + 前端 3 秒轮询（无 WebSocket 依赖，后续可升级）。

---

## 3. 角色权限体系

### 3.1 角色定义

| 角色 | role_code | 数据范围 | 可分配角色 |
|------|-----------|---------|-----------|
| 管理员 | ADMIN | 全部数据 | 所有角色 |
| 主管 | MANAGER | 本部门 | 仅 EMPLOYEE |
| 员工 | EMPLOYEE | 本人 | 无 |

### 3.2 权限码完整列表

```
user:add, user:update, user:delete, user:query
department:add, department:update, department:delete, department:query
position:add, position:update, position:delete, position:query
role:add, role:update, role:delete, role:query, role:menu, role:permission
menu:add, menu:update, menu:delete, menu:query
document:add, document:update, document:delete, document:query, document:index
attendance:rule:add, attendance:rule:update, attendance:rule:delete, attendance:rule:query
attendance:check, attendance:self, attendance:stats
approval:submit, approval:self, approval:handle, approval:all
colleague:query    （新增）
chat:query         （新增）
```

### 3.3 权限架构

```
用户 → 角色(N:1) → 角色-权限关联表 → 权限 → @PreAuthorize 校验
                 → 角色-菜单关联表 → 菜单 → 前端侧边栏渲染
```

- 认证：Spring Security + JWT（无状态，Redis 黑名单管理登出）
- 授权：`@EnableGlobalMethodSecurity(prePostEnabled = true)` + `@PreAuthorize("hasAuthority('xxx')")`
- 数据范围：`DataScopeService` 在 Service 层按角色过滤

---

## 4. 核心类速查

### 4.1 关键 Service

| 类 | 职责 |
|---|------|
| `DataScopeService` | 角色判断（ADMIN/MANAGER/EMPLOYEE）、数据范围过滤 |
| `UserDetailsServiceImpl` | Spring Security 认证，从 DB 加载权限列表 |
| `UserServiceImpl` | 登录/登出/JWT 签发、用户 CRUD + 数据范围 |
| `ApplicationServiceImpl` | Flowable 流程启动/审批/撤回 |
| `AttendanceApprovalSyncServiceImpl` | 审批通过后同步考勤数据（补卡/请假/加班） |
| `DocumentServiceImpl` | MySQL + Elasticsearch 双写，全文检索 |
| `ColleagueServiceImpl` | 同事检索 + 角色可见性过滤 |
| `ChatServiceImpl` | 通讯录 + 消息收发 + 角色可见性过滤 |

### 4.2 关键 Controller

| 类 | 路径前缀 |
|---|---------|
| `AuthController` | `/api/auth` |
| `UserController` | `/api/users` |
| `DepartmentController` | `/api/departments` |
| `RoleController` | `/api/roles` |
| `MenuController` | `/api/menus` |
| `DocumentController` | `/api/documents` |
| `AttendanceController` | `/api/attendance` |
| `ApplicationController` | `/api/applications` |
| `ColleagueController` | `/api/colleagues` |
| `ChatController` | `/api/chat` |

### 4.3 核心实体

| 实体 | 对应表 | 说明 |
|------|--------|------|
| `SysUser` | `sys_user` | 用户（含 deptId/positionId/roleId） |
| `SysDepartment` | `sys_department` | 部门（parentId 自引用） |
| `SysRole` | `sys_role` | 角色（ADMIN/MANAGER/EMPLOYEE） |
| `SysPermission` | `sys_permission` | 权限（module:action 格式） |
| `SysMenu` | `sys_menu` | 菜单（parentId 自引用） |
| `AttRecord` | `att_record` | 打卡记录 |
| `AttDailySummary` | `att_daily_summary` | 每日考勤汇总 |
| `AppApplication` | `app_application` | 审批申请 |
| `ChatMessage` | `chat_message` | 聊天消息（新增） |

---

## 5. 前端路由总表

| 路径 | 组件 | 权限检查 |
|------|------|---------|
| `/login` | LoginView | 公开 |
| `/dashboard` | DashboardView | 需登录 |
| `/documents` | DocumentsView | `document:query` |
| `/colleagues` | ColleaguesView | `colleague:query`（新增） |
| `/chat` | ChatView | `chat:query`（新增） |
| `/system/user` | UserManageView | `user:query` |
| `/system/dept` | DepartmentManageView | `department:query` |
| `/system/position` | PositionManageView | `position:query` |
| `/system/role` | RoleManageView | `role:query` |
| `/system/menu` | MenuManageView | `menu:query` |
| `/attendance/rules` | AttendanceRuleView | `attendance:rule:query` |
| `/attendance/my` | MyAttendanceView | `attendance:self` |
| `/attendance/stats` | AttendanceStatsView | `attendance:stats` |
| `/approvals/my` | MyApplicationsView | `approval:self` |
| `/approvals/tasks` | ApprovalTasksView | `approval:handle` |
| `/approvals/all` | AllApplicationsView | `approval:all` |

### 路由守卫逻辑（router.beforeEach）

1. 未登录 → 跳转 `/login`
2. 已登录访问 `/login` → 跳转 `/dashboard`
3. 访问 `/approvals/tasks` 无 `approval:handle` → 跳转 `/approvals/my`
4. 访问 `/approvals/all` 无 `approval:all` → 跳转 `/approvals/my`

---

## 6. 数据库迁移脚本执行顺序

```
1. oa_system_v2.sql                         # 基础建库 + 种子数据
2. db_migration_20260720_flowable_approval.sql # Flowable 审批表 + 权限
3. db_migration_20260720_attendance_permissions_menu.sql  # 考勤权限/菜单
4. db_migration_20260720_colleague_search.sql   # 同事检索权限/菜单（新增）
5. db_migration_20260720_chat_module.sql        # 聊天表/权限/菜单（新增）
```

所有迁移脚本使用 `WHERE NOT EXISTS` / `INSERT IGNORE` / `CREATE TABLE IF NOT EXISTS`，可重复执行。

---

## 7. 启动方式

### 7.1 环境依赖

- JDK 8+
- MySQL 8.0+
- Redis（用于 JWT 黑名单）
- Elasticsearch（仅文档检索模块需要）
- Nacos（服务发现/配置，可选——application.yml 中可禁用）
- Node.js 16+

### 7.2 后端

```bash
cd oa-attendance-system
# 修改 src/main/resources/application.yml 中的数据库/Redis/ES 连接信息
mvn clean package -DskipTests
java -jar target/attendance-system-0.0.1-SNAPSHOT.jar
```

### 7.3 前端

```bash
cd oa-attendance-web
# 开发模式
npm install
npm run dev
# 生产构建
npm run build
```

### 7.4 默认测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 管理员 |
| zhangwei | 123456 | 主管（研发部） |
| liming | 123456 | 员工（后端组） |

---

## 8. 前后端交互约定

### 8.1 统一响应格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

- `Result.success(msg, data)` → code=200
- `Result.error(msg)` → code=500（业务异常由 GlobalExceptionHandler 统一处理）

### 8.2 认证方式

- 请求头：`Authorization: Bearer <jwt_token>`
- 前端 `http.js` 拦截器自动注入
- 401 时自动清除 authStore

### 8.3 数据范围约定

- Controller 层：`@PreAuthorize` 控制接口权限
- Service 层：`DataScopeService` 控制数据范围
- 新模块只需关注 Service 层过滤，Controller 注解保持一致

---

## 9. 新模块开发指南

### 9.1 后端新增模块步骤

1. 创建 Entity → `@TableName` 映射表名
2. 创建 Mapper → `extends BaseMapper<Entity>`，自定义 SQL 用 `@Select`
3. 创建 VO / DTO → 请求体和响应体分开
4. 创建 Service 接口 + impl → 注入 `DataScopeService` 做权限过滤
5. 创建 Controller → `@PreAuthorize("hasAuthority('xxx:query')")`
6. 写迁移 SQL → 权限码 + 菜单 + 角色分配
7. `mvn compile` 验证编译

### 9.2 前端新增模块步骤

1. 创建 `src/api/xxx.js` → 封装 HTTP 请求
2. 创建 `src/views/XxxView.vue` → `<AppShell>` 作为根组件
3. 在 `router.js` 中注册路由
4. 执行迁移 SQL 添加菜单（侧边栏自动加载）
5. `npm run build` 验证构建

### 9.3 注意事项

- 不要在前端做权限判断作为唯一防线——后端 `@PreAuthorize` + `DataScopeService` 是最终保障
- Entity 的临时字段用 `@TableField(exist = false)` 标记
- 逻辑删除用 `deleted` 字段（0=正常 1=删除），已有实体遵循此约定
- BPMN 流程文件放在 `resources/processes/` 下，Flowable 自动部署

---

## 10. 已知限制与后续建议

| 项 | 现状 | 建议 |
|----|------|------|
| 聊天实时性 | 3 秒轮询 | 升级为 WebSocket / SSE |
| 聊天消息 | 无分页 | 历史消息加滚动分页 |
| 同事检索 | 无分页 | 加 MyBatis-Plus 分页插件 |
| 审批流程 | 单节点审批 | 按需加多级审批节点 |
| 应用监控 | 无 | 接入 Spring Boot Actuator |
| 文件上传 | 仅支持 URL | 加 OSS/本地文件上传 |
| 国际化 | 硬编码中文 | 迁移到 i18n |

---

## 11. 变更日志

| 日期 | 变更 | 文件 |
|------|------|------|
| 2026-07-20 | 新增同事检索模块 | ColleagueVO, IColleagueService, ColleagueServiceImpl, ColleagueController, ColleaguesView.vue, db_migration_20260720_colleague_search.sql |
| 2026-07-20 | 新增聊天模块 | ChatMessage, ChatMessageMapper, ChatContactVO, ChatMessageVO, IChatService, ChatServiceImpl, ChatController, ChatView.vue, db_migration_20260720_chat_module.sql |
| 2026-07-20 | SysUser 增加 roleCode 字段 | SysUser.java |
| 2026-07-20 | SysUserMapper 增加通用查询方法 | SysUserMapper.java（searchColleagues, selectAllActiveWithDetails） |
