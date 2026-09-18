-- A checked page menu must also grant the API permission required to open that page.
UPDATE `t_menu`
SET `api_perms` = 'knowledge:document:query', `update_time` = NOW()
WHERE `menu_id` = 901;

UPDATE `t_menu`
SET `api_perms` = 'knowledge:base:query', `update_time` = NOW()
WHERE `menu_id` = 902;

UPDATE `t_menu`
SET `api_perms` = 'knowledge:assistant:query', `update_time` = NOW()
WHERE `menu_id` = 903;

UPDATE `t_menu`
SET `api_perms` = 'ai:parse-plan:query', `update_time` = NOW()
WHERE `menu_id` = 858;
