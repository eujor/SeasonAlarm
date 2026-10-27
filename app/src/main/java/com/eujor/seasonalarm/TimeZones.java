package com.eujor.seasonalarm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class TimeZones {
    public static final String HOME_ZONE = "Africa/Nairobi";
    public static final String LOCAL_ZONE = "LOCAL";

    private TimeZones() {}

    public static List<City> cities() {
        ArrayList<City> c = new ArrayList<>();
        c.add(new City("Local device time", LOCAL_ZONE));
        c.add(new City("Nairobi, Kenya", "Africa/Nairobi"));
        c.add(new City("London, UK", "Europe/London"));
        c.add(new City("New York, USA", "America/New_York"));
        c.add(new City("Washington DC, USA", "America/New_York"));
        c.add(new City("Boston, USA", "America/New_York"));
        c.add(new City("Toronto, Canada", "America/Toronto"));
        c.add(new City("Chicago, USA", "America/Chicago"));
        c.add(new City("Dallas, Texas", "America/Chicago"));
        c.add(new City("Houston, Texas", "America/Chicago"));
        c.add(new City("Austin, Texas", "America/Chicago"));
        c.add(new City("Denver, USA", "America/Denver"));
        c.add(new City("Phoenix, USA", "America/Phoenix"));
        c.add(new City("Seattle, USA", "America/Los_Angeles"));
        c.add(new City("Los Angeles, USA", "America/Los_Angeles"));
        c.add(new City("San Francisco, USA", "America/Los_Angeles"));
        c.add(new City("Vancouver, Canada", "America/Vancouver"));
        c.add(new City("Paris, France", "Europe/Paris"));
        c.add(new City("Frankfurt, Germany", "Europe/Berlin"));
        c.add(new City("Amsterdam, Netherlands", "Europe/Amsterdam"));
        c.add(new City("Dubai, UAE", "Asia/Dubai"));
        c.add(new City("Mumbai / Delhi, India", "Asia/Kolkata"));
        c.add(new City("Beijing, China", "Asia/Shanghai"));
        c.add(new City("Shanghai, China", "Asia/Shanghai"));
        c.add(new City("Hong Kong", "Asia/Hong_Kong"));
        c.add(new City("Singapore", "Asia/Singapore"));
        c.add(new City("Tokyo, Japan", "Asia/Tokyo"));
        c.add(new City("Seoul, South Korea", "Asia/Seoul"));
        c.add(new City("Sydney, Australia", "Australia/Sydney"));
        c.add(new City("Melbourne, Australia", "Australia/Melbourne"));
        return c;
    }

    public static ZoneId resolveZone(String id) {
        if (LOCAL_ZONE.equals(id)) return ZoneId.of(TimeZone.getDefault().getID());
        return ZoneId.of(id);
    }

    public static String seasonText(ZoneId zone, Instant instant) {
        ZoneRules rules = zone.getRules();
        boolean hasSeasonalShift = !rules.getTransitionRules().isEmpty();
        if (!hasSeasonalShift) return "No seasonal clock change";
        boolean dst = rules.isDaylightSavings(instant);
        return dst ? "Daylight Saving (summer clock)" : "Standard Time (winter clock)";
    }

    public static String currentClockLine(String name, String zoneId) {
        ZoneId zone = resolveZone(zoneId);
        ZonedDateTime now = ZonedDateTime.now(zone);
        String t = now.format(DateTimeFormatter.ofPattern("EEE, dd MMM  HH:mm:ss  z"));
        return name + "\n" + t + "\n" + seasonText(zone, now.toInstant());
    }
}
