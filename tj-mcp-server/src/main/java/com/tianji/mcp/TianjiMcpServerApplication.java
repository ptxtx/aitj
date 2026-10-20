package com.tianji.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 天机学堂 MCP Server。
 *
 * <p>该进程只负责对外暴露经过审核的 MCP 工具，不加载大模型，也不保存会话。</p>
 */
@SpringBootApplication
public class TianjiMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TianjiMcpServerApplication.class, args);
    }
}
