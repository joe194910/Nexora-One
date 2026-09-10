package com.nexoraone.admin.module.business.application.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationFavoriteForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationLaunchForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationPortalQueryForm;
import com.nexoraone.admin.module.business.application.service.ApplicationPortalService;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 应用市场和用户应用门户接口。
 */
@RestController
@RequestMapping("/application/portal")
@Tag(name = "应用中心-应用门户")
public class ApplicationPortalController {

    @Resource
    private ApplicationPortalService portalService;

    /** 分页查询当前用户可见的应用市场。 */
    @Operation(summary = "查询应用市场")
    @PostMapping("/market/query")
    @SaCheckPermission("application:market")
    public ResponseDTO<PageResult<Map<String, Object>>> market(
            @RequestBody @Valid ApplicationPortalQueryForm form) {
        return portalService.queryMarket(form);
    }

    /** 查询应用市场分类。 */
    @Operation(summary = "查询应用分类")
    @GetMapping("/category/list")
    @SaCheckPermission("application:market")
    public ResponseDTO<List<String>> categories() {
        return portalService.categories();
    }

    /** 查询我的应用、收藏和最近访问。 */
    @Operation(summary = "查询我的应用")
    @GetMapping("/my")
    @SaCheckPermission("application:portal")
    public ResponseDTO<Map<String, Object>> myApplications() {
        return portalService.myApplications();
    }

    /** 查询首页我的应用和开放平台统计。 */
    @Operation(summary = "查询首页应用概况")
    @GetMapping("/home/overview")
    @SaCheckPermission("application:portal")
    public ResponseDTO<Map<String, Long>> homeOverview() {
        return portalService.homeOverview();
    }

    /** 收藏或取消收藏应用。 */
    @Operation(summary = "更新应用收藏")
    @PostMapping("/favorite")
    @SaCheckPermission("application:portal")
    public ResponseDTO<String> favorite(@RequestBody @Valid ApplicationFavoriteForm form) {
        return portalService.favorite(form);
    }

    /** 查询最近访问应用。 */
    @Operation(summary = "查询最近访问")
    @GetMapping("/recent")
    @SaCheckPermission("application:portal")
    public ResponseDTO<List<Map<String, Object>>> recent() {
        return portalService.recent();
    }

    /** 签发一次性授权码并返回应用跳转地址。 */
    @Operation(summary = "进入应用")
    @PostMapping("/launch")
    @SaCheckPermission("application:portal")
    public ResponseDTO<Map<String, Object>> launch(@RequestBody @Valid ApplicationLaunchForm form) {
        return portalService.launch(form);
    }
}
