package com.example.IdentityManagementService;

import com.example.IdentityManagementService.common.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableFeignClients(basePackages = "com.example.IdentityManagementService.client")
@EnableAspectJAutoProxy
@EnableConfigurationProperties(CorsProperties.class)
@SpringBootApplication(scanBasePackages = {
		"com.example.IdentityManagementService",
})
public class IdentityManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityManagementServiceApplication.class, args);
	}

}
