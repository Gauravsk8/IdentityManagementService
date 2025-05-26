package com.example.IdentityManagementService.utils;

import com.example.IdentityManagementService.dto.request.pagenationDto.SortRequest;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;

public class SortUtil {
    public static List<SortRequest> parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) return Collections.emptyList();

        return Arrays.stream(sortParam.split(","))
                .map(field -> {
                    SortRequest sort = new SortRequest();
                    if (field.startsWith("-")) {
                        sort.setField(field.substring(1));
                        sort.setDirection("desc");
                    } else {
                        sort.setField(field);
                        sort.setDirection("asc");
                    }
                    return sort;
                })
                .collect(Collectors.toList());
    }
    public static Sort getSort(List<SortRequest> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = sorts.stream()
                .map(s -> new Sort.Order(
                        "desc".equalsIgnoreCase(s.getDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                        s.getField()))
                .toList();

        return Sort.by(orders);
    }


}
