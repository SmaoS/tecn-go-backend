package com.tecngo.catalogs.service;

import com.tecngo.catalogs.dto.CatalogItemResponse;
import com.tecngo.catalogs.entity.City;
import com.tecngo.catalogs.entity.Country;
import com.tecngo.catalogs.entity.Department;
import com.tecngo.catalogs.repository.CityRepository;
import com.tecngo.catalogs.repository.CountryRepository;
import com.tecngo.catalogs.repository.DepartmentRepository;
import com.tecngo.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeographicCatalogService {
    private final CountryRepository countries;
    private final DepartmentRepository departments;
    private final CityRepository cities;

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> countries() {
        return countries.findByActiveTrueOrderByNameAsc().stream()
                .map(item -> new CatalogItemResponse(item.getId(), item.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> departments(UUID countryId) {
        requireCountry(countryId);
        return departments.findByCountryIdAndActiveTrueOrderByNameAsc(countryId).stream()
                .map(item -> new CatalogItemResponse(item.getId(), item.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> cities(UUID departmentId) {
        requireDepartment(departmentId);
        return cities.findByDepartmentIdAndActiveTrueOrderByNameAsc(departmentId).stream()
                .map(item -> new CatalogItemResponse(item.getId(), item.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GeographicSelection requireSelection(UUID countryId, UUID departmentId, UUID cityId) {
        if (countryId == null || departmentId == null || cityId == null) {
            throw new IllegalArgumentException("Country, department and city are required");
        }
        Country country = requireCountry(countryId);
        Department department = requireDepartment(departmentId);
        City city = cities.findById(cityId)
                .filter(City::isActive)
                .orElseThrow(() -> new NotFoundException("City not found"));
        if (!department.getCountry().getId().equals(country.getId())
                || !city.getDepartment().getId().equals(department.getId())) {
            throw new IllegalArgumentException("Invalid country, department and city selection");
        }
        return new GeographicSelection(country, department, city);
    }

    @Transactional(readOnly = true)
    public City requireCity(UUID cityId) {
        return cities.findById(cityId)
                .filter(City::isActive)
                .orElseThrow(() -> new NotFoundException("City not found"));
    }

    private Country requireCountry(UUID id) {
        return countries.findById(id).filter(Country::isActive)
                .orElseThrow(() -> new NotFoundException("Country not found"));
    }

    private Department requireDepartment(UUID id) {
        return departments.findById(id).filter(Department::isActive)
                .orElseThrow(() -> new NotFoundException("Department not found"));
    }

    public record GeographicSelection(Country country, Department department, City city) {}
}
