package com.berdachuk.medexpertmatch.core.util;

import java.math.BigDecimal;

public final class GeoDistance {

    /** Earth's mean radius in kilometers (WGS-84). */
    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    /**
     * Haversine distance between two geographic coordinates.
     *
     * @return distance in kilometers, or {@code null} if any coordinate is null
     */
    public static Double calculateDistanceKm(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude) {
        if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) {
            return null;
        }

        double lat1 = Math.toRadians(fromLatitude.doubleValue());
        double lon1 = Math.toRadians(fromLongitude.doubleValue());
        double lat2 = Math.toRadians(toLatitude.doubleValue());
        double lon2 = Math.toRadians(toLongitude.doubleValue());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
