package com.example.identitymanagementservice.utils;

import com.example.identitymanagementservice.dto.request.pagenationdto.FilterRequest;

import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

public class FilterUtil {
    public static List<FilterRequest> parseFilters(Map<String, String> allParams) {
        return allParams.entrySet().stream()
                .filter(e -> !List.of("offset", "limit", "sort").contains(e.getKey()))
                .map(e -> {
                    FilterRequest fr = new FilterRequest();
                    String[] parts = e.getKey().split("__");
                    fr.setField(parts[0]);
                    fr.setOperator(parts.length > 1 ? parts[1] : "eq");
                    fr.setValue(e.getValue());
                    return fr;
                })
                .collect(Collectors.toList());
    }
}
