package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.PublicKey;

public record KeyExchangeMessage(PublicKey publicKey) implements Serializable {
}
