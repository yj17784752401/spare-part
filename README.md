# 备件出库与库存预警系统

## 项目简介
基于 Spring Boot 2.6.13 + MyBatis-Plus 实现的备件出库管理，支持库存扣减、自动替代备件选择和库存预警。

## 技术栈
- Java 8
- Spring Boot 2.6.13
- MyBatis-Plus 3.4.1
- MySQL (生产) / H2 (测试)
- Lombok

## 快速开始
### 1. 数据库准备
执行 `src/main/resources/db/schema.sql` 创建表结构。
sql里面需要添加
#创建数据库
create database if not exists sparepart;
# 使用数据库
use sparepart;

然后就可以创建表结构了

### 2. 修改配置
编辑 `application.yml` 中的数据库连接信息。

### 3. 构建运行
配置好相关配置后，就可以启动项目，访问   [点击访问](http://localhost:8123/) 也可以使用swagger进行接口测试  [点击访问](http://localhost:8123/doc.html)


### 效果图

<img width="1040" height="785" alt="image" src="https://github.com/user-attachments/assets/8cfb11b4-317b-45d7-b8b6-50c4c93e655a" />

<img width="731" height="1007" alt="image" src="https://github.com/user-attachments/assets/2ee52c8b-b85c-4770-8767-e1f178754527" />

<img width="787" height="851" alt="image" src="https://github.com/user-attachments/assets/c7c528d5-2ef0-44f8-a02a-ea455e3c1fb4" />




