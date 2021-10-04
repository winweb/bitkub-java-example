package cc.magickiat.crypto.botnaja.service;

import cc.magickiat.crypto.botnaja.dto.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static okhttp3.logging.HttpLoggingInterceptor.Level;

@Slf4j
public class BitKubService {

    private static BitKubService bitKubServiceInstance;
    public static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);
    public static final OkHttpClient clientBuilder = new OkHttpClient.Builder()
                                                        .connectionPool(new ConnectionPool(50, 50, TimeUnit.SECONDS))
                                                        .addInterceptor(loggingInterceptor)
                                                        .addInterceptor(new ErrorInterceptor())
                                                        .connectTimeout(10, TimeUnit.SECONDS)
                                                        .readTimeout(20, TimeUnit.SECONDS)
                                                        .writeTimeout(20, TimeUnit.SECONDS)
                                                        .retryOnConnectionFailure(true)
                                                        .build();

    public static final ObjectMapper mapper = new ObjectMapper();

    public static Retrofit normalRetrofit;
    public static Retrofit secureRetrofit;
    public static BitKubApi normalBitkubApi;
    public static BitKubApi secureBitkubApi;

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        normalRetrofit = createRetrofit(clientBuilder);

        OkHttpClient.Builder secureClientBuilder = clientBuilder.newBuilder();

        secureClientBuilder.addInterceptor(new ApiKeySecretInterceptor());
        secureRetrofit = createRetrofit(secureClientBuilder.build());

        normalBitkubApi = normalRetrofit.create(BitKubApi.class);
        secureBitkubApi = secureRetrofit.create(BitKubApi.class);
    }

    public BitKubService() {

        if (log.isTraceEnabled()) {
            loggingInterceptor.setLevel(Level.BODY);
        }
        else if (log.isDebugEnabled()) {
            loggingInterceptor.setLevel(Level.BASIC);
        }
    }

    public static BitKubService getInstance() {
        if (bitKubServiceInstance == null) {
            bitKubServiceInstance = new BitKubService();
        }
        return bitKubServiceInstance;
    }

    public static Retrofit createRetrofit(OkHttpClient clientBuilder) {
        return new Retrofit.Builder()
                            .baseUrl("https://api.bitkub.com")
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .addConverterFactory(JacksonConverterFactory.create(mapper))
                            .client(clientBuilder)
                            .build();
    }

    public Long getServerTime() throws IOException {
        return normalBitkubApi.serverTime()
                                .execute()
                                .body();
    }

    public Map<String, Balance> getBalances() throws IOException {
        return getBalances(getServerTime());
    }

    public Map<String, Balance> getBalances(long ts) throws IOException {
        BitKubRequestBody requestBody = new BitKubRequestBody();
        requestBody.setTs(ts);
        requestBody.setSig(HmacService.calculateHmac("{\"ts\":" + ts + "}"));

        BitKubResponseBody<Map<String, Balance>> result = secureBitkubApi.getBalances(requestBody)
                                                                            .execute()
                                                                            .body();

        assert result != null;

        return result.getResult();
    }

    public Map<String, Ticker> getTickers() throws IOException {
        return normalBitkubApi.marketTickers(null)
                                .execute()
                                .body();
    }

    public Map<String, Ticker> getTicker(String symbol) throws IOException {
        return normalBitkubApi.marketTickers(symbol)
                                .execute()
                                .body();
    }

    public Map<String, BalanceInfo> getBalanceInfo() throws IOException {
        Map<String, Balance> balances = getBalances();
        Map<String, Ticker> tickers = getTickers();

        return getBalanceInfo(balances, tickers);
    }

    public Map<String, BalanceInfo> getBalanceInfo(Long ts) throws IOException {
        Map<String, Balance> balances = getBalances(ts);
        Map<String, Ticker> tickers = getTickers();

        return getBalanceInfo(balances, tickers);
    }

    public Map<String, BalanceInfo> getBalanceInfo(Map<String, Balance> balances, final Map<String, Ticker> tickerMap) {

        Map<String, BalanceInfo> balanceInfoMap = balances.entrySet()
            .stream()
            .filter(b -> {
                Balance balance = b.getValue();
                return balance.getAvailable().compareTo(BigDecimal.ZERO) != 0 ||
                        balance.getReserved().compareTo(BigDecimal.ZERO) != 0;
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    final Balance balance = e.getValue();
                    final BigDecimal available = balance.getAvailable();
                    final BigDecimal reserved = balance.getReserved();
                    final String symbol = "THB_" + e.getKey();
                    final Ticker ticker = !tickerMap.containsKey(symbol)? new Ticker(): tickerMap.get(symbol);

                    return BalanceInfo.builder()
                            .balance(balance)
                            .ticker(ticker)
                            .value(ticker.getLast() == null? BigDecimal.ZERO: available.add(reserved).multiply(ticker.getLast()))
                            .build();
                }));

        return new TreeMap<>(balanceInfoMap);
    }
}