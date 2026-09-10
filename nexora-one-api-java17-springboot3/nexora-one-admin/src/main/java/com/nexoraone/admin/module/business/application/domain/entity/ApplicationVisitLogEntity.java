package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户进入应用的访问日志。
 */
@Data
@TableName("nexora_one_application_visit_log")
public class ApplicationVisitLogEntity {

    /** 访问日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long visitLogId;
    /** 应用主键。 */
    private Long applicationId;
    /** 应用名称快照。 */
    private String applicationName;
    /** 员工主键。 */
    private Long employeeId;
    /** 员工姓名快照。 */
    private String employeeName;
    /** 部门主键。 */
    private Long departmentId;
    /** 部门名称快照。 */
    private String departmentName;
    /** 最终跳转地址。 */
    private String launchUrl;
    /** 是否访问成功。 */
    private Boolean successFlag;
    /** 失败原因。 */
    private String failureReason;
    /** 请求IP。 */
    private String ipAddress;
    /** 浏览器标识。 */
    private String userAgent;
    /** 访问时间。 */
    private LocalDateTime visitTime;
}
