package com.tecngo.system_parameters.service;

import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.dto.SystemParameterResponse;
import com.tecngo.system_parameters.entity.ParameterType;
import com.tecngo.system_parameters.entity.SystemParameter;
import com.tecngo.system_parameters.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemParameterService {
    public static final String QUOTE_EXPIRATION_MINUTES = "QUOTE_EXPIRATION_MINUTES";
    public static final String PLATFORM_COMMISSION_PERCENTAGE = "PLATFORM_COMMISSION_PERCENTAGE";
    public static final String TECHNICIAN_OFFLINE_AFTER_MINUTES = "TECHNICIAN_OFFLINE_AFTER_MINUTES";
    public static final String LOCATION_POLLING_SECONDS = "LOCATION_POLLING_SECONDS";
    public static final String SERVICE_POLLING_SECONDS = "SERVICE_POLLING_SECONDS";
    public static final String MAX_SERVICE_REQUEST_IMAGES = "MAX_SERVICE_REQUEST_IMAGES";
    public static final String MAX_SERVICE_EVIDENCE_FILES = "MAX_SERVICE_EVIDENCE_FILES";
    public static final String MAX_PAYMENT_PROOF_FILES = "MAX_PAYMENT_PROOF_FILES";
    public static final String REQUIRE_LEGAL_ACCEPTANCE = "REQUIRE_LEGAL_ACCEPTANCE";
    public static final String REQUIRE_PROFILE_FACE_DETECTION = "REQUIRE_PROFILE_FACE_DETECTION";
    public static final String REFERRAL_PROGRAM_ENABLED = "REFERRAL_PROGRAM_ENABLED";
    public static final String REFERRAL_REQUIRED_RATING = "REFERRAL_REQUIRED_RATING";
    public static final String REFERRAL_REWARD_EXPIRATION_DAYS = "REFERRAL_REWARD_EXPIRATION_DAYS";
    public static final String REFERRAL_REWARD_ONLY_IF_COMMISSION_GT_ZERO = "REFERRAL_REWARD_ONLY_IF_COMMISSION_GT_ZERO";
    public static final String APP_VERSION_CHECK_ENABLED = "APP_VERSION_CHECK_ENABLED";

    private final SystemParameterRepository repository;

    @Value("${app.parameters.quote-expiration-minutes:10}")
    private int quoteExpirationFallback;
    @Value("${app.parameters.platform-commission-percentage:10}")
    private BigDecimal commissionFallback;
    @Value("${app.parameters.technician-offline-after-minutes:3}")
    private int offlineFallback;
    @Value("${app.parameters.max-service-request-images:5}")
    private int maxImagesFallback;
    @Value("${app.parameters.max-service-evidence-files:10}")
    private int maxEvidenceFallback;
    @Value("${app.parameters.max-payment-proof-files:3}")
    private int maxPaymentProofFallback;
    @Value("${app.parameters.require-legal-acceptance:true}")
    private boolean requireLegalFallback;
    @Value("${app.parameters.require-profile-face-detection:false}")
    private boolean requireFaceDetectionFallback;

    @Transactional(readOnly = true)
    public List<SystemParameterResponse> list() {
        return repository.findAll().stream()
                .sorted(java.util.Comparator.comparing(SystemParameter::getKey))
                .map(this::map)
                .toList();
    }

    @Transactional
    public SystemParameterResponse update(String key, String value) {
        SystemParameter parameter = repository.findByKeyAndActiveTrue(key)
                .orElseThrow(() -> new NotFoundException("System parameter not found"));
        validate(parameter, value);
        parameter.setValue(value.trim());
        return map(repository.save(parameter));
    }

    public int quoteExpirationMinutes() {
        return integer(QUOTE_EXPIRATION_MINUTES, quoteExpirationFallback);
    }

    public BigDecimal platformCommissionPercentage() {
        return decimal(PLATFORM_COMMISSION_PERCENTAGE, commissionFallback);
    }

    public int technicianOfflineAfterMinutes() {
        return integer(TECHNICIAN_OFFLINE_AFTER_MINUTES, offlineFallback);
    }

    public int maxServiceRequestImages() {
        return integer(MAX_SERVICE_REQUEST_IMAGES, maxImagesFallback);
    }
    public int maxServiceEvidenceFiles() { return integer(MAX_SERVICE_EVIDENCE_FILES, maxEvidenceFallback); }
    public int maxPaymentProofFiles() { return integer(MAX_PAYMENT_PROOF_FILES, maxPaymentProofFallback); }
    public boolean requireLegalAcceptance() {
        return repository.findByKeyAndActiveTrue(REQUIRE_LEGAL_ACCEPTANCE)
                .map(item -> Boolean.parseBoolean(item.getValue())).orElse(requireLegalFallback);
    }
    public boolean requireProfileFaceDetection() {
        return repository.findByKeyAndActiveTrue(REQUIRE_PROFILE_FACE_DETECTION)
                .map(item -> Boolean.parseBoolean(item.getValue())).orElse(requireFaceDetectionFallback);
    }
    public boolean referralProgramEnabled() { return bool(REFERRAL_PROGRAM_ENABLED, true); }
    public int referralRequiredRating() { return integer(REFERRAL_REQUIRED_RATING, 5); }
    public int referralRewardExpirationDays() { return integer(REFERRAL_REWARD_EXPIRATION_DAYS, 0); }
    public boolean referralRewardOnlyIfCommissionGtZero() {
        return bool(REFERRAL_REWARD_ONLY_IF_COMMISSION_GT_ZERO, true);
    }
    public boolean appVersionCheckEnabled() { return bool(APP_VERSION_CHECK_ENABLED, true); }

    private boolean bool(String key, boolean fallback) {
        return repository.findByKeyAndActiveTrue(key)
                .map(item -> Boolean.parseBoolean(item.getValue())).orElse(fallback);
    }

    private int integer(String key, int fallback) {
        return repository.findByKeyAndActiveTrue(key)
                .map(item -> Integer.parseInt(item.getValue()))
                .orElse(fallback);
    }

    private BigDecimal decimal(String key, BigDecimal fallback) {
        return repository.findByKeyAndActiveTrue(key)
                .map(item -> new BigDecimal(item.getValue()))
                .orElse(fallback);
    }

    private void validate(SystemParameter parameter, String raw) {
        String value = raw.trim();
        BigDecimal number = null;
        if (parameter.getType() == ParameterType.INTEGER) {
            try {
                number = new BigDecimal(Integer.parseInt(value));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Parameter value must be an integer");
            }
        } else if (parameter.getType() == ParameterType.DECIMAL) {
            try {
                number = new BigDecimal(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Parameter value must be numeric");
            }
        } else if (parameter.getType() == ParameterType.BOOLEAN
                && !value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Parameter value must be true or false");
        }
        if (number != null && number.signum() < 0) {
            throw new IllegalArgumentException("Parameter value cannot be negative");
        }
        if (parameter.getKey().equals(QUOTE_EXPIRATION_MINUTES)
                && (number.intValue() < 1 || number.intValue() > 60)) {
            throw new IllegalArgumentException("QUOTE_EXPIRATION_MINUTES must be between 1 and 60");
        }
        if (parameter.getKey().equals(PLATFORM_COMMISSION_PERCENTAGE)
                && (number.compareTo(BigDecimal.ZERO) < 0
                || number.compareTo(BigDecimal.valueOf(50)) > 0)) {
            throw new IllegalArgumentException("PLATFORM_COMMISSION_PERCENTAGE must be between 0 and 50");
        }
    }

    private SystemParameterResponse map(SystemParameter item) {
        return new SystemParameterResponse(item.getId(), item.getKey(), item.getValue(),
                item.getDescription(), item.getType(), item.isActive(), item.getUpdatedAt());
    }
}
