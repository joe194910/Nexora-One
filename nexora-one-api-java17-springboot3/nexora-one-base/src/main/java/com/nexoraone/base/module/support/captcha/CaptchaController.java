package com.nexoraone.base.module.support.captcha;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.nexoraone.base.common.controller.SupportBaseController;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.constant.SwaggerTagConst;
import com.nexoraone.base.module.support.captcha.domain.CaptchaVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码业务
 *
 * @Author NexoraOne: 胡克
 * @Date 2021-09-02 20:21:10
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright <a href="#">NexoraOne</a>
 */
@Tag(name = SwaggerTagConst.Support.CAPTCHA)
@RestController
public class CaptchaController extends SupportBaseController {

    @Resource
    private CaptchaService captchaService;

    @Operation(summary = "获取图形验证码 @author 胡克")
    @GetMapping("/captcha")
    public ResponseDTO<CaptchaVO> generateCaptcha() {
        return ResponseDTO.ok(captchaService.generateCaptcha());
    }

}
