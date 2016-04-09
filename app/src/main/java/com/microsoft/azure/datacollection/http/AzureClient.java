package com.microsoft.azure.datacollection.http;

import com.microsoft.azure.iothub.DeviceClientConfig;
import com.microsoft.azure.iothub.auth.IotHubSasToken;
import com.microsoft.azure.iothub.net.IotHubUri;

import java.io.IOException;
import java.net.URISyntaxException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

/**
 * Created by liuteng on 16-4-9.
 */
public class AzureClient {

    public static AzureClient instance = null;
    private final Retrofit client;
    private final AzureService azureService;

    private DeviceClientConfig config;

    private AzureClient() throws URISyntaxException {
        config = new DeviceClientConfig("devicecontrolhub.azure-devices.net", "LchouchouT", "qmBQUbf3JJNaniP6glRF9JnKTAoGFNz7i1+87pln9wg=");
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(
                new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Request original = chain.request();
                        IotHubSasToken sasToken = new IotHubSasToken(IotHubUri.getResourceUri(config.getIotHubHostname(), config.getDeviceId()),
                                config.getDeviceKey(),
                                System.currentTimeMillis() / 1000l + config.getTokenValidSecs() + 1l);
                        Request request = original.newBuilder()
                                .addHeader("api-version", "2016-02-03")
                                .addHeader("IoTHubName", config.getIotHubName())
                                .addHeader("Authorization", sasToken.toString())
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build();

                        return chain.proceed(request);
                    }
                });

        OkHttpClient okClient = httpClient.build();
        client = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .client(okClient)
                .build();
        azureService = client.create(AzureService.class);
    }

    public static AzureClient getInstance() throws URISyntaxException {
        if (instance == null) {
            synchronized (AzureClient.class) {
                if (instance == null) {
                    instance = new AzureClient();
                }
            }
        }
        return instance;
    }

    public AzureService getAzureService() {
        return azureService;
    }
}
