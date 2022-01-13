package io.github.crabzilla.accounts.projectors.transfers

import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.projection.EventsProjectorProvider


class TransfersProjectorFactory: EventsProjectorProvider {
  override fun create(): EventsProjector {
    return TransferProjector()
  }
}