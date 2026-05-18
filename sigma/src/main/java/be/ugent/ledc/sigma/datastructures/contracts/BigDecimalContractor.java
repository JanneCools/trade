package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Interval;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class BigDecimalContractor extends OrdinalContractor<BigDecimal>
{
    /**
     * The granularity of this contractor.
     */
    private final BigDecimal granularity;
    
    /**
     * The scale of the BigDecimals modeled by this contractor.
     */
    private final byte scale;

    public BigDecimalContractor(byte scale)
    {
        super(TypeContractorFactory.BIGDECIMAL);
        this.scale = scale;
        this.granularity = BigDecimal.ONE.movePointLeft(scale);
    }
    
    @Override
    public boolean hasNext(BigDecimal current)
    {
        return current != null && get(current).compareTo(last().subtract(granularity)) <= 0;
    }

    @Override
    public BigDecimal next(BigDecimal current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no next value.");

        if (hasNext(current)) {
            return get(current).add(granularity);
        }

        throw new SigmaContractException("BigDecimal has no next value for value " + current + " (overflow detected).");
    }

    @Override
    public boolean hasPrevious(BigDecimal current)
    {
        return current != null &&  get(current).compareTo(first().add(granularity)) >= 0;
    }

    @Override
    public BigDecimal previous(BigDecimal current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no previous value.");

        if (hasPrevious(current)) {
            return get(current).subtract(granularity);
        }

        throw new SigmaContractException("BigDecimal has no previous value for value " + current + " (overflow detected).");
    }

    @Override
    public BigDecimal get(BigDecimal current)
    {
        return current == null
            ? null
            : current.setScale(scale, RoundingMode.DOWN);
    }

    @Override
    public BigDecimal getFromDataObject(DataObject o, String attribute)
    {
        return get(o.getBigDecimal(attribute));
    }
    
    @Override
    public String name()
    {
        return "decimal_scale_" + scale;
    }

    @Override
    public long cardinality(Interval<BigDecimal> range)
    {
        if(range.getLeftBound() == null
            || range.getRightBound() == null
            || range.getLeftBound().equals(first())
            || range.getRightBound().equals(last()))
                return Long.MAX_VALUE;
        
        BigDecimal low = range.isLeftOpen() ? next(range.getLeftBound()) : range.getLeftBound();
        BigDecimal high = range.isRightOpen() ? previous(range.getRightBound()) : range.getRightBound();

        BigDecimal l = get(low);
        BigDecimal u = get(high);

        if (l.compareTo(u) > 0)
        {
            return 0;
        }

        BigDecimal result = u.subtract(l)
                .multiply(BigDecimal.TEN.pow(scale))
                .add(BigDecimal.ONE)
                .setScale(0, RoundingMode.HALF_UP);

        if (result.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new SigmaContractException("Cardinality of BigDecimal interval " + result + " cannot be represented by a Long value.");
        } else {
            return result.longValue();
        }

    }

    @Override
    public BigDecimal first()
    {
        return get(BigDecimal.valueOf(-Double.MAX_VALUE));
    }

    @Override
    public BigDecimal last()
    {
        return get(BigDecimal.valueOf(Double.MAX_VALUE));
    }

    @Override
    public BigDecimal add(BigDecimal value, long units) throws SigmaContractException
    {
        Objects.requireNonNull(value, "Cannot add units to null value.");
        
        if( units == 0 ||
            units > 0 && value.compareTo(last().subtract(granularity.multiply(BigDecimal.valueOf(units)))) <= 0 ||
            units < 0 && value.compareTo(first().subtract(granularity.multiply(BigDecimal.valueOf(units)))) >= 0)
                return get(value).add(granularity.multiply(BigDecimal.valueOf(units)));
            
            throw new SigmaContractException("Overflow detected in addition of BigDecimal " + value + " with " + units + " units.");
    }

    @Override
    public BigDecimal parseConstant(String constantAsString)
    {
        return get(new BigDecimal(constantAsString));
    }
}
