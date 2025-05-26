package com.example.IdentityManagementService.dto.request.pagenationDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterRequest {
    private String field;
    private String operator; // "eq", "like", "gt", "lt"
    private String value;

}

