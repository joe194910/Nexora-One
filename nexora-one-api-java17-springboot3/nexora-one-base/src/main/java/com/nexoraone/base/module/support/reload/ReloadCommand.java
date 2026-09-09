package com.nexoraone.base.module.support.reload;

import jakarta.annotation.Resource;
import com.nexoraone.base.common.util.SmartBeanUtil;
import com.nexoraone.base.module.support.reload.core.AbstractSmartReloadCommand;
import com.nexoraone.base.module.support.reload.core.domain.SmartReloadItem;
import com.nexoraone.base.module.support.reload.core.domain.SmartReloadResult;
import com.nexoraone.base.module.support.reload.dao.ReloadItemDao;
import com.nexoraone.base.module.support.reload.dao.ReloadResultDao;
import com.nexoraone.base.module.support.reload.domain.ReloadItemEntity;
import com.nexoraone.base.module.support.reload.domain.ReloadResultEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * reload 操作
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2015-03-02 19:11:52
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Component
public class ReloadCommand extends AbstractSmartReloadCommand {

    @Resource
    private ReloadItemDao reloadItemDao;

    @Resource
    private ReloadResultDao reloadResultDao;

    /**
     * 读取数据库中SmartReload项
     *
     * @return List<ReloadItem>
     */
    @Override
    public List<SmartReloadItem> readReloadItem() {
        List<ReloadItemEntity> reloadItemEntityList = reloadItemDao.selectList(null);
        return SmartBeanUtil.copyList(reloadItemEntityList, SmartReloadItem.class);
    }


    /**
     * 保存reload结果
     *
     * @param smartReloadResult
     */
    @Override
    public void handleReloadResult(SmartReloadResult smartReloadResult) {
        ReloadResultEntity reloadResultEntity = SmartBeanUtil.copy(smartReloadResult, ReloadResultEntity.class);
        reloadResultDao.insert(reloadResultEntity);
    }
}
