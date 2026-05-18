package be.ugent.ledc.core.binding.csv;

import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.config.FeatureBuilder;
import be.ugent.ledc.core.config.FeatureConsumer;
import be.ugent.ledc.core.config.FeatureMap;
import be.ugent.ledc.core.util.ListOperations;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVBindingFactory implements FeatureConsumer<CSVBinder>, FeatureBuilder<CSVBinder>
{
    public static final String FILE         = "file";
    public static final String HEADERLINE   = "headerLine";
    public static final String NULL_SYMBOLS = "nullSymbols";
    public static final String COMMENT      = "commentSymbol";
    public static final String SEPARATOR    = "separator";
    public static final String QUOTE_CHAR   = "quoteCharacter";
    public static final String ENCODING     = "encoding";
    
    private final String[] requiredFeatures = new String[]{
        FILE,
        HEADERLINE,
        SEPARATOR
    };
    
    @Override
    public FeatureMap buildFeatureMap(CSVBinder binder)
    {
        FeatureMap m = new FeatureMap()
            .addFeature(ENCODING, binder.getCsvProperties().getEncoding())
            .addFeature(FILE, binder.getCsvFile())
            .addFeature(HEADERLINE, binder.getCsvProperties().isFirstLineIsHeader())
            .addFeature(SEPARATOR, binder.getCsvProperties().getColumnSeparator())
            .addFeature(QUOTE_CHAR, binder.getCsvProperties().getQuoteCharacter())
            .addFeature(NULL_SYMBOLS, binder
                .getCsvProperties()
                .getNullSymbols()
                .stream()
                .collect(Collectors.toList()));
        
        if(binder.getCsvProperties().getCommentSymbol() != null)
            m.addFeature(COMMENT, binder.getCsvProperties().getCommentSymbol());
        
        return m;
    }
    
    @Override
    public CSVBinder buildFromFeatures(FeatureMap featureMap) throws ParseException
    {
        for(String requiredFeature: requiredFeatures)
            if(featureMap.get(requiredFeature) == null)
                throw new ParseException("Cannot instantiate CSVBinder from JSON file: required feature '" + requiredFeature + "' is missing");
        
        return new CSVBinder(
            new CSVProperties(
                featureMap.getBoolean(HEADERLINE),
                featureMap.getString(SEPARATOR),
                featureMap.getString(QUOTE_CHAR) == null || featureMap.getString(QUOTE_CHAR).isEmpty()
                    ? null
                    : featureMap.getString(QUOTE_CHAR).charAt(0),
                featureMap.getList(NULL_SYMBOLS) == null
                    ? Stream.of("").collect(Collectors.toSet())
                    : ListOperations.asSet(featureMap.getList(NULL_SYMBOLS)),
                featureMap.getString(COMMENT),
                featureMap.getString(ENCODING) == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(featureMap.getString(ENCODING))),
            new File(featureMap.getString(FILE))
        );

    }
}
