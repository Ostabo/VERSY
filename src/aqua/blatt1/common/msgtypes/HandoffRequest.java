package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.FishModel;

import java.io.Serializable;

public record HandoffRequest(FishModel fish) implements Serializable {
}
