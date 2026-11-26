package com.auction.system.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SellerVerificationDTO {

    @NotBlank(message = "Số CCCD/CMND không được để trống")
    @Size(max = 30)
    private String idCardNumber;

    @NotBlank(message = "Ảnh mặt trước CCCD không được để trống")
    @Size(max = 512)
    private String idCardFrontUrl;

    @NotBlank(message = "Ảnh mặt sau CCCD không được để trống")
    @Size(max = 512)
    private String idCardBackUrl;

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getIdCardFrontUrl() {
        return idCardFrontUrl;
    }

    public void setIdCardFrontUrl(String idCardFrontUrl) {
        this.idCardFrontUrl = idCardFrontUrl;
    }

    public String getIdCardBackUrl() {
        return idCardBackUrl;
    }

    public void setIdCardBackUrl(String idCardBackUrl) {
        this.idCardBackUrl = idCardBackUrl;
    }
}
