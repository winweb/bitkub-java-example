package cc.magickiat.crypto.botnaja.service;

import cc.magickiat.crypto.botnaja.dto.Balance;
import cc.magickiat.crypto.botnaja.dto.BitKubRequestBody;
import cc.magickiat.crypto.botnaja.dto.BitKubResponseBody;
import cc.magickiat.crypto.botnaja.dto.Ticker;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface BitKubApi {
    @GET("api/servertime")
    Call<Long> serverTime();

    @GET("api/market/ticker")
    Call<Map<String, Ticker>> marketTickers(@Query("sym") String sym);

    @POST("api/market/balances")
    @Headers({
            "accept: application/json",
            "content-type: application/json"
    })
    Call<BitKubResponseBody<Map<String, Balance>>> getBalances(@Body BitKubRequestBody body);
}