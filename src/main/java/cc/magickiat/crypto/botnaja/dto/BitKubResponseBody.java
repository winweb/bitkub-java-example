package cc.magickiat.crypto.botnaja.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BitKubResponseBody<T> {

    private int error;
    private T result;

}