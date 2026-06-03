# 备件出库与库存预警系统

## 项目简介
基于 Spring Boot 2.5 + MyBatis-Plus 实现的备件出库管理，支持库存扣减、自动替代备件选择和库存预警。

## 技术栈
- Java 8
- Spring Boot 2.5.12
- MyBatis-Plus 3.4.1
- MySQL (生产) / H2 (测试)
- Lombok

## 快速开始
### 1. 数据库准备
执行 `src/main/resources/db/schema.sql` 创建表结构。

### 2. 修改配置
编辑 `application.yml` 中的数据库连接信息。

### 3. 构建运行
```bash
mvn clean package -DskipTests
java -jar target/spare-part-outbound-1.0.0.jar