package com.vinayak.medireach.utils;

/**
 * LocationUtils utility class for location-based calculations.
 * Provides methods to calculate distances between geographical coordinates.
 */
public class LocationUtils {

    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    // Default radius for nearby location check (in kilometers)
    private static final double NEARBY_RADIUS_KM = 10.0;

    /**
     * Calculates the distance between two geographical points using the Haversine formula.
     * The Haversine formula calculates the great-circle distance between two points
     * on a sphere given their latitudes and longitudes.
     *
     * @param lat1 Latitude of the first point in degrees
     * @param lon1 Longitude of the first point in degrees
     * @param lat2 Latitude of the second point in degrees
     * @param lon2 Longitude of the second point in degrees
     * @return Distance between the two points in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double latRad1 = Math.toRadians(lat1);
        double lonRad1 = Math.toRadians(lon1);
        double latRad2 = Math.toRadians(lat2);
        double lonRad2 = Math.toRadians(lon2);

        // Calculate differences
        double dLat = latRad2 - latRad1;
        double dLon = lonRad2 - lonRad1;

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(latRad1) * Math.cos(latRad2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Distance in kilometers
        double distanceKm = EARTH_RADIUS_KM * c;

        return distanceKm;
    }

    /**
     * Checks if two geographical points are within 10 kilometers of each other.
     *
     * @param lat1 Latitude of the first point in degrees
     * @param lon1 Longitude of the first point in degrees
     * @param lat2 Latitude of the second point in degrees
     * @param lon2 Longitude of the second point in degrees
     * @return true if the distance is less than or equal to 10 km, false otherwise
     */
    public static boolean isWithin10km(double lat1, double lon1, double lat2, double lon2) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= NEARBY_RADIUS_KM;
    }

    /**
     * Checks if two geographical points are within a specified radius.
     *
     * @param lat1       Latitude of the first point in degrees
     * @param lon1       Longitude of the first point in degrees
     * @param lat2       Latitude of the second point in degrees
     * @param lon2       Longitude of the second point in degrees
     * @param radiusKm   Radius in kilometers
     * @return true if the distance is less than or equal to the specified radius, false otherwise
     */
    public static boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= radiusKm;
    }
}

