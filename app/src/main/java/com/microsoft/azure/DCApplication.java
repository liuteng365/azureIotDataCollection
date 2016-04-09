package com.microsoft.azure;

import android.app.Application;

import com.microsoft.azure.iothub.DeviceClientConfig;

import java.net.URISyntaxException;

/**
 * Created by liuteng on 16-4-9.
 */
public class DCApplication extends Application {

    public DeviceClientConfig config;
    private static DCApplication app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        try {
            config = new DeviceClientConfig("devicecontrolhub.azure-devices.net", "device1", "g/Bu9TNbQ+CER56BlAhP5u3MnHgRF014vkW7bSKgTOc=");
        } catch (URISyntaxException e) {
        }
    }

    public static DCApplication getInstance() {
        return app;
    }

    public DeviceClientConfig getConfig() {
        if (config == null) {
            try {
                config = new DeviceClientConfig("devicecontrolhub.azure-devices.net", "device1", "g/Bu9TNbQ+CER56BlAhP5u3MnHgRF014vkW7bSKgTOc=");
            } catch (URISyntaxException e) {
            }
        }
        return config;
    }
}
