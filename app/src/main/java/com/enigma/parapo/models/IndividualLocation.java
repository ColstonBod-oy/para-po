package com.enigma.parapo.models;

import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

/**
 * POJO class for an individual location
 */
public class IndividualLocation {

    private String name;
    private String address;
    private String dropOff;
    private String fare;
    private String distance;
    private LineString route;
    private Point location;

    public IndividualLocation(String name, String address, String dropOff, String fare, LineString route, Point location) {
        this.name = name;
        this.address = address;
        this.dropOff = dropOff;
        this.fare = fare;
        this.route = route;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getDropOff() {
        return dropOff;
    }

    public String getFare() {
        return fare;
    }

    public LineString getRoute() {
        return route;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public Point getLocation() {
        return location;
    }
}
