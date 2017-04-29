package com.nooverlap314.prontoremote;

/**
 * Created by Marc on 16/04/2017.
 */

public class RemoteButton {
    private String deviceName;
    private String buttonName;
    private String buttonCode;

    RemoteButton(String deviceName, String buttonName, String buttonCode) {
        this.deviceName = deviceName;
        this.buttonName = buttonName;
        this.buttonCode = buttonCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getButtonName() {
        return buttonName;
    }

    public String getButtonCode() {
        return buttonCode;
    }
}
