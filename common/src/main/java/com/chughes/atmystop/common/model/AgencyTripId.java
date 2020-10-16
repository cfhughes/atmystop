package com.chughes.atmystop.common.model;

import java.io.Serializable;

public class AgencyTripId implements Serializable {

    private String agencyId;

    private String tripId;

    public AgencyTripId(String agencyId, String tripId) {
        this.agencyId = agencyId;
        this.tripId = tripId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgencyTripId that = (AgencyTripId) o;

        if (agencyId != null ? !agencyId.equals(that.agencyId) : that.agencyId != null) return false;
        return tripId != null ? tripId.equals(that.tripId) : that.tripId == null;
    }

    @Override
    public int hashCode() {
        int result = agencyId != null ? agencyId.hashCode() : 0;
        result = 31 * result + (tripId != null ? tripId.hashCode() : 0);
        return result;
    }

    public String getTripId() {
        return tripId;
    }

    public String getAgencyId() {
        return agencyId;
    }
}
