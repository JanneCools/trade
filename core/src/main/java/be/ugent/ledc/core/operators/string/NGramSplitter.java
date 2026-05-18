package be.ugent.ledc.core.operators.string;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NGramSplitter implements Splitter
{
    /*
    * Size of the n grams included in this splitter
    */
    private final int n;

    public NGramSplitter(int n)
    {
        this.n = n;
    }
    
    @Override
    public List<String> apply(String s)
    {
        int m = Math.max(1, n);
        
        return n >= s.length()
            ? Stream.of(s).toList()
            : IntStream
            .rangeClosed(0, s.length() - m)
            .mapToObj(idx -> s.substring(idx, idx + m))
            .toList();
    }

    @Override
    public String toString() {
        return "NGramSplitter (n=" + n+")";
    }
}
