package cc.magickiat.crypto.botnaja.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@Data
public class BalanceInfo extends Balance{

    private BigDecimal value;
    private Ticker ticker;

    @Override
    public String toString() {
        return String.format("BalanceInfo{available=%16s, reserved=%16s, value=%20s, last=%12s, percentChange=%7s}", available, reserved, value, ticker.getLast(), ticker.getPercentChange());
    }
}