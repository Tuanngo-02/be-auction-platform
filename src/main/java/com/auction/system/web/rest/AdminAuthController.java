package com.auction.system.web.rest;

import com.auction.system.service.AdminAuthService;
import com.auction.system.service.dto.AdminActivationRequestDTO;
import com.auction.system.service.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/admin/auth/activation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyAdminActivationToken(@RequestParam("token") String token) {
        return ResponseEntity.ok(ApiResponse.success(adminAuthService.verifyActivationToken(token)));
    }

    @PostMapping("/admin/auth/activate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activateAdminAccount(@Valid @RequestBody AdminActivationRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success(adminAuthService.activateAdminAccount(req)));
    }

    @GetMapping("/me/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponse.success(adminAuthService.getMyPermissions()));
    }
}
