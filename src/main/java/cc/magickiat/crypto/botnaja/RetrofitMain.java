package cc.magickiat.crypto.botnaja;

import cc.magickiat.crypto.botnaja.dto.Balance;
import cc.magickiat.crypto.botnaja.dto.BalanceInfo;
import cc.magickiat.crypto.botnaja.dto.Ticker;
import cc.magickiat.crypto.botnaja.service.BitKubService;

import java.util.Map;

public class RetrofitMain {

    public static void main(String[] args) throws Exception {
        BitKubService service = BitKubService.getInstance();

        Long serverTime = service.getServerTime();
        System.out.println("===== Server Time =====");
        System.out.println(serverTime);

        Map<String, Balance> balances = service.getBalances(serverTime);
        System.out.println("===== My Balances =====");
        balances.forEach((k, v) -> System.out.printf("%-6s = %s%n", k, v));

        Map<String, Ticker> tickerMap = service.getTickers();
        System.out.println("===== Tickers =====");
        tickerMap.forEach((k, v) -> System.out.printf("%-6s = %s%n", k, v));

        Map<String, Ticker> finalTickerMap = tickerMap;

        tickerMap = service.getTicker("THB_BTC");
        System.out.println("===== Ticker for THB_BTC =====");
        System.out.println(tickerMap);

        Map<String, BalanceInfo> sortBalanceInfoMap = service.getBalanceInfo(balances, finalTickerMap);
        System.out.println("\n===== My Balances Info =====");
        sortBalanceInfoMap.forEach((k, v) -> System.out.printf("%-6s = %s%n", k, v));

    }
}