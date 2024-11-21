package nl.tno.essim.util;

import java.time.LocalDateTime;
import java.util.Date;

public class DateRangeUtils {
    /**
     * Determines if two date ranges overlap.
     *
     * @param from1 The start date-time of the first date range (inclusive).
     * @param to1 The end date-time of the first date range (exclusive). Can be null to indicate open-ended range.
     * @param from2 The start date-time of the second date range (inclusive).
     * @param to2 The end date-time of the second date range (exclusive). Can be null to indicate open-ended range.
     * @return true if there is an overlap in the date ranges, false otherwise.
     */
    public static boolean hasOverlap(LocalDateTime from1, LocalDateTime to1, LocalDateTime from2, LocalDateTime to2) {
        // If both `to` values are null, they overlap infinitely
        if (to1 == null && to2 == null) {
            return true;
        }

        // Swap null end dates with the latest possible date
        if (to1 == null) {
            to1 = LocalDateTime.MAX;
        }
        if (to2 == null) {
            to2 = LocalDateTime.MAX;
        }

        // Compare the ranges
        return (from1.isBefore(to2) || from1.equals(to2)) && (from2.isBefore(to1) || from2.equals(to1));
    }
}
