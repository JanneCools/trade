package be.ugent.ledc.core.operators.string;

public class WordSplitter extends RegexSplitter
{   
    public WordSplitter()
    {
        super("\\s+");
    }
    
    @Override
    public String toString() {
        return "WordSplitter";
    }
}