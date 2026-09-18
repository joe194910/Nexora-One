package com.nexoraone.admin.module.business.knowledge.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.knowledge.domain.*;
import org.apache.ibatis.annotations.Mapper;

/** 用户文档数据访问。 */
public final class KnowledgeMappers {
    private KnowledgeMappers() {}
    /** 文档映射器。 */
    @Mapper public interface DocumentDao extends BaseMapper<KnowledgeDocument> {}
    /** 入库上下文映射器。 */
    @Mapper public interface ContextDao extends BaseMapper<KnowledgeIngestContext> {}
    /** 业务知识库映射器。 */
    @Mapper public interface BaseDao extends BaseMapper<KnowledgeBase> {}
    /** 文档关联映射器。 */
    @Mapper public interface BaseDocumentDao extends BaseMapper<KnowledgeBaseDocument> {}
    /** 助手映射器。 */
    @Mapper public interface AssistantDao extends BaseMapper<KnowledgeAssistant> {}
    /** 智能助手收藏映射器。 */
    @Mapper public interface AssistantFavoriteDao extends BaseMapper<KnowledgeAssistantFavorite> {}
    /** 助手关联映射器。 */
    @Mapper public interface AssistantBaseDao extends BaseMapper<KnowledgeAssistantBase> {}
    /** 会话映射器。 */
    @Mapper public interface ConversationDao extends BaseMapper<KnowledgeConversation> {}
    /** 消息映射器。 */
    @Mapper public interface MessageDao extends BaseMapper<KnowledgeMessage> {}
}
