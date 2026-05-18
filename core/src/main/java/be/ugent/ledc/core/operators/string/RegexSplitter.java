package be.ugent.ledc.core.operators.string;

import java.util.List;
import java.util.stream.Stream;

public class RegexSplitter implements Splitter
{   
    private final String regex;

    public RegexSplitter(String regex) {
        this.regex = regex;
    }
    
    @Override
    public List<String> apply(String s)
    {
        return Stream.of(s.split(regex)).toList();
    }

    @Override
    public String toString() {
        return "RegexSplitter:" + regex;
    }
}