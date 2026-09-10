package com.nexoraone.admin.module.business.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.oa.enterprise.dao.EnterpriseEmployeeDao;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.admin.util.AdminRequestUtil;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Application data scope service shared by application center and API open platform.
 */
@Slf4j
@Service
public class ApplicationDataScopeService {

    @Resource
    private EnterpriseEmployeeDao enterpriseEmployeeDao;
    @Resource
    private ApplicationDao applicationDao;

    /**
     * Adds enterprise and creator ownership scope to an application query.
     */
    public void applyScope(LambdaQueryWrapper<ApplicationEntity> wrapper) {
        applyScope(wrapper, null);
    }

    /**
     * Adds ownership scope unless the current employee has the specified platform permission.
     */
    public void applyScope(LambdaQueryWrapper<ApplicationEntity> wrapper, String platformPermission) {
        RequestEmployee employee = requireEmployee();
        if (isPlatformAdministrator(employee)
                || platformPermission != null && StpUtil.hasPermission(platformPermission)) {
            return;
        }
        List<Long> enterpriseIds = getEnterpriseIds(employee);
        wrapper.and(scope -> {
            scope.eq(ApplicationEntity::getCreateUserId, employee.getEmployeeId());
            if (!enterpriseIds.isEmpty()) {
                scope.or().in(ApplicationEntity::getEnterpriseId, enterpriseIds);
            }
        });
    }

    /**
     * Returns whether the current employee can manage an application.
     */
    public boolean canManage(ApplicationEntity application) {
        if (application == null) {
            return false;
        }
        RequestEmployee employee = requireEmployee();
        return isPlatformAdministrator(employee)
                || Objects.equals(application.getCreateUserId(), employee.getEmployeeId());
    }

    /**
     * Returns all application IDs visible in the current employee's management scope.
     */
    public List<Long> getVisibleApplicationIds() {
        LambdaQueryWrapper<ApplicationEntity> wrapper = new LambdaQueryWrapper<ApplicationEntity>()
                .select(ApplicationEntity::getApplicationId);
        applyScope(wrapper);
        return applicationDao.selectList(wrapper).stream()
                .map(ApplicationEntity::getApplicationId)
                .toList();
    }

    /**
     * Returns whether the current employee may assign an application to the specified enterprise.
     */
    public boolean canAssignEnterprise(Long enterpriseId) {
        RequestEmployee employee = requireEmployee();
        return enterpriseId == null
                || isPlatformAdministrator(employee)
                || getEnterpriseIds(employee).contains(enterpriseId);
    }

    /**
     * Returns whether the current employee is a platform administrator.
     */
    public boolean isPlatformAdministrator() {
        return isPlatformAdministrator(requireEmployee());
    }

    /**
     * Returns whether the current employee is a platform administrator or has a platform permission.
     */
    public boolean hasPlatformPermission(String permission) {
        return isPlatformAdministrator(requireEmployee()) || StpUtil.hasPermission(permission);
    }

    /**
     * Returns the current employee and rejects unauthenticated management operations.
     */
    public RequestEmployee requireEmployee() {
        RequestEmployee employee;
        try {
            employee = AdminRequestUtil.getRequestUser();
        } catch (Exception exception) {
            log.debug("No logged-in employee was found when checking application data scope", exception);
            employee = null;
        }
        if (employee == null || employee.getEmployeeId() == null) {
            throw new IllegalStateException("Current login has expired");
        }
        return employee;
    }

    /**
     * Returns enterprise IDs associated with an employee.
     */
    private List<Long> getEnterpriseIds(RequestEmployee employee) {
        return enterpriseEmployeeDao.selectEnterpriseIdByEmployeeId(employee.getEmployeeId());
    }

    /**
     * Checks the platform administrator flag.
     */
    private boolean isPlatformAdministrator(RequestEmployee employee) {
        return Boolean.TRUE.equals(employee.getAdministratorFlag());
    }
}
