package cc.magickiat.crypto.botnaja.service;

import cc.magickiat.crypto.botnaja.dto.Balance;
import cc.magickiat.crypto.botnaja.dto.BalanceInfo;
import cc.magickiat.crypto.botnaja.dto.BitKubRequestBody;
import cc.magickiat.crypto.botnaja.dto.Ticker;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


import static okhttp3.logging.HttpLoggingInterceptor.Level.*;

@Log4j2
public class BitKubService {

    protected static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);

    static {
        if (log.isTraceEnabled()) {
            loggingInterceptor.level(BODY);
        }
        else if (log.isDebugEnabled()) {
            loggingInterceptor.level(BASIC);
        }
    }

    protected static final String BASE_URL = "https://api.bitkub.com";

    protected Retrofit createNormalRetrofit() {
        OkHttpClient.Builder clientBuilder = createHttpClientBuilder();
        return createRetrofit(clientBuilder);
    }

    protected Retrofit createSecuredRetrofit() {
        OkHttpClient.Builder clientBuilder = createHttpClientBuilder();
        clientBuilder.addInterceptor(new ApiKeySecretInterceptor());
        return createRetrofit(clientBuilder);
    }

    protected Retrofit createRetrofit(OkHttpClient.Builder clientBuilder) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .client(clientBuilder.build())
                .build();
    }

    protected OkHttpClient.Builder createHttpClientBuilder() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(loggingInterceptor);
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
                            Balance balance = e.getValue();
                            final BigDecimal available = balance.getAvailable();
                            final BigDecimal reserved = balance.getReserved();
                            final Ticker ticker = "THB".equals(e.getKey())? null: finalTickerMap.get("THB_" + e.getKey());

                            return BalanceInfo.builder()
                                    .balance(balance)
                                    .ticker(ticker == null? new Ticker(): ticker)
                                    .value(ticker == null? null: available.add(reserved).multiply(ticker.getLast()))
                                    .build();
                        }));

        return new TreeMap<>(balanceInfoMap);
    }
}