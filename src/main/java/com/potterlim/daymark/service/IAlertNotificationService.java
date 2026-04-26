package com.potterlim.daymark.service;

public interface IAlertNotificationService {

    void sendOperationalAlert(String alertType, String message);
}
