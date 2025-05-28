package com.example.identitymanagementservice.common.email.service;



import com.example.identitymanagementservice.common.email.EmailProvider;

import java.util.Map;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    String loadTemplate(String templateName, Map<String, String> variables);
    void setProvider(EmailProvider provider);
}
