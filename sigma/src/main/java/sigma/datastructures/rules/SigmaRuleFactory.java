package sigma.datastructures.rules;

import sigma.datastructures.atoms.ConstantOrdinalAtom;
import sigma.datastructures.atoms.SetAtom;
import sigma.datastructures.atoms.SetNominalAtom;
import sigma.datastructures.atoms.SetOrdinalAtom;
import sigma.datastructures.operators.InequalityOperator;
import sigma.datastructures.operators.SetOperator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sigma.datastructures.contracts.OrdinalContractor;
import sigma.datastructures.contracts.NominalContractor;

public class SigmaRuleFactory
{
    public static <T extends Comparable<? super T>> SigmaRule createNominalDomainRule(String attribute, Set<T> permittedValues, NominalContractor<?> contractor)
    {
        SetAtom<?, NominalContractor<?>> atom = new SetNominalAtom(
            contractor,
            attribute,
            SetOperator.NOTIN,
            permittedValues);
        
        return new SigmaRule(Stream.of(atom).collect(Collectors.toSet()));
    }
    
    public static <T extends Comparable<? super T>> SigmaRule createOrdinalDomainRule(String attribute, Set<T> permittedValues, OrdinalContractor<?> contractor)
    {
        SetAtom<?, OrdinalContractor<?>> atom = new SetOrdinalAtom(
            contractor, 
            attribute,
            SetOperator.NOTIN,
            permittedValues);
        
        return new SigmaRule(Stream.of(atom).collect(Collectors.toSet()));
    }
    
    public static <T extends Comparable<? super T>> SigmaRule createLowerBoundDomainRule(String attribute, T lowerBound, OrdinalContractor<?> contractor)
    {
        ConstantOrdinalAtom<?> atom = new ConstantOrdinalAtom(
            contractor, 
            attribute,
            InequalityOperator.LT,
            lowerBound);
        
        return new SigmaRule(Stream.of(atom).collect(Collectors.toSet()));
    }
    
    public static <T extends Comparable<? super T>> SigmaRule createUpperBoundDomainRule(String attribute, T upperBound, OrdinalContractor<?> contractor)
    {
        ConstantOrdinalAtom<?> atom = new ConstantOrdinalAtom(
            contractor, 
            attribute,
            InequalityOperator.GT,
            upperBound);
        
        return new SigmaRule(Stream.of(atom).collect(Collectors.toSet()));
    }
}
