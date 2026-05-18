package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.dataset.Contract;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.SimpleDataset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PreparedJDBCDataReader extends AbstractJDBCDataReader
{
    public static final String NAME_PREFIX = "@";
    
    private final NamedPreparedStatement namedStatement;

    public PreparedJDBCDataReader(JDBCBinder binder, String sqlWithNamedParameters, Set<String> parameterNames) throws SQLException, BindingException
    {
        super(binder);
        this.namedStatement = new NamedPreparedStatement(sqlWithNamedParameters, parameterNames);
    }
    
    public PreparedJDBCDataReader(JDBCBinder binder, boolean strongBinding, String sqlWithNamedParameters, Set<String> parameterNames) throws SQLException, BindingException
    {
        super(binder, strongBinding);
        this.namedStatement = new NamedPreparedStatement(sqlWithNamedParameters, parameterNames);
    }

    public Dataset readData(DataObject parameterValues) throws DataReadException
    {
        try
        {
            //Set parameters
            namedStatement.setParameters(parameterValues);

            //Execute the query
            ResultSet resultSet = namedStatement.executeQuery();
            
            Dataset data = new SimpleDataset();
            addData(resultSet, data);

            //Return
            return data;
        }
        catch (SQLException ex)
        {
            throw new DataReadException(ex);
        }
    }

    public ContractedDataset readContractedData(DataObject parameterValues) throws DataReadException
    {
        try
        {
            //Set parameters
            namedStatement.setParameters(parameterValues);

            //Execute the query
            ResultSet resultSet = namedStatement.executeQuery();
            
            //Builder for the contract
            Contract.ContractBuilder builder = new Contract.ContractBuilder();
            
            //Consult meta data of the result set
            ResultSetMetaData metaData = resultSet.getMetaData();
            
            //Complete the contract
            for(int i=1; i<=metaData.getColumnCount(); i++)
            {
                builder.addContractor(
                    metaData.getColumnLabel(i),
                    toTypeContractor(
                        metaData.getColumnType(i),
                        metaData.getColumnTypeName(i),
                        metaData.getColumnClassName(i)
                    ));
            }
            
            //Initialize a dataset
            ContractedDataset dataset = new ContractedDataset(builder.build());
            
            addData(resultSet, dataset);

            //Return
            return dataset;
        }
        catch(SQLException ex)
        {
            throw new DataReadException(ex);
        }
    }
    
    private class NamedPreparedStatement
    {

        private final PreparedStatement preparedStatement;

        private final Map<Integer, String> attributeIndexMap;

        public NamedPreparedStatement(String statementWithNames, Set<String> parameterNames) throws SQLException, BindingException
        {
            attributeIndexMap = new HashMap<>();
            
            //Compile name pattern
            String namePattern = parameterNames
                .stream()
                .collect(Collectors.joining("|", "(", ")"));

            Pattern findParametersPattern = Pattern
                .compile("(?<!')(" 
                    + NAME_PREFIX
                    + namePattern
                    +")"
                    + "(?!')");
            
            Matcher matcher = findParametersPattern.matcher(statementWithNames);

            int count = 1;

            while (matcher.find())
            {
                attributeIndexMap.put(count++, matcher.group().substring(1));
            }

            String transformedStatement = statementWithNames
                .replaceAll(
                    findParametersPattern.pattern(),
                    "?");
            
            //Now build the statement
            preparedStatement = getConnection()
                .prepareStatement(
                    transformedStatement,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
        }

        public void setParameters(DataObject o) throws DataReadException, SQLException
        {
            if(o.getAttributes().equals(attributeIndexMap.values()))
                throw new DataReadException("Could not set parameter values of named prepared statement: parameter names mismatch." + 
                    "\nExpected parameters: " + attributeIndexMap.keySet() +
                    "\nReceived parameter: " + o
                );
            
            preparedStatement.clearParameters();
            
            for(Integer parameterIndex: attributeIndexMap.keySet())
            {
                preparedStatement
                    .setObject(
                        parameterIndex,
                        o.get(attributeIndexMap.get(parameterIndex))
                    );
            }
            
        }
        
        public ResultSet executeQuery() throws SQLException
        {
            return preparedStatement.executeQuery();
        }

        public void close() throws SQLException
        {
            preparedStatement.close();
        }
    }
}
