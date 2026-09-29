package core.binding.jdbc.agents;

import core.binding.BindingException;
import core.binding.jdbc.schema.DatabaseSchema;
import core.binding.jdbc.schema.TableSchema;
import java.util.List;

public interface DDLGenerator
{
    public List<String> generateDDLScript(DatabaseSchema schema) throws BindingException;
    
    public List<String> generateDDLScript(TableSchema signature) throws BindingException;
}
