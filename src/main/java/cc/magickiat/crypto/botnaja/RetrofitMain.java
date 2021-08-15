package cc.magickiat.crypto.botnaja;

import cc.magickiat.crypto.botnaja.dto.Balance;
import cc.magickiat.crypto.botnaja.dto.BalanceInfo;
import cc.magickiat.crypto.botnaja.dto.Ticker;
import cc.magickiat.crypto.botnaja.service.BitKubService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RetrofitMain {

    public static void main(String[] args) throws Exception {
        BitKubService service = new BitKubService();

        Long serverTime = service.getServerTime();
        System.out.println("===== Server Time =====");
        System.out.println(serverTime);

        Map<String, Balance> balances = service.getBalances();
        System.out.println("===== My Balances =====");
        System.out.println(balances);

        Map<String, Ticker> tickerMap = service.getTickers();
        System.out.println("===== Tickers =====");
        System.out.println(tickerMap);

        Map<String, Ticker> finalTickerMap = tickerMap;

        tickerMap = service.getTicker("THB_BTC");
        System.out.println("===== Ticker for THB_BTC =====");
        System.out.println(tickerMap);

        Map<String, BalanceInfo> balanceInfoMap = balances.entrySet()
                .stream()
                .filter(b -> b.getValue().getAvailable().compareTo(BigDecimal.ZERO) != 0 || b.getValue().getReserved().compareTo(BigDecimal.ZERO) != 0)
                .collect(Collectors.toMap(
                        t -> t.getKey(),
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

        Map<String, BalanceInfo> sortBalanceInfoMap = new TreeMap<>(balanceInfoMap);

        System.out.println("\n===== My Balances Info =====");
        sortBalanceInfoMap.forEach((k, v) -> System.out.printf("%-6s = %s%n", k, v));

    }
}