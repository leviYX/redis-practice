# 授权认证中心

## 介绍

授权认证采用SpringSecurity + OAuth2搭建，将登录用户的令牌以及登录信息存入Redis中，确保数据能快速读取，提高性能。

本授权认证中心采用password、refresh_token模式，在Redis缓存中共存储了9个键值对，其中有5个跟access_token相关，4个和refresh_token相关。

- access_token相关access：(OAuth2AccessToken)、auth：(OAuth2Authentication)、auth_to_access:(OAuth2AccessToken)、client_id_to_access:(OAuth2AccessToken)、uname_to_access:(OAuth2AccessToken)

- refresh_token相关refresh:(OAuth2RefreshToken)、refresh_auth:(OAuth2Authentication)、access_to_refresh(refresh_token):、refresh_to_access:(refresh_token)

  

![](/README.assets/1605500149687.png)



## 模块介绍

- 在初始化项目下添加**ms-oauth2-server**微服务，在里面使用SpringSecurity + OAuth完成授权认证模块
- 在ms-gateway微服务中利用Filter完成网关登录认证
- 在ms-diners中完成客户端Diner的登录



## Redis数据存储

- access:存储键是access: + access_token（OAuth2AccessToken）保存的是OAuth2AccessToken序列化数据
- access_to_refresh:存储键是access_to_refresh:+access_token，值是refresh_token值
- auth:存储键是auth:access_token，值是OAuth2Authentication对象序列化后的值（认证后相关数据）

- auth_to_access:存储键是auth_to_access:+ username、client_id、scope三个MD5加密后的值，保存OAuth2AccessToken序列化值
- client_id_to_access:存储的键是：client_id_to_access:+clientId，值是OAuth2AccessToken序列化后的值
- refresh:存储的是refresh:+refresh_token，值是OAuth2RefreshToken对象序列化后的值

- refresh_auth:存储的是refresh_auth:refresh_token,值是OAuth2Authentication序列化后的值

- access_to_refresh:存储的key是access_to_refresh:+access_token，值是refresh_token值

- refresh_to_access:存储的键是refresh_to_access:refresh_token，值是refresh后的refresh_token值

- uname_to_access:存储键是uname_to_access:client_password:user，值是OAuth2AccessToken序列化后值



## 测试访问

### 测试授权

访问时设置 `Basic Auth` 和 `Body` 请求体信息。

![](/README.assets/image-20201113175527486.png)

![](/README.assets/image-20201113175603320.png)

返回结果：

```shell
{
    "code": 1,
    "message": "Successful.",
    "path": "/oauth/token",
    "data": {
        "nickname": "test",
        "avatarUrl": "/123",
        "accessToken": "dc7c8ba6-efe2-4f80-afec-27dda52f4409",
        "expireIn": 3579,
        "scopes": [
            "api"
        ],
        "refreshToken": "01f9db59-7416-400f-9f7b-d9ee81ad37e5"
    }
}
```



### 测试登录

访问：http://localhost:8081/signin?account=test&password=123456

![](README.assets\image-20201114094540250.png)

返回结果：

```shell
{
    "code": 1,
    "message": "Successful.",
    "path": "/signin",
    "data": {
        "nickname": "test",
        "token": "74cb18de-0645-4f3b-96bc-df6856eaee31",
        "avatarUrl": "/123"
    }
}
```



### 获取当前用户

先通过登录逻辑调用认证中心生成 token。

访问：http://localhost:8081/signin?account=test&password=123456

![](/README.assets/image-20201114112834889.png)

然后测试用户中心是否可用。

获取当前用户信息。

访问：http://localhost:8082/user/me?access_token=

![](/README.assets/image-20201114112638457.png)

安全退出。

访问：http://localhost:8082/user/logout?access_token=

![](/README.assets/image-20201114112908656.png)



### 网关测试

访问：http://localhost/diners/signin?account=test&password=123456

![](/README.assets/image-20201114120232901.png)

访问：http://localhost/auth/user/me?access_token=

![](/README.assets/image-20201114120313372.png)

访问：http://localhost/auth/user/logout?access_token=



![](/README.assets/image-20201114120558506.png)

## 总结

![](/README.assets/image-20201115164057228.png)



![](/README.assets/image-20201115164136838.png)

