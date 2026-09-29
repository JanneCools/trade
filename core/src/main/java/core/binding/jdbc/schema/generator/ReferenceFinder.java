package core.binding.jdbc.schema.generator;

import core.binding.jdbc.schema.Reference;
import core.binding.jdbc.schema.DatabaseSchema;
import java.util.List;
import java.util.Map;

public interface ReferenceFinder
{
    public Map<String, List<Reference>> findMissingReferences(DatabaseSchema schema);
}
