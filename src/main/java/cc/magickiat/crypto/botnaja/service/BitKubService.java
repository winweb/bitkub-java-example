package cc.magickiat.crypto.botnaja.service;

import cc.magickiat.crypto.botnaja.dto.Balance;
import cc.magickiat.crypto.botnaja.dto.BalanceInfo;
import cc.magickiat.crypto.botnaja.dto.BitKubRequestBody;
import cc.magickiat.crypto.botnaja.dto.Ticker;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


import static okhttp3.logging.HttpLoggingInterceptor.Level.*;

public class BitKubService {

    private static final String BASE_URL = "https://api.bitkub.com";

    private Retrofit createNormalRetrofit() {
        OkHttpClient.Builder clientBuilder = createHttpClientBuilder();
        return createRetrofit(clientBuilder);
    }

    private Retrofit createSecuredRetrofit() {
        OkHttpClient.Builder clientBuilder = createHttpClientBuilder();
        clientBuilder.addInterceptor(new ApiKeySecretInterceptor());
        return createRetrofit(clientBuilder);
    }

    private Retrofit createRetrofit(OkHttpClient.Builder clientBuilder) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .client(clientBuilder.build())
                .build();
    }

    private OkHttpClient.Builder createHttpClientBuilder() {
        HttpLoggingInterceptor logging = HttpClient.getLoggingInterceptor();

        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(logging);
        client.addInterceptor(new ErrorInterceptor());
        return client;
    }

    public Long getServerTime() throws IOException {
        Retrofit retrofit = createNormalRetrofit();
        BitKubApi api = retrofit.create(BitKubApi.class);
        return api.serverTime().execute().body();
    }

    public Map<String, Balance> getBalances() throws IOException {
        Retrofit securedRetrofit = createSecuredRetrofit();
        BitKubApi api = securedRetrofit.create(BitKubApi.class);

        Long ts = getServerTime();
        BitKubRequestBody requestBody = new BitKubRequestBody();
        requestBody.setTs(ts);
        requestBody.setSig(HmacService.calculateHmac("{\"ts\":" + ts + "}"));

        var result = api.getBalances(requestBody).execute().body();

        assert result != null;
        return result.getResult();
    }

    public Map<String, Ticker> getTickers() throws IOException {
        Retrofit retrofit = createNormalRetrofit();
        BitKubApi api = retrofit.create(BitKubApi.class);
        return api.marketTickers(null).execute().body();
    }

    public Map<String, Ticker> getTicker(String symbol) throws IOException {
        Retrofit retrofit = createNormalRetrofit();
        BitKubApi api = retrofit.create(BitKubApi.class);
        return api.marketTickers(symbol).execute().body();
    }

    public Map<String, BalanceInfo> getBalanceInfo() throws IOException {
        return getBalanceInfo(null, null);
    }

    public Map<String, BalanceInfo> getBalanceInfo(Map<String, Balance> balances, Map<String, Ticker> tickerMap) throws IOException {
        if(balances == null) {
            balances = getBalances();
        }

        if(tickerMap == null) {
            tickerMap = getTickers();
        }

        Map<String, Ticker> finalTickerMap = tickerMap;

        Map<String, BalanceInfo> balanceInfoMap = balances.entrySet()
                .stream()
                .filter(b -> b.getValue().getAvailable().compareTo(BigDecimal.ZERO) != 0 || b.getValue().getReserved().compareTo(BigDecimal.ZERO) != 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            final BigDecimal available = e.getValue().getAvailable();
                            final BigDecimal reserved = e.getValue().getReserved();
                            final Ticker ticker = "THB".equals(e.getKey())? null: finalTickerMap.get("THB_" + e.getKey());

                            return BalanceInfo.builder()
                                    .available(available)
                                    .reserved(reserved)
                                    .ticker(ticker == null? new Ticker(): ticker)
                                    .value(ticker == null? null: available.add(reserved).multiply(ticker.getLast()))
                                    .build();
                        }));

        return new TreeMap<>(balanceInfoMap);
    }
}

class HttpClient {
    private static final Logger log = LogManager.getLogger(HttpClient.class);

    private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);

    static {
        if (log.isTraceEnabled()) {
            loggingInterceptor.level(BODY);
        }
        else if (log.isDebugEnabled()) {
            loggingInterceptor.level(BASIC);
        }
    }

    public static HttpLoggingInterceptor getLoggingInterceptor() {
        return loggingInterceptor;
    }
}