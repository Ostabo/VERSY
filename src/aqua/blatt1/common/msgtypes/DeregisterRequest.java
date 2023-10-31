package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public record DeregisterRequest(String id) implements Serializable {
}
