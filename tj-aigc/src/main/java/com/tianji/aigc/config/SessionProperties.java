package com.tianji.aigc.config;

import com.tianji.aigc.vo.SessionVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "tj.ai.session")
@Data
public class SessionProperties {
    private String name;
    private String describe;
    private List<SessionVO.Example> examples;

}
