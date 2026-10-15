package com.example.wycrimeapp.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Utility for converting latitude/longitude into a human-friendly street address.
 *
 * Note:
 * - Android's Geocoder may require network connectivity depending on device services.
 * - Always call this off the UI thread.
 */
public final class AddressUtils {

    private AddressUtils() {}

    /**
     * Returns a best-effort single-line street address for the given coordinates.
     * If unavailable, returns a user-friendly fallback.
     */
    public static String getStreetAddress(Context context, double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> results = geocoder.getFromLocation(latitude, longitude, 1);
            if (results == null || results.isEmpty()) {
                return "Address unavailable";
            }

            Address a = results.get(0);
            // getAddressLine(0) is usually the most complete single line.
            String line = a.getAddressLine(0);
            if (line != null && !line.trim().isEmpty()) {
                return line;
            }

            // Fallback to composing something reasonable.
            String thoroughfare = a.getThoroughfare();
            String locality = a.getLocality();
            String postcode = a.getPostalCode();

            StringBuilder sb = new StringBuilder();
            if (thoroughfare != null) sb.append(thoroughfare);
            if (locality != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(locality);
            }
            if (postcode != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(postcode);
            }

            return sb.length() > 0 ? sb.toString() : "Address unavailable";

        } catch (IOException | IllegalArgumentException e) {
            return "Address unavailable";
        }
    }
}
