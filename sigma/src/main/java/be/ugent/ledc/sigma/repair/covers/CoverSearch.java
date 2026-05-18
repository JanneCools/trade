package be.ugent.ledc.sigma.repair.covers;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.repair.NullBehavior;
import java.util.Set;

public interface CoverSearch
{
    Set<Set<String>> findMinimalCovers(DataObject object, NullBehavior nullBehavior);
}
