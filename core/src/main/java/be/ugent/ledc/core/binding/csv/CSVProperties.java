package be.ugent.ledc.core.binding.csv;

import be.ugent.ledc.core.util.SetOperations;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVProperties
{
    /**
     * A default configuration of CSV properties, with comma as separator
     * and three null symbols.
     */
    public static final CSVProperties DEFAULT_COMMA = new CSVProperties(
        true,
        ",",
        '"',
        SetOperations.set("", "null", "N/A")
    );
    
    /**
     * A default configuration of CSV properties, with semi-colon as separator
     * and three null symbols.
     */
    public static final CSVProperties DEFAULT_SEMI_COLON = new CSVProperties(
        true,
        ";",
        '"',
        SetOperations.set("", "null", "N/A")
    );
    
    /**
     * A flag that indicates whether or not the first line represents a header;
     */
    private final boolean firstLineIsHeader;

    /**
     * The column separator
     */
    private final String columnSeparator;

    /**
     * The quote character
     */
    private final Character quoteCharacter;
    
    /**
     * Set of symbols that will be treated as NULL values
     */
    private final Set<String> nullSymbols;
    
    /**
     * The symbol string that indicates a comment line. Null by default.
     */
    private final String commentSymbol;
    
    /**
     * The encoding to be used
     */
    private final Charset encoding;

    public CSVProperties(boolean firstLineIsHeader, String columnSeparator, Character quoteCharacter, Set<String> nullSymbols, String commentSymbol, Charset encoding)
    {
        this.firstLineIsHeader = firstLineIsHeader;
        this.columnSeparator = columnSeparator;
        this.quoteCharacter = quoteCharacter;
        this.nullSymbols = nullSymbols;
        this.commentSymbol = commentSymbol;
        this.encoding = encoding;
    }
    
    public CSVProperties(boolean firstLineIsHeader, String columnSeparator, Character quoteCharacter, Set<String> nullSymbols, String commentSymbol)
    {
        this(firstLineIsHeader, columnSeparator, quoteCharacter, nullSymbols, commentSymbol, StandardCharsets.UTF_8);
    }
    
    public CSVProperties(boolean firstLineIsHeader, String columnSeparator, Character quoteCharacter, Set<String> nullSymbols)
    {
        this(firstLineIsHeader, columnSeparator, quoteCharacter, nullSymbols, null);
    }
    
    public CSVProperties(boolean firstLineIsHeader, String columnSeparator, Character quoteCharacter)
    {
        this(firstLineIsHeader, columnSeparator, quoteCharacter, Stream.of("").collect(Collectors.toSet()));
    }

    public CSVProperties()
    {
        this(true,";", null);
    }

    public boolean isFirstLineIsHeader()
    {
        return firstLineIsHeader;
    }

    public String getColumnSeparator()
    {
        return columnSeparator;
    }

    public Character getQuoteCharacter()
    {
        return quoteCharacter;
    }

    public Set<String> getNullSymbols()
    {
        return nullSymbols;
    }

    public String getCommentSymbol()
    {
        return commentSymbol;
    }

    public Charset getEncoding()
    {
        return encoding;
    }
}
