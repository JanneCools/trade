package be.ugent.ledc.core.binding.jdbc.agents.postgres;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.DBMS;
import be.ugent.ledc.core.binding.jdbc.agents.DefaultDDLGenerator;
import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import be.ugent.ledc.core.binding.jdbc.agents.TableModifiers;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;

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
