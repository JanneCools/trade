package be.ugent.ledc.sigma.datastructures.contracts;

public interface ForwardNavigator<T>
{
    boolean hasNext(T current);

    T next(T current) throws SigmaContractException;
    
    T first();
}
