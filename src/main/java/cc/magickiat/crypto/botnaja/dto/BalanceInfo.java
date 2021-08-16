package cc.magickiat.crypto.botnaja.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@NoArgsConstructor
@Data
public class BalanceInfo{

    private Balance balance;
    private Ticker ticker;

    private BigDecimal value;

    @Override
    public String toString() {
        return String.format("BalanceInfo{available=%16s, reserved=%16s, value=%20s, last=%12s, percentChange=%7s}", balance.available, balance.reserved, value, ticker.last, ticker.percentChange);
    }
}