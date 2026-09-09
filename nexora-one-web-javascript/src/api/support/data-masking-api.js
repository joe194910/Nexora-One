/**
 *  数据脱敏api
 *
 * @Author:    NexoraOne-主任-卓大
 * @Date:      2024-07-31 21:02:37
 * @Copyright  NexoraOne
 */
import { getRequest } from '/@/lib/axios';

export const dataMaskingApi = {
  /**
   * 查询脱敏数据
   */
  query: () => {
    return getRequest('/support/dataMasking/demo/query');
  },
};
