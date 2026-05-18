package be.ugent.ledc.core.binding.jdbc.agents.postgres;

import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import be.ugent.ledc.core.binding.jdbc.agents.SchemaAgent;

/**
 *
 * @author abronsel
 */
public class PostgresSchemaAgent implements SchemaAgent
{

    @Override
    public String getColumnQuery(RelationalDB rdb)
    {
        String schemaName = rdb.getSchemaName();

        String query = "SELECT t.TABLE_NAME as " + TABLE_NAME + ", " + COLUMN_NAME + ", " + DATA_TYPE + ", " + IS_NULLABLE + ", substring(c.column_default from '''(.+)''::[a-zA-Z\\s]+') as " + DEFAULT_VALUE + ", " + TABLE_TYPE + ", "
                + "c.character_maximum_length as " + DATA_TYPE_LENGTH + ", c.numeric_precision as " + DATA_TYPE_PRECISION + ", c.numeric_scale as " + DATA_TYPE_SCALE + ", c.numeric_precision_radix as " + DATA_TYPE_PRECISION_RADIX + ", "
                + "case when column_default ilike 'nextval(%)' then 'YES' else 'NO' end as " + IS_AUTO_INCREMENT + " "
                + "FROM INFORMATION_SCHEMA.TABLES t join INFORMATION_SCHEMA.COLUMNS c on (t.TABLE_NAME = c.TABLE_NAME)"
                + "WHERE t.TABLE_SCHEMA = '" + schemaName + "' and c.TABLE_SCHEMA = '" + schemaName + "'"
                + "ORDER BY t.TABLE_NAME";

        return query;
    }

    @Override
    public String getPrimaryKeyQuery(RelationalDB rdb)
    {
        String schemaName = rdb.getSchemaName();
        String query = "SELECT k.table_name AS " + TABLE_NAME + ", k.column_name AS " + COLUMN_NAME + " "
                + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t ON (k.constraint_name = t.constraint_name)"
                + "WHERE t.constraint_type = 'PRIMARY KEY' AND k.constraint_schema = '" + schemaName + "' AND t.constraint_schema = '" + schemaName + "' "
                + "ORDER BY k.table_name";

        return query;
    }

    @Override
    public String getForeignKeyQuery(RelationalDB rdb)
    {
        String schemaName = rdb.getSchemaName();

        String query = "SELECT r.constraint_name AS CONSTRAINT_NAME, k.table_name AS LEFT_TABLE_NAME, k.column_name AS LEFT_COLUMN_NAME, kk.table_name AS RIGHT_TABLE_NAME, kk.column_name AS RIGHT_COLUMN_NAME, CASE r.update_rule WHEN 'SET DEFAULT' THEN 'SET NULL' WHEN 'RESTRICT' THEN 'NO ACTION' ELSE r.update_rule END AS UPDATE_RULE, CASE r.delete_rule WHEN 'SET DEFAULT' THEN 'SET NULL' WHEN 'RESTRICT' THEN 'NO ACTION' ELSE r.delete_rule END AS DELETE_RULE "
                + "FROM information_schema.referential_constraints r inner join information_schema.key_column_usage k on r.constraint_name = k.constraint_name and k.constraint_schema = '" + schemaName + "' inner join information_schema.key_column_usage kk on r.unique_constraint_name = kk.constraint_name and kk.constraint_schema = '" + schemaName + "' "
                + "WHERE r.constraint_schema = '" + schemaName + "' and k.ordinal_position = kk.ordinal_position "
                + "ORDER BY k.table_name, r.constraint_name";

        return query;
    }

    @Override
    public String getSequenceQuery(RelationalDB rdb)
    {
        return "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = '" + rdb.getSchemaName() + "'";
    }

    @Override
    public String getIndexQuery(RelationalDB rdb)
    {
        return null;
    }

    @Override
    public String getCheckConstraintQuery(RelationalDB rdb)
    {
        String schemaName = rdb.getSchemaName();

        String query = "SELECT " + CONSTRAINT_NAME + ", " + TABLE_NAME + ", check_clause as " + CHECK_CONSTRAINT + " "
                + "FROM information_schema.table_constraints tc join information_schema.check_constraints cc using (constraint_name) "
                + " WHERE constraint_type = 'CHECK' and tc.constraint_schema = '" + schemaName + "' and cc.constraint_schema = '" + schemaName + "' "
                + "ORDER BY " + TABLE_NAME;

        return query;
    }

    @Override
    public String getUniqueConstraintQuery(RelationalDB dbConnectionInfo)
    {
        String schemaName = dbConnectionInfo.getSchemaName();

        String query = "SELECT t.constraint_name as " + CONSTRAINT_NAME + ", k.table_name as " + TABLE_NAME + ", k.column_name as " + COLUMN_NAME + " "
                + "FROM information_schema.table_constraints t join information_schema.key_column_usage k on (k.constraint_name = t.constraint_name) "
                + "WHERE t.constraint_type = 'UNIQUE' and k.constraint_schema = '" + schemaName + "' and t.constraint_schema = '" + schemaName + "' "
                + "ORDER BY k.table_name";

        return query;
    }
}
