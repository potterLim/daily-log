package com.potterlim.daylog.service;

public interface IAlertNotificationService {

    void sendOperationalAlert(String alertType, String message);
}
