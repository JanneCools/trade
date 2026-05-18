package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.ContractorException;
import java.util.stream.Stream;

import static be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory.*;
import java.util.List;
import java.util.stream.Collectors;

public class SigmaContractorUtil {

    public static Stream<NominalContractor<?>> getNominalContractors()
    {
        return Stream.of(
            STRING
        );
    }

    public static Stream<OrdinalContractor<?>> getOrdinalContractors()
    {
        return Stream.of
        (INTEGER,
        LONG,
        BOOLEAN,
        BIGDECIMAL_ZERO_DECIMALS,
        BIGDECIMAL_ONE_DECIMAL,
        BIGDECIMAL_TWO_DECIMALS,
        BIGDECIMAL_THREE_DECIMALS,
        DATE_DAYS,
        DATE_MONTHS,
        DATE_YEARS,
        TIME_HOURS,
        TIME_MINUTES,
        TIME_SECONDS,
        DATETIME_HOURS,
        DATETIME_MINUTES,
        DATETIME_DAYS,
        DATETIME_SECONDS,
        DATETIME_MONTHS,
        DATETIME_YEARS,
        DURATION_MINUTES,
        DURATION_SECONDS,
        INSTANT_SECONDS
        );
    }

    public static SigmaContractor<?> getTypedContractorByName(String name) throws ContractorException
    {
        List<SigmaContractor<?>> permittedContractors = Stream.concat(
            getNominalContractors(),
            getOrdinalContractors()
        ).toList();
        
        return permittedContractors
        .stream()
        .filter(contractor -> contractor.name().equals(name.toLowerCase().trim()))
        .findFirst()
        .orElseThrow(() -> new ContractorException(
            "Given name " + name + " does not represent a known SigmaContractor object. Valid names are: \n"
            + permittedContractors.stream().map(SigmaContractor::name).collect(Collectors.joining("\n"))));
    }
    
    public static boolean isNominalContractor(SigmaContractor<?> contractorToTest)
    {
        return SigmaContractorUtil
        .getNominalContractors()
        .anyMatch(c -> c.equals(contractorToTest));
    }
    
    public static boolean isOrdinalContractor(SigmaContractor<?> contractorToTest)
    {
        return SigmaContractorUtil
        .getOrdinalContractors()
        .anyMatch(c -> c.equals(contractorToTest));
    }

}
