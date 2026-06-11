package com.tecngo.service_requests.service;

import com.tecngo.files.service.FileStorage;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestImageServiceTest {
    @Mock
    ServiceRequestImageRepository images;
    @Mock
    ServiceRequestRepository requests;
    @Mock
    FileStorage storage;
    @Mock
    SystemParameterService parameters;
    @InjectMocks
    ServiceRequestImageService service;

    @Test
    void blocksImagesAboveConfiguredMaximum() {
        UUID requestId = UUID.randomUUID();
        User client = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();
        ServiceRequest request = ServiceRequest.builder()
                .id(requestId).client(client).status(RequestStatus.QUOTE_PENDING).build();
        when(requests.findById(requestId)).thenReturn(Optional.of(request));
        when(parameters.maxServiceRequestImages()).thenReturn(5);
        when(images.countByServiceRequestId(requestId)).thenReturn(5L);

        var file = new MockMultipartFile("file", "problem.jpg", "image/jpeg", new byte[]{1});
        assertThatThrownBy(() -> service.upload(requestId, file, client))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Maximum");
    }
}
