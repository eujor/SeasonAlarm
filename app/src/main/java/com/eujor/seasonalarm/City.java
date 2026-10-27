package com.eujor.seasonalarm;

public class City {
    public final String name;
    public final String zoneId;
    public final String region;

    public City(String name, String zoneId) {
        this(name, zoneId, "Other");
    }

    public City(String name, String zoneId, String region) {
        this.name = name;
        this.zoneId = zoneId;
        this.region = region;
    }

    @Override public String toString() { return name; }
}
