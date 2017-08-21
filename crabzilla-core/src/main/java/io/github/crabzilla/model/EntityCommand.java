package io.github.crabzilla.model;

public interface EntityCommand extends Command {

  EntityId getTargetId();

}
