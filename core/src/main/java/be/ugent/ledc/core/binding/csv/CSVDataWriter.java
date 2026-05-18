package be.ugent.ledc.core.binding.csv;

import be.ugent.ledc.core.binding.DataWriteException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CSVDataWriter
{
    private PrintWriter writer;
    
    private boolean mustWriteHeader = false;
            
    private final CSVBinder binder;
    
    public CSVDataWriter(CSVBinder binder)
    {
        this.binder = binder;
    }
    
    private void writeDataObject(DataObject o, List<String> attributes) throws DataWriteException
    {
        if(writer == null)
        {
            try
            {
                //open the writer, with the correct encoding and auto-flush
                writer = new PrintWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(
                                binder.getCsvFile()),
                                binder.getCsvProperties().getEncoding()
                            )
                        ),
                        true);

                if(binder.getCsvProperties() != null && binder.getCsvProperties().isFirstLineIsHeader())
                {
                    mustWriteHeader = true;
                }
            }
            catch(FileNotFoundException ex)
            {
                throw new DataWriteException(ex);
            }
        }
        
        Character q = binder.getCsvProperties().getQuoteCharacter();
        
        String c = binder.getCsvProperties().getColumnSeparator();
        
        Set<String> nullSymbols = binder.getCsvProperties().getNullSymbols();
        
        //Choose null symbol
        final String nil = nullSymbols
            .stream()
            .findFirst()
            .orElse("")
            .trim();
        
        if(mustWriteHeader)
        {
            String line = attributes
                .stream()
                .map(a -> q == null
                    ? a
                    : ""+ q + a + q) //Quote symbols
                .collect(Collectors.joining(c)); //Join with separator c

            
            //Mark header as written
            mustWriteHeader = false;
            
            //Write the header
            writer.println(line);
        }
        
        //Write the values
        String line = "";
        
        line = attributes
            .stream()
            .map(a -> build(o.get(a), nil, q))
            .collect(Collectors.joining(c));

        //Write the header
        writer.println(line);
    }
    
    private String build(Object o, String nil, Character q)
    {
        if(o == null)
            return nil;
        
        String s = o.toString();
        
        //Escape and return
        return q == null
            ? s
            : "" + q + s.replaceAll(""+q, ""+q+q) + q;
    }
    
    public void writeData(Dataset dataset, List<String> attributes) throws DataWriteException
    {
        for(DataObject o: dataset.getDataObjects())
        {
            writeDataObject(o, attributes);
        }
    }
    
    public void writeData(Dataset dataset) throws DataWriteException
    {
        List<String> attributes = dataset
            .stream()
            .flatMap(o -> o.getAttributes().stream())
            .distinct()
            .collect(Collectors.toList());
        
        for(DataObject o: dataset.getDataObjects())
        {
            writeDataObject(o, attributes);
        }
    }
    
    public void close()
    {
        if(writer != null)
        {
            writer.flush();
            writer.close();
        }
    }
}
