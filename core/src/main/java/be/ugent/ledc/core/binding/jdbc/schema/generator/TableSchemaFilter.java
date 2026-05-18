package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;


public interface TableSchemaFilter
{
    public boolean accept(TableSchema tableSchema);
}
