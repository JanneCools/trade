package be.ugent.ledc.core.binding.jdbc.agents;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.schema.DatabaseSchema;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import java.util.List;

public interface DDLGenerator
{
    public List<String> generateDDLScript(DatabaseSchema schema) throws BindingException;
    
    public List<String> generateDDLScript(TableSchema signature) throws BindingException;
}
