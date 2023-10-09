package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class DeregisterRequest implements Serializable {
	private final String id;

	public DeregisterRequest(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
