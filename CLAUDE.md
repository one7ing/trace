# CLAUDE.md — Trace 项目编码规范

## 项目概述

Trace · 个人成长轨迹 — Spring Boot 3 + Vue 3 全栈应用。后端 Java 21 + MyBatis-Plus + MySQL，前端 TypeScript + Vite + Element Plus + ECharts。

## Java 编码规范（强制）

### 1. 代码风格
- 遵循**阿里巴巴《Java开发手册》**规范
- 缩进使用 **4 个空格**，禁止使用 Tab 字符
- 单行长度不超过 **120 个字符**，过长时自动换行并对齐
- 每个方法之间空一行，逻辑块之间用空行分隔
- 控制结构（if/for/while）即使只有一行也**必须加花括号**

### 2. 注释规范
- 每个类必须有清晰的 **Javadoc 类注释**，说明类的职责和用途
- 每个 public/protected 方法必须有 **Javadoc 方法注释**，说明功能、参数和返回值
- 每个字段必须有简洁的注释说明其含义
- 复杂逻辑块使用行内注释解释"为什么这么做"

### 3. 方法签名
- 参数和注解在方法签名上**每个参数一行**，或对齐排列
- 禁止把所有参数挤在一行
- 示例：
```java
@GetMapping("/growth")
public ResponseEntity<ApiResponse<Map<String, Object>>> growth(
        @AuthenticationPrincipal Long userId) {
    // ...
}
```

### 4. 类型使用
- 返回类型和参数类型使用简洁形式（提倡简洁）
- 仅在必要时使用全限定名（避免歧义时）
- 优先使用 `var` 推断局部变量类型（Java 21 特性）

### 5. 数据访问
- 使用 MyBatis-Plus 的 Mapper 接口进行数据访问
- 复杂查询在 Mapper 中定义方法，简单查询使用 BaseMapper 内置方法
- 日期范围查询统一使用 `findByUserIdAndXxxBetween` 命名模式

### 6. 格式化要求
- 代码必须格式化良好，可以直接提交到生产项目
- 使用 IDE 自动格式化（IDEA Ctrl+Alt+L），配置文件使用项目默认设置
- 提交前确保无未使用的 import 和无用变量

### 7. 命名规范
- 类名：UpperCamelCase（如 `DashboardController`）
- 方法名：lowerCamelCase（如 `findByUserIdAndCreatedAtBetween`）
- 常量：UPPER_SNAKE_CASE（如 `MAX_RETRY_COUNT`）
- 包名：全小写，点分隔（如 `com.trace.controller`）
- 实体类使用 Lombok `@Data` / `@Builder` 简化代码

## TypeScript / Vue 3 编码规范

### 1. 组件结构
- 使用 `<script setup lang="ts">` 语法
- 组件顺序：`<template>` → `<script setup>` → `<style lang="scss" scoped>`
- 组件 Props 使用 TypeScript 类型推断，复杂类型显式声明

### 2. 样式规范
- 使用 SCSS 嵌套语法，嵌套深度不超过 4 层
- 颜色使用 CSS 变量（`var(--color-xxx)`），禁止硬编码颜色值
- 新组件样式优先使用项目已定义的 CSS 变量
- 动画使用 `var(--transition)` 统一过渡时间

### 3. API 调用
- 统一通过 `@/api` 模块发起请求
- 使用项目统一的响应拦截器处理错误
- API 路径使用 `/api/模块名/操作` 的 RESTful 风格

## 项目架构

```
trace/
├── src/main/java/com/trace/
│   ├── controller/   # REST 控制器
│   ├── entity/       # 数据实体
│   ├── mapper/       # MyBatis-Plus Mapper
│   ├── service/      # 业务服务（如存在）
│   └── dto/          # 数据传输对象
├── frontend/src/
│   ├── views/        # 页面组件
│   ├── components/   # 通用组件
│   ├── api/          # API 封装
│   ├── router/       # 路由配置
│   ├── stores/       # Pinia 状态管理
│   └── styles/       # 全局样式
```

## 提交规范

- 提交信息使用中文，简洁描述变更内容
- 一个提交只做一件事
- 提交前确保前端构建和后端编译均通过
