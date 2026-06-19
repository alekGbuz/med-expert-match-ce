package com.berdachuk.medexpertmatch.facility.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacilityTest {

    @Test
    @DisplayName("record construction with all fields")
    void recordConstruction() {
        Facility facility = new Facility(
                "fac-001", "City General Hospital", "ACADEMIC",
                "Boston", "MA", "US",
                new BigDecimal("42.3601"), new BigDecimal("-71.0589"),
                List.of("ICU", "SURGERY", "PCI"),
                500, 320);

        assertEquals("fac-001", facility.id());
        assertEquals("City General Hospital", facility.name());
        assertEquals("ACADEMIC", facility.facilityType());
        assertEquals("Boston", facility.locationCity());
        assertEquals("MA", facility.locationState());
        assertEquals("US", facility.locationCountry());
        assertEquals(new BigDecimal("42.3601"), facility.locationLatitude());
        assertEquals(new BigDecimal("-71.0589"), facility.locationLongitude());
        assertEquals(List.of("ICU", "SURGERY", "PCI"), facility.capabilities());
        assertEquals(500, facility.capacity());
        assertEquals(320, facility.currentOccupancy());
    }

    @Test
    @DisplayName("community facility with minimal capabilities")
    void communityFacility() {
        Facility facility = new Facility(
                "fac-002", "Rural Clinic", "COMMUNITY",
                "Springfield", "IL", "US",
                new BigDecimal("39.7817"), new BigDecimal("-89.6501"),
                List.of("PRIMARY_CARE"),
                50, 45);

        assertEquals("COMMUNITY", facility.facilityType());
        assertEquals(1, facility.capabilities().size());
        assertEquals(50, facility.capacity());
        assertEquals(45, facility.currentOccupancy());
    }
}
