package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public record NameResolutionRequest(String tankId, String reqId) implements Serializable {
}
