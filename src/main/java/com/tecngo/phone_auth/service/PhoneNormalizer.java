package com.tecngo.phone_auth.service;

import com.tecngo.catalogs.repository.CountryRepository;
import com.tecngo.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PhoneNormalizer {
    private static final String DEFAULT_COUNTRY_CODE = "CO";
    private static final String DEFAULT_DIAL_CODE = "+57";

    private final CountryRepository countries;

    public String normalize(String rawPhone) {
        return international(rawPhone, null);
    }

    public String local(String rawPhone) {
        if (rawPhone == null) throw new IllegalArgumentException("Phone is required");
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.length() != 10) {
            throw new IllegalArgumentException("El celular debe tener exactamente 10 dígitos");
        }
        return digits;
    }

    public String international(String rawPhone, UUID countryId) {
        return dialCode(countryId) + local(rawPhone);
    }

    private String dialCode(UUID countryId) {
        if (countryId == null) {
            return countries.findByCodeAndActiveTrue(DEFAULT_COUNTRY_CODE)
                    .map(country -> cleanDialCode(country.getMobileDialCode()))
                    .orElse(DEFAULT_DIAL_CODE);
        }
        return countries.findById(countryId)
                .filter(country -> country.isActive() && country.getMobileDialCode() != null
                        && !country.getMobileDialCode().isBlank())
                .map(country -> cleanDialCode(country.getMobileDialCode()))
                .orElseThrow(() -> new NotFoundException("Country phone prefix not found"));
    }

    private String cleanDialCode(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.isBlank()) return DEFAULT_DIAL_CODE;
        return "+" + digits;
    }
}
