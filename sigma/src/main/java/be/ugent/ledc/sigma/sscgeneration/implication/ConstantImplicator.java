package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.AtomOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AtomType;
import be.ugent.ledc.sigma.datastructures.atoms.ConstantAtom;
import be.ugent.ledc.sigma.datastructures.atoms.ConstantOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.values.IntervalOperations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A RuleImplicator that operates on rules for which the generator appears only 
 * in non-variable atoms (i.e., constant atoms or set atoms).
 * The implicator draws its efficiency by converting the generating atoms into 
 * intervals on the domain of the attribute. For nominal data, this requires a
 * mapping of each nominal element onto an integer. The encoding is done in a greedy way
 * by first encoding larger sets.
 *
 * In each case, this allows to index rules by an interval on the generating attribute.
 * Implication for such a structures is done by using the ordinal implicator algorithm
 * proposed <a href="https://doi.org/10.1016/j.ins.2021.12.114">here</a>.
 *
 * @author abronsel
 * @param <T>
 */
public class ConstantImplicator<T extends Comparable<? super T>> extends SigmaRuleImplicator<T>
{
    @Override
    public Set<SigmaRule> generate(String generator, SigmaContractor<T> generatorContractor, Set<SigmaRule> contributors)
    {
        final Set<SigmaRule> rules = new HashSet<>();
        
        //Are we dealing with strings?
        if(generatorContractor instanceof NominalContractor)
        {
            //Index as integer
            Map<Interval<Integer>, List<CPF>> index = stringIndex(generator, contributors);
            
            //Generate from index structure and return
            rules.addAll(generate(SigmaContractorFactory.INTEGER, index));
        }
        else
        {
            //Index
            Map<Interval<T>, List<CPF>> index = index(generator, contributors);
            
            //Generate from index structure and return
            rules.addAll(generate(
                (OrdinalContractor<T>)generatorContractor,
                index
            ));
        }
        
        //Remove redundant rules
        rules.removeIf(rule -> rules
            .stream()
            .anyMatch(dRule  ->
                    !rule.equals(dRule)
                &&  rule.isRedundantTo(dRule))
        );
        
        return rules;
    }

    private Map<Interval<Integer>, List<CPF>> stringIndex(String generator, Set<SigmaRule> contributors)
    {
        Map<Interval<Integer>, List<CPF>> index = new HashMap<>();
        
        Map<String, Integer> stringValues = new HashMap<>();
            
        for(SigmaRule rule: contributors)
        {
            //Get atoms
            Set<AbstractAtom<?, ?, ?>> generatingAtoms = new HashSet<>();
            Set<AbstractAtom<?, ?, ?>> otherAtoms = new HashSet<>();
            
            for(AbstractAtom<?,?,?> atom: rule.getAtoms())
            {
                if(atom.involves(generator) && atom.getAtomType() != AtomType.VARIABLE)
                {
                    if(atom instanceof ConstantAtom)
                    {
                        String c = ((ConstantAtom<String, ?, ?>)atom).getConstant();
                        stringValues.merge(c, stringValues.size(), Integer::min);
                        
                        generatingAtoms.add(new ConstantOrdinalAtom<>
                        (
                            SigmaContractorFactory.INTEGER,
                            generator,
                            ((ConstantAtom<?, ?, ?>) atom).getOperator(),
                            stringValues.get(c)
                        ));
                    }
                    else if(atom instanceof SetAtom)
                    {
                        ((SetAtom<String, ?>)atom)
                            .getConstants()
                            .forEach(c -> stringValues.merge(c, stringValues.size(), Integer::min));
                        
                        generatingAtoms.add(new SetOrdinalAtom<>
                        (
                            SigmaContractorFactory.INTEGER,
                            generator,
                            ((SetAtom<String,?>)atom).getOperator(),
                            ((SetAtom<String,?>)atom)
                                .getConstants()
                                .stream()
                                .map(stringValues::get)
                                .collect(Collectors.toSet())
                        ));
                    }
                }
                else
                {
                    otherAtoms.add(atom);
                }
            }
            
            CPF residual = new CPF(otherAtoms);
            
            Set<Interval<Integer>> intervals = AtomOperations.getIntervalsFromOrdinalAtoms(generatingAtoms, generator);
            
            for(Interval<Integer> i: intervals)
            {
                if(!index.containsKey(i))
                {
                    index.put(i, new ArrayList<>());
                }
                
                index.get(i).add(residual);
            }
        }
        
        return index;
    }
    
    private <O extends Comparable<? super O>> Map<Interval<O>, List<CPF>> index(String generator, Set<SigmaRule> contributors)
    {
        Map<Interval<O>, List<CPF>> index = new HashMap<>();
        
        for(SigmaRule rule: contributors)
        {
            //Get atoms
            Set<AbstractAtom<?, ?, ?>> generatingAtoms = new HashSet<>();
            Set<AbstractAtom<?, ?, ?>> otherAtoms = new HashSet<>();
            
            for(AbstractAtom<?,?,?> atom: rule.getAtoms())
            {
                if(atom.involves(generator) && atom.getAtomType() != AtomType.VARIABLE)
                {
                    generatingAtoms.add(atom);
                }
                else
                {
                    otherAtoms.add(atom);
                }
            }
            
            CPF residual = new CPF(otherAtoms);
            
            Set<Interval<O>> intervals = AtomOperations.getIntervalsFromOrdinalAtoms(generatingAtoms, generator);
            
            for(Interval<O> i: intervals)
            {
                if(!index.containsKey(i))
                {
                    index.put(i, new ArrayList<>());
                }
                
                index.get(i).add(residual);
            }
        }
        
        return index;
    }
    
    /**
     * Based on the index, we can now generate new rules, where we choose the algorithm
     * based on the properties of the index keys.
     * @param <O>
     * @param index
     * @return 
     */
    private <O extends Comparable<? super O>> Set<SigmaRule> generate(OrdinalContractor<O> contractor, Map<Interval<O>, List<CPF>> index)
    {        
        //Test if some interval "range" left-matches the domain
        Predicate<Interval<O>> lBoundMatch = range -> range.getLeftBound() == null;
        
        //Test if some interval "range" right-matches the domain
        Predicate<Interval<O>> rBoundMatch = range -> range.getRightBound() == null;
        
        
        //We re-organize the index in three indices
        Map<Interval<O>, List<CPF>> leftIndex = new HashMap<>();
        Map<Interval<O>, List<CPF>> rightIndex = new HashMap<>();
        
        TreeMap<Interval<O>, List<CPF>> middleIndex = new TreeMap<>(Interval
            .<O>leftBoundComparator()
            .thenComparing(Interval.rightBoundComparator())
        );
        
        //Each entry of the index goes to one of the three indices
        for(Interval<O> i: index.keySet())
        {
            if(lBoundMatch.test(i))
                leftIndex.put(i, index.get(i));
            else if(rBoundMatch.test(i))
                rightIndex.put(i, index.get(i));
            else
                middleIndex.put(i, index.get(i));
        }
 
        //If all ranges either left-match or right-match the domain, we can generate all NNR rules with a double loop
        if(middleIndex.isEmpty())
            return generateWithDoubleLoop(
                contractor,
                leftIndex,
                rightIndex);
        //Else: we use the sorting approach
        else
            return generateWithSortedIntervals(
                contractor,
                leftIndex,
                middleIndex,
                rightIndex);
    }

    /**
     * This method generates implied edits with a double loop.
     * @param <O>
     * @param contractor
     * @param leftIndex
     * @param rightIndex
     * @return 
     */
    private <O extends Comparable<? super O>> Set<SigmaRule> generateWithDoubleLoop(OrdinalContractor<O> contractor, Map<Interval<O>, List<CPF>> leftIndex, Map<Interval<O>, List<CPF>> rightIndex)
    {
        //New rules
        Set<SigmaRule> newRules = new HashSet<>();

        for(Interval<O> leftRange: leftIndex.keySet())
        {
            for(Interval<O> rightRange: rightIndex.keySet())
            {
                if(IntervalOperations.canMergeWith(contractor, leftRange, rightRange))
                {
                    for(CPF leftCPF: leftIndex.get(leftRange))
                    {
                        for(CPF rightCPF: rightIndex.get(rightRange))
                        {
                            CPF joined = join(leftCPF,rightCPF);
                            
                            if(!joined.equals(new CPF(AbstractAtom.ALWAYS_FALSE)))
                            {
                                newRules.add(new SigmaRule(joined));
                            }
                        }
                    }
                }
            }
        }

        return newRules;
    }

    private <O extends Comparable<? super O>> Set<SigmaRule> generateWithSortedIntervals(OrdinalContractor<O> contractor, Map<Interval<O>, List<CPF>> leftIndex, TreeMap<Interval<O>, List<CPF>> middleIndex, Map<Interval<O>, List<CPF>> rightIndex)
    {
        //New rules
        Set<SigmaRule> newRules = new HashSet<>();
        
        //Check if each index contains at least one entry
        if(leftIndex.isEmpty() || rightIndex.isEmpty() || middleIndex.isEmpty())
            return newRules;

        //Add right index to sorted middle index
        for(Interval<O> right: rightIndex.keySet())
        {
            middleIndex.put(right, rightIndex.get(right));
        }
        
        //Initialize stack
        LinkedList<Sequence<O>> stack = new LinkedList<>(leftIndex
            .keySet()
            .stream()
            .flatMap(range -> leftIndex
                .get(range)
                .stream()
                .map(cpf -> new Sequence<>(cpf, range)))
            .toList());
        
        //Continue until the stack is empty
        while(!stack.isEmpty())
        {
            //System.out.println("Stack size: " + stack.size());
            Sequence<O> base = stack.pop();

            Interval<O> bInterval = base.getLastGeneratorInterval();
            CPF baseCPF = base.getCpf();

            Interval<O> key = leftIndex.containsKey(bInterval)
                ? middleIndex.firstKey()
                : middleIndex.ceilingKey(Interval.closed(bInterval.getLeftBound(),bInterval.getLeftBound()));
            
            while(key != null)
            {
                //CPFs associated with this interval
                List<CPF> cpfs = middleIndex.get(key);

                //If the intervals can't merge, break the loop
                if(!IntervalOperations.canMergeWith(contractor, bInterval, key))
                    break;

                //If the last interval of the sequence does not overlap and does not meet the new interval, this rule will be redundant
                if(bInterval.con(key))
                {
                    key = middleIndex.higherKey(key);
                    continue;
                }
                
                //If the second last interval is not null and overlaps with the candidate, we don't need the last interval
                if(base.getSecondLastGeneratorInterval() != null
                    && IntervalOperations.canMergeWith(contractor, base.getSecondLastGeneratorInterval(), key))
                {
                    key = middleIndex.higherKey(key);
                    continue;
                }

                for(CPF nextCPF: cpfs)
                {
                    CPF joined = join(baseCPF, nextCPF);
                    
                    //If the merged CPF is a contradiction, we discard it
                    if(joined.equals(new CPF(AbstractAtom.ALWAYS_FALSE)))
                        continue;

                    //Check if the implied rule is essentially new
                    if(rightIndex.containsKey(key))
                    {
                        newRules.add(new SigmaRule(joined));
                    }
                    else if(middleIndex.ceilingKey(Interval.closed(key.getLeftBound(),key.getLeftBound())) != null)
                    {
                        stack.push(new Sequence<>(
                            joined,
                            key,
                            base.getLastGeneratorInterval())
                        );
                    }
                }
                
                key = middleIndex.higherKey(key);
            }
        }
        
        return newRules;
    }

    @Override
    public boolean isApplicable(String generator, SigmaContractor<?> generatorContractor, Set<SigmaRule> contributors)
    {
        //The implicator is applicable if the generator does not appear in variable atoms.
        return contributors
            .stream()
            .allMatch(rule -> rule
                .getVariableAtoms()
                .stream()
                .noneMatch(vAtom -> vAtom.involves(generator))
            );
    }

    private static class Sequence<O extends Comparable<? super O>>
    {
        private final Interval<O> lastGeneratorInterval;

        private final Interval<O> secondLastGeneratorInterval;

        private final CPF cpf;

        public Sequence(CPF cpf, Interval<O> lastGeneratorInterval, Interval<O> secondLastGeneratorInterval)
        {
            this.lastGeneratorInterval = lastGeneratorInterval;
            this.secondLastGeneratorInterval = secondLastGeneratorInterval;
            this.cpf = cpf;
        }

        public Sequence(CPF cpf, Interval<O> lastGeneratorInterval)
        {
            this(cpf, lastGeneratorInterval, null);
        }

        public Interval<O> getLastGeneratorInterval() {
            return lastGeneratorInterval;
        }

        public Interval<O> getSecondLastGeneratorInterval() {
            return secondLastGeneratorInterval;
        }

        public CPF getCpf() {
            return cpf;
        }
    }
}
