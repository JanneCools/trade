package core.binding.jdbc.schema.generator;

import core.binding.jdbc.schema.TableSchema;


public interface TableSchemaFilter
{
    public boolean accept(TableSchema tableSchema);
}
