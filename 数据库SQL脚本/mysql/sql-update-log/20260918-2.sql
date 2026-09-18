-- 修复应用市场菜单已授权但后端接口仍提示无权限的问题。
-- 应用市场页面包含市场查询、收藏和进入应用操作，勾选该菜单时应同时授予对应接口权限。
UPDATE `t_menu`
SET `api_perms` = 'application:market,application:portal',
    `web_perms` = 'application:market',
    `perms_type` = 1,
    `update_time` = NOW()
WHERE `menu_id` = 812
   OR `path` = '/application/market';
