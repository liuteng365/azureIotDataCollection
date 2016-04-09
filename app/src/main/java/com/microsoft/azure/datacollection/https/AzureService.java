package com.microsoft.azure.datacollection.https;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by liuteng on 16-4-9.
 */
public interface AzureService {
    @PUT("devices/{deviceId}")
    Call<JsonObject> register(@Path("deviceId") String deviceId, @Body JsonObject object);
}
