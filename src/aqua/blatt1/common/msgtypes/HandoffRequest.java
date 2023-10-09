package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

import aqua.blatt1.common.FishModel;

@SuppressWarnings("serial")
public final class HandoffRequest implements Serializable {
	private final FishModel fish;

	public HandoffRequest(FishModel fish) {
		this.fish = fish;
	}

	public FishModel getFish() {
		return fish;
	}
}
