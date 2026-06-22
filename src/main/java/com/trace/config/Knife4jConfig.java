package com.trace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (Swagger) 接口文档配置。
 * <p>访问地址：<a href="http://localhost:8080/doc.html">http://localhost:8080/doc.html</a></p>
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI traceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trace 系统接口文档")
                        .description("""
                                Trace 个人成长轨迹记录与分析系统 API 文档。
                                
                                **功能模块：**
                                - 用户认证（注册/登录/JWT）
                                - AI 知识问答（SSE 流式对话）
                                - 刷题练习（题库抽题/AI 判题）
                                - 成长日记（CRUD）
                                - 学习计划（AI 生成/打卡）
                                - 周报生成（自动/手动）
                                - 知识库管理（上传/向量检索）
                                - 成长仪表盘（评分/热力图/趋势）
                                - 长期记忆（自动提取）
                                                                
                                **认证方式：** 登录后获取 Token，在请求头中添加 `Authorization: Bearer <token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Trace 开发团队")
                                .email("trace@example.com"))
                        .license(new License()
                                .name("内部使用")
                                .url("https://github.com")));
    }
}
