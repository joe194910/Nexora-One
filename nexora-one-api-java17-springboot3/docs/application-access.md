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
POST /open-api/oauth/token
Content-Type: application/json

{
  "appId": "app_nxo_20260909_xxxxxxxxxxxx",
  "appSecret": "创建或重置密钥时获得的完整Secret",
  "grantType": "client_credentials"
}
```

也支持OAuth 2.0表单格式：

```bash
curl -X POST "{platformBaseUrl}/open-api/oauth/token" \
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
curl "{platformBaseUrl}/open-api/connect/ping" \
  -H "Authorization: Bearer {accessToken}"
```

请求成功后，应用的`access_status`会更新为`2`，表示凭证签发和Bearer Token请求链路均已验证通过。

## 3. 调用后续开放API

后续开放API至少携带：

```http
Authorization: Bearer {accessToken}
X-App-Id: {appId}
```

Token scope由两部分组成：

- `permission_level = 1`且已启用的公开API。
- 当前应用申请并审核通过的API。

开放API后端接入时，网关会按API的`apiCode`校验授权。未授权、Token过期、应用下架或密钥重置都会拒绝请求。

应用安全配置支持三种鉴权模式：

- `SIGNATURE`：Access Token、App ID和请求签名，默认且推荐。
- `TOKEN`：Access Token和App ID，不校验请求签名。
- `IP`：Access Token、App ID和来源IP白名单，不校验请求签名。

网关会使用标准HTTP状态码表达调用结果：

- `401`：Access Token、App ID或请求签名无效。
- `403`：应用没有接口权限、来源IP不在白名单内，或请求违反HTTPS安全策略。
- `404`：没有找到匹配的已上架API路由。
- `429`：超过每秒请求上限、每日调用上限或授权额度。
- `502`：下游业务服务调用失败。
- `503`：API未配置可用的生产环境地址。

当使用`SIGNATURE`模式时，还需要携带应用安全配置中指定的时间戳、随机串和签名请求头。默认请求头为：

```http
X-Timestamp: {当前毫秒时间戳}
X-Nonce: {本次请求唯一随机串}
X-Signature: {小写十六进制签名}
```

规范请求字符串为：

```text
HTTP_METHOD + "\n"
+ REQUEST_TARGET + "\n"
+ TIMESTAMP + "\n"
+ NONCE + "\n"
+ SHA256_HEX(REQUEST_BODY)
```

其中`REQUEST_TARGET`必须使用客户端实际发送的原始请求路径。存在查询参数时，必须包含问号和原始查询串，例如：

```text
/open-api/v1/employees/10001?fields=name,email&active=true
```

签名密钥和签名值按以下规则生成：

```text
SIGNING_KEY = SHA256_HEX(APP_SECRET)
SIGNATURE = HEX_LOWER(HMAC(SIGNING_KEY, CANONICAL_REQUEST))
```

HMAC算法使用应用安全配置选择的`HMAC-SHA256`或`HMAC-SHA512`。请求体为空时，对空字节数组计算SHA-256。时间戳允许偏差和随机串防重放有效期由应用安全配置决定。

## 4. 密钥重置

数据库只保存App Secret的SHA-256摘要，不保存明文。重置后密钥版本会递增；旧Access Token即使尚未到期，也会在下一次请求时因为密钥版本不匹配而立即失效。

## 5. 安全要求

- App Secret只允许保存在调用方服务端，不得写入浏览器、移动端或公开仓库。
- 生产环境必须使用HTTPS。
- 平台通过Servlet解析后的请求协议和客户端地址执行HTTPS与IP白名单校验。反向代理必须覆盖并清理客户端传入的`Forwarded`、`X-Forwarded-*`请求头，且应用服务不得绕过受信任代理直接暴露到公网。
- 不得在日志中打印App Secret或完整Access Token。
- Access Token过期后重新执行`client_credentials`流程，不使用永久Token。
