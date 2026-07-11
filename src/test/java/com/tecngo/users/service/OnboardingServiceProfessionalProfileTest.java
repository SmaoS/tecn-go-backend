package com.tecngo.users.service;

import com.tecngo.catalogs.entity.City;
import com.tecngo.catalogs.entity.Country;
import com.tecngo.catalogs.entity.Department;
import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.legal.dto.LegalStatusResponse;
import com.tecngo.legal.service.LegalService;
import com.tecngo.phone_auth.service.PhoneNormalizer;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.dto.CertificateRequest;
import com.tecngo.users.dto.OnboardingMainDataRequest;
import com.tecngo.users.dto.TechnicianProfessionalProfileRequest;
import com.tecngo.users.entity.DocumentType;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceProfessionalProfileTest {
    @Mock UserRepository users;
    @Mock GeographicCatalogService geographicCatalogs;
    @Mock ManagedContentPolicy managedContent;
    @Mock LegalService legal;
    @Mock TechnicianProfileRepository technicianProfiles;
    @Mock ServiceCategoryService serviceCategories;
    @Mock PhoneNormalizer phones;
    @InjectMocks OnboardingService service;

    @Test
    void phoneVerifiedUserCanAdvanceFromMainDataWithoutEmailVerification() {
        User client = User.builder()
                .id(UUID.randomUUID())
                .role(Role.CLIENT)
                .fullName("Cliente TecnGo")
                .phone("3001234567")
                .phoneVerified(true)
                .build();
        UUID countryId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID cityId = UUID.randomUUID();
        Country country = Country.builder().id(countryId).code("CO").name("Colombia").mobileDialCode("+57").active(true).build();
        Department department = Department.builder().id(departmentId).country(country).name("Meta").active(true).build();
        City city = City.builder().id(cityId).department(department).name("Villavicencio").active(true).build();
        when(geographicCatalogs.requireSelection(countryId, departmentId, cityId))
                .thenReturn(new GeographicCatalogService.GeographicSelection(country, department, city));
        when(phones.local("3001234567")).thenReturn("3001234567");
        when(phones.international("3001234567", countryId)).thenReturn("+573001234567");
        when(legal.status(client)).thenReturn(new LegalStatusResponse(false, List.of(), List.of(), false, false));

        var result = service.mainData(client, new OnboardingMainDataRequest(
                "Cliente TecnGo",
                "3001234567",
                countryId,
                departmentId,
                cityId,
                "Calle 10 # 20-30",
                "Centro",
                DocumentType.CC,
                "123456789"
        ));

        assertThat(result.emailVerified()).isFalse();
        assertThat(result.phoneVerified()).isTrue();
        assertThat(result.currentStep()).isEqualTo(OnboardingStep.LEGAL_ACCEPTANCE);
        assertThat(client.getOnboardingStep()).isEqualTo(OnboardingStep.LEGAL_ACCEPTANCE);
        verify(users).save(client);
    }

    @Test
    void technicianWithCategoryAndExperienceAdvancesToCertificate() {
        User technician = readyTechnician();
        UUID categoryId = UUID.randomUUID();
        ServiceCategory category = ServiceCategory.builder()
                .id(categoryId).name("Electricista").slug("electricista").active(true).build();
        TechnicianProfile profile = TechnicianProfile.builder().user(technician).build();
        when(serviceCategories.requireActive(categoryId)).thenReturn(category);
        when(technicianProfiles.findByUserId(technician.getId())).thenReturn(Optional.of(profile));
        when(legal.status(technician)).thenReturn(new LegalStatusResponse(true, List.of(), List.of(), true, false));

        var result = service.professionalProfile(technician,
                new TechnicianProfessionalProfileRequest(Set.of(categoryId),
                        "Tengo más de cinco años instalando y reparando redes eléctricas residenciales."));

        assertThat(result.currentStep()).isEqualTo(OnboardingStep.TECHNICIAN_CERTIFICATE);
        assertThat(profile.getCategories()).containsExactly(category);
        assertThat(profile.getDescription()).isEqualTo(technician.getWorkExperienceDescription());
        verify(technicianProfiles).save(profile);
        verify(users).save(technician);
    }

    @Test
    void clientPrimaryRoleCanCompleteTechnicianProfileInTechnicianMode() {
        User technician = readyTechnician();
        technician.setRole(Role.CLIENT);
        technician.addRole(Role.TECHNICIAN);
        technician.setActiveMode(ActiveMode.TECHNICIAN);
        UUID categoryId = UUID.randomUUID();
        ServiceCategory category = ServiceCategory.builder()
                .id(categoryId).name("Electricista").slug("electricista").active(true).build();
        TechnicianProfile profile = TechnicianProfile.builder().user(technician).build();
        when(serviceCategories.requireActive(categoryId)).thenReturn(category);
        when(technicianProfiles.findByUserId(technician.getId())).thenReturn(Optional.of(profile));
        when(legal.status(technician)).thenReturn(new LegalStatusResponse(true, List.of(), List.of(), true, false));

        var result = service.professionalProfile(technician,
                new TechnicianProfessionalProfileRequest(Set.of(categoryId),
                        "Tengo más de cinco años instalando y reparando redes eléctricas residenciales."));

        assertThat(result.currentStep()).isEqualTo(OnboardingStep.TECHNICIAN_CERTIFICATE);
        assertThat(profile.getCategories()).containsExactly(category);
    }

    @Test
    void clientCannotSaveTechnicianProfessionalProfile() {
        User client = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).emailVerified(true).build();

        assertThatThrownBy(() -> service.professionalProfile(client,
                new TechnicianProfessionalProfileRequest(Set.of(UUID.randomUUID()),
                        "Descripción suficientemente larga para cumplir la validación requerida.")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void nonexistentCategoryIsRejected() {
        User technician = readyTechnician();
        UUID categoryId = UUID.randomUUID();
        when(serviceCategories.requireActive(categoryId)).thenThrow(new NotFoundException("Service category not found"));

        assertThatThrownBy(() -> service.professionalProfile(technician,
                new TechnicianProfessionalProfileRequest(Set.of(categoryId),
                        "Descripción suficientemente larga para cumplir la validación requerida.")))
                .isInstanceOf(NotFoundException.class);
        verify(technicianProfiles, never()).save(any());
    }

    @Test
    void technicianWithoutCategoriesDoesNotAdvance() {
        User technician = readyTechnician();

        assertThatThrownBy(() -> service.professionalProfile(technician,
                new TechnicianProfessionalProfileRequest(Set.of(),
                        "Descripción suficientemente larga para cumplir la validación requerida.")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
        assertThat(technician.getOnboardingStep())
                .isEqualTo(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE);
        verifyNoInteractions(technicianProfiles);
    }

    @Test
    void technicianWithoutValidExperienceDoesNotAdvance() {
        User technician = readyTechnician();

        assertThatThrownBy(() -> service.professionalProfile(technician,
                new TechnicianProfessionalProfileRequest(Set.of(UUID.randomUUID()), "Muy corta")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 30 and 1000");
        assertThat(technician.getOnboardingStep())
                .isEqualTo(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE);
        verifyNoInteractions(technicianProfiles, serviceCategories);
    }

    @Test
    void certificateCannotCompleteWithoutProfessionalProfile() {
        User technician = readyTechnician();
        when(technicianProfiles.findByUserId(technician.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.certificate(technician, new CertificateRequest(null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("professional profile");
        assertThat(technician.isOnboardingCompleted()).isFalse();
    }

    @Test
    void skippingOptionalCertificateCompletesOnboardingWhenProfessionalProfileExists() {
        User technician = readyTechnician();
        technician.setWorkExperienceDescription(
                "Tengo más de cinco años instalando y reparando redes eléctricas residenciales.");
        technician.setOnboardingStep(OnboardingStep.TECHNICIAN_CERTIFICATE);
        TechnicianProfile profile = TechnicianProfile.builder()
                .user(technician)
                .description(technician.getWorkExperienceDescription())
                .categories(Set.of(ServiceCategory.builder()
                        .id(UUID.randomUUID()).name("Electricista").slug("electricista").active(true).build()))
                .build();
        when(technicianProfiles.findByUserId(technician.getId())).thenReturn(Optional.of(profile));
        when(legal.status(technician)).thenReturn(new LegalStatusResponse(true, List.of(), List.of(), true, false));

        var result = service.skipCertificate(technician);

        assertThat(result.onboardingCompleted()).isTrue();
        assertThat(result.currentStep()).isEqualTo(OnboardingStep.COMPLETED);
        assertThat(technician.isOnboardingCompleted()).isTrue();
        verify(users).save(technician);
    }

    private User readyTechnician() {
        return User.builder()
                .id(UUID.randomUUID())
                .role(Role.TECHNICIAN)
                .emailVerified(true)
                .phone("3001234567")
                .documentType(DocumentType.CC)
                .documentNumber("123456789")
                .documentFrontUrl("front")
                .documentBackUrl("back")
                .documentPhotoUrl("front")
                .profilePhotoUrl("profile")
                .homeAddress("Calle 10 # 20-30")
                .city(City.builder().id(UUID.randomUUID()).name("Villavicencio").active(true).build())
                .onboardingStep(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE)
                .build();
    }
}
