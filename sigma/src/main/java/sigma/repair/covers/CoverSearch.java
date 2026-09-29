package sigma.repair.covers;

import core.dataset.DataObject;
import sigma.repair.NullBehavior;
import java.util.Set;

public interface CoverSearch
{
    Set<Set<String>> findMinimalCovers(DataObject object, NullBehavior nullBehavior);
}
