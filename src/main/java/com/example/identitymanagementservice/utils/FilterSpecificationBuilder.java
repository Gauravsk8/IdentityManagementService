package com.example.identitymanagementservice.utils;

import com.example.identitymanagementservice.dto.request.pagenationdto.FilterRequest;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class FilterSpecificationBuilder<T> {
    public Specification<T> build(List<FilterRequest> filters) {
        return (root, query, criteriaBuilder) -> {
            if (filters == null || filters.isEmpty()) {
                return null;
            }

            List<Predicate> predicates = new ArrayList<>();

            for (FilterRequest filter : filters) {
                Path<String> path = root.get(filter.getField());
                String value = filter.getValue();

                switch (filter.getOperator()) {
                    case "eq":
                        predicates.add(criteriaBuilder.equal(path, filter.getValue()));
                        break;
                    case "like":
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(path),
                                "%" + filter.getValue().toLowerCase() + "%"
                        ));
                        break;
                    case "gt":
                        predicates.add(criteriaBuilder.greaterThan(path, filter.getValue()));
                        break;
                    case "lt":
                        predicates.add(criteriaBuilder.lessThan(path, filter.getValue()));
                        break;
                }

            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

