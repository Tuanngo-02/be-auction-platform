package com.auction.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionSystemAppTimeZoneTest {

    private TimeZone original;

    @BeforeEach
    void saveDefault() {
        original = TimeZone.getDefault();
    }

    @AfterEach
    void restoreDefault() {
        TimeZone.setDefault(original);
    }

    @Test
    void mapsLegacySaigonAliasToHoChiMinh() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Saigon"));

        AuctionSystemApp.normalizeLegacyTimeZone();

        assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Ho_Chi_Minh");
    }

    @Test
    void leavesOtherZonesUntouched() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        AuctionSystemApp.normalizeLegacyTimeZone();

        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
    }
}
