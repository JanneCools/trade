package sigma.datastructures.contracts;

public interface ForwardNavigator<T>
{
    boolean hasNext(T current);

    T next(T current) throws SigmaContractException;
    
    T first();
}
