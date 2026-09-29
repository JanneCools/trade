package core.binding.jdbc.agents;

import core.binding.BindingException;
import core.binding.jdbc.DBMS;
import core.binding.jdbc.schema.Reference;
import core.binding.jdbc.schema.DatabaseSchema;

public class SchemaSpecifier
{
    public static DatabaseSchema specify(DatabaseSchema sourceSchema, DBMS targetVendor) throws BindingException
    {
        if (targetVendor == null)
        {
            return sourceSchema;
        }

        //Get vendor stub
        JDBCAgent targetVendorStub = targetVendor.getAgent();

        for (String signatureName : sourceSchema.keySet())
        {
            targetVendorStub.getMigrationAgent().specifySignature(sourceSchema.get(signatureName));

            for (Reference reference : sourceSchema.get(signatureName).getReferences())
            {
                targetVendorStub.getMigrationAgent().specifyReference(reference);
            }
        }

        return sourceSchema;
    }
}
