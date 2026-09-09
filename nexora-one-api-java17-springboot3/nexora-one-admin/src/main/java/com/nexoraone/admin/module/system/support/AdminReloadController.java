package com.nexoraone.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.nexoraone.base.common.controller.SupportBaseController;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.constant.SwaggerTagConst;
import com.nexoraone.base.module.support.reload.ReloadService;
import com.nexoraone.base.module.support.reload.domain.ReloadForm;
import com.nexoraone.base.module.support.reload.domain.ReloadItemVO;
import com.nexoraone.base.module.support.reload.domain.ReloadResultVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * reload (内存热加载、钩子等)
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2015-03-02 19:11:52
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.RELOAD)
public class AdminReloadController extends SupportBaseController {

    @Resource
    private ReloadService reloadService;

    @Operation(summary = "查询reload列表 @author 开云")
    @GetMapping("/reload/query")
    public ResponseDTO<List<ReloadItemVO>> query() {
        return reloadService.query();
    }

    @Operation(summary = "获取reload result @author 开云")
    @GetMapping("/reload/result/{tag}")
    @SaCheckPermission("support:reload:result")
    public ResponseDTO<List<ReloadResultVO>> queryReloadResult(@PathVariable("tag") String tag) {
        return reloadService.queryReloadItemResult(tag);
    }

    @Operation(summary = "通过tag更新标识 @author 开云")
    @PostMapping("/reload/update")
    @SaCheckPermission("support:reload:update")
    public ResponseDTO<String> updateByTag(@RequestBody @Valid ReloadForm reloadForm) {
        return reloadService.updateByTag(reloadForm);
    }
}
