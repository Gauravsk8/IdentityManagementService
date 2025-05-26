package com.example.IdentityManagementService.common.email.service.serviceImpl;



import com.example.IdentityManagementService.common.email.EmailProvider;
import com.example.IdentityManagementService.common.email.service.EmailService;

import java.util.Map;

public class DynamicEmailService implements EmailService {

    private final JavaMailEmailService javaMailEmailService;
    private final AwsSesEmailService awsSesEmailService;
    private EmailProvider provider = EmailProvider.JAVA_MAIL;

    public DynamicEmailService(JavaMailEmailService javaMailEmailService, AwsSesEmailService awsSesEmailService) {
        this.javaMailEmailService = javaMailEmailService;
        this.awsSesEmailService = awsSesEmailService;
    }

    @Override
    public void sendEmail(String to, String subject, String text) {
        getDelegate().sendEmail(to, subject, text);
    }

    @Override
    public String loadTemplate(String templateName, Map<String, String> variables) {
        return getDelegate().loadTemplate(templateName, variables);
    }

    @Override
    public void setProvider(EmailProvider provider) {
        this.provider = provider;
    }

    private EmailService getDelegate() {
        return switch (provider) {
            case JAVA_MAIL -> javaMailEmailService;
            case AWS_SES -> {
                if (awsSesEmailService == null) {
                    throw new IllegalStateException("AWS SES Email Service is not configured");
                }
                yield awsSesEmailService;
            }
        };
    }
}

