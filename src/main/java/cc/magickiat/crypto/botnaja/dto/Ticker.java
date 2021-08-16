package cc.magickiat.crypto.botnaja.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@NoArgsConstructor
@Data
@ToString
public class Ticker {

    protected int id;
    protected BigDecimal last;
    protected BigDecimal lowestAsk;
    protected BigDecimal highestBid;
    protected BigDecimal change;
    protected BigDecimal percentChange;
    protected BigDecimal baseVolume;
    protected BigDecimal quoteVolume;
    protected int isFrozen;
    protected BigDecimal high24hr;
    protected BigDecimal low24hr;

}