package com.nexoraone.base.module.support.reload;

import jakarta.annotation.Resource;
import com.nexoraone.base.common.code.UserErrorCode;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.module.support.reload.dao.ReloadItemDao;
import com.nexoraone.base.module.support.reload.dao.ReloadResultDao;
import com.nexoraone.base.module.support.reload.domain.ReloadForm;
import com.nexoraone.base.module.support.reload.domain.ReloadItemEntity;
import com.nexoraone.base.module.support.reload.domain.ReloadItemVO;
import com.nexoraone.base.module.support.reload.domain.ReloadResultVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
@Service
public class ReloadService {

    @Resource
    private ReloadItemDao reloadItemDao;

    @Resource
    private ReloadResultDao reloadResultDao;

    /**
     * 查询
     *
     * @return
     */
    public ResponseDTO<List<ReloadItemVO>> query() {
        List<ReloadItemVO> list = reloadItemDao.query();
        return ResponseDTO.ok(list);
    }

    public ResponseDTO<List<ReloadResultVO>> queryReloadItemResult(String tag) {
        List<ReloadResultVO> reloadResultList = reloadResultDao.query(tag);
        return ResponseDTO.ok(reloadResultList);
    }


    /**
     * 通过标签更新标识符
     *
     * @param reloadForm
     * @return
     */
    public ResponseDTO<String> updateByTag(ReloadForm reloadForm) {
        ReloadItemEntity reloadItemEntity = reloadItemDao.selectById(reloadForm.getTag());
        if (null == reloadItemEntity) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        reloadItemEntity.setIdentification(reloadForm.getIdentification());
        reloadItemEntity.setUpdateTime(LocalDateTime.now());
        reloadItemEntity.setArgs(reloadForm.getArgs());
        reloadItemDao.updateById(reloadItemEntity);
        return ResponseDTO.ok();
    }
}
