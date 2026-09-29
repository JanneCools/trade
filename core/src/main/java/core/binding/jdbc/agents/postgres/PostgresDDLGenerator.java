package core.binding.jdbc.agents.postgres;

import core.binding.BindingException;
import core.binding.jdbc.DBMS;
import core.binding.jdbc.agents.DefaultDDLGenerator;
import core.binding.jdbc.RelationalDB;
import core.binding.jdbc.agents.TableModifiers;
import core.binding.jdbc.schema.TableSchema;

public class PostgresDDLGenerator extends DefaultDDLGenerator
{

    public PostgresDDLGenerator(RelationalDB rdb, boolean overwriteTables, TableModifiers tableModifiers, boolean deleteTables) throws BindingException
    {
        super(DBMS.POSTGRESQL.getAgent().escaping(), rdb, overwriteTables, tableModifiers, deleteTables);
    }

    @Override
    public String getIdentitySuffix()
    {
        return "";
    }

    @Override
    public String generateModifyColumn(String schemaName, String columnName, TableSchema signature)
    {
        return "alter table " + getEscaping().apply(schemaName) + "." + getEscaping().apply(signature.getName()) + " alter " + getEscaping().apply(columnName) + " type " + getFullDatatype(columnName, signature) + ";";
    }
}
