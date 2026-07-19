# syntax=docker/dockerfile:1
# chunk-2d：Java 单体多阶段构建（maven install → eclipse-temurin:21-jre 运行）。
# build context = shiwujie-backend/（compose 配 context: ../shiwujie-backend），故此处路径相对该目录。
# 产物 = shiwujieBootstrap-0.0.1-SNAPSHOT.jar（唯一 repackage，spring-boot-maven-plugin）。

# ── stage 1: build（maven:3.9 + JDK21）──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# 先拷父 pom + 两子模块 pom，预下载依赖（缓存层；改动源码不重下依赖）
COPY pom.xml ./
COPY shiwujie-model/pom.xml ./shiwujie-model/pom.xml
COPY shiwujie-bootstrap/pom.xml ./shiwujie-bootstrap/pom.xml
RUN mvn -pl shiwujie-model,shiwujie-bootstrap -am dependency:go-offline -q || true
# 拷全量源码 → 真 install（跳测试：Docker 构建不跑 286 单测，CI/本地 mvn test 另行）
COPY shiwujie-model/ ./shiwujie-model/
COPY shiwujie-bootstrap/ ./shiwujie-bootstrap/
RUN mvn install -DskipTests -q

# ── stage 2: runtime（JRE21 only，镜像小）──
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar /app/app.jar
# 8100 = HTTP /api/** + WS /api/ws/call + MCP /mcp（公网发布，见 docker-compose.yml）
EXPOSE 8100
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
