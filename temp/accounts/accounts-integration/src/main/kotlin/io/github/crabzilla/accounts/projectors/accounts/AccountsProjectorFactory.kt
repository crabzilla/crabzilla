package io.github.crabzilla.accounts.projectors.accounts

import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.projection.EventsProjectorProvider

class AccountsProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return AccountsProjector("accounts_view")
  }
}
