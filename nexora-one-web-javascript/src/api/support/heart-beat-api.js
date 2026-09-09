/*
 * 心跳
 *
 * @Author:    NexoraOne-主任：卓大
 * @Date:      2022-09-03 21:55:47
 * @Wechat:    NexoraOne
 * @Email:     NexoraOne
 * @Copyright  NexoraOne （ # ），Since 2012
 */
import { postRequest } from '/@/lib/axios';

export const heartBeatApi = {
  // 分页查询 @author 卓大
  queryList: (param) => {
    return postRequest('/support/heartBeat/query', param);
  },
};
