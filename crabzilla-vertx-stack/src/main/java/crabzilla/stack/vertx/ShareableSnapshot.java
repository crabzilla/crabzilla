package crabzilla.stack.vertx;

import crabzilla.model.Snapshot;
import io.vertx.core.shareddata.Shareable;

public class ShareableSnapshot<A> extends Snapshot<A> implements Shareable {

  public ShareableSnapshot(final Snapshot<A> snapshot) {
    super(snapshot.getInstance(), snapshot.getVersion());
  }

}
