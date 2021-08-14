package cc.magickiat.crypto.botnaja.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class Ticker {

    private int id;
    private BigDecimal last;

}