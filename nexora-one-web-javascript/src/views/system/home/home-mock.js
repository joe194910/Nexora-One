export const applicationData = [
  { name: '企业管理', description: '组织、成员与权限管理', icon: 'ApartmentOutlined', color: '#1677ff' },
  { name: '商品管理', description: '商品、类目与库存管理', icon: 'ShopOutlined', color: '#20b65b' },
  { name: '文档中心', description: '知识库与文档协作', icon: 'FileTextOutlined', color: '#7253df' },
  { name: '监控服务', description: '系统监控与告警管理', icon: 'BarChartOutlined', color: '#fa8c16' },
  { name: '系统设置', description: '平台基础配置', icon: 'SettingOutlined', color: '#1ab8aa' },
];

export const assistantShortcutData = [
  { label: '应用接入', icon: 'LinkOutlined', question: '如何将一个新的应用接入 NexoraOne？' },
  { label: '发布 API', icon: 'CodeOutlined', question: '如何创建、调试并发布一个 API？' },
  { label: '接入 MCP', icon: 'DatabaseOutlined', question: '如何配置并接入 MCP 服务？' },
];

export const todoList = [
  { title: '审核应用上架申请', date: '2026-09-09', level: 'high' },
  { title: '处理 API 发布申请', date: '2026-09-09', level: 'high' },
  { title: 'MCP 服务资源开通', date: '2026-09-09', level: 'medium' },
  { title: '企业成员权限变更', date: '2026-09-08', level: 'medium' },
  { title: '查看系统告警日志', date: '2026-09-08', level: 'low' },
  { title: '回复用户技术咨询', date: '2026-09-08', level: 'normal' },
  { title: '编写开发者文档', date: '2026-09-07', level: 'normal' },
];

export const knowledgeList = [
  { title: 'NexoraOne 接入指南', date: '2026-09-09 10:20' },
  { title: 'API 开发规范', date: '2026-09-08 16:32' },
  { title: 'MCP 服务配置手册', date: '2026-09-08 14:18' },
  { title: '企业管理功能使用说明', date: '2026-09-07 11:03' },
  { title: '系统部署与运维', date: '2026-09-06 17:45' },
  { title: '常见问题（FAQ）', date: '2026-09-05 09:28' },
  { title: '安全与权限最佳实践', date: '2026-09-04 15:12' },
];

export const platformStatData = [
  { label: '已上架应用', value: 12, icon: 'AppstoreOutlined', color: '#1677ff', background: '#eaf4ff' },
  { label: '已发布 API', value: 28, icon: 'ApiOutlined', color: '#20b65b', background: '#eaf8ef' },
  { label: 'MCP 在线服务', value: 6, icon: 'DatabaseOutlined', color: '#7253df', background: '#f0edff' },
  { label: '待审核', value: 3, icon: 'ClockCircleOutlined', color: '#fa8c16', background: '#fff4e8', valueColor: '#fa8c16' },
];

export const alertList = [
  { title: 'API 请求异常 (5xx)', date: '2026-09-09 09:14', level: 'high' },
  { title: 'MCP 服务实例响应超时', date: '2026-09-09 08:32', level: 'medium' },
  { title: '应用发布构建失败', date: '2026-09-08 21:03', level: 'medium' },
  { title: '系统磁盘空间不足（< 10%）', date: '2026-09-08 17:26', level: 'low' },
  { title: '数据库连接数接近上限', date: '2026-09-07 13:11', level: 'low' },
];
