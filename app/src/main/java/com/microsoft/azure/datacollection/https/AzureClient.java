package com.microsoft.azure.datacollection.https;

import android.util.Log;

import com.microsoft.azure.DCApplication;
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
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by liuteng on 16-4-9.
 */
public class AzureClient {

    public static AzureClient instance = null;
    private final Retrofit client;
    private final AzureService azureService;

    private AzureClient() throws URISyntaxException {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(
                new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Request original = chain.request();
                        IotHubSasToken sasToken = new IotHubSasToken(IotHubUri.getResourceUri(getConfig().getIotHubHostname(), getConfig().getDeviceId()),
                                getConfig().getDeviceKey(),
                                System.currentTimeMillis() / 1000l + getConfig().getTokenValidSecs() + 1l);
                        Log.e("1111111111111111", sasToken.toString());
                        Request request = original.newBuilder()
                                .addHeader("api-version", "2016-02-03")
                                .addHeader("IoTHubName", getConfig().getIotHubName())
                                .addHeader("Authorization", sasToken.toString())
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build();

                        return chain.proceed(request);
                    }
                });

        OkHttpClient okClient = httpClient.build();
        client = new Retrofit.Builder()
                .baseUrl("https://" + getConfig().getIotHubHostname())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okClient)
                .build();
        azureService = client.create(AzureService.class);
    }

    public static AzureClient getInstance() {
        if (instance == null) {
            synchronized (AzureClient.class) {
                if (instance == null) {
                    try {
                        instance = new AzureClient();
                    } catch (URISyntaxException e) {
                    }
                }
            }
        }
        return instance;
    }

    public AzureService getAzureService() {
        return azureService;
    }

    public static DeviceClientConfig getConfig() {
        return DCApplication.getInstance().getConfig();
    }
}
