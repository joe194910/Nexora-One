# NexoraOne应用接入认证

第三方应用不能直接把App Secret当作业务接口令牌使用。正确流程是：

1. 在应用中心创建应用，安全保存一次性展示的App ID和App Secret。
2. 服务端使用App ID和App Secret换取短期Access Token。
3. 携带Access Token请求平台连通性接口。
4. 连通性请求成功后，应用状态才会变为“已接入”。
5. 后续调用开放API时继续携带该Access Token，平台按Token中的API scope授权。

## 1. 换取Access Token

请求：

```http
POST /open/application/oauth/token
Content-Type: application/json

{
  "appId": "app_nxo_20260909_xxxxxxxxxxxx",
  "appSecret": "创建或重置密钥时获得的完整Secret",
  "grantType": "client_credentials"
}
```

也支持OAuth 2.0表单格式：

```bash
curl -X POST "{platformBaseUrl}/open/application/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "app_id={appId}" \
  --data-urlencode "app_secret={appSecret}" \
  --data-urlencode "grant_type=client_credentials"
```

成功响应中的核心字段：

```json
{
  "code": 0,
  "ok": true,
  "data": {
    "access_token": "nxo_at_xxx",
    "token_type": "Bearer",
    "expires_in": 7200,
    "scope": "identity:user:info enterprise:info",
    "scopes": [
      "identity:user:info",
      "enterprise:info"
    ]
  }
}
```

Token有效期读取应用“登录与单点跳转配置”中的`tokenTtl`，平台限制为60到86400秒。

## 2. 验证平台连通性

```bash
curl "{platformBaseUrl}/open/application/connect/ping" \
  -H "Authorization: Bearer {accessToken}"
```

请求成功后，应用的`access_status`会更新为`2`，表示凭证签发和Bearer Token请求链路均已验证通过。

## 3. 调用后续开放API

后续开放API统一使用：

```http
Authorization: Bearer {accessToken}
```

Token scope由两部分组成：

- `permission_level = 1`且已启用的公开API。
- 当前应用申请并审核通过的API。

开放API后端接入时，调用`ApplicationOpenAuthService.authorize`并传入当前API的`apiCode`。未授权、Token过期、应用下架或密钥重置都会拒绝请求。

## 4. 密钥重置

数据库只保存App Secret的SHA-256摘要，不保存明文。重置后密钥版本会递增；旧Access Token即使尚未到期，也会在下一次请求时因为密钥版本不匹配而立即失效。

## 5. 安全要求

- App Secret只允许保存在调用方服务端，不得写入浏览器、移动端或公开仓库。
- 生产环境必须使用HTTPS。
- 不得在日志中打印App Secret或完整Access Token。
- Access Token过期后重新执行`client_credentials`流程，不使用永久Token。
