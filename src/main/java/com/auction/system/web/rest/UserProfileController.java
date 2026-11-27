package com.auction.system.web.rest;

import com.auction.system.service.UserProfileService;
import com.auction.system.service.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileDTO>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getMyProfile()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateMyProfile(@Valid @RequestBody UpdateProfileDTO req) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.updateMyProfile(req)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeDTO req) {
        userProfileService.changePassword(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<LoginSessionDTO>>> getActiveSessions() {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getActiveSessions()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable("sessionId") Long sessionId) {
        userProfileService.revokeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getUserAddresses() {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getUserAddresses()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressDTO>> createUserAddress(@Valid @RequestBody AddressDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(userProfileService.createUserAddress(req)));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserAddress(@PathVariable("addressId") Long addressId) {
        userProfileService.deleteUserAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/seller-verification")
    @PreAuthorize("hasAuthority('ROLE_BIDDER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitSellerVerification(@Valid @RequestBody SellerVerificationDTO req) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.submitSellerVerification(req)));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyActivities(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getMyActivities(page, size)));
    }
}
