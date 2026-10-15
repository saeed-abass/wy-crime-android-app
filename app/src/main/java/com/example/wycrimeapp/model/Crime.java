package com.example.wycrimeapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "crime")
public class Crime implements Serializable {

    @PrimaryKey
    @NonNull
    private String crimeId;

    private String month;
    private String reportedBy;
    private String fallsWithin;
    private double longitude;
    private double latitude;
    private String location;
    private String lsoaCode;
    private String lsoaName;
    private String crimeType;
    private String lastOutcomeCategory;
    private String context;

    // Empty constructor
    public Crime() {}

    // Full constructor
    public Crime(@NonNull String crimeId, String month, String reportedBy, String fallsWithin,
                 double longitude, double latitude, String location, String lsoaCode,
                 String lsoaName, String crimeType, String lastOutcomeCategory, String context) {
        this.crimeId = crimeId;
        this.month = month;
        this.reportedBy = reportedBy;
        this.fallsWithin = fallsWithin;
        this.longitude = longitude;
        this.latitude = latitude;
        this.location = location;
        this.lsoaCode = lsoaCode;
        this.lsoaName = lsoaName;
        this.crimeType = crimeType;           // FIXED: was "crimeTypeType"
        this.lastOutcomeCategory = lastOutcomeCategory;
        this.context = context;
    }

    // GETTERS & SETTERS
    @NonNull
    public String getCrimeId() { return crimeId; }
    public void setCrimeId(@NonNull String crimeId) { this.crimeId = crimeId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public String getFallsWithin() { return fallsWithin; }
    public void setFallsWithin(String fallsWithin) { this.fallsWithin = fallsWithin; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLsoaCode() { return lsoaCode; }
    public void setLsoaCode(String lsoaCode) { this.lsoaCode = lsoaCode; }

    public String getLsoaName() { return lsoaName; }
    public void setLsoaName(String lsoaName) { this.lsoaName = lsoaName; }

    public String getCrimeType() { return crimeType; }
    public void setCrimeType(String crimeType) { this.crimeType = crimeType; }

    public String getLastOutcomeCategory() { return lastOutcomeCategory; }
    public void setLastOutcomeCategory(String lastOutcomeCategory) { this.lastOutcomeCategory = lastOutcomeCategory; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}