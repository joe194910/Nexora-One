package com.nexoraone.admin.module.business.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationApiPermissionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationReviewDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationVersionDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationApiPermissionForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationReviewForm;
import com.nexoraone.admin.module.business.application.manager.ApplicationCredentialManager;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartRequestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 应用中心授权与审核边界单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationDao applicationDao;
    @Mock
    private ApplicationCredentialDao credentialDao;
    @Mock
    private OpenApiDao openApiDao;
    @Mock
    private ApplicationApiPermissionDao permissionDao;
    @Mock
    private ApplicationReviewDao reviewDao;
    @Mock
    private ApplicationVersionDao versionDao;
    @Mock
    private ApplicationCredentialManager credentialManager;
    @Mock
    private ApplicationDataScopeService applicationDataScopeService;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private ApplicationService applicationService;

    /**
     * 公开API应自动授权，受控API必须进入平台审核。
     */
    @Test
    void shouldSeparatePublicAndControlledApiPermissionStates() {
        ApplicationEntity application = editableApplication();
        when(applicationDao.selectById(10L)).thenReturn(application);
        when(applicationDataScopeService.canManage(application)).thenReturn(true);
        when(openApiDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                publishedApi(101L, 1),
                publishedApi(102L, 2)));
        when(permissionDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        prepareRequestEmployee();

        ApplicationApiPermissionForm form = new ApplicationApiPermissionForm();
        form.setApplicationId(10L);
        form.setOpenApiIdList(List.of(101L, 102L));
        form.setApplyReason("接入企业业务系统");

        ResponseDTO<String> result = applicationService.saveApiPermissions(form);

        assertTrue(result.getOk());
        ArgumentCaptor<ApplicationApiPermissionEntity> captor =
                ArgumentCaptor.forClass(ApplicationApiPermissionEntity.class);
        verify(permissionDao, org.mockito.Mockito.times(2)).insert(captor.capture());
        ApplicationApiPermissionEntity publicPermission = captor.getAllValues().stream()
                .filter(item -> item.getOpenApiId().equals(101L))
                .findFirst()
                .orElseThrow();
        ApplicationApiPermissionEntity controlledPermission = captor.getAllValues().stream()
                .filter(item -> item.getOpenApiId().equals(102L))
                .findFirst()
                .orElseThrow();

        assertEquals(2, publicPermission.getApplyStatus());
        assertEquals("公开API自动授权", publicPermission.getReviewRemark());
        assertNotNull(publicPermission.getEffectiveTime());
        assertEquals(1, controlledPermission.getApplyStatus());
        assertNull(controlledPermission.getReviewerId());
        assertNull(controlledPermission.getEffectiveTime());
    }

    /**
     * 应用上架审核不得代替API权限审核或批量修改授权结果。
     */
    @Test
    void shouldNotChangeApiPermissionsWhenReviewingApplicationListing() {
        ApplicationEntity application = new ApplicationEntity();
        application.setApplicationId(20L);
        application.setListingStatus(1);
        application.setConfigLocked(true);
        when(applicationDao.selectById(20L)).thenReturn(application);
        when(versionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        prepareRequestEmployee();

        ApplicationReviewForm form = new ApplicationReviewForm();
        form.setApplicationId(20L);
        form.setReviewStatus(2);
        form.setReviewRemark("应用资料审核通过");

        ResponseDTO<String> result = applicationService.review(form);

        assertTrue(result.getOk());
        assertEquals(2, application.getListingStatus());
        verifyNoInteractions(permissionDao);
    }

    /**
     * 构造允许继续编辑的应用。
     */
    private ApplicationEntity editableApplication() {
        ApplicationEntity application = new ApplicationEntity();
        application.setApplicationId(10L);
        application.setWorkflowStep(4);
        application.setConfigLocked(false);
        return application;
    }

    /**
     * 构造已发布且可申请的API。
     */
    private OpenApiEntity publishedApi(Long openApiId, Integer permissionLevel) {
        OpenApiEntity api = new OpenApiEntity();
        api.setOpenApiId(openApiId);
        api.setPermissionLevel(permissionLevel);
        api.setStatus(4);
        api.setEnabledFlag(true);
        return api;
    }

    /**
     * 设置与正式请求一致的当前员工上下文。
     */
    private void prepareRequestEmployee() {
        RequestEmployee employee = new RequestEmployee();
        employee.setEmployeeId(10001L);
        employee.setActualName("测试管理员");
        SmartRequestUtil.setRequestUser(employee);
    }

    /**
     * 清理线程中的登录员工，避免测试之间相互影响。
     */
    @AfterEach
    void cleanUp() {
        SmartRequestUtil.remove();
    }
}
