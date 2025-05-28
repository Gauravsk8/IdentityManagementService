package com.example.identitymanagementservice.repository;

import com.example.identitymanagementservice.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmailAndIsActiveTrue(String email);
    Optional<Employee> findByEmployeeCodeAndIsActiveTrue(String employeeCode);

    List<Employee> findAllByIsActiveTrue();
    Optional<Employee> findByKeycloakUserIdAndIsActiveTrue(String keycloakUserId);

    List<Employee> findByManagerCodeAndIsActiveTrue(String managerCode);




}
