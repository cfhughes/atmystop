package com.chughes.atmystop.common.model;

import java.io.Serializable;

public class TripHeadSign implements Serializable {

    private String name;

    private String route;

    private String color;

    private String textColor;

    public TripHeadSign(String name, String route, String color, String textColor) {
        this.name = name;
        this.route = route;
        this.color = color;
        this.textColor = textColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TripHeadSign that = (TripHeadSign) o;

        if (!name.equals(that.name)) return false;
        return route.equals(that.route);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + route.hashCode();
        return result;
    }
}
