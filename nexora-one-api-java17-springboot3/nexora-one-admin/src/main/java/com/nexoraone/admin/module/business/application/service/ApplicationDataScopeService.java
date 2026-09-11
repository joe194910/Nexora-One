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
 * 应用中心和API开放平台共用的数据权限服务。
 */
@Slf4j
@Service
public class ApplicationDataScopeService {

    @Resource
    private EnterpriseEmployeeDao enterpriseEmployeeDao;
    @Resource
    private ApplicationDao applicationDao;

    /**
     * 为应用查询增加创建人数据范围。
     */
    public void applyScope(LambdaQueryWrapper<ApplicationEntity> wrapper) {
        applyScope(wrapper, null);
    }

    /**
     * 除平台管理员或具有指定平台权限的人员外，仅允许查询本人创建的应用。
     */
    public void applyScope(LambdaQueryWrapper<ApplicationEntity> wrapper, String platformPermission) {
        RequestEmployee employee = requireEmployee();
        if (isPlatformAdministrator(employee)
                || platformPermission != null && StpUtil.hasPermission(platformPermission)) {
            return;
        }
        wrapper.eq(ApplicationEntity::getCreateUserId, employee.getEmployeeId());
    }

    /**
     * 判断当前员工是否可以管理指定应用。
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
     * 查询当前员工管理范围内的全部应用主键。
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
     * 判断当前员工是否可以把应用归属到指定企业。
     */
    public boolean canAssignEnterprise(Long enterpriseId) {
        RequestEmployee employee = requireEmployee();
        return enterpriseId == null
                || isPlatformAdministrator(employee)
                || getEnterpriseIds(employee).contains(enterpriseId);
    }

    /**
     * 判断指定员工是否属于目标企业。
     */
    public boolean isEnterpriseMember(Long enterpriseId, Long employeeId) {
        if (enterpriseId == null || employeeId == null) {
            return false;
        }
        return enterpriseEmployeeDao.selectEnterpriseIdByEmployeeId(employeeId).contains(enterpriseId);
    }

    /**
     * 判断当前员工是否为平台管理员。
     */
    public boolean isPlatformAdministrator() {
        return isPlatformAdministrator(requireEmployee());
    }

    /**
     * 判断当前员工是否为平台管理员或具有指定平台权限。
     */
    public boolean hasPlatformPermission(String permission) {
        return isPlatformAdministrator(requireEmployee()) || StpUtil.hasPermission(permission);
    }

    /**
     * 获取当前登录员工，未登录时拒绝继续执行管理操作。
     */
    public RequestEmployee requireEmployee() {
        RequestEmployee employee;
        try {
            employee = AdminRequestUtil.getRequestUser();
        } catch (Exception exception) {
            log.debug("检查应用数据权限时未获取到登录员工", exception);
            employee = null;
        }
        if (employee == null || employee.getEmployeeId() == null) {
            throw new IllegalStateException("当前登录状态已失效");
        }
        return employee;
    }

    /**
     * 查询员工关联的企业主键。
     */
    private List<Long> getEnterpriseIds(RequestEmployee employee) {
        return enterpriseEmployeeDao.selectEnterpriseIdByEmployeeId(employee.getEmployeeId());
    }

    /**
     * 检查平台管理员标识。
     */
    private boolean isPlatformAdministrator(RequestEmployee employee) {
        return Boolean.TRUE.equals(employee.getAdministratorFlag());
    }
}
