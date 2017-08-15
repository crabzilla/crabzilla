package crabzilla.model;

public interface EntityCommand extends Command {

  EntityId getTargetId();

}
