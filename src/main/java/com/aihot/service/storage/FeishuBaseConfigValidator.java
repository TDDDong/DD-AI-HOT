package com.aihot.service.storage;

import com.aihot.config.properties.FeishuProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FeishuBaseConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(FeishuBaseConfigValidator.class);

    private final FeishuProperties feishuProperties;

    public FeishuBaseConfigValidator(FeishuProperties feishuProperties) {
        this.feishuProperties = feishuProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        if (feishuProperties.getBase().isConfigured()) {
            log.info("飞书 Base 已配置: table-id={}", feishuProperties.getBase().getTableId());
        } else {
            log.warn("飞书 Base 未配置 app-token/table-id，存储模块暂不可用（见 feishu.yaml feishu.base）");
        }
    }
}
