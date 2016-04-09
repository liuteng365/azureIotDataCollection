package com.microsoft.azure.datacollection.http;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by liuteng on 16-4-9.
 */
public interface AzureService {
    @GET("api/{school}")
    Call<List<String>> listName(@Path("school") String school);
}
