package com.auction.system.web.rest;

import com.auction.system.service.AuthModuleService;
import com.auction.system.service.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthModuleService authService;

    public AuthController(AuthModuleService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequestDTO req) {
        Map<String, Object> result = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmailOtp(@Valid @RequestBody VerifyOtpRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyEmailOtp(req)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenDTO>> login(@Valid @RequestBody LoginRequestDTO req, HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req, httpReq)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(token)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) Map<String, String> body) {
        String token = body != null ? body.get("refreshToken") : null;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPasswordSendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPasswordSendOtp(email)));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPasswordVerifyOtp(@Valid @RequestBody VerifyOtpRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPasswordVerifyOtp(req)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
