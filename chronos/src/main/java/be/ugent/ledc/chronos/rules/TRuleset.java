package be.ugent.ledc.chronos.rules;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetException;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetInverter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A TRuleset holds a set of transition rules, which are selection rules on
 * a special set of attributes. More specifically, for a given set of attributes
 * a1,...,ak that appear in a temporal dataset, we construct the derived set
 * of attributes a1#curr, ... ,ak#curr,a1#next,... ak#next. A transition rule
 * for the set a1,...,ak is then simply a selection rule on the derived set.
 *
 * Semantically, a transition rule creates constraints either within a snapshot
 * (e.g., a rule defined solely on a1#curr, ... ,ak#curr) or constraints that
 * dictate how the transition from one snapshot to the next must behave.
 *
 *
 * @author abronsel
 * @param <I>
 */
public class TRuleset<I extends Comparable<? super I>> extends SigmaRuleset
{
    public static final String CURR  = "#curr";
    public static final String NEXT  = "#next";

    public TRuleset(Map<String, SigmaContractor<?>> contractors, Set<SigmaRule> sigmaRules) throws SigmaRulesetException
    {
        super(contractors, sigmaRules);

        for(SigmaRule rule: sigmaRules)
        {
            //Atom sanity checks
            for(AbstractAtom<?,?,?> atom: rule.getAtoms())
            {            
                //Time indicator test
                if(atom.getAttributes().anyMatch(a -> !a.endsWith(NEXT) && !a.endsWith(CURR)))
                {
                    throw new SigmaRulesetException("Invalid transition rule: "
                        + "attributes in transition rule atoms must end with a time indicator "
                        + "(" + NEXT + " or " + CURR + ")."
                        + "Found: " + atom);
                }

                if(atom.getAttributes().count() == 1L || atom.getAttributes().allMatch(a -> a.endsWith(CURR))
                        || atom.getAttributes().allMatch(a -> a.endsWith(NEXT)))
                    continue;

                //Singular signal test
                if(atom
                    .getAttributes()
                    .map(a -> a.replaceAll(NEXT + "|" + CURR, ""))
                    .distinct()
                    .count() != 1
                )
                {
                    throw new SigmaRulesetException("Invalid transition rule: "
                        + "a transition atom must be defined on one attribute. "
                        + "Found: " + atom);
                }
            }
        }
    }

    public static DataObject createObject(DataObject current, DataObject next, Set<String> attributes)
    {
        DataObject o = new DataObject();

        for(String a: attributes)
        {
            o.set(a.concat(TRuleset.CURR), current.get(a));
            o.set(a.concat(TRuleset.NEXT), next.get(a));
        }

        return o;
    }

    public SigmaRuleset projectOverCurrent()
    {
        return new SigmaRuleset(
            getContractors()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().endsWith(CURR))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(0, e.getKey().length() - CURR.length()),
                        Map.Entry::getValue
                )),
            getRules()
                .stream()
                .filter(rule -> rule
                    .getInvolvedAttributes()
                    .stream()
                    .noneMatch(a -> a.endsWith(NEXT)))
                .map(TRuleset::makeStationary)
                .collect(Collectors.toSet())
        );
    }

    public SigmaRuleset projectOverNext()
    {
        return new SigmaRuleset(
            getContractors()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().endsWith(NEXT))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(0, e.getKey().length() - NEXT.length()),
                        Map.Entry::getValue
                )),
            getRules()
                .stream()
                .filter(rule -> rule
                    .getInvolvedAttributes()
                    .stream()
                    .noneMatch(a -> a.endsWith(CURR)))
                .map(TRuleset::makeStationary)
                .collect(Collectors.toSet())
        );
    }

    /**
     * A key method for verification of a TRuleset is to check its symmetry.
     *
     * A symmetric ruleset is one where the projection over indicator CURR is
     * equivalent to the projection over indicator NEXT.
     * @return
     */
    public boolean isSymmetric()
    {
        if (getRules().isEmpty())
            return true;

        Set<CPF> currImplied = projectOverCurrent().stream()
                .map(rule -> new CPF(rule.getAtoms()))
                .map(CPFImplicator::imply)
                .collect(Collectors.toSet());
        Set<CPF> nextImplied = projectOverNext().stream()
                .map(rule -> new CPF(rule.getAtoms()))
                .map(CPFImplicator::imply)
                .collect(Collectors.toSet());

        Set<CPF> currInverted = SigmaRulesetInverter.invert(projectOverCurrent());
        Set<CPF> nextInverted = SigmaRulesetInverter.invert(projectOverNext());

        // check if the sets are identical
        if (currImplied.equals(nextImplied))
            return true;

        // check if the projection over current implies the projection over next
        boolean implying = CPFImplicator.setIsImpliedBySet(currInverted, nextInverted, nextImplied);
        if (!implying)
            return false;

        // check if the projection over next implies the projection over current
        implying = CPFImplicator.setIsImpliedBySet(nextInverted, currInverted, currImplied);
        return implying;
    }

    /**
     * A helper method that "unfolds" attribute mappings into a mapping where
     * each attribute appears once with CURR annotator and once with the NEXT
     * annotator
     * @param <X>
     * @param mapping
     * @return
     * @throws ChronosException
     */
    public static <X> Map<String, X> unfold(Map<String, X> mapping) throws ChronosException
    {
        Map<String, X> unfolded = new HashMap<>();

        for(String a: mapping.keySet())
        {
            //Attribute a.CURR
            unfolded.put(
                a.concat(CURR),
                mapping.get(a)
            );

            //Attribute a.NEXT
            unfolded.put(
                a.concat(NEXT),
                mapping.get(a)
            );
        }

        return unfolded;
    }

    public static DataObject projectOverCurrent(DataObject o)
    {
        return project(o, CURR);
    }

    public static DataObject projectOverNext(DataObject o)
    {
        return project(o, NEXT);
    }

    private static DataObject project(DataObject o, String indicator)
    {
        DataObject t = new DataObject();

        for(String a: o.getAttributes())
        {
            if(a.endsWith(indicator))
            {
                t.set(
                    a.substring(0, a.length() - indicator.length()),
                    o.get(a)
                );
            }
        }

        return t;
    }

    private static SigmaRule makeStationary(SigmaRule rule)
    {
        return new SigmaRule(rule
            .getAtoms()
            .stream()
            .map(atom -> atom.nameTransform(name -> name.replaceAll(CURR + "|" + NEXT, "")))
            .collect(Collectors.toSet())
        );
    }
}
