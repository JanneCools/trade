package be.ugent.ledc.sigma.repair.covers;

import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;

/**
 * A degenerate case of the ConditionalSetCoverSearch where no conditions apply.
 * This CoverSearch will find all set covers that are subset-minimal, meaning for
 * each cover found, no subset is also a cover.
 * @author abronsel
 */
public class SubsetMinimalCoverSearch extends ConditionalSetCoverSearch
{    
    public SubsetMinimalCoverSearch(SCCSigmaRuleset ruleset)
    {
        super(ruleset);
    }  
}
