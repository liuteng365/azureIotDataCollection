package com.microsoft.azure.datacollection.https;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by liuteng on 16-4-9.
 */
public interface AzureService {
    @PUT("devices/{deviceId}")
    Call<JsonObject> register(@Path("deviceId") String deviceId, @Body JsonObject object);

    @POST("devices/{deviceId}/messages/events")
    Call<JsonObject> sendEvent(@Path("deviceId") String deviceId, @Body String data);

    @GET("devices/{deviceId}/messages/devicebound")
    Call<String> receiveEvent(@Path("deviceId") String deviceId);
}
