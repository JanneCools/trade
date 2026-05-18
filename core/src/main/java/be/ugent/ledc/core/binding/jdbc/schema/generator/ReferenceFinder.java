package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.jdbc.schema.Reference;
import be.ugent.ledc.core.binding.jdbc.schema.DatabaseSchema;
import java.util.List;
import java.util.Map;

public interface ReferenceFinder
{
    public Map<String, List<Reference>> findMissingReferences(DatabaseSchema schema);
}
