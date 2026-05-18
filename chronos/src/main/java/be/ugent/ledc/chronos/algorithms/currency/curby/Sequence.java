package be.ugent.ledc.chronos.algorithms.currency.curby;

public class Sequence
{
    private static Sequence INSTANCE = null;
    
    private int number;
    
    private Sequence()
    {
        number = 0;
    }
    
    public static Sequence getInstance()
    {
        if(INSTANCE == null)
            INSTANCE = new Sequence();
        
        return INSTANCE;
    }
    
    public int pop()
    {
        return number++;
    }
}
