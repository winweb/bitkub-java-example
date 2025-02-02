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
public class Balance {

    protected BigDecimal available;
    @ToString.Exclude
    protected BigDecimal reserved;

}