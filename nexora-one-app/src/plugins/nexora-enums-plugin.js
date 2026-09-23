/*
 * 枚举插件
 * 此插件为 NexoraOne 自创的插件
 *
 * @Author:    NexoraOne-主任：卓大
 * @Date:      2022-09-06 20:51:03
 * @Wechat:    NexoraOne
 * @Email:     NexoraOne
 * @Copyright  NexoraOne （ # ），Since 2012
 */
import _ from 'lodash';
import { FLAG_NUMBER_ENUM } from '@/constants/common-const';

export default {
  install: (app, nexoraEnumWrapper) => {
    const nexoraEnumPlugin = {};
    /**
     * 根据枚举值获取描述
     * @param {*} constantName 枚举名
     * @param {*} value          枚举值
     * @returns
     */
    nexoraEnumPlugin.getDescByValue = function (constantName, value) {
      if (!nexoraEnumWrapper || !Object.prototype.hasOwnProperty.call(nexoraEnumWrapper, constantName)) {
        return '';
      }
      // boolean类型需要做特殊处理
      if (constantName === 'FLAG_NUMBER_ENUM' && !_.isUndefined(value) && typeof value === 'boolean') {
        value = value ? FLAG_NUMBER_ENUM.TRUE.value : FLAG_NUMBER_ENUM.FALSE.value;
      }

      let nexoraEnum = nexoraEnumWrapper[constantName];
      for (let item in nexoraEnum) {
        if (nexoraEnum[item].value === value) {
          return nexoraEnum[item].desc;
        }
      }
      return '';
    };
    /**
     * 根据枚举值获取对象
     * @param {*} constantName 枚举名
     * @param {*} value          枚举值
     * @returns
     */
    nexoraEnumPlugin.getObjectByValue = function (constantName, value) {
      if (!nexoraEnumWrapper || !Object.prototype.hasOwnProperty.call(nexoraEnumWrapper, constantName)) {
        return '';
      }

      let nexoraEnum = nexoraEnumWrapper[constantName];
      for (let item in nexoraEnum) {
        if (nexoraEnum[item].value === value) {
          return nexoraEnum[item];
        }
      }
      return null;
    };
    /**
     * 根据枚举名获取对应的描述键值对[{value:desc}]
     * @param {*} constantName 枚举名
     * @returns
     */
    nexoraEnumPlugin.getValueDescList = function (constantName) {
      if (!Object.prototype.hasOwnProperty.call(nexoraEnumWrapper, constantName)) {
        return [];
      }
      const result = [];
      let targetNexoraEnum = nexoraEnumWrapper[constantName];
      for (let item in targetNexoraEnum) {
        result.push(targetNexoraEnum[item]);
      }
      return result;
    };

    /**
     * 根据枚举名获取对应的value描述键值对{value:desc}
     * @param {*} constantName 枚举名
     * @returns
     */
    nexoraEnumPlugin.getValueDesc = function (constantName) {
      if (!Object.prototype.hasOwnProperty.call(nexoraEnumWrapper, constantName)) {
        return {};
      }
      let nexoraEnum = nexoraEnumWrapper[constantName];
      let result = {};
      for (let item in nexoraEnum) {
        let key = nexoraEnum[item].value + '';
        result[key] = nexoraEnum[item].desc;
      }
      return result;
    };

    app.config.globalProperties.$nexoraEnumPlugin = nexoraEnumPlugin;
    app.provide('nexoraEnumPlugin', nexoraEnumPlugin);
  },
};
