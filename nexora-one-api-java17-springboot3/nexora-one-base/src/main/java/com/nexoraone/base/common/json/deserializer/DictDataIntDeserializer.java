package com.nexoraone.base.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 字典值为 int类型的 反序列化
 *
 * @Author NexoraOne: 卓大
 * @Date 2026-04-05 22:17:53
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright <a href="#">NexoraOne</a>
 */
@Slf4j
public class DictDataIntDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode listOrObjectNode = objectCodec.readTree(jsonParser);
        return Integer.parseInt(listOrObjectNode.asText());
    }

}
