//package com.example.IdentityManagementService.config;
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import software.amazon.awssdk.services.ses.SesClient;
//import software.amazon.awssdk.regions.Region;
//
//@Configuration
//public class AwsConfig {
//
//    @Bean
//    public SesClient sesClient() {
//        return SesClient.builder()
//                .region(Region.of("us-east-1"))
//                .build();
//    }
//}
