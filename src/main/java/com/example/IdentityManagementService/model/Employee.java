package com.example.IdentityManagementService.model;

import com.example.IdentityManagementService.common.audit.Audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@Table(name = "employee")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Employee extends Audit {

    @Id
    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Email(message = "Email should be valid")
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "employee_type")
    private String employeeType;

    @Column(name = "manager_code")
    private String managerCode;

    @Column(name = "is_active")
    private boolean isActive = true;
}
