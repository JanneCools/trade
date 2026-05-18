package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.dependency;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay.RangeDecay;
import be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay.UniformRangeDecay;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.util.SetOperations;
import java.util.Set;
import java.util.TreeMap;

public class CCNode<T> extends AgeNode<T>
{
    private final AgeNode<?> parent;

    private final Interval<Integer> range;
    
    private final RangeDecay decay;
     
    public CCNode(String attribute, AgeNode<?> parent, Interval<Integer> range, RangeDecay decay) throws ChronosException
    {
        super(attribute);
        
        if(range.isLeftOpen() || range.isRightOpen())
            throw new ChronosException("Invalid range. Range must be a closed interval.");
        
        this.parent = parent;
        this.range = range;
        this.decay = decay;
    }
    
    public CCNode(String attribute, AgeNode<?> parent, Interval<Integer> range) throws ChronosException
    {
        this(attribute, parent, range, new UniformRangeDecay()); 
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return SetOperations.set(parent);
    }    

    @Override
    public String getType()
    {
        return "cc";
    }

    @Override
    public void updateBelief()
    {        
        int tailAge = getAge();
        
        //If age drops to 0, we forget previous beliefs and reset belief in current age
        if(tailAge == 0)
        {
//            System.out.println("Tail change!");
            getAgeMap().clear();
            getAgeMap().put(0, UnitScore.ONE);
            return;
        }
        
        int span = range.getRightBound() - range.getLeftBound();
        
        //Initialize the new belief by shifting all previous ages
        TreeMap<Integer, UnitScore> updatedAgeMap = shift();
        
        //Was there a change in the recent past to satisfy the CC assuming head changes now
        if(range.getLeftBound() < 0 && tailAge <= -range.getLeftBound())
        {
//            System.out.println("CC satisfaction");
            getAgeMap().clear();
            getAgeMap().putAll(updatedAgeMap);
            return;
        }
        
        //Probability that parent has changed now
        UnitScore prior = parent.belief().probability(0);
        
        UnitScore residual = updatedAgeMap.containsKey(0) ? updatedAgeMap.get(0) : UnitScore.ZERO;
        
        for(int i=range.getLeftBound(); i<= range.getRightBound(); i++)
        {
            UnitScore conditional = decay.probability(i-range.getLeftBound(), span);
            UnitScore changeBelief = UnitScore.times(prior, conditional);
            
            updatedAgeMap.merge(-i, changeBelief, UnitScore::plus);
            
            if(i == 0)
                residual = UnitScore.plus(residual, changeBelief);
            else if(range.getLeftBound() < 0 && i < 0)
                residual = UnitScore.plus(residual, changeBelief);
        }
        
        
        //Ensure age map contains all ages
        for(int i=0;i >= range.getLeftBound(); i--)
        {   
            updatedAgeMap.merge(-i, UnitScore.ZERO, UnitScore::plus);
        }
        
        //If we have more belief in the current age than we've added
        //we subtract the added probability mass
        if(updatedAgeMap.get(tailAge).compareTo(residual) >= 0)
        {
            updatedAgeMap.merge(tailAge, residual, UnitScore::minus);
        }
        //Else, we subtract belief from the current age and scale other ages
        else
        {          
            UnitScore scale = UnitScore.minus(residual, updatedAgeMap.get(tailAge));
            updatedAgeMap.put(tailAge, UnitScore.ZERO);
            
            int scaleOffSet = range.getLeftBound() < 0 ? 1-range.getLeftBound() : 1;
            
            for(int i=scaleOffSet; i<tailAge; i++)
            {
                updatedAgeMap.merge(i, scale, UnitScore::times);
            }
        }
        
//        System.out.println("Age map update: " + updatedAgeMap + " Observed: " + tailAge);
        
        //Finally, update the belief of age
        getAgeMap().clear();
        getAgeMap().putAll(updatedAgeMap);
    }
}
