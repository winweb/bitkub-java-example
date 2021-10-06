package cc.magickiat.crypto.botnaja.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    protected int id;
    protected BigDecimal last;
    @JsonIgnore
    protected BigDecimal lowestAsk;
    @JsonIgnore
    protected BigDecimal highestBid;
    protected BigDecimal change;
    protected BigDecimal percentChange;
    protected BigDecimal baseVolume;
    protected BigDecimal quoteVolume;
    @JsonIgnore
    protected int isFrozen;
    protected BigDecimal high24hr;
    protected BigDecimal low24hr;

}
