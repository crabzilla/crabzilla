package crabzilla.stack;


import crabzilla.model.ProjectionData;

import java.util.List;

public interface ProjectionRepository {

  List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize);

}
