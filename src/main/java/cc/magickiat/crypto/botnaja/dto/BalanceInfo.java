package cc.magickiat.crypto.botnaja.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@Data
@ToString(callSuper = true)
public class BalanceInfo extends Balance{

    private BigDecimal price;
    private Ticker ticker;

}