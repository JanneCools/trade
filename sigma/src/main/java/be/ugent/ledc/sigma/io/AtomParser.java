package be.ugent.ledc.sigma.io;

import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.operators.AbstractOperator;
import be.ugent.ledc.sigma.datastructures.operators.Operator;
import be.ugent.ledc.sigma.datastructures.operators.OperatorUtil;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AtomParser
{

    public static AbstractAtom<?, ?, ?> parseAtom(String atom, Map<String, SigmaContractor<?>> contractors) throws ParseException
    {
//        System.out.println("Parsing atom: '" + atom + "'");

        String regex = contractors //PART 1: an attribute name
            .keySet()
            .stream()
            .collect(Collectors.joining("|", "(", ")"))
        .concat("\\s*")
        .concat("(")//These outer brackets are necessary because the case insensitive modifier ?i:
                    //makes the inner group non-captured
        .concat(OperatorUtil
            .getAbstractOperators()
            .map(AbstractOperator::getSymbol)
            .sorted(Comparator.comparing(String::length).reversed())
            .collect(Collectors.joining("|", "(?i:", ")"))
        )
        .concat(")")
        .concat("\\s*")
        .concat("(.+)");

        Pattern p = Pattern.compile(regex);

        Matcher matcher = p.matcher(atom.trim());

        if(!matcher.matches())
            throw new ParseException("Cannot parse atom '" + atom + "'");

        //Get left operand
        String leftOperand = matcher.group(1);

        //Get the operator symbol
        String operatorSymbol = matcher.group(2);

        //Convert into an actual operator
        Operator<?, ?> operator = OperatorUtil.getOperatorBySymbol(operatorSymbol);

        //Get the right operand
        String rightOperand = matcher.group(3);

        if(operator instanceof SetOperator)
            return SetAtom.parseSetAtom(
                leftOperand,
                (SetOperator) operator,
                rightOperand,
                contractors.get(leftOperand)
            );
        else if (contractors
                .keySet()
                .stream()
                .anyMatch(a -> rightOperand.matches(VariableAtom.buildVarioPatternExponential(a))
                                || rightOperand.matches(VariableAtom.buildVarioPatternLinear(a))
                                || rightOperand.matches(VariableAtom.buildVarioPatternGaussian(a))
                                || rightOperand.matches(VariableAtom.buildVarioPatternSpherical(a))))
        {

            //Verify equality of contractors
            SigmaContractor<?> rightContractor = contractors
                    .entrySet()
                    .stream()
                    .filter(e -> rightOperand.startsWith(e.getKey())
                            ||  rightOperand.matches(VariableAtom.buildVarioPatternExponential(e.getKey()))
                            || rightOperand.matches(VariableAtom.buildVarioPatternLinear(e.getKey()))
                            || rightOperand.matches(VariableAtom.buildVarioPatternGaussian(e.getKey()))
                            || rightOperand.matches(VariableAtom.buildVarioPatternSpherical(e.getKey()))
                    )

                    .map(Map.Entry::getValue)
                    .findFirst()
                    .get();

            if(!rightContractor.equals(contractors.get(leftOperand)))
                throw new AtomException("Could not parse atom '" + atom + "'. Variable vario atoms require two attributes with the same contract.");

            //Verify equality of contractors of the vario attributes
            SigmaContractor<?> varioContractor1 = contractors
                    .entrySet()
                    .stream()
                    .filter(e ->
                            rightOperand.matches(VariableAtom.buildVarioPatternExpGauSph1(e.getKey()))
                                    || rightOperand.matches(VariableAtom.buildVarioPatternLin1(e.getKey())))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .get();

            SigmaContractor<?> varioContractor2 = contractors
                    .entrySet()
                    .stream()
                    .filter(e ->
                            rightOperand.matches(VariableAtom.buildVarioPatternExpGauSph1(e.getKey()))
                                    || rightOperand.matches(VariableAtom.buildVarioPatternLin1(e.getKey())))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .get();

            if (!varioContractor1.equals(varioContractor2)) {
                throw new AtomException("Could not parse atom '" + atom + "'. " +
                        "Vario atoms require two vario attributes with the same contract to compute their difference.");
            }

            return VariableAtom.parseVariableVarioAtom(
                    leftOperand, operator, rightOperand, contractors.get(leftOperand), varioContractor1);

        }
        else if(contractors
            .keySet()
            .stream()
            .anyMatch(a ->
                rightOperand.startsWith(a)
            ||  rightOperand.matches(VariableAtom.buildSuccessorPattern(a))
            ))
        {
            //Verify equality if contractors
            SigmaContractor<?> rightContractor = contractors
                .entrySet()
                .stream()
                .filter(e -> rightOperand.startsWith(e.getKey())
                    ||  rightOperand.matches(VariableAtom.buildSuccessorPattern(e.getKey()))
                )

                .map(Map.Entry::getValue)
                .findFirst()
                .get();

            if(!rightContractor.equals(contractors.get(leftOperand)))
                throw new AtomException("Could not parse atom '" + atom + "'. Variable atoms require two attributes with the same contract.");

            return VariableAtom.parseVariableAtom(
                leftOperand,
                operator,
                rightOperand,
                contractors.get(leftOperand)
            );
        }
        else
            return ConstantAtom.parseConstantAtom(
                leftOperand,
                operator,
                rightOperand,
                contractors.get(leftOperand));
    }
}
