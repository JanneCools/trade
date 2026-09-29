package core.binding.jdbc.agents;

import core.binding.jdbc.schema.Reference;
import core.binding.jdbc.schema.TableSchema;

public interface MigrationAgent
{
    public void generalizeSignature(TableSchema signature);
    
    public void specifySignature(TableSchema signature);
    
    public void specifyReference(Reference reference);
    
    public Class convertToClass(String datatype);
}
