package com.auction.system.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public class UserProfileDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String reputation;
    private String sellerStatus;
    private String accountStatus;
    private Boolean emailVerified;
    private Set<String> roles;
    private BigDecimal availableBalance = BigDecimal.ZERO;
    private BigDecimal lockedDeposit = BigDecimal.ZERO;
    private long participatedCount;
    private long watchlistCount;
    private long ordersCount;
    private String defaultAddress;
    private String maskedIdCard;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getReputation() {
        return reputation;
    }

    public void setReputation(String reputation) {
        this.reputation = reputation;
    }

    public String getSellerStatus() {
        return sellerStatus;
    }

    public void setSellerStatus(String sellerStatus) {
        this.sellerStatus = sellerStatus;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getLockedDeposit() {
        return lockedDeposit;
    }

    public void setLockedDeposit(BigDecimal lockedDeposit) {
        this.lockedDeposit = lockedDeposit;
    }

    public long getParticipatedCount() {
        return participatedCount;
    }

    public void setParticipatedCount(long participatedCount) {
        this.participatedCount = participatedCount;
    }

    public long getWatchlistCount() {
        return watchlistCount;
    }

    public void setWatchlistCount(long watchlistCount) {
        this.watchlistCount = watchlistCount;
    }

    public long getOrdersCount() {
        return ordersCount;
    }

    public void setOrdersCount(long ordersCount) {
        this.ordersCount = ordersCount;
    }

    public String getDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(String defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public String getMaskedIdCard() {
        return maskedIdCard;
    }

    public void setMaskedIdCard(String maskedIdCard) {
        this.maskedIdCard = maskedIdCard;
    }
}
