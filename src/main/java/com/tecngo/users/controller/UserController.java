package com.tecngo.users.controller;

import com.tecngo.users.dto.FcmTokenRequest;
import com.tecngo.users.dto.ChangePasswordRequest;
import com.tecngo.password_recovery.dto.PasswordMessageResponse;
import com.tecngo.users.dto.UserProfileRequest;
import com.tecngo.users.dto.UserProfileResponse;
import com.tecngo.users.dto.ActiveModeResponse;
import com.tecngo.users.dto.ChangeActiveModeRequest;
import com.tecngo.users.dto.ProfileSelfieChangeRequestCreate;
import com.tecngo.users.dto.ProfileSelfieChangeRequestResponse;
import com.tecngo.users.dto.VerifyUserPhoneRequest;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.ActiveModeService;
import com.tecngo.users.service.ProfileSelfieChangeRequestService;
import com.tecngo.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.tecngo.auth.security.JwtAuthenticationFilter;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final ActiveModeService activeModes;
    private final ProfileSelfieChangeRequestService profileSelfieChanges;

    @PutMapping("/me/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFcmToken(@Valid @RequestBody FcmTokenRequest request,
                               @AuthenticationPrincipal User user) {
        service.updateFcmToken(user, request.token());
    }

    @GetMapping("/me/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal User user) {
        return service.profile(user);
    }

    @PutMapping("/me/profile")
    public UserProfileResponse updateProfile(@Valid @RequestBody UserProfileRequest request,
                                             @AuthenticationPrincipal User user) {
        return service.updateProfile(user, request);
    }

    @PutMapping("/me/phone-verification")
    public UserProfileResponse verifyPhone(@Valid @RequestBody VerifyUserPhoneRequest request,
                                           @AuthenticationPrincipal User user) {
        return service.verifyPhone(user, request.phone(), request.verificationToken());
    }

    @PostMapping("/me/change-password")
    public PasswordMessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                  @AuthenticationPrincipal User user) {
        return service.changePassword(user, request);
    }

    @PutMapping("/me/active-mode")
    public ActiveModeResponse changeActiveMode(@Valid @RequestBody ChangeActiveModeRequest request,
                                               @AuthenticationPrincipal User user,
                                               HttpServletRequest servletRequest) {
        Object value = servletRequest.getAttribute(JwtAuthenticationFilter.SESSION_ID_ATTRIBUTE);
        return activeModes.change(user, request.mode(),
                value instanceof java.util.UUID id ? id : null);
    }

    @PostMapping("/me/profile-selfie-change-requests")
    public ProfileSelfieChangeRequestResponse requestProfileSelfieChange(
            @Valid @RequestBody ProfileSelfieChangeRequestCreate request,
            @AuthenticationPrincipal User user) {
        return profileSelfieChanges.create(user, request);
    }

    @GetMapping("/me/profile-selfie-change-requests")
    public java.util.List<ProfileSelfieChangeRequestResponse> profileSelfieChangeRequests(
            @AuthenticationPrincipal User user) {
        return profileSelfieChanges.mine(user);
    }
}
