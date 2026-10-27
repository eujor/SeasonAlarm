package com.eujor.seasonalarm;

public class City {
    public final String name;
    public final String zoneId;

    public City(String name, String zoneId) {
        this.name = name;
        this.zoneId = zoneId;
    }

    @Override public String toString() { return name; }
}
