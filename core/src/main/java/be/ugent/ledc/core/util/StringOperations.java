package be.ugent.ledc.core.util;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringOperations
{
    /**
     * An operation where only characters that pass the filter are retained
     * @param original
     * @param filter
     * @return 
     */
    public static String filterCharacters(String original, Predicate<Character> filter)
    {
        return original == null
            ? null
            :original
            .chars()
            .mapToObj(i -> (char)i)
            .filter(c -> filter.test(c))
            .map(c -> c.toString())
            .collect(Collectors.joining());
    }
    
    public static String digitsOnly(String original)
    {
        return filterCharacters(original, Character::isDigit);
    }
    
    public static String alphaNumericOnly(String original)
    {
        return filterCharacters(original, Character::isLetterOrDigit);
    }
    
    public static String compressWhitespaces(String original)
    {
        return original == null
            ? null
            : original.replaceAll("\\s+", " ").trim();
    }
    
    public static String normalizeNonBreackingSpaces(String original)
    {
        return original == null
            ? null
            : original.replaceAll("\\xA0", " ");
    }
    
    public static String leftPad(String input, int requiredLength, char padding)
    {
        return input == null
            ? paddingString(0, requiredLength, padding)
            : paddingString(input.length(), requiredLength, padding).concat(input);
    }
    
    public static String rightPad(String input, int requiredLength, char padding)
    {
        return input == null
            ? paddingString(0, requiredLength, padding)
            : input.concat(paddingString(input.length(), requiredLength, padding));
    }
    
    private static String paddingString(int actualLength, int requiredLength, char padding)
    {
        return actualLength >= requiredLength
            ? ""
            : IntStream
                .range(0, requiredLength - actualLength)
                .mapToObj(i->String.valueOf(padding))
                .collect(Collectors.joining());
    }
    
}
