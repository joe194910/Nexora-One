import boot3Dependencies from './spring-boot-3/dependencies.xml?raw';
import boot3Application from './spring-boot-3/application.yml?raw';
import boot3Properties from './spring-boot-3/McpServerProperties.java?raw';
import boot3AuthenticationFilter from './spring-boot-3/McpAuthenticationFilter.java?raw';
import boot3Tool from './spring-boot-3/McpTool.java?raw';
import boot3Configuration from './spring-boot-3/McpServerConfiguration.java?raw';
import boot3OrderExample from './spring-boot-3/OrderQueryToolExample.java?raw';
import boot2Application from './spring-boot-2/application.yml?raw';
import boot2Properties from './spring-boot-2/McpProperties.java?raw';
import boot2Tool from './spring-boot-2/McpTool.java?raw';
import boot2Controller from './spring-boot-2/McpController.java?raw';
import boot2OrderExample from './spring-boot-2/OrderQueryToolExample.java?raw';

/**
 * MCP 接入指南的两个实现版本。
 *
 * <p>代码文件以 Vite raw 资源载入，只用于页面展示和复制，不参与 NexoraOne
 * 前端或服务端的编译。</p>
 */
export const mcpGuideEditions = [
  {
    key: 'spring-boot-3',
    tab: 'Spring Boot 3 官方 SDK版',
    title: 'Spring Boot 3 官方 SDK版',
    description: '适用于 Java 17 和 jakarta.servlet 项目，使用 MCP Java SDK 提供标准 Streamable HTTP Server。',
    tagText: '推荐',
    tagColor: 'green',
    alertType: 'success',
    notice: '优先选择此版本。官方 SDK 负责协议协商、会话、tools/list、tools/call 和输入 Schema 校验。',
    checklist: [
      '复制依赖和配置，并把示例包名 com.partner.mcp 改成对方项目包名。',
      '在原登录框架中放行 /mcp 和 /mcp/**，不要要求先调用业务登录接口。',
      '生产环境通过 MCP_AUTH_SECRET 注入静态凭证，不要把真实 Token 提交到代码仓库。',
      '每个业务能力实现一个 McpTool 并标注 @Component，原 Service 可以直接复用。',
      '启动后把完整 /mcp 地址和对应鉴权方式登记到 NexoraOne。',
      '在 NexoraOne 执行探活、测试、审核、启用，再到智能助手页面关联工具。',
    ],
    sections: [
      {
        key: 'boot3-dependencies',
        title: '1. 添加官方 SDK 依赖',
        fileName: 'pom.xml',
        description: '已有统一 dependencyManagement 时，可将版本改为对方项目统一管理的版本。',
        code: boot3Dependencies,
      },
      {
        key: 'boot3-application',
        title: '2. 配置 endpoint 和独立鉴权',
        fileName: 'application.yml',
        description: '支持 NONE、BEARER、API_KEY。生产环境推荐 Bearer Token 或 API Key。',
        code: boot3Application,
      },
      {
        key: 'boot3-properties',
        title: '3. 创建 MCP 配置实体',
        fileName: 'McpServerProperties.java',
        code: boot3Properties,
      },
      {
        key: 'boot3-authentication',
        title: '4. 创建 MCP 独立鉴权过滤器',
        fileName: 'McpAuthenticationFilter.java',
        description: '该过滤器只保护 MCP endpoint，不依赖对方系统的用户登录态。',
        code: boot3AuthenticationFilter,
      },
      {
        key: 'boot3-tool',
        title: '5. 创建工具契约',
        fileName: 'McpTool.java',
        code: boot3Tool,
      },
      {
        key: 'boot3-configuration',
        title: '6. 注册 Streamable HTTP Server',
        fileName: 'McpServerConfiguration.java',
        code: boot3Configuration,
      },
      {
        key: 'boot3-example',
        title: '7. 复制一个工具并接入现有 Service',
        fileName: 'OrderQueryToolExample.java',
        description: '保留工具定义和参数校验，将 TODO 位置替换为对方已有业务 Service 调用。',
        code: boot3OrderExample,
      },
    ],
  },
  {
    key: 'spring-boot-2',
    tab: 'Spring Boot 2 老项目兼容版',
    title: 'Spring Boot 2 老项目兼容版',
    description: '适用于 Java 8/11 和 javax.servlet 老项目，不引入 jakarta.servlet，尽量减少升级和改造成本。',
    tagText: '兼容方案',
    tagColor: 'orange',
    alertType: 'warning',
    notice: '该版本实现 NexoraOne 当前接入所需的 MCP 工具子集，不等同于官方 SDK 的全部能力；新项目应使用 Spring Boot 3 官方 SDK版。',
    checklist: [
      '项目需要已有 spring-boot-starter-web 和 Jackson，一般无需新增业务依赖。',
      '复制配置、McpProperties、McpTool 和 McpController，并修改示例包名。',
      '在原登录框架中放行 /mcp 和 /mcp/**，由 McpController 独立校验静态凭证。',
      '支持 NONE、BEARER、API_KEY，生产环境不要选择 NONE。',
      '每个业务能力实现一个 McpTool 并标注 @Component，Controller 会自动发现。',
      '登记后依次执行探活、工具测试、审核、启用和智能助手关联。',
    ],
    sections: [
      {
        key: 'boot2-application',
        title: '1. 配置 endpoint 和独立鉴权',
        fileName: 'application.yml',
        description: '静态凭证通过环境变量注入，对方不需要再提供登录接口。',
        code: boot2Application,
      },
      {
        key: 'boot2-properties',
        title: '2. 创建 MCP 配置实体',
        fileName: 'McpProperties.java',
        code: boot2Properties,
      },
      {
        key: 'boot2-tool',
        title: '3. 创建工具契约',
        fileName: 'McpTool.java',
        code: boot2Tool,
      },
      {
        key: 'boot2-controller',
        title: '4. 创建 MCP 兼容入口',
        fileName: 'McpController.java',
        description: '统一处理 initialize、ping、tools/list 和 tools/call，并完成静态凭证校验。',
        code: boot2Controller,
      },
      {
        key: 'boot2-example',
        title: '5. 复制一个工具并接入现有 Service',
        fileName: 'OrderQueryToolExample.java',
        description: '将示例返回值替换为对方已有业务 Service 的真实查询结果。',
        code: boot2OrderExample,
      },
    ],
  },
];
