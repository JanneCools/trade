package core.binding.jdbc.schema;

import core.binding.BindingException;
import core.binding.jdbc.DBMS;

public class SchemaGeneralizer
{
    public static DatabaseSchema generalize(DatabaseSchema sourceSchema, DBMS sourceVendor) throws BindingException
    {
        if(sourceVendor == null)
        {
            return sourceSchema;
        }
        
        for(String signatureName: sourceSchema.keySet())
        {
            sourceVendor.getAgent().getMigrationAgent().generalizeSignature(sourceSchema.get(signatureName));
        }
        
        return sourceSchema;
    }
}
