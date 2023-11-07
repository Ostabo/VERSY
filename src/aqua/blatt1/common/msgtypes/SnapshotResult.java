package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.FishModel;

import java.io.Serializable;
import java.util.HashSet;

public record SnapshotResult(HashSet<FishModel> snapshotResult) implements Serializable {
}
