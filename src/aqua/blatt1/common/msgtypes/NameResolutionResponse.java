package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public record NameResolutionResponse(InetSocketAddress address, String reqId) implements Serializable {
}
