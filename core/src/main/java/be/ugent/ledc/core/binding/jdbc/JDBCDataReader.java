package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.SimpleDataset;
import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.Contract;
import be.ugent.ledc.core.dataset.ContractedDataset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDBCDataReader extends AbstractJDBCDataReader
{    
    public JDBCDataReader(JDBCBinder binder)
    {
        super(binder);
    }

    public JDBCDataReader(JDBCBinder binder, boolean strongBinding)
    {
        super(binder, strongBinding);
    }
    
    /**
     * Reads data from the relational database indicated by the binder by executing
     * a SQL query. The dataset returned by this method is not contracted for performance reasons.
     * 
     * When contracting is necessary a suitable conversion from the retrieved dataset
     * needs to be done.
     * @param sqlQuery
     * @return
     * @throws be.ugent.ledc.core.binding.DataReadException
     */
    public Dataset readData(String sqlQuery) throws DataReadException
    {
        try
        {
            //Open a statement
            Statement stmt = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            //Execute the query
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            Dataset data = new SimpleDataset();
            addData(resultSet, data);

            //Close
            stmt.close();

            //Return
            return data;
        }
        catch(BindingException | SQLException | DataException ex)
        {
            throw new DataReadException(ex);
        }
    }
    
    /**
     * Reads data from the relational database indicated by the binder by executing
     * a SQL query. The dataset returned here is not contracted for performance reasons.
     * 
     * When contracting is necessary a suitable conversion from the retrieved dataset
     * needs to be done.
     * @param sqlQuery
     * @return
     * @throws be.ugent.ledc.core.binding.DataReadException
     */
    public ContractedDataset readContractedData(String sqlQuery) throws DataReadException
    {
        try
        {
            //Open a statement
            Statement stmt = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            //Execute the query
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            
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

            //Close
            stmt.close();

            //Return
            return dataset;
        }
        catch(BindingException | SQLException | DataException ex)
        {
            throw new DataReadException(ex);
        }
    }
    
    /**
     * Reads data from the relational database indicated by the binder by executing
     * a SQL query, but does so in an iterable manner.
     * 
     * As with the regular read data, the dataset returned is not contracted.
     * If contracting is deemed necessary a suitable conversion from the retrieved dataset
     * needs to be done to ensure the contract is obeyed.
     * @param sqlQuery
     * @param numberOfObjects The number of objects that is read in one go.
     * @return
     * @throws be.ugent.ledc.core.binding.DataReadException
     */
    public Iterator<Dataset> readIterableData(String sqlQuery, int numberOfObjects) throws DataReadException
    {
        try
        {
            //Open a statement
            Statement stmt = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(numberOfObjects);

            //Execute the query
            ResultSet resultSet = stmt.executeQuery(sqlQuery);

            //Return
            return new Iterator<Dataset>()
            {
                @Override
                public boolean hasNext()
                {
                    try
                    {
                        return !resultSet.isAfterLast();
                    }
                    catch (SQLException ex)
                    {
                        Logger.getLogger(JDBCDataReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return false;
                }

                @Override
                public Dataset next()
                {
                    Dataset dataset = new SimpleDataset();

                    try
                    {
                        //Scroll over the resultset
                        while (resultSet.next() && dataset.getSize() <= numberOfObjects)
                        {
                            //Add data object
                            dataset.addDataObject(createDataObject(resultSet));
                        }

                        if(resultSet.isAfterLast())
                        {
                            //Close result set
                            resultSet.close();
                        }
                    }
                    catch(SQLException ex)
                    {
                        Logger.getLogger(JDBCDataReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return dataset;
                }
            };
        }
        catch(BindingException | SQLException | DataException ex)
        {
            throw new DataReadException(ex);
        }
    }
}
