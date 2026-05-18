package be.ugent.ledc.core.cost.errormechanism.decimal;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.util.StringOperations;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DecimalRoundingError implements ErrorMechanism<BigDecimal> {

    @Override
    public boolean explains(BigDecimal originalValue, BigDecimal repairedValue, DataObject originalObject) {

        if(originalValue == null || repairedValue == null)
            return false;

        String o = originalValue.toString();
        String r = repairedValue.toString();

        Pattern pattern = Pattern.compile("((\\d*[1-9])(0*)\\.([1-9]*))(0+)");
        Matcher matcher = pattern.matcher(o);

        return matcher.matches() && r.startsWith(matcher.group(1));

    }

    @Override
    public List<BigDecimal> explanations(BigDecimal originalValue, DataObject originalObject) {

        List<BigDecimal> explanations = new ArrayList<>();

        if(originalValue != null) {
            String o = originalValue.toString();

            Pattern pattern = Pattern.compile("((\\d*[1-9])(0*)\\.([1-9]*))(0+)");
            Matcher matcher = pattern.matcher(o);

            if (matcher.matches()) {

                int zeroNonDecimalDigits = matcher.group(3).length();
                int nonZeroDecimalDigits = matcher.group(4).length();
                int zeroDecimalDigits = matcher.group(5).length();

                if (nonZeroDecimalDigits == 0) {

                    String prefix = matcher.group(2);

                    for (int i = 1; i < Math.pow(10, zeroNonDecimalDigits + zeroDecimalDigits); i++) {

                        String value = StringOperations.leftPad(""+i,zeroNonDecimalDigits + zeroDecimalDigits, '0');

                        String nonDecimal = value.substring(0, zeroNonDecimalDigits);
                        String decimal = value.substring(zeroNonDecimalDigits);

                        explanations.add(new BigDecimal(prefix.concat(nonDecimal).concat(".").concat(decimal)));
                    }

                } else {

                    String prefix = matcher.group(1);

                    for (int i = 1; i < Math.pow(10, zeroDecimalDigits); i++) {

                        String decimal = StringOperations.leftPad(""+i, zeroDecimalDigits, '0');

                        explanations.add(new BigDecimal(prefix.concat(decimal)));
                    }
                }
            }
        }

        return explanations;

    }
    
}
