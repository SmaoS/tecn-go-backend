package com.tecngo.catalogs.service;

import com.tecngo.catalogs.entity.City;
import com.tecngo.catalogs.entity.Country;
import com.tecngo.catalogs.entity.Department;
import com.tecngo.catalogs.repository.CityRepository;
import com.tecngo.catalogs.repository.CountryRepository;
import com.tecngo.catalogs.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeographicCatalogServiceTest {
    @Mock CountryRepository countries;
    @Mock DepartmentRepository departments;
    @Mock CityRepository cities;
    @InjectMocks GeographicCatalogService service;

    @Test
    void listsActiveCatalogsByParent() {
        UUID countryId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Country country = Country.builder().id(countryId).name("Colombia").mobileDialCode("+57").active(true).build();
        Department department = Department.builder().id(departmentId).country(country)
                .name("Meta").active(true).build();
        City city = City.builder().id(UUID.randomUUID()).department(department)
                .name("Villavicencio").active(true).build();
        when(countries.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(country));
        when(countries.findById(countryId)).thenReturn(Optional.of(country));
        when(departments.findByCountryIdAndActiveTrueOrderByNameAsc(countryId)).thenReturn(List.of(department));
        when(departments.findById(departmentId)).thenReturn(Optional.of(department));
        when(cities.findByDepartmentIdAndActiveTrueOrderByNameAsc(departmentId)).thenReturn(List.of(city));

        assertThat(service.countries()).extracting("name").containsExactly("Colombia");
        assertThat(service.countries()).extracting("phonePrefix").containsExactly("+57");
        assertThat(service.departments(countryId)).extracting("name").containsExactly("Meta");
        assertThat(service.cities(departmentId)).extracting("name").containsExactly("Villavicencio");
    }

    @Test
    void validatesCompleteGeographicHierarchy() {
        Country country = Country.builder().id(UUID.randomUUID()).active(true).build();
        Department department = Department.builder().id(UUID.randomUUID()).country(country).active(true).build();
        City city = City.builder().id(UUID.randomUUID()).department(department).active(true).build();
        when(countries.findById(country.getId())).thenReturn(Optional.of(country));
        when(departments.findById(department.getId())).thenReturn(Optional.of(department));
        when(cities.findById(city.getId())).thenReturn(Optional.of(city));

        assertThat(service.requireSelection(country.getId(), department.getId(), city.getId()).city())
                .isSameAs(city);
    }
}
