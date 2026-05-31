# 拼团营销系统微服务改造记录

## 1. 项目背景

这是一个拼团营销学习项目，核心业务包括活动配置、优惠试算、用户锁单、支付结算、成团通知、超时退款和人群标签筛选。

## 2. 当前架构

当前项目是 Maven 多模块单体架构：

- xuele-group-buy-api：对外接口契约
- xuele-group-buy-app：启动模块
- xuele-group-buy-domain：领域业务逻辑
- xuele-group-buy-infrastructure：数据库、Redis、MQ 等基础设施适配
- xuele-group-buy-trigger：HTTP、MQ Listener、定时任务入口
- xuele-group-buy-types：通用类型、枚举、异常

## 3. 当前核心业务流程

### 3.1 营销试算

用户进入商品页后，系统根据用户、商品、渠道、活动、人群标签计算可见性、可购买性和优惠价格。

### 3.2 锁单

用户发起拼团后，系统校验活动、库存、拼团队伍容量、用户参与次数，并生成待支付订单。

### 3.3 结算

支付成功后，系统更新订单状态，判断是否成团，成团后发送通知任务。

### 3.4 退款

超时未支付或业务退款时，系统恢复锁定库存，并维护订单状态。

## 4. 微服务拆分目标

计划按业务边界拆分为：

- gateway：统一入口
- tag-service：人群标签服务
- activity-service：活动与营销试算服务
- trade-service：拼团交易服务
- notify-service：通知补偿服务，后期拆分

## 5. 改造原则

- 先补强单体，再拆微服务
- 按业务边界拆，不按技术层拆
- 交易核心最后拆
- 优先使用本地事务 + MQ 最终一致性
- 所有跨服务调用必须考虑超时、重试、熔断、降级和幂等

## 6. 技术点规划

- Spring Boot 3.x
- Spring Cloud Alibaba
- Nacos 注册配置中心
- Spring Cloud Gateway
- OpenFeign
- Sentinel
- RabbitMQ
- Redis / Redisson
- MyBatis
- Flyway
- Docker
- OpenTelemetry
- Prometheus + Grafana