package com.nexoraone.base.module.support.mail;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.base.module.support.mail.domain.MailTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * 邮件模板
 *
 * @Author NexoraOne-创始人兼主任:卓大
 * @Date 2024/8/5
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright <a href="#">NexoraOne</a> ，Since 2012
 */
@Mapper
public interface MailTemplateDao extends BaseMapper<MailTemplateEntity> {

}
