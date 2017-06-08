package crabzilla.stack;

import java.util.List;

public interface EventsProjector {

  String getEventsChannelId();

  Long getLastUowSeq();

  void handle(List<ProjectionData> uowList);

}
