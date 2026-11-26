package com.auction.system.service.dto;

public class SellerPublicProfileDTO {

    private Long sellerId;
    private String fullName;
    private String reputation;
    private long totalAuctions;
    private boolean verified;

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getReputation() {
        return reputation;
    }

    public void setReputation(String reputation) {
        this.reputation = reputation;
    }

    public long getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(long totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
