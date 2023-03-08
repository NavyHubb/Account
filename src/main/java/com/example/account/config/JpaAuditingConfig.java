package com.example.account.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing  // 객체가 생성, 변경될 때 자동으로 값을 넣어줌
public class JpaAuditingConfig {
}
