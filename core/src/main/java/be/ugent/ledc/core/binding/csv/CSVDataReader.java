package be.ugent.ledc.core.binding.csv;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.FixedTypeDataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CSVDataReader
{
    private BufferedReader reader;

    private final CSVBinder binder;
    
    private final Map<Integer, String> columnIndexMap = new HashMap<>();
    
    public CSVDataReader(CSVBinder binder)
    {
        this.binder = binder;
    }

    public CSVBinder getBinder()
    {
        return binder;
    }

    public List<String> getColumnNames()
    {
        return columnIndexMap.values().stream().collect(Collectors.toList());
    }
    
    /**
     * Reads all data attributes as string values into a FixedTypeDataset.
     * @return
     * @throws DataReadException 
     */
    public FixedTypeDataset<String> readData() throws DataReadException
    {
        if(reader == null)
        {
            init();
        }        
        
        //Initialize the line
        String line;
        
        FixedTypeDataset<String> dataset = new FixedTypeDataset<>(
            new HashSet<>(columnIndexMap.values()),
            TypeContractorFactory.STRING);

        while ((line = nextLine()) != null)
        {
            //Parsed object
            DataObject parsedObject = CSVLineParser.parseLine(
                line,
                binder.getCsvProperties()
            );
            
            //Check if attributes amount is in line with expected
            if(parsedObject.getAttributes().size() < dataset.getContract().getAttributes().size())
            {
                throw new DataReadException("Signature mismatch: line has too few columns. Found:\n" + line);
            }
            if(parsedObject.getAttributes().size() > dataset.getContract().getAttributes().size())
            {
                throw new DataReadException("Signature mismatch: line has too many columns. Possibly quoting symbol is missing. Found:\n" + line);
            }

            if(!parsedObject.getAttributes().isEmpty())
            {
                if(!this.columnIndexMap.isEmpty())
                {
                    //Initialize new object
                    DataObject o = new DataObject();

                    for(String attributeIndex: parsedObject.getAttributes())
                    {
                        o.setString(
                            this.columnIndexMap.get(Integer.valueOf(attributeIndex)),
                            parsedObject.getString(attributeIndex)
                        );
                    }

                    dataset.addDataObject(o);
                }
            }   
        }

        //Return the dataset
        return dataset;
    }
    
    /**
     * Reads all data attributes as string values into a ContractedDataset and tries
     * to infer the correct datatype of each attribute. Inference is done on a sample
     * of the first rows. The amount of rows is indicated by the sampleSize argument.
     * 
     * Important: selection is a head selection and no random sample.
     * @param sampleSize Amount of rows used as a sample to do type inference.
     * @return
     * @throws DataReadException 
     */
    public ContractedDataset readDataWithTypeInference(int sampleSize) throws DataReadException
    {
        ContractedDataset dataset = readData();
        
        Set<String> attributes = dataset.getContract().getAttributes();
        
        Map<Predicate<String>, BiFunction<ContractedDataset, String, ContractedDataset>> regexMap = new LinkedHashMap<>();
        
        //Number patterns
        regexMap.put(
            regexTest("\\-?\\d+")
                .and(s -> Math.abs(Long.parseLong(s)) < Integer.MAX_VALUE),
            (d,a) -> d.asInteger(a)
        );
        regexMap.put(regexTest("\\-?\\d+"), (d,a) -> d.asLong(a));
        regexMap.put(regexTest("(\\-?\\d+)|(\\-?\\d*[.]\\d{1,3})"), (d,a) -> d.asBigDecimal(a));
        regexMap.put(regexTest("(\\-?\\d+)|(\\-?\\d*[.]\\d+)"), (d,a) -> d.asDouble(a));
        regexMap.put(regexTest("true|false"), (d,a) -> d.asBoolean(a));

        //Time pattern
        regexMap.put(regexTest("\\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asTime(a));

        //Year-month-day patterns
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d"), (d,a) -> d.asDate(a));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d"), (d,a) -> d.asDate(a, "yyyy/MM/dd"));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d(T|t)\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?"), (d,a) -> d.asDateTime(a));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d \\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy-MM-dd HH:mm"));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d(T|t)\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy-MM-dd'T'HH:mm"));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d \\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy/MM/dd HH:mm"));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d(T|t)\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy/MM/dd'T'HH:mm"));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d \\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy-MM-dd HH:mm:ss"));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d(T|t)\\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy-MM-dd'T'HH:mm:ss"));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d \\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy/MM/dd HH:mm:ss"));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d(T|t)\\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "yyyy/MM/dd'T'HH:mm:ss"));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d \\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "yyyy-MM-dd HH:mm:ssX"));
        regexMap.put(regexTest("\\d{4}\\-(0\\d|10|11|12)\\-[0123]\\d(T|t)\\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "yyyy-MM-dd'T'HH:mm:ssX"));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d \\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "yyyy/MM/dd HH:mm:ssX"));
        regexMap.put(regexTest("\\d{4}/(0\\d|10|11|12)/[0123]\\d(T|t)\\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "yyyy/MM/dd'T'HH:mm:ssX"));

        
        //Day-month-year patterns
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4}"), (d,a) -> d.asDate(a, "dd-MM-yyyy"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4}"), (d,a) -> d.asDate(a, "dd/MM/yyyy"));
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4} \\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd-MM-yyyy HH:mm"));
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4}(T|t)\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd-MM-yyyy'T'HH:mm"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4} \\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd/MM/yyyy HH:mm"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4}(T|t)\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd/MM/yyyy'T'HH:mm"));
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4} \\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd-MM-yyyy HH:mm:ss"));
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4}(T|t)\\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd-MM-yyyy'T'HH:mm:ss"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4} \\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd/MM/yyyy HH:mm:ss"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4}(T|t)\\d{2}:\\d{2}:\\d{2}"), (d,a) -> d.asDateTime(a, "dd/MM/yyyy'T'HH:mm:ss"));
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4} \\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "dd-MM-yyyy HH:mm:ssX"));
        regexMap.put(regexTest("[0123]\\d\\-(0\\d|10|11|12)\\-\\d{4}(T|t)\\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "dd-MM-yyyy'T'HH:mm:ssX"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4} \\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "dd/MM/yyyy HH:mm:ssX"));
        regexMap.put(regexTest("[0123]\\d/(0\\d|10|11|12)/\\d{4}(T|t)\\d{2}:\\d{2}:\\d{2}\\+\\d{1,2}"), (d,a) -> d.asDateTime(a, "dd/MM/yyyy'T'HH:mm:ssX"));
        
        for(String a: attributes)
        {
            List<String> sample = dataset
                .head(sampleSize)
                .stream()
                .filter(o -> o.get(a) != null)
                .map(o -> o.getString(a))
                .collect(Collectors.toList());
            
            if(sample.isEmpty())
                continue;
            
            for(Predicate p: regexMap.keySet())
            {
                //Try predicate
                if(sample.stream().allMatch(v -> p.test(v)))
                {
                    try
                    {
                        //If the regex matches, break
                        dataset = regexMap.get(p).apply(dataset, a);
                        break;
                    }
                    catch(DataException ex){}
                }
            }
            
        }
        
        return dataset;
    }
    
    private Predicate<String> regexTest(String pattern)
    {
        return (s) -> s.trim().matches(pattern);
    }
    
    /**
     * Initialization of the reader.
     * @throws DataReadException 
     */
    public void init() throws DataReadException
    {

        //Check if the reader is already initialized
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (IOException ex)
            {
                throw new DataReadException(ex);
            }
        }

        if (binder != null)
        {
            try
            {
                //Re-initialize the reader
                reader = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(binder.getCsvFile()),
                        binder.getCsvProperties().getEncoding()
                    ));
            }
            catch (FileNotFoundException ex)
            {
                throw new DataReadException(ex);
            }
        }
        else
        {
            throw new DataReadException("Csv file is null.");
        }

        try
        {
            //Read the first line
            String line = nextLine();

            if (line != null)
            {
                //Parse the line
                DataObject header = CSVLineParser.parseLine(
                    line,
                    binder.getCsvProperties()
                );
                
                //Clear the index map
                this.columnIndexMap.clear();

                if(!header.getAttributes().isEmpty())
                {
                    //For each column index
                    for (String index : header.getAttributes())
                    {
                        //Construct the column name. In case of no headers, the index is the name
                        String columnName = binder
                            .getCsvProperties()
                            .isFirstLineIsHeader()
                                ? header.get(index).toString()
                                : index;
                        
                        //Link the column name to the index
                        this.columnIndexMap.put(Integer.valueOf(index), columnName.toLowerCase());
                    }
                }
            }
            
            //If the first line was not the header, re initialize the reader
            if (!binder.getCsvProperties().isFirstLineIsHeader())
            {
                //Close the reader
                reader.close();
                
                //Re initialize
                reader = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(binder.getCsvFile()),
                        binder.getCsvProperties().getEncoding()
                    ));
            }
        }
        catch (IOException ex)
        {
            throw new DataReadException(ex);
        }
    }

    public void close() throws IOException
    {
        if(reader != null)
            reader.close();
    }
             
    private String nextLine() throws DataReadException
    {
        if(reader == null)
        {
            init();
        }
        
        //Initialize the line
        String line;
        
        try
        {
            while((line = reader.readLine()) != null)
            {
                if(!skipLine(line))
                {
                    return line;
                }
            }
        }
        catch(IOException ex)
        {
            throw new DataReadException(ex);
        }
        
        //If no more valid lines, return null
        return null;
    }
    
    private boolean skipLine(String line)
    {
        //Skip the line if line is null, empty, or starts with a comment symbol( if one is defined)
        return line == null || line.trim().isEmpty() || (getBinder().getCsvProperties().getCommentSymbol() != null && line.trim().startsWith(getBinder().getCsvProperties().getCommentSymbol()));
    }
}
