package com.microsoft.azure.datacollection.https;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by liuteng on 16-4-9.
 */
public interface AzureService {
    @POST("devices/{deviceId}/messages/events")
    Call<String> sendEvent(@Path("deviceId") String deviceId, @Body String data);

    @GET("devices/{deviceId}/messages/devicebound")
    Call<String> receiveEvent(@Path("deviceId") String deviceId);

    @DELETE("/devices/{deviceId}/messages/devicebound/{eTag}")
    Call<String> deleteEvent(@Path("deviceId") String deviceId, @Path("eTag") String eTag);
}
