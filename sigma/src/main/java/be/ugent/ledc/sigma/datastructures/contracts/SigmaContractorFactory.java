package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import static java.time.temporal.ChronoUnit.*;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SigmaContractorFactory
{
    public static final NominalContractor<String> STRING = new NominalContractor<>(TypeContractorFactory.STRING)
    {
        private static final char QUOTE = '\'';
        
        @Override
        public String parseConstant(String constant)
        {
            constant = constant.trim();
            if(!constant.startsWith("'"))
                throw new AtomException("String constants must be surrounded with single quotes. Found: " + constant);
            
            if(!constant.endsWith("'"))
                throw new AtomException("String constants must be surrounded with single quotes. Found: " + constant);
            
            return unescape(constant
                .substring(1, constant.length()-1));
        }
        
        @Override
        public String printConstant(String constant)
        {
            return QUOTE + escape(constant) + QUOTE;
        }
        
        @Override
        public Set<String> parseSetOfConstants(String setAsString)
        {
            return Stream.of(setAsString
            .trim()
            .substring(1, setAsString.length()-1)
            .split("(?<=" + QUOTE+ ")\\s*" + CONSTANT_SEPARATOR + "\\s*" + "(?=" + QUOTE+ ")\\s*"))
            .map(this::parseConstant)
            .collect(Collectors.toSet());
        }

        @Override
        public SetNominalAtom<String> buildSetAtom(String attribute, SetOperator op, Set<String> constants)
        {
            return new SetNominalAtom<>(this, attribute, op, constants);
        }

        @Override
        public ConstantAtom<String, ? extends SigmaContractor<String>, ? extends ComparableOperator> buildConstantAtom(String attribute, ComparableOperator op, String constant)
        {
            return new ConstantNominalAtom<>(this, attribute, (EqualityOperator)op, constant);
        }

        @Override
        public VariableAtom<String, ? extends SigmaContractor<String>, ? extends ComparableOperator> buildVariableAtom(String leftAttribute, ComparableOperator op, String rightAttribute)
        {
            return new VariableNominalAtom<>(this, leftAttribute, (EqualityOperator)op, rightAttribute);
        }

        private String escape(String constant)
        {
            return constant.replaceAll(  CPF.ATOM_SEPARATOR, "\\\\".concat(CPF.ATOM_SEPARATOR));
        }
        
        private String unescape(String constant)
        {
            return constant.replaceAll("\\\\".concat(CPF.ATOM_SEPARATOR), CPF.ATOM_SEPARATOR);
        }
    };

    public static final OrdinalContractor<Integer> INTEGER = new OrdinalContractor<>(TypeContractorFactory.INTEGER)
    {
        @Override
        public boolean hasNext(Integer current) {
            return current != null && get(current).compareTo(last() - 1) <= 0;
        }

        @Override
        public Integer next(Integer current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no next value.");

            if (hasNext(current)) {
                return get(current) + 1;
            }

            throw new SigmaContractException("Integer has no next value for value " + current + " (overflow detected).");

        }

        @Override
        public boolean hasPrevious(Integer current) {
            return current != null && get(current).compareTo(first() + 1) >= 0;
        }

        @Override
        public Integer previous(Integer current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no previous value.");

            if (hasPrevious(current)) {
                return get(current) - 1;
            }

            throw new SigmaContractException("Integer has no previous value for value " + current + " (overflow detected).");

        }

        @Override
        public Integer get(Integer current) {
            return current;
        }

        @Override
        public long cardinality(Interval<Integer> range)
        {
            if(range.getLeftBound() == null
            || range.getRightBound() == null
            || range.getLeftBound().equals(first())
            || range.getRightBound().equals(last()))
                return Long.MAX_VALUE;
            
            Integer low = range.isLeftOpen() ? next(range.getLeftBound()) : range.getLeftBound();
            Integer high = range.isRightOpen() ? previous(range.getRightBound()) : range.getRightBound();
            
            if(low > high)
                return 0;
            
            return high - low + 1;
        }

        @Override
        public Integer first()
        {
            return get(Integer.MIN_VALUE);
        }

        @Override
        public Integer last()
        {
            return get(Integer.MAX_VALUE);
        }

        @Override
        public Integer add(Integer value, long units) throws SigmaContractException
        {
            Objects.requireNonNull(value, "Cannot add units to null value.");
            if(units == 0 ||
                    units > 0 && value.compareTo(last() - (int)units) <= 0 ||
                    units < 0 && value.compareTo(first() - (int)units) >= 0)
                return value + (int)units;
            
            throw new SigmaContractException("Overflow detected in addition of Integer " + value + " with " + units + " units.");
        }

        @Override
        public Integer parseConstant(String constantAsString)
        {
            return get(Integer.valueOf(constantAsString));
        }
    };

    public static final OrdinalContractor<Long> LONG = new OrdinalContractor<>(TypeContractorFactory.LONG)
    {
        @Override
        public boolean hasNext(Long current) {
            return current != null && get(current).compareTo(last() - 1) <= 0;
        }

        @Override
        public Long next(Long current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no next value.");

            if (hasNext(current)) {
                return get(current) + 1;
            }

            throw new SigmaContractException("Long has no next value for value " + current + " (overflow detected).");
        }

        @Override
        public boolean hasPrevious(Long current) {
            return current != null && get(current).compareTo(first() + 1) >= 0;
        }

        @Override
        public Long previous(Long current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no previous value.");

            if (hasPrevious(current)) {
                return get(current) - 1;
            }

            throw new SigmaContractException("Long has no previous value for value " + current + " (overflow detected).");
        }

        @Override
        public Long get(Long current) {
            return current;
        }

        @Override
        public long cardinality(Interval<Long> range)
        {
            if(range.getLeftBound() == null
            || range.getRightBound() == null
            || range.getLeftBound().equals(first())
            || range.getRightBound().equals(last()))
                return Long.MAX_VALUE;
            
            Long low = range.isLeftOpen() ? next(range.getLeftBound()) : range.getLeftBound();
            Long high = range.isRightOpen() ? previous(range.getRightBound()) : range.getRightBound();
            
            if(low > high)
                return 0;
            
            return high - low + 1;
        }
        
        @Override
        public Long first()
        {
            return get(Long.MIN_VALUE);
        }

        @Override
        public Long last()
        {
            return get(Long.MAX_VALUE);
        }

        @Override
        public Long add(Long value, long units) throws SigmaContractException
        {
            Objects.requireNonNull(value, "Cannot add units to null value.");
            if(units == 0 ||
                units > 0 && value.compareTo(last() - units) <= 0 ||
                units < 0 && value.compareTo(first() - units) >= 0)
                return value + units;
            throw new SigmaContractException("Overflow detected in addition of Long " + value + " with " + units + " units.");
        }

        @Override
        public Long parseConstant(String constantAsString)
        {
            return get(Long.valueOf(constantAsString));
        }
    };

    public static final OrdinalContractor<Boolean> BOOLEAN = new OrdinalContractor<>(TypeContractorFactory.BOOLEAN)
    {
        @Override
        public boolean hasNext(Boolean current)
        {
            return current != null && !get(current);
        }

        @Override
        public Boolean next(Boolean current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no next value.");

            if (hasNext(current)) {
                return !get(current);
            }

            throw new SigmaContractException("Boolean has no next value for value " + current + " (overflow detected).");
        }

        @Override
        public boolean hasPrevious(Boolean current) {
            return current != null && get(current);
        }

        @Override
        public Boolean previous(Boolean current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no previous value.");

            if (hasPrevious(current)) {
                return !get(current);
            }

            throw new SigmaContractException("Boolean has no previous value for value " + current + " (overflow detected).");
        }

        @Override
        public Boolean get(Boolean current) {
            return current;
        }

        @Override
        public long cardinality(Interval<Boolean> range)
        {
            Boolean low = range.getLeftBound() != null && (range.isLeftOpen()
                    ? next(range.getLeftBound())
                    : range.getLeftBound());
            
            Boolean high = range.getRightBound() == null || (range.isRightOpen()
                    ? previous(range.getRightBound())
                    : range.getRightBound());
            
            if(low.compareTo(high) > 0)
                return 0;
            
            if(low.equals(high))
                return 1;
            
            return 2;
        }

        @Override
        public Boolean first()
        {
            return false;
        }
        
        @Override
        public Boolean last()
        {
            return true;
        }

        @Override
        public Boolean add(Boolean value, long units) throws SigmaContractException
        {
            Objects.requireNonNull(value, "Cannot add units to null value.");
            if (units == 0)
                return value;

            if(!value && units == 1)
                return true;
            
            if(value && units == -1)
                return false;
            
            throw new SigmaContractException("Overflow detected in addition of Boolean " + value + " with " + units + " units.");
        }
        
        @Override
        public Boolean parseConstant(String constantAsString)
        {
            return get(Boolean.valueOf(constantAsString));
        }
    };

    public static final OrdinalContractor<BigDecimal> BIGDECIMAL_ZERO_DECIMALS = new BigDecimalContractor((byte)0);

    public static final OrdinalContractor<BigDecimal> BIGDECIMAL_ONE_DECIMAL = new BigDecimalContractor((byte)1);

    public static final OrdinalContractor<BigDecimal> BIGDECIMAL_TWO_DECIMALS = new BigDecimalContractor((byte)2);

    public static final OrdinalContractor<BigDecimal> BIGDECIMAL_THREE_DECIMALS = new BigDecimalContractor((byte)3);

    public static final OrdinalContractor<LocalDate> DATE_DAYS = new DateContractor(
        LocalDate::plusDays,
        DAYS,
        (current) -> current
    );

    public static final OrdinalContractor<LocalDate> DATE_MONTHS = new DateContractor(
        LocalDate::plusMonths,
        MONTHS,
        current -> current.withDayOfMonth(1)
    );

    public static final OrdinalContractor<LocalDate> DATE_YEARS = new DateContractor(
        LocalDate::plusYears,
        YEARS,
        current -> current.withDayOfMonth(1).withMonth(1)
    );

    public static final OrdinalContractor<LocalTime> TIME_HOURS = new TimeContractor(LocalTime::plusHours, HOURS);

    public static final OrdinalContractor<LocalTime> TIME_MINUTES = new TimeContractor(LocalTime::plusMinutes, MINUTES);

    public static final OrdinalContractor<LocalTime> TIME_SECONDS = new TimeContractor(LocalTime::plusSeconds, SECONDS);

    public static final OrdinalContractor<LocalDateTime> DATETIME_DAYS = new DateTimeContractor(LocalDateTime::plusDays, DAYS);

    public static final OrdinalContractor<LocalDateTime> DATETIME_MONTHS = new DateTimeContractor(LocalDateTime::plusMonths, MONTHS);

    public static final OrdinalContractor<LocalDateTime> DATETIME_YEARS = new DateTimeContractor(LocalDateTime::plusYears, YEARS);

    public static final OrdinalContractor<LocalDateTime> DATETIME_HOURS = new DateTimeContractor(LocalDateTime::plusHours, HOURS);

    public static final OrdinalContractor<LocalDateTime> DATETIME_MINUTES = new DateTimeContractor(LocalDateTime::plusMinutes, MINUTES);

    public static final OrdinalContractor<LocalDateTime> DATETIME_SECONDS = new DateTimeContractor(LocalDateTime::plusSeconds, SECONDS);

    public static final OrdinalContractor<Duration> DURATION_MINUTES = new OrdinalContractor<>(TypeContractorFactory.DURATION)
    {
        @Override
        public boolean hasNext(Duration current) {
            return current != null && get(current).compareTo(last().minusMinutes(1)) <= 0;
        }

        @Override
        public Duration next(Duration current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no next value.");

            if (hasNext(current)) {
                return get(current).plusMinutes(1);
            }

            throw new SigmaContractException("Duration has no next value for value " + current + " (overflow detected).");
        }

        @Override
        public boolean hasPrevious(Duration current) {
            return current != null && get(current).compareTo(first().plusMinutes(1)) >= 0;
        }

        @Override
        public Duration previous(Duration current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no previous value.");

            if (hasPrevious(current)) {
                return get(current).minusMinutes(1);
            }

            throw new SigmaContractException("Duration has no previous value for value " + current + " (overflow detected).");
        }

        @Override
        public Duration get(Duration current) {
            return Duration.ofSeconds(current.toMinutes() * 60);
        }

        @Override
        public String toString() {
            return "DURATION_ORDINAL_MINUTES";
        }
        
        @Override
        public String name() {
            return "duration_in_minutes";
        }

        @Override
        public long cardinality(Interval<Duration> range)
        {
            if(range.getRightBound() == null || get(range.getRightBound()).equals(last()))
                return Long.MAX_VALUE;
            
            if(range.getLeftBound() == null)
                range.setLeftBound(first());
            
            Duration low = range.isLeftOpen() ? next(range.getLeftBound()) : get(range.getLeftBound());
            Duration high = range.isRightOpen() ? previous(range.getRightBound()) : get(range.getRightBound());
            
            if(low.compareTo(high) > 0)
                return 0;
            
            return 1 + high.minus(low).toMillis()/60000L;
        }
        
        @Override
        public Duration first()
        {
            return get(Duration.ZERO);
        }
        
        @Override
        public Duration last()
        {
            return get(ChronoUnit.FOREVER.getDuration());
        }

        @Override
        public Duration add(Duration value, long units) throws SigmaContractException
        {
            Objects.requireNonNull(value, "Cannot add units to null value.");
            
             if(units == 0 ||
               (units > 0 && get(value).compareTo(last().minusMinutes(units)) <= 0) ||
               (units < 0 && get(value).compareTo(first().minusMinutes(units)) >= 0))
                return get(value).plusMinutes(units);
            
            throw new SigmaContractException("Overflow detected in addition of Duration " + value + " with " + units + " minutes.");
        }

        @Override
        public Duration parseConstant(String constantAsString)
        {
            if(constantAsString.startsWith("-"))
                throw new SigmaContractException("Cannot parse constant '" + constantAsString + "'. ledc-sigma does not accept negative durations.");
            return get(Duration.parse(constantAsString));
        }
    };

    public static final OrdinalContractor<Duration> DURATION_SECONDS = new OrdinalContractor<>(TypeContractorFactory.DURATION)
    {
        @Override
        public boolean hasNext(Duration current) {
            return current != null && get(current).compareTo(last().minusSeconds(1)) <= 0;
        }

        @Override
        public Duration next(Duration current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no next value.");

            if (hasNext(current)) {
                return get(current).plusSeconds(1);
            }

            throw new SigmaContractException("Duration has no next value for value " + current + " (overflow detected).");
        }

        @Override
        public boolean hasPrevious(Duration current) {
            return current != null && get(current).compareTo(first().plusSeconds(1)) >= 0;
        }

        @Override
        public Duration previous(Duration current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no previous value.");

            if (hasPrevious(current)) {
                return get(current).minusSeconds(1);
            }

            throw new SigmaContractException("Duration has no previous value for value " + current + " (overflow detected).");
        }

        @Override
        public Duration get(Duration current) {
            return current.withNanos(0);
        }

        @Override
        public String toString() {
            return "DURATION_ORDINAL_SECONDS";
        }
        
        @Override
        public String name() {
            return "duration_in_seconds";
        }
        
        @Override
        public long cardinality(Interval<Duration> range)
        {
           if(range.getRightBound() == null || get(range.getRightBound()).equals(last()))
                return Long.MAX_VALUE;
            
            if(range.getLeftBound() == null)
                range.setLeftBound(first());
            
            Duration low = range.isLeftOpen() ? next(range.getLeftBound()) : get(range.getLeftBound());
            Duration high = range.isRightOpen() ? previous(range.getRightBound()) : get(range.getRightBound());
            
            if(low.compareTo(high) > 0)
                return 0;
            
            return high.getSeconds() - low.getSeconds() + 1;
        }
        
        @Override
        public Duration first()
        {
            return get(Duration.ZERO);
        }
        
        @Override
        public Duration last()
        {
            return get(ChronoUnit.FOREVER.getDuration());
        }
        
        @Override
        public Duration add(Duration value, long units) throws SigmaContractException
        {
            Objects.requireNonNull(value, "Cannot add units to null value.");
            
            if(units == 0 ||
              (units > 0 && value.compareTo(last().minusSeconds(units)) <= 0) ||
              (units < 0 && value.compareTo(first().minusSeconds(units)) >= 0))
                return get(value).plusSeconds(units);
            
            throw new SigmaContractException("Overflow detected in addition of Duration " + value + " with " + units + " seconds.");
        }
        
        @Override
        public Duration parseConstant(String constantAsString)
        {
            if(constantAsString.startsWith("-"))
                throw new SigmaContractException("Cannot parse constant '" + constantAsString + "'. ledc-sigma does not accept negative durations.");
            return get(Duration.parse(constantAsString));
        }
    };

    public static final OrdinalContractor<Instant> INSTANT_SECONDS = new OrdinalContractor<>(TypeContractorFactory.INSTANT)
    {
        @Override
        public boolean hasNext(Instant current) {
            return current != null && get(current).compareTo(last().minusSeconds(1)) <= 0;
        }

        @Override
        public Instant next(Instant current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no next value.");

            if (hasNext(current)) {
                return get(current).plusSeconds(1);
            }

            throw new SigmaContractException("Instant has no next value for value " + current + " (overflow detected).");
        }

        @Override
        public boolean hasPrevious(Instant current) {
            return current != null && get(current).compareTo(first().plusSeconds(1)) >= 0;
        }

        @Override
        public Instant previous(Instant current) throws SigmaContractException
        {
            Objects.requireNonNull(current, "Null has no previous value.");

            if (hasPrevious(current)) {
                return get(current).minusSeconds(1);
            }

            throw new SigmaContractException("Instant has no previous value for value " + current + " (overflow detected).");
        }

        @Override
        public Instant get(Instant current) {
            return current.truncatedTo(SECONDS);
        }

        @Override
        public String toString() {
            return "INSTANT_ORDINAL_SECONDS";
        }
        
        @Override
        public String name() {
            return "instant_in_seconds";
        }

        @Override
        public long cardinality(Interval<Instant> range)
        {
            if(range.getLeftBound() == null
            || range.getRightBound() == null
            || get(range.getLeftBound()).equals(first())
            || get(range.getRightBound()).equals(last()))
                return Long.MAX_VALUE;
            
            Instant low = range.isLeftOpen()
                ? next(range.getLeftBound())
                : get(range.getLeftBound());
            
            Instant high = range.isRightOpen()
                ? previous(range.getRightBound())
                : get(range.getRightBound());
            
            if(low.isAfter(high))
                return 0;
            
            return SECONDS.between(low, high) + 1;
        }
        
        @Override
        public Instant first()
        {
            return get(Instant.MIN);
        }

        @Override
        public Instant last()
        {
            return get(Instant.MAX);
        }

        @Override
        public Instant add(Instant value, long units) throws SigmaContractException
        {
            Objects.requireNonNull(value, "Cannot add units to null value.");
            
            if( units == 0 ||
                units > 0 && get(value).compareTo(last().minusSeconds(units)) <= 0 ||
                units < 0 && get(value).compareTo(first().minusSeconds(units)) >= 0)
                    return get(value).plusSeconds(units);
            
            throw new SigmaContractException("Overflow detected in addition of Instant " + value + " with " + units + " seconds.");
        }
        
        @Override
        public Instant parseConstant(String constantAsString)
        {
            return get(Instant.parse(constantAsString));
        }      
    };
}
