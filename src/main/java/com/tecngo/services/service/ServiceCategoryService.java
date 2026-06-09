package com.tecngo.services.service;

import com.tecngo.services.dto.ServiceCategoryRequest;
import com.tecngo.services.dto.ServiceCategoryResponse;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.repository.ServiceCategoryRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCategoryService {
    private final ServiceCategoryRepository categories;

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> active() {
        return categories.findByActiveTrueOrderByNameAsc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> all() {
        return categories.findAll().stream().map(this::map).toList();
    }

    @Transactional
    public ServiceCategoryResponse create(ServiceCategoryRequest request) {
        if (categories.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Service category name already exists");
        }
        String slug = uniqueSlug(request.name());
        return map(categories.save(ServiceCategory.builder()
                .name(request.name().trim())
                .slug(slug)
                .description(clean(request.description()))
                .active(request.active())
                .build()));
    }

    @Transactional
    public ServiceCategoryResponse update(UUID id, ServiceCategoryRequest request) {
        ServiceCategory category = find(id);
        if (!category.getName().equalsIgnoreCase(request.name().trim())
                && categories.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Service category name already exists");
        }
        category.setName(request.name().trim());
        category.setDescription(clean(request.description()));
        category.setActive(request.active());
        return map(category);
    }

    @Transactional
    public void delete(UUID id) {
        ServiceCategory category = find(id);
        category.setActive(false);
    }

    public ServiceCategory requireActive(UUID id) {
        ServiceCategory category = find(id);
        if (!category.isActive()) throw new ConflictException("Service category is inactive");
        return category;
    }

    private ServiceCategory find(UUID id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Service category not found"));
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String slug = base;
        int suffix = 2;
        while (categories.existsBySlug(slug)) slug = base + "-" + suffix++;
        return slug;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    public ServiceCategoryResponse map(ServiceCategory item) {
        return new ServiceCategoryResponse(item.getId(), item.getName(), item.getSlug(),
                item.getDescription(), item.isActive());
    }
}
