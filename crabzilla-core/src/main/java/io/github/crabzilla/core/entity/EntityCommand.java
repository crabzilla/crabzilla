package io.github.crabzilla.core.entity;

import io.github.crabzilla.core.Command;

public interface EntityCommand extends Command {

  EntityId getTargetId();

}
