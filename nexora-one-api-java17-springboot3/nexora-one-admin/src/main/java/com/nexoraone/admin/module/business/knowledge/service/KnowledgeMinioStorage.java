package com.nexoraone.admin.module.business.knowledge.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import java.net.URI;

/** 独立的 MinIO 文件服务，不改变系统原有文件存储模式。 */
@Service
public class KnowledgeMinioStorage {
    @Value("${knowledge.minio.endpoint:${KNOWLEDGE_MINIO_ENDPOINT:}}") private String endpoint;
    @Value("${knowledge.minio.bucket:${KNOWLEDGE_MINIO_BUCKET:nexora-knowledge}}") private String bucket;
    @Value("${knowledge.minio.access-key:${KNOWLEDGE_MINIO_ACCESS_KEY:}}") private String accessKey;
    @Value("${knowledge.minio.secret-key:${KNOWLEDGE_MINIO_SECRET_KEY:}}") private String secretKey;
    @Value("${knowledge.minio.region:${KNOWLEDGE_MINIO_REGION:us-east-1}}") private String region;
    @Value("${knowledge.minio.path-style-access:${KNOWLEDGE_MINIO_PATH_STYLE_ACCESS:true}}")
    private boolean pathStyleAccess;

    /** 上传文档对象，首次使用时检测存储桶是否已创建。 */
    public void put(String key, byte[] bytes, String contentType) {
        try (S3Client client = client()) {
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception exception) {
                if (exception.statusCode() != 404) throw exception;
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            }
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream")).build(),
                    RequestBody.fromBytes(bytes));
        }
    }

    /** 下载源文件，供所有者下载或恢复失效的本地解析工作文件。 */
    public byte[] get(String key) {
        try (S3Client client = client()) {
            return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        }
    }

    /** 仅在没有知识库引用时删除对象。 */
    public void delete(String key) {
        try (S3Client client = client()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
    }

    /** 使用专用凭据与路径式寻址创建 MinIO 客户端。 */
    private S3Client client() {
        if (StrUtil.hasBlank(endpoint, accessKey, secretKey))
            throw new IllegalStateException("请配置 knowledge.minio.endpoint、access-key 和 secret-key");
        return S3Client.builder().region(Region.of(region)).endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build()).build();
    }
}
