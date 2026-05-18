package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.poisson;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.AbstractCUSUMNode;
import be.ugent.ledc.core.dataset.DataObject;

/**
 * A node that assumes the generating distribution of integer data (e.g., counts)
 * is a Poisson distribution. The node allows to detect a change in the parameter.
 * @author abronsel
 */
public class PoissonCusum extends AbstractCUSUMNode
{
    private int deltaTime;
    
    private final double groundLambda;
    
    private final double alternativeLambda;

    public PoissonCusum(String attribute, double groundLambda, double alternativeLambda, double a, double b) throws ChronosException
    {
        super(attribute, a, b);
        if(groundLambda <= 0)
            throw new ChronosException("Avg. ground rate must be a strict positive number. Found: " + groundLambda);
        
        if(alternativeLambda <= 0)
            throw new ChronosException("Avg. alternative rate must be a strict positive number. Found: " + alternativeLambda);
        
        this.deltaTime = 1;
        this.groundLambda = groundLambda;
        this.alternativeLambda = alternativeLambda;
    }
    
    public PoissonCusum(String attribute, double groundLambda, double alternativeLambda) throws ChronosException
    {
        this(
            attribute,
            groundLambda,
            alternativeLambda,
            1.0,
            5.0);
    }


    @Override
    public String getType()
    {
        return "poisson-cusum";
    }

    @Override
    public void updateState(DataObject current)
    {
        if(current == null || current.get(getAttribute()) == null)
        {
            deltaTime++;
        }
        else
        {
            Integer k = current.getInteger(getAttribute());
        
            double ll = k * Math.log(deltaTime * alternativeLambda)
                +   deltaTime * groundLambda
                -   deltaTime * alternativeLambda
                -   k * Math.log(deltaTime * groundLambda);

            //Update the CUSUM
            setCusum(Math.max(0.0, getCusum() + ll));
            
            deltaTime = 1;
        }
    }

    @Override
    public void clear()
    {
        super.clear();
        this.deltaTime = 1;
    }
}
