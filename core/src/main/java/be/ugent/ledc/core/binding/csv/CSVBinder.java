package be.ugent.ledc.core.binding.csv;

import java.io.File;

public class CSVBinder
{
    private final CSVProperties csvProperties;
    
    private final File csvFile;

    public CSVBinder(CSVProperties csvProperties, File csvFile)
    {
        this.csvFile = csvFile;
        this.csvProperties = csvProperties;
    }

    public CSVProperties getCsvProperties()
    {
        return csvProperties;
    }

    public File getCsvFile()
    {
        return csvFile;
    }
}
