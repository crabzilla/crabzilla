package crabzilla.stack;

import crabzilla.model.ProjectionData;

import java.util.List;

public interface EventProjector {

  String getEventsChannelId();

  Long getLastUowSeq();

  void handle(List<ProjectionData> uowList);

}
