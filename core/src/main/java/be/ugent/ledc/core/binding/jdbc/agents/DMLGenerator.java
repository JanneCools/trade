package be.ugent.ledc.core.binding.jdbc.agents;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.PreparedJDBCDataReader;
import be.ugent.ledc.core.binding.jdbc.schema.Key;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import be.ugent.ledc.core.dataset.DataObject;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DMLGenerator
{
    private final Function<String,String> escape;
    
    public DMLGenerator(Function<String,String> escape)
    {
        this.escape = escape;
    }
    
    public String prepareUpdateWhereCode(String setAttribute, String table, String whereAttribute) throws BindingException
    {
        List<String> setAttributes = new ArrayList<>();
        setAttributes.add(setAttribute);
        List<String> whereAttributes = new ArrayList<>();
        whereAttributes.add(whereAttribute);
        
        return prepareUpdateWhereCode(setAttributes, table, whereAttributes);
    }
    
    public String prepareUpdateWhereCode(List<String> setAttributes, String table, List<String> whereAttributes) throws BindingException
    {
        return "update " + escape.apply(table)
            + " set " 
            + setAttributes
            .stream()
            .map(a -> escape.apply(a) + " = ?")
            .collect(Collectors.joining(", "))
            + " where "
            + whereAttributes
            .stream()
            .map(a -> escape.apply(a) + " = ?")
            .collect(Collectors.joining(" and "));
    }
    
    public String prepareInsertCode(String table, List<String> attributes) throws BindingException
    {
        //Construct insert statement
        return "insert into "
            + escape.apply(table)
            + " "
            + attributes
                .stream()
                .collect(Collectors.joining(", ", "(", ")"))
            + " values "
            + attributes
                .stream()
                .map(a -> "?")
                .collect(Collectors.joining(", ", "(", ")"));
    }
    
    public synchronized String prepareKeySelectCode(Set<String> attributesToSelect, TableSchema tableSchema, String schema)
    {
        Key key = tableSchema.getPrimaryKey();
        
        String sql = "select "
            + buildSelectClause(attributesToSelect)
            + " from "
            + getEscape().apply(schema) + "." + getEscape().apply(tableSchema.getName())
            + " where "
            + key
                .stream()
                .sorted()
                .map(a -> getEscape().apply(a))
                .collect(Collectors.joining(",", "(", ")"))
            + " in ("
            + key
                .stream()
                .sorted()
                .map(a -> PreparedJDBCDataReader.NAME_PREFIX + a)
                .collect(Collectors.joining(",", "(", ")"))
            + ");";
        
        return sql;
    }
    
    public synchronized String generateSelectFromWhereCode(Set<String> projection, String tableName, DataObject equiConditions, String schema)
    {                
        return "select "
                + buildSelectClause(projection)
                + " from "
                + getEscape().apply(schema) + "." + getEscape().apply(tableName)
                + " where " + buildWhereClause(equiConditions);
    }
    
    public synchronized String generateSelectFromWhereCode(Set<String> projection, String tableName, List<DataObject> alternatives, String schema)
    {                
        return "select "
                + buildSelectClause(projection)
                + " from "
                + getEscape().apply(schema)
                + "." + getEscape().apply(tableName)
                + " where " + buildExtendedWhereClause(alternatives);
    }
    
    public synchronized String generateInsertCode(DataObject solution, String tableName, String schema)
    {
        String columnClause = solution.
            getAttributes()
            .stream()
            .map(a -> getEscape().apply(a))
            .collect(Collectors.joining(",", "(", ")"));
        
        String valueClause = solution
            .getAttributes()
            .stream()
            .map(a -> stringValue(solution.get(a), false))
            .collect(Collectors.joining(",","(", ")"));

        //Make query
        String sqlCode = "insert into "
            + getEscape().apply(schema)
            + "." + getEscape().apply(tableName)
            + columnClause + " values " + valueClause + ";";

        //Return
        return sqlCode;
    }

    public synchronized String generateUpdateCode(DataObject solution, String tableName, DataObject key, String schema)
    {
        //Build the SET clause
        String setClause = solution
            .getAttributes()
            .stream()
            .map(a -> getEscape().apply(a) + "=" + stringValue(solution.get(a), false))
            .collect(Collectors.joining(", "));

        //Make sql string
        String sqlCode = "update " + getEscape().apply(schema)
            + "." + getEscape().apply(tableName)
            + " set " + setClause
            + " where " + buildWhereClause(key) + ";";

        //Return
        return sqlCode;
    }
    
    public synchronized String generateDeleteCode(DataObject record, TableSchema tableSchema, String schema)
    {
        //Make a WHERE clause
        String whereClause = tableSchema
            .keySet()
            .stream()
            .filter(a -> tableSchema.isKeyAttribute(a))
            .map(a -> getEscape().apply(a) + "=" + stringValue(record.get(a), false))
            .collect(Collectors.joining(" and "));

        //Make sql string
        String sqlCode = "delete from "
            + getEscape().apply(schema)
            + "." + getEscape().apply(tableSchema.getName())
            + " where " + whereClause + ";";

        //Return
        return sqlCode;
    }
    
    private synchronized String buildWhereClause(DataObject equiConditions)
    {
        return equiConditions
            .getAttributes()
            .stream()
            .map(a -> equiConditions.get(a) == null
                ? getEscape().apply(a) + " is null" 
                : getEscape().apply(a) + "=" + stringValue(equiConditions.get(a), false))
            .collect(Collectors.joining(" and "));
    }
    
    private synchronized String buildSelectClause(Set<String> attributes)
    {
        //SELECT clause
        return attributes == null
            ? "*"
            : attributes
            .stream()
            .map(a -> getEscape().apply(a))
            .collect(Collectors.joining(","));
    }

    private String buildExtendedWhereClause(List<DataObject> alternatives)
    {
        return alternatives
            .stream()
            .map(o -> "(" + buildWhereClause(o) + ")")
            .collect(Collectors.joining(" or "));
    }
    
    public static void setParameter(PreparedStatement preparedStatement, int index, Object value) throws SQLException
    {        
        if(value == null)
        {
            preparedStatement.setObject(index, value);   
        }
        else if (value instanceof Integer)
        {
            preparedStatement.setInt(index, (Integer) value);
        }
        else if (value instanceof Double)
        {
            preparedStatement.setDouble(index, (Double) value);
        }
        else if (value instanceof Boolean)
        {
            preparedStatement.setBoolean(index, (Boolean) value);
        }
        else if (value instanceof Long)
        {
            preparedStatement.setLong(index, (Long) value);
        }
        else if (value instanceof String)
        {
            String stringValue = (String) value;
            preparedStatement.setString(index, stringValue);
        }
        else
        {
//            System.out.println(value);
            preparedStatement.setObject(index, value);
            //System.out.println("Warning: used setobject to set update value");
        }
    }

    public Function<String, String> getEscape()
    {
        return escape;
    }
    
    public String stringValue(Object object, boolean wildcards)
    {
        String strValue = null;

        if (object != null)
        {
            //Transform into string
            strValue = object.toString();

            if (object instanceof LocalDate
                || object instanceof LocalTime
                || object instanceof LocalDateTime
                || object instanceof String
                || object instanceof java.sql.Date
                || object instanceof java.sql.Time
                || object instanceof java.sql.Timestamp)
            {
                //Wildcard check
                strValue = (wildcards && object instanceof String)
                    ? "%" + strValue + "%"
                    : strValue;

                //Escape single quotes
                strValue = strValue.replaceAll("'", "''");
                
                //To string
                strValue = "'" + strValue + "'";
            }
        }

        //Return
        return strValue;
    }
}
