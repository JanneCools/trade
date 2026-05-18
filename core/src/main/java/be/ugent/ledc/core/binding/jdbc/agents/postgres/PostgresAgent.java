package be.ugent.ledc.core.binding.jdbc.agents.postgres;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.agents.DDLGenerator;
import be.ugent.ledc.core.binding.jdbc.agents.JDBCAgent;
import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import be.ugent.ledc.core.binding.jdbc.agents.SchemaAgent;
import be.ugent.ledc.core.binding.jdbc.agents.TableModifiers;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;
import be.ugent.ledc.core.binding.jdbc.agents.MigrationAgent;

public class PostgresAgent implements JDBCAgent
{
    @Override
    public String getConnectionString(RelationalDB rdbInfo)
    {
        return "jdbc:postgresql://" + rdbInfo.getHost() + ":" + rdbInfo.getPort() + "/" + rdbInfo.getDatabaseName();
    }

    @Override
    public SchemaAgent getSchemaAgent()
    {
        return new PostgresSchemaAgent();
    }

    @Override
    public MigrationAgent getMigrationAgent()
    {
        return new PostgresMigrationAgent();
    }

    @Override
    public DDLGenerator getDDLGenerator(RelationalDB rdb, boolean overwriteTables, TableModifiers tableModifiers, boolean deleteTables) throws BindingException
    {
        PostgresDDLGenerator ddlGenerator = new PostgresDDLGenerator(rdb, overwriteTables, tableModifiers, deleteTables);
        
        ddlGenerator.getDatatypesWithLength().add("character varying");
        ddlGenerator.getDatatypesWithPrecision().add("numeric");
        ddlGenerator.getDatatypesWithPrecision().add("decimal");
        
        return ddlGenerator;
    }
   
    @Override
    public void specifySchema(Connection conn, RelationalDB info) throws BindingException
    {
        if (info.getSchemaName() != null && !info.getSchemaName().isEmpty())
        {
            try
            {              
                PreparedStatement ps2 = conn.prepareStatement("set search_path to " + escaping().apply(info.getSchemaName()));
                ps2.execute();
            }
            catch( SQLException ex)
            {
                throw new BindingException(ex);
            }
        }
    }

    @Override
    public String explain(String query)
    {
        return "EXPLAIN " + query ; 
    }

    @Override
    public Function<String, String> escaping()
    {
        return (name) -> "\"" + name + "\"";
    }
}
