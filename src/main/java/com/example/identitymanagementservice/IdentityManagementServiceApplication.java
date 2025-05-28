package com.example.identitymanagementservice;

import com.example.identitymanagementservice.common.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableFeignClients(basePackages = "com.example.IdentityManagementService.client")
@EnableAspectJAutoProxy
@EnableConfigurationProperties(CorsProperties.class)
@SpringBootApplication(scanBasePackages = {"com.example.identitymanagementservice",
})
public class IdentityManagementServiceApplication {
   public static void main(String[] args) {
      SpringApplication.run(IdentityManagementServiceApplication.class, args);
  }

}
