package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.shelflife;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.HashSet;
import java.util.Set;

/**
 * A node that uses a Geometric prior, but updates the parameter as new evidence 
 * is observed.
 * @author abronsel
 * @param <T>
 */
public class DynamicShelfLife<T> extends GeometricShelfLife<T>
{
    /**
     * A smoothing parameter that determines how much new evidence must be taken into
     * account when re-estimating the parameter of the geometric distribution
     */
    private final UnitScore alpha;
    
    /**
     * The initial estimate of the parameter of the geometric distribution.
     */
    private final UnitScore initialProb;
    
    public DynamicShelfLife(String attribute, UnitScore alpha, UnitScore p)
    {
        super(attribute, p);
        this.alpha = alpha;
        this.initialProb = p;
    }

    @Override
    public Set<INode<?>> getParents()
    {
        //A prior model has no parents
        return new HashSet<>();
    }

    @Override
    public String getType()
    {
        return "geometric-dynamic";
    }

    @Override
    public void updateState(DataObject current)
    {
        Object cValue = current.get(getAttribute());

        //If a change has occurred, we update age and previous value
        if(cValue != null && !cValue.equals(getPreviousValue()))
        {
            //Update change probability if not at start
            if(getPreviousValue() != null)
            {
                double estimate =
                    alpha.getValue() * getAge()
                +   alpha.getComplement().getValue() * (1.0/getP().getValue());

                //Update parameter
                setP(new UnitScore(1.0 / estimate));
            }
        }

        super.updateState(current);
    }
    
//    @Override
//    public void updateBelief()
//    {
//        Integer prevAge = getAgeMap().lastKey();
//        getAgeMap().clear();
//
//        if(getAge() == 0)
//        {
//
//        }
//        else
//        {            
//            for(int i=0;i<getAge();i++)
//            {
//                getAgeMap().put(i,new GeometricDistribution(p).probability(i));
//            }
//            
//            getAgeMap().put(
//                getAge(),
//                new GeometricDistribution(p).tail(getAge())
//            );
//        }
//    }

    @Override
    public void clear()
    {
        super.clear();
        super.setP(initialProb);
    }
    
    
}
