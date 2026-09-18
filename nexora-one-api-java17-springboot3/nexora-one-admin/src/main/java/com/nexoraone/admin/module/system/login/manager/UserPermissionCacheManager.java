package com.nexoraone.admin.module.system.login.manager;

import com.nexoraone.admin.constant.AdminCacheConst;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

/**
 * User permission cache manager.
 */
@Service
public class UserPermissionCacheManager {

    @Resource
    private CacheManager cacheManager;

    public void clear(Collection<Long> employeeIdList) {
        if (CollectionUtils.isEmpty(employeeIdList)) {
            return;
        }

        Cache cache = cacheManager.getCache(AdminCacheConst.Login.USER_PERMISSION);
        if (cache == null) {
            return;
        }

        employeeIdList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(cache::evict);
    }
}
