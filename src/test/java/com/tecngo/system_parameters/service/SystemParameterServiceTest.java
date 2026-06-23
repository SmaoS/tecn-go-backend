package com.tecngo.system_parameters.service;

import com.tecngo.system_parameters.entity.ParameterType;
import com.tecngo.system_parameters.entity.SystemParameter;
import com.tecngo.system_parameters.repository.SystemParameterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemParameterServiceTest {
    @Mock
    SystemParameterRepository repository;
    @InjectMocks
    SystemParameterService service;

    @Test
    void rejectsCommissionOutsideAllowedRange() {
        SystemParameter parameter = SystemParameter.builder()
                .key(SystemParameterService.PLATFORM_COMMISSION_PERCENTAGE)
                .value("10")
                .type(ParameterType.DECIMAL)
                .active(true)
                .build();
        when(repository.findByKeyAndActiveTrue(parameter.getKey())).thenReturn(Optional.of(parameter));

        assertThatThrownBy(() -> service.update(parameter.getKey(), "51"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 50");
        verify(repository, never()).save(any());
    }

    @Test
    void updatesValidQuoteExpiration() {
        SystemParameter parameter = SystemParameter.builder()
                .key(SystemParameterService.QUOTE_EXPIRATION_MINUTES)
                .value("10")
                .type(ParameterType.INTEGER)
                .active(true)
                .build();
        when(repository.findByKeyAndActiveTrue(parameter.getKey())).thenReturn(Optional.of(parameter));
        when(repository.save(parameter)).thenReturn(parameter);

        service.update(parameter.getKey(), "15");

        verify(repository).save(parameter);
    }

    @Test
    void updatesValidServiceRequestExpiration() {
        SystemParameter parameter = SystemParameter.builder()
                .key(SystemParameterService.SERVICE_REQUEST_EXPIRATION_HOURS)
                .value("24")
                .type(ParameterType.INTEGER)
                .active(true)
                .build();
        when(repository.findByKeyAndActiveTrue(parameter.getKey())).thenReturn(Optional.of(parameter));
        when(repository.save(parameter)).thenReturn(parameter);

        service.update(parameter.getKey(), "48");

        assertThat(parameter.getValue()).isEqualTo("48");
    }
}
