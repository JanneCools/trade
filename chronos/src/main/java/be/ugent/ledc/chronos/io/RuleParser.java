package be.ugent.ledc.chronos.io;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.io.ConstraintIo;
import be.ugent.ledc.sigma.io.SigmaRuleParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay.BinomialRangeDecay;
import be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay.GeometricRangeDecay;
import be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay.RangeDecay;
import be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay.UniformRangeDecay;
import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RuleParser
{
    private static final String CC_PATTERN =
        "([a-zA-Z0-9_\\-.\\s]+)"                    //Source attribute: group 1
        + "\\s*-\\[\\s*(\\d+)\\s*\\]->\\s*"         //Upper: group 2
        + "([a-zA-Z0-9_\\-.]+)"                     //Target attribute: group 3
        + "\\s*(change model "                      //Change model: group 4
            + "("
            + "uniform"
            + "|"
            + "(?:geometric\\s*\\(([01]\\.?\\d*)\\))"
            + "|"
            + "(?:binomial\\s*\\("
            + "([01]\\.?\\d*)"
            + "\\))"
            + ")"
            + "\\s*"
            + ")?";
    

    /**
     * Parses a transition rule for a temporal dataset.
     * @param line The line that contains the textual representation of rule to be
     * parsed
     * @param tContractors Contractors to be used during parsing
     * @return 
     * @throws be.ugent.ledc.chronos.ChronosException 
     */
    private static SigmaRule parseTransitionRule(String line, Map<String, SigmaContractor<?>> tContractors)throws ParseException
    {        
        return SigmaRuleParser.parseSigmaRule(line, tContractors);
        
    }

    /**
     * Parses a TransitionRuleset to applied to a temporal dataset from a set of lines.It is assumed that each line identifies one single transition rule.
     * @param lines Lines to be parsed. It is assumed each line corresponds to one rule
     * @param dataContractors Contractors for the attributes mentioned in the rules.
     * This map must contain one contractor for each attribute mentioned in the ruleset
     * but without time indicators.
     * @return 
     * @throws be.ugent.ledc.chronos.ChronosException 
     * @throws be.ugent.ledc.core.ParseException 
     */
    public static TRuleset parseTransitionRuleset(List<String> lines, Map<String, SigmaContractor<?>> dataContractors) throws ChronosException, ParseException
    {
        Map<String, SigmaContractor<?>> tContractors = TRuleset.unfold(dataContractors);
        
        Set<SigmaRule> tRules = new HashSet<>();
        
        for(String line: lines)
        {
            tRules.add(parseTransitionRule(line, tContractors));
        }
        
        return new TRuleset(tContractors, tRules);
    }

    public static Pair<TRuleset, ContractedDataset> parseRulesetAndContractors(List<String> lines, ContractedDataset dataset) throws ChronosException, ParseException, IOException
    {
        // split list into contractors and rules
        Map<Boolean, List<String>> results = lines.stream()
                .filter(line -> !line.startsWith(ConstraintIo.COMMENT_PREFIX) && !line.isBlank())
                .collect(Collectors.partitioningBy(line -> line.startsWith(ConstraintIo.CONTRACT_PREFIX)));

        // get contractors
        Map<String, SigmaContractor<?>> dataContractors = SigmaRuleParser.parseContractors(results.get(true));
        for (String key: dataContractors.keySet())
            dataset = setContractor(dataset, key, dataContractors.get(key));

        // convert to transition contractors
        Map<String, SigmaContractor<?>> tContractors = TRuleset.unfold(dataContractors);

        // get rules
        Set<SigmaRule> tRules = new HashSet<>();
        for (String line: results.get(false))
        {
            if (line.contains("#curr") || line.contains("#next"))
            {
                // it is a transition rule
                tRules.add(SigmaRuleParser.parseSigmaRule(line, tContractors));
            } else
            {
                // it is a selection rule
                SigmaRule rule = SigmaRuleParser.parseSigmaRule(line, dataContractors);
                SigmaRule ruleCurr = new SigmaRule(rule.getAtoms().stream()
                        .map(atom -> atom.nameTransform(name -> name + TRuleset.CURR))
                        .collect(Collectors.toSet()));
                SigmaRule ruleNext = new SigmaRule(rule.getAtoms().stream()
                        .map(atom-> atom.nameTransform(name -> name + TRuleset.NEXT))
                        .collect(Collectors.toSet()));
                tRules.add(ruleCurr);
                tRules.add(ruleNext);
            }
        }

        return new Pair<>(new TRuleset(tContractors, tRules), dataset);
    }

    public static ContractedDataset setContractor(ContractedDataset cDataset, String attribute, SigmaContractor<?> contractor) {
        if (contractor == SigmaContractorFactory.INTEGER)
            cDataset = cDataset.asInteger(attribute, SigmaContractorFactory.INTEGER);
        else if (contractor == SigmaContractorFactory.LONG)
            cDataset = cDataset.asLong(attribute, SigmaContractorFactory.LONG);
        else if (contractor == SigmaContractorFactory.BOOLEAN)
            cDataset = cDataset.asBoolean(attribute, SigmaContractorFactory.BOOLEAN);
        else if (contractor == SigmaContractorFactory.BIGDECIMAL_ZERO_DECIMALS)
            cDataset = cDataset.asBigDecimal(attribute, SigmaContractorFactory.BIGDECIMAL_ZERO_DECIMALS);
        else if (contractor == SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL)
            cDataset = cDataset.asBigDecimal(attribute, SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL);
        else if (contractor == SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS)
            cDataset = cDataset.asBigDecimal(attribute, SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS);
        else if (contractor == SigmaContractorFactory.BIGDECIMAL_THREE_DECIMALS)
            cDataset = cDataset.asBigDecimal(attribute, SigmaContractorFactory.BIGDECIMAL_THREE_DECIMALS);
        else if (contractor == SigmaContractorFactory.DATE_DAYS)
            cDataset = cDataset.asDate(attribute, SigmaContractorFactory.DATE_DAYS);
        else if (contractor == SigmaContractorFactory.DATE_MONTHS)
            cDataset = cDataset.asDate(attribute, SigmaContractorFactory.DATE_MONTHS);
        else if (contractor == SigmaContractorFactory.DATE_YEARS)
            cDataset = cDataset.asDate(attribute, SigmaContractorFactory.DATE_YEARS);
        else if (contractor == SigmaContractorFactory.TIME_HOURS)
            cDataset = cDataset.asTime(attribute, SigmaContractorFactory.TIME_HOURS);
        else if (contractor == SigmaContractorFactory.TIME_MINUTES)
            cDataset = cDataset.asTime(attribute, SigmaContractorFactory.TIME_MINUTES);
        else if (contractor == SigmaContractorFactory.TIME_SECONDS)
            cDataset = cDataset.asTime(attribute, SigmaContractorFactory.TIME_SECONDS);
        else if (contractor == SigmaContractorFactory.DATETIME_DAYS)
            cDataset = cDataset.asDateTime(attribute, SigmaContractorFactory.DATETIME_DAYS);
        else if (contractor == SigmaContractorFactory.DATETIME_MONTHS)
            cDataset = cDataset.asDateTime(attribute, SigmaContractorFactory.DATETIME_MONTHS);
        else if (contractor == SigmaContractorFactory.DATETIME_YEARS)
            cDataset = cDataset.asDateTime(attribute, SigmaContractorFactory.DATETIME_YEARS);
        else if (contractor == SigmaContractorFactory.DATETIME_HOURS)
            cDataset = cDataset.asDateTime(attribute, SigmaContractorFactory.DATETIME_HOURS);
        else if (contractor == SigmaContractorFactory.DATETIME_MINUTES)
            cDataset = cDataset.asDateTime(attribute, SigmaContractorFactory.DATETIME_MINUTES);
        else if (contractor == SigmaContractorFactory.DATETIME_SECONDS)
            cDataset = cDataset.asDateTime(attribute, SigmaContractorFactory.DATETIME_SECONDS);
        return cDataset;
    }
}
