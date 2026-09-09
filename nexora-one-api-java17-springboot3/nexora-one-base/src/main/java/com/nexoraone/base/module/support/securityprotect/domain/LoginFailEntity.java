package com.nexoraone.base.module.support.securityprotect.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录失败记录
 *
 * @Author NexoraOne-主任:卓大
 * @Date 2023/10/11 19:29:18
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>，Since 2012
 */

@Data
@Builder
@TableName("t_login_fail")
@AllArgsConstructor
@NoArgsConstructor
public class LoginFailEntity {


    @TableId(type = IdType.AUTO)
    private Long loginFailId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 锁定状态
     */
    private Boolean lockFlag;

    /**
     * 登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 连续登录失败锁定开始时间
     */
    private LocalDateTime loginLockBeginTime;


    private LocalDateTime updateTime;

    private LocalDateTime createTime;


}
