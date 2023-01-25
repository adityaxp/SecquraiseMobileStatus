package com.aditya.secquraisemobilestatus;

public class MobileStatusData {

    String imeiNumber, connectionStatus, chargingStatus, batteryPercentage, locationLatitude, locationLongitude, capturedImageURL, timeStamp;

    public MobileStatusData(){}

    public MobileStatusData(String imeiNumber, String connectionStatus, String chargingStatus, String batteryPercentage, String locationLatitude, String locationLongitude, String capturedImageURL, String timeStamp) {
        this.imeiNumber = imeiNumber;
        this.connectionStatus = connectionStatus;
        this.chargingStatus = chargingStatus;
        this.batteryPercentage = batteryPercentage;
        this.locationLatitude = locationLatitude;
        this.locationLongitude = locationLongitude;
        this.capturedImageURL = capturedImageURL;
        this.timeStamp = timeStamp;
    }

    public String getImeiNumber() {
        return imeiNumber;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public String getChargingStatus() {
        return chargingStatus;
    }

    public String getBatteryPercentage() {
        return batteryPercentage;
    }

    public String getLocationLatitude() {
        return locationLatitude;
    }

    public String getLocationLongitude() {
        return locationLongitude;
    }

    public String getCapturedImageURL() {
        return capturedImageURL;
    }

    public String getTimeStamp() {
        return timeStamp;
    }
}
