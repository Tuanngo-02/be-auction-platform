package com.auction.system.web.rest;

import com.auction.system.service.UserProfileService;
import com.auction.system.service.dto.ApiResponse;
import com.auction.system.service.dto.SellerPublicProfileDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    private final UserProfileService userProfileService;

    public SellerController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{sellerId}/profile")
    public ResponseEntity<ApiResponse<SellerPublicProfileDTO>> getSellerPublicProfile(@PathVariable("sellerId") Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getSellerPublicProfile(sellerId)));
    }
}
