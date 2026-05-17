# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
# 使用阿里云镜像加速 Maven 依赖下载
RUN echo '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><name>aliyun maven</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > settings.xml
COPY pom.xml .
COPY src ./src
RUN mvn -s settings.xml clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
