package cc.magickiat.crypto.botnaja.service;

import cc.magickiat.crypto.botnaja.dto.Balance;
import cc.magickiat.crypto.botnaja.dto.BalanceInfo;
import cc.magickiat.crypto.botnaja.dto.BitKubRequestBody;
import cc.magickiat.crypto.botnaja.dto.BitKubResponseBody;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import static okhttp3.logging.HttpLoggingInterceptor.Level.*;

@Log4j2
public class BitKubService {

    public static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);
    public static final ObjectMapper mapper = new ObjectMapper();
    public static final ApiKeySecretInterceptor apiKeySecretInterceptor = new ApiKeySecretInterceptor();
    public static final String BASE_URL = "https://api.bitkub.com";

    static {
        if (log.isTraceEnabled()) {
            loggingInterceptor.level(BODY);
        }
        else if (log.isDebugEnabled()) {
            loggingInterceptor.level(BASIC);
        }

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected Retrofit createNormalRetrofit() {
        OkHttpClient.Builder clientBuilder = createHttpClientBuilder();
        return createRetrofit(clientBuilder);
    }

    protected Retrofit createSecuredRetrofit() {
        OkHttpClient.Builder clientBuilder = createHttpClientBuilder();
        clientBuilder.addInterceptor(apiKeySecretInterceptor);
        return createRetrofit(clientBuilder);
    }

    protected OkHttpClient.Builder createHttpClientBuilder() {
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new ErrorInterceptor())
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
    }

    protected Retrofit createRetrofit(OkHttpClient.Builder clientBuilder) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .client(clientBuilder.build())
                .build();
    }

    public Long getServerTime() throws IOException {
        Retrofit retrofit = createNormalRetrofit();
        BitKubApi api = retrofit.create(BitKubApi.class);
        return api.serverTime().execute().body();
    }

    public Map<String, Balance> getBalances() throws IOException {
        return  getBalances(getServerTime());
    }

    public Map<String, Balance> getBalances(long ts) throws IOException {
        Retrofit securedRetrofit = this.createSecuredRetrofit();
        BitKubApi api = securedRetrofit.create(BitKubApi.class);
        BitKubRequestBody requestBody = new BitKubRequestBody();
        requestBody.setTs(ts);
        requestBody.setSig(HmacService.calculateHmac("{\"ts\":" + ts + "}"));
        BitKubResponseBody<Map<String, Balance>> result = api.getBalances(requestBody).execute().body();

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
        return getBalanceInfo(getBalances(), getTickers());
    }

    public Map<String, BalanceInfo> getBalanceInfo(Long ts) throws IOException {
        return getBalanceInfo(getBalances(ts), getTickers());
    }

    public Map<String, BalanceInfo> getBalanceInfo(Map<String, Balance> balances, final Map<String, Ticker> tickerMap) {

        Map<String, BalanceInfo> balanceInfoMap = balances.entrySet()
            .stream()
            .filter(b -> {
                Balance balance = b.getValue();
                return balance.getAvailable().compareTo(BigDecimal.ZERO) != 0 || balance.getReserved().compareTo(BigDecimal.ZERO) != 0;
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Balance balance = e.getValue();
                    final BigDecimal available = balance.getAvailable();
                    final BigDecimal reserved = balance.getReserved();
                    final String symbol = "THB_" + e.getKey();
                    final Ticker ticker = !tickerMap.containsKey(symbol)? new Ticker(): tickerMap.get(symbol);

                    return BalanceInfo.builder()
                            .balance(balance)
                            .ticker(ticker)
                            .value(ticker.getLast() == null? null: available.add(reserved).multiply(ticker.getLast()))
                            .build();
                }));

        return new TreeMap<>(balanceInfoMap);
    }
}