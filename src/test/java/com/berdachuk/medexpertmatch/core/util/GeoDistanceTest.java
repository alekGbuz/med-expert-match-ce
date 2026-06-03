package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

class GeoDistanceTest {

    private static final double TOLERANCE_KM = 20.0;

    @Test
    void londonToParisApprox343km() {
        BigDecimal londonLat = new BigDecimal("51.5074");
        BigDecimal londonLon = new BigDecimal("-0.1278");
        BigDecimal parisLat = new BigDecimal("48.8566");
        BigDecimal parisLon = new BigDecimal("2.3522");

        Double distance = GeoDistance.calculateDistanceKm(londonLat, londonLon, parisLat, parisLon);

        assertThat(distance).isNotNull();
        assertThat(distance).isCloseTo(343.0, byLessThan(TOLERANCE_KM));
    }

    @Test
    void antipodalApprox20037km() {
        BigDecimal lat1 = new BigDecimal("0.0");
        BigDecimal lon1 = new BigDecimal("0.0");
        BigDecimal lat2 = new BigDecimal("0.0");
        BigDecimal lon2 = new BigDecimal("180.0");

        Double distance = GeoDistance.calculateDistanceKm(lat1, lon1, lat2, lon2);

        assertThat(distance).isNotNull();
        assertThat(distance).isCloseTo(20037.0, byLessThan(100.0));
    }

    @Test
    void zeroDistanceForSamePoint() {
        BigDecimal lat = new BigDecimal("40.7128");
        BigDecimal lon = new BigDecimal("-74.0060");

        Double distance = GeoDistance.calculateDistanceKm(lat, lon, lat, lon);

        assertThat(distance).isNotNull();
        assertThat(distance).isCloseTo(0.0, byLessThan(0.001));
    }

    @Test
    void nullCoordinateReturnsNull() {
        assertThat(GeoDistance.calculateDistanceKm(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isNull();
        assertThat(GeoDistance.calculateDistanceKm(BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO))
                .isNull();
        assertThat(GeoDistance.calculateDistanceKm(BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO))
                .isNull();
        assertThat(GeoDistance.calculateDistanceKm(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isNull();
    }
}
