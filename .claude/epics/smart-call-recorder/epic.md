---
name: smart-call-recorder
status: backlog
created: 2025-09-04T07:46:10Z
progress: 0%
prd: .claude/prds/smart-call-recorder.md
github: https://github.com/bluedolphi/callRecode/issues/1
---

# Epic: 智能通话录音助手 (Smart Call Recorder)

## Overview

构建一款Android原生应用，实现自动电话和微信通话录音，支持云端上传和数据分析。核心技术栈使用Android原生开发，采用MVVM架构模式，集成录音服务、文件管理、网络上传和数据可视化功能。

**新增功能特性**:
- **录音模式管理**: 支持自动录音和手动录音两种模式
- **来电去电分别配置**: 用户可单独设置来电和去电的录音策略  
- **通话中控制**: 手动模式下提供通话中录音开始/停止控制

## Architecture Decisions

- **开发框架**: Android原生开发（Kotlin + Java）
- **架构模式**: MVVM + Repository模式
- **数据库**: Room (SQLite封装) 用于元数据存储  
- **网络请求**: OkHttp + Retrofit 处理文件上传
- **UI框架**: Material Design + ViewBinding
- **图表库**: MPAndroidChart 用于数据可视化
- **权限管理**: 使用Android官方权限请求框架
- **后台服务**: Foreground Service 确保录音持续性

## Technical Approach

### 核心Android组件
- **录音监听服务 (RecordingService)**
  - TelephonyManager + PhoneStateListener 监听电话状态
  - AccessibilityService 监听微信通话界面
  - MediaRecorder 实现音频录制
  - 前台服务保证后台运行

- **文件管理模块 (FileManager)**
  - 录音文件本地存储和命名规则
  - 文件加密/解密 (AES)
  - 文件元数据管理

- **网络上传模块 (UploadManager)**
  - 队列化上传任务
  - 断点续传支持
  - 网络状态监听 (WiFi优先策略)
  - Token认证机制

### UI层组件
- **主Activity**: 录音列表和状态展示
- **设置Fragment**: 云端配置、录音质量设置
- **统计Fragment**: 数据大盘和图表展示
- **权限引导**: 录音权限、无障碍权限申请流程

### 数据层设计
```
数据库表结构:
- recordings: 录音文件元数据 (id, filename, contact, duration, upload_status, created_at)
- upload_configs: 云端上传配置 (url, token, upload_strategy)
- call_logs: 通话记录统计 (contact, call_count, total_duration, last_call)
```

## Implementation Strategy

### 开发阶段划分
1. **Phase 1**: 核心录音功能 (2-3周)
   - 电话状态监听和录音
   - 基础文件管理和存储
   
2. **Phase 2**: 微信集成 (1-2周)  
   - AccessibilityService实现
   - 微信通话界面识别
   
3. **Phase 3**: 云端功能 (2-3周)
   - 上传配置和队列管理
   - 批量操作实现
   
4. **Phase 4**: 数据分析 (1-2周)
   - 统计数据计算
   - 图表界面实现

### 风险缓解策略
- **权限问题**: 提供详细的权限说明和引导流程
- **兼容性**: 针对主流Android版本进行适配测试
- **微信更新**: 使用通用的界面特征识别，降低版本依赖

## Task Breakdown Preview

高级任务分类 (限制在8个核心任务内):

- [ ] **录音核心引擎**: 实现电话状态监听和录音服务
- [ ] **微信监听集成**: AccessibilityService和界面识别
- [ ] **文件管理系统**: 本地存储、命名规则、加密
- [ ] **云端上传功能**: 配置管理、队列上传、状态同步  
- [ ] **用户界面开发**: Material Design UI和交互逻辑
- [ ] **数据统计模块**: 统计计算和图表展示
- [ ] **权限管理流程**: 权限申请和用户引导
- [ ] **测试和优化**: 功能测试、性能优化、兼容性测试

## Dependencies

### 外部依赖
- **Android系统API**: API 23+ (Android 6.0+)
- **第三方库**: 
  - OkHttp (网络请求)
  - MPAndroidChart (图表显示)
  - Room (数据库ORM)
- **用户云端服务**: 用户提供的上传URL和认证Token

### 内部依赖
- **无障碍权限**: 微信录音必需
- **录音权限**: 应用核心功能必需  
- **存储权限**: 文件管理必需

## Success Criteria (Technical)

### 性能基准
- 录音启动延迟: < 2秒
- 应用冷启动: < 3秒  
- 内存占用: < 100MB常驻
- 电池消耗: < 5%/小时

### 质量门槛
- 应用崩溃率: < 0.1%
- 录音成功率: > 98%
- 文件上传成功率: > 95%
- UI响应时间: < 500ms

### 功能验收
- 支持主流Android设备 (华为、小米、OPPO、vivo)
- 微信录音兼容最近3个版本
- 批量操作支持100+文件

## Estimated Effort

### 总体时间估算
- **开发周期**: 8-10周 (2个开发者)
- **测试周期**: 2周
- **发布准备**: 1周

### 资源需求  
- **Android开发者**: 2名 (高级1名 + 中级1名)
- **测试设备**: 5-8台不同品牌Android设备
- **云端测试环境**: 搭建模拟上传服务

### 关键路径
1. 录音引擎开发 (核心功能，阻塞其他模块)
2. 权限流程设计 (影响用户体验)
3. 微信适配测试 (兼容性风险最高)

## Tasks Created
- [ ] #5 - 项目初始化和依赖配置 (parallel: true)
- [ ] #7 - 数据库设计和Room集成 (parallel: true)  
- [ ] #8 - 录音核心引擎开发 (parallel: false, depends_on: [5])
- [ ] #9 - 微信监听AccessibilityService开发 (parallel: true, depends_on: [5])
- [ ] #2 - 文件管理和加密系统 (parallel: true, depends_on: [7])
- [ ] #3 - 云端上传功能实现 (parallel: true, depends_on: [5, 7])
- [ ] #4 - 用户界面开发和交互逻辑 (parallel: true, depends_on: [7, 2])
- [ ] #6 - 数据统计和可视化展示 (parallel: true, depends_on: [7, 3])

Total tasks: 8
Parallel tasks: 7
Sequential tasks: 1
Estimated total effort: 180 hours

## Latest Updates
- 新增陌生号码专用录音策略和UI配置
- 增加录音质量预设功能(标准/高质量)
- 完善录音模式管理(自动/手动切换)
- 优化通话中录音控制体验
- 增强文件管理和上传状态跟踪功能