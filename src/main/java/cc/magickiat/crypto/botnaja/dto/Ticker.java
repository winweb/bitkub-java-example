package cc.magickiat.crypto.botnaja.dto;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class Ticker {

    private int id;
    private BigDecimal last;
    private BigDecimal percentChange;

}