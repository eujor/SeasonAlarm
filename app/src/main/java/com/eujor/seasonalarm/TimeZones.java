package com.eujor.seasonalarm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

public final class TimeZones {
    public static final String HOME_ZONE = "LOCAL";
    public static final String LOCAL_ZONE = "LOCAL";

    private TimeZones() {}

    public static List<City> cities() {
        ArrayList<City> c = new ArrayList<>();
        c.add(new City("Local device time", LOCAL_ZONE, "Home"));
        c.add(new City("Nairobi, Kenya", "Africa/Nairobi", "Africa"));

        c.add(new City("London, UK", "Europe/London", "Europe"));
        c.add(new City("Dublin, Ireland", "Europe/Dublin", "Europe"));
        c.add(new City("Lisbon, Portugal", "Europe/Lisbon", "Europe"));
        c.add(new City("Paris, France", "Europe/Paris", "Europe"));
        c.add(new City("Amsterdam, Netherlands", "Europe/Amsterdam", "Europe"));
        c.add(new City("Frankfurt, Germany", "Europe/Berlin", "Europe"));
        c.add(new City("Rome, Italy", "Europe/Rome", "Europe"));
        c.add(new City("Athens, Greece", "Europe/Athens", "Europe"));
        c.add(new City("Istanbul, Türkiye", "Europe/Istanbul", "Europe"));
        c.add(new City("Moscow, Russia", "Europe/Moscow", "Europe"));

        c.add(new City("New York, USA", "America/New_York", "Americas"));
        c.add(new City("Washington DC, USA", "America/New_York", "Americas"));
        c.add(new City("Boston, USA", "America/New_York", "Americas"));
        c.add(new City("Miami, USA", "America/New_York", "Americas"));
        c.add(new City("Toronto, Canada", "America/Toronto", "Americas"));
        c.add(new City("Halifax, Canada", "America/Halifax", "Americas"));
        c.add(new City("Chicago, USA", "America/Chicago", "Americas"));
        c.add(new City("Dallas, Texas", "America/Chicago", "Americas"));
        c.add(new City("Houston, Texas", "America/Chicago", "Americas"));
        c.add(new City("Austin, Texas", "America/Chicago", "Americas"));
        c.add(new City("Mexico City, Mexico", "America/Mexico_City", "Americas"));
        c.add(new City("Denver, USA", "America/Denver", "Americas"));
        c.add(new City("Phoenix, USA", "America/Phoenix", "Americas"));
        c.add(new City("Seattle, USA", "America/Los_Angeles", "Americas"));
        c.add(new City("Los Angeles, USA", "America/Los_Angeles", "Americas"));
        c.add(new City("San Francisco, USA", "America/Los_Angeles", "Americas"));
        c.add(new City("Vancouver, Canada", "America/Vancouver", "Americas"));
        c.add(new City("Anchorage, Alaska", "America/Anchorage", "Americas"));
        c.add(new City("Honolulu, Hawaii", "Pacific/Honolulu", "Americas"));
        c.add(new City("São Paulo, Brazil", "America/Sao_Paulo", "Americas"));
        c.add(new City("Buenos Aires, Argentina", "America/Argentina/Buenos_Aires", "Americas"));
        c.add(new City("Santiago, Chile", "America/Santiago", "Americas"));

        c.add(new City("Lagos, Nigeria", "Africa/Lagos", "Africa"));
        c.add(new City("Cairo, Egypt", "Africa/Cairo", "Africa"));
        c.add(new City("Johannesburg, South Africa", "Africa/Johannesburg", "Africa"));
        c.add(new City("Addis Ababa, Ethiopia", "Africa/Addis_Ababa", "Africa"));

        c.add(new City("Riyadh, Saudi Arabia", "Asia/Riyadh", "Asia"));
        c.add(new City("Dubai, UAE", "Asia/Dubai", "Asia"));
        c.add(new City("Karachi, Pakistan", "Asia/Karachi", "Asia"));
        c.add(new City("Mumbai / Delhi, India", "Asia/Kolkata", "Asia"));
        c.add(new City("Dhaka, Bangladesh", "Asia/Dhaka", "Asia"));
        c.add(new City("Bangkok, Thailand", "Asia/Bangkok", "Asia"));
        c.add(new City("Jakarta, Indonesia", "Asia/Jakarta", "Asia"));
        c.add(new City("Singapore", "Asia/Singapore", "Asia"));
        c.add(new City("Kuala Lumpur, Malaysia", "Asia/Kuala_Lumpur", "Asia"));
        c.add(new City("Hong Kong", "Asia/Hong_Kong", "Asia"));
        c.add(new City("Beijing, China", "Asia/Shanghai", "Asia"));
        c.add(new City("Shanghai, China", "Asia/Shanghai", "Asia"));
        c.add(new City("Taipei, Taiwan", "Asia/Taipei", "Asia"));
        c.add(new City("Manila, Philippines", "Asia/Manila", "Asia"));
        c.add(new City("Tokyo, Japan", "Asia/Tokyo", "Asia"));
        c.add(new City("Seoul, South Korea", "Asia/Seoul", "Asia"));

        c.add(new City("Perth, Australia", "Australia/Perth", "Oceania"));
        c.add(new City("Adelaide, Australia", "Australia/Adelaide", "Oceania"));
        c.add(new City("Sydney, Australia", "Australia/Sydney", "Oceania"));
        c.add(new City("Melbourne, Australia", "Australia/Melbourne", "Oceania"));
        c.add(new City("Brisbane, Australia", "Australia/Brisbane", "Oceania"));
        c.add(new City("Auckland, New Zealand", "Pacific/Auckland", "Oceania"));
        if (c.size() > 1) {
            List<City> rest = new ArrayList<>(c.subList(1, c.size()));
            rest.sort(Comparator.comparing(x -> x.name.toLowerCase(java.util.Locale.ROOT)));
            ArrayList<City> sorted = new ArrayList<>();
            sorted.add(c.get(0));
            sorted.addAll(rest);
            return sorted;
        }
        return c;
    }

    public static String[] regions() {
        return new String[]{"All", "Home", "Americas", "Europe", "Africa", "Asia", "Oceania"};
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
        return dst ? "Daylight Saving • summer clock" : "Standard Time • winter clock";
    }

    public static String deviceRegionLabel() {
        String id = TimeZone.getDefault().getID();
        String[] p = id.split("/");
        if (p.length >= 2) return p[p.length-1].replace('_',' ') + ", " + p[0];
        return id;
    }

    public static List<City> searchableCities() {
        ArrayList<City> out = new ArrayList<>(cities());
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (City c : out) seen.add(c.zoneId + "|" + c.name);

        ArrayList<String> ids = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(ids);
        for (String id : ids) {
            if (!id.contains("/") || id.startsWith("Etc/")) continue;
            String[] parts = id.split("/");
            String city = parts[parts.length - 1].replace('_', ' ');
            String region = parts[0].equals("America") ? "Americas" : parts[0];
            String display = city + "  •  " + id;
            String key = id + "|" + display;
            if (seen.add(key)) out.add(new City(display, id, region));
        }

        out.sort((a, b) -> {
            if (LOCAL_ZONE.equals(a.zoneId)) return -1;
            if (LOCAL_ZONE.equals(b.zoneId)) return 1;
            int byName = a.name.compareToIgnoreCase(b.name);
            return byName != 0 ? byName : a.zoneId.compareToIgnoreCase(b.zoneId);
        });
        return out;
    }

    public static String currentClockLine(String name, String zoneId) {
        ZoneId zone = resolveZone(zoneId);
        ZonedDateTime now = ZonedDateTime.now(zone);
        String t = now.format(DateTimeFormatter.ofPattern("EEE, dd MMM  HH:mm:ss  z"));
        return name + "\n" + t + "\n" + seasonText(zone, now.toInstant());
    }

    public static String offsetFromHome(String zoneId) {
        Instant now = Instant.now();
        ZoneId zone = resolveZone(zoneId);
        ZoneId home = resolveZone(HOME_ZONE);
        int diff = zone.getRules().getOffset(now).getTotalSeconds() - home.getRules().getOffset(now).getTotalSeconds();
        if (diff == 0) return "Same time as device";
        int abs = Math.abs(diff);
        int hours = abs / 3600;
        int minutes = (abs % 3600) / 60;
        String amount = hours + "h" + (minutes == 0 ? "" : " " + minutes + "m");
        return (diff > 0 ? "+" : "−") + amount + " vs device";
    }
}
