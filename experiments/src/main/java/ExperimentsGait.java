import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.cost.AggregateCostFunction;
import be.ugent.ledc.core.cost.ConstantCostFunction;
import be.ugent.ledc.sigma.datastructures.contracts.DateContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.cost.functions.*;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

public class ExperimentsGait extends Experiments<LocalDate> {

    public ExperimentsGait(String path, String datasetFilename, String rulesetFilename,
                           String timeAttribute, String partitionAttribute, boolean earlyStop) {
        super(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
    }

    public ExperimentsGait(ExperimentsGait other) {
        super(other);
    }

    /**
     * Give, for each patient (patient_upn), the median value over the dataset for a specific attribute.
     * Some values should not be considered, and an alternative can be given if all values are null or values to be skipped.
     * @param attribute     the attribute to compute the median value for
     * @param skip          the values that should not be considered when computing the median
     * @param alternative   the alternative for the median value
     * @param averager      a binary operator that computes the average of two values
     * @param <T>           the type of the attribute values
     * @return              a map with the median value for each partition key
     */
    private <T extends Comparable<? super T>> Map<Integer, T> getMedians(
            String attribute, Set<T> skip, T alternative, BinaryOperator<T> averager
    ) throws ChronosException {
        Map<Integer, T> medians = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<LocalDate> dataset = this.partitionedDatasets.get(key);
            Signal<LocalDate, T> signal = dataset.getAttributeSignal(attribute);

            List<T> values = new ArrayList<>();
            LocalDate curr = signal.start();
            while (curr != null) {
                T value = signal.get(curr);
                if (value != null && skip.stream().noneMatch(bd -> bd.compareTo(value) == 0))
                    values.add(value);
                curr = signal.nextIndex(curr);
            }

            if (values.isEmpty()) medians.put(Integer.parseInt(key), alternative);
            else {
                values.sort(T::compareTo);
                T median = values.get(values.size()/2);
                if (values.size() % 2 == 0) {
                    median = averager.apply(values.get(values.size()/2), values.get(values.size()/2 - 1));
                }

                medians.put(Integer.parseInt(key), median);
            }
        }

        return medians;
    }

    @Override
    public void createTemporalDatasets() throws ChronosException {
        DateContractor dateContractor = new DateContractor(LocalDate::plusDays, ChronoUnit.DAYS, (current) -> current);
        this.partitionedDatasets = TemporalDataset.create(fullDataset, timeAttribute, dateContractor, partitionAttribute);
    }

    @Override
    protected NonConstantCostModel getNonConstantCostModel(Set<String> exclude) throws ChronosException {
        Map<String, IterableCostFunction<?>> costFunctions = new HashMap<>();

        costFunctions.put("gender", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("test_indication", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));
        costFunctions.put("living_status_i", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));
        costFunctions.put("living_status_ii", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>()));
        costFunctions.put("previous_fall_s", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>()));
        costFunctions.put("previous_fall_12_months", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));
        costFunctions.put("fear_of_falling", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));
        costFunctions.put("physical_activities_no_yes", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));
        costFunctions.put("eyeglasses_yes_no", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>()));
        costFunctions.put("eyeglasses_worn_during_test", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));
        costFunctions.put("hearing_aid_no_yes", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>()));
        costFunctions.put("hearing_aid_worn_during_test", new IterableCostFunctionWrapper<>(new ConstantCostFunction<Integer>(2)));

        costFunctions.put("gender_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("test_indication_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("living_status_i_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("living_status_ii_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("previous_fall_s_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("previous_fall_12_months_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>(2)));
        costFunctions.put("fear_of_falling_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("physical_activities_no_yes_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("eyeglasses_yes_no_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("eyeglasses_worn_during_test_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>(2)));
        costFunctions.put("eyeglasses_test_type_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("hearing_aid_no_yes_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("hearing_aid_side_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("hearing_aid_worn_during_test_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>(2)));
        costFunctions.put("hearing_aid_which_worn_during_test_txt", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>(2)));

        // attributes with numerical values
        BigDecimal zero = new BigDecimal("0");
        BigDecimal two = new BigDecimal("2");
        BigDecimal value999 = new BigDecimal("999");
        BinaryOperator<Integer> averagerI = (a, b) -> (a+b) / 2;
        BinaryOperator<BigDecimal> averagerBD = (a, b) -> a.add(b).divide(two, RoundingMode.HALF_UP);
        BiPredicate<BigDecimal, BigDecimal> predicate = (or, re) -> or == null || zero.compareTo(or) >= 0 || value999.compareTo(or) == 0;

        Map<Integer, Integer> mediansMmse = getMedians("mmse", Set.of(999), 999, averagerI);
        RangedDistanceCostFunction<Integer,?> mmseDistanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.INTEGER, 0, 30);
        PartitionTargetCostFunction<Integer,?,?,?> mmseTargetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.INTEGER, SigmaContractorFactory.INTEGER, "patient_upn#curr", mediansMmse
        );
        costFunctions.put("mmse", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(Map.of(mmseDistanceCost, 1, mmseTargetCost, 1))));

        Map<Integer, BigDecimal> mediansHeight = getMedians("height_m", Set.of(zero, value999), value999, averagerBD);
        RangedDistanceCostFunction<BigDecimal, ?> heightDistanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, zero, new BigDecimal("300"));
        PartitionTargetCostFunction<BigDecimal,?,?,?> heightTargetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER,
                "patient_upn#curr", mediansHeight
        );
        costFunctions.put("height_m", new IterableCostFunctionWrapper<>(new PredicateCostFunction<>(
                predicate,
                heightTargetCost,
                AggregateCostFunction.createAggregateBySum(Map.of(heightDistanceCost, 1, heightTargetCost, 1))))
        );

        Map<Integer, BigDecimal> mediansWeight = getMedians("weight_kg", Set.of(zero, value999), value999, averagerBD);
        RangedDistanceCostFunction<BigDecimal, ?> weightDistanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, zero, new BigDecimal("500"));
        PartitionTargetCostFunction<BigDecimal,?,?,?> weightTargetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER,
                "patient_upn#curr", mediansWeight
        );
        costFunctions.put("weight_kg", new IterableCostFunctionWrapper<>(new PredicateCostFunction<>(
                predicate,
                weightTargetCost,
                AggregateCostFunction.createAggregateBySum(Map.of(weightDistanceCost, 1, weightTargetCost, 1))))
        );

        Map<Integer, BigDecimal> mediansBMI = getMedians("bmi_kg_m2", Set.of(zero, value999), value999, averagerBD);
        RangedDistanceCostFunction<BigDecimal, ?> bmiDistanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, zero, new BigDecimal("60"));
        PartitionTargetCostFunction<BigDecimal,?,?,?> bmiTargetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.INTEGER,
                "patient_upn#curr", mediansBMI
        );
        costFunctions.put("bmi_kg_m2", new IterableCostFunctionWrapper<>(new PredicateCostFunction<>(
                predicate,
                bmiTargetCost,
                AggregateCostFunction.createAggregateBySum(Map.of(bmiDistanceCost, 1, bmiTargetCost, 1))))
        );

        Map<Integer, BigDecimal> mediansLegRight = getMedians("leg_right", Set.of(zero, value999), value999, averagerBD);
        RangedDistanceCostFunction<BigDecimal, ?> legRightDistanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, zero, new BigDecimal("250"));
        PartitionTargetCostFunction<BigDecimal,?,?,?> legRightTargetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER,
                "patient_upn#curr", mediansLegRight
        );
        costFunctions.put("leg_right", new IterableCostFunctionWrapper<>(new PredicateCostFunction<>(
                predicate,
                legRightTargetCost,
                AggregateCostFunction.createAggregateBySum(Map.of(legRightDistanceCost, 1, legRightTargetCost, 1))))
        );

        Map<Integer, BigDecimal> mediansLegLeft = getMedians("leg_left", Set.of(zero, value999), value999, averagerBD);
        RangedDistanceCostFunction<BigDecimal, ?> legLeftDistanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, zero, new BigDecimal("250"));
        PartitionTargetCostFunction<BigDecimal,?,?,?> legLeftTargetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER,
                "patient_upn#curr", mediansLegLeft
        );
        costFunctions.put("leg_left", new IterableCostFunctionWrapper<>(new PredicateCostFunction<>(
                predicate,
                legLeftTargetCost,
                AggregateCostFunction.createAggregateBySum(Map.of(legLeftDistanceCost, 1, legLeftTargetCost, 1))))
        );

        this.costFunctions = new LinkedHashMap<>();
        this.costFunctions.putAll(costFunctions);

        return new NonConstantCostModel(TRuleset.unfold(costFunctions), new HashMap<>());
    }

    public static void main(String[] args) throws ChronosException, DataReadException, ParseException, IOException, RepairException {
        long startTotal = System.currentTimeMillis();

        String path = "data/gait";
        String datasetFilename = "dataset.csv";
        String rulesetFilename = "rules.rbx";
        String timeAttribute = "datum";
        String partitionAttribute = "patient_upn";

        boolean baseline = true;
        int numAnchors = 1;
        boolean earlyStop = true;

        System.out.println("Running ExperimentsGait with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");

        int amount = 10;
        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;
        double executionTime = 0.0;
        double executionTimePreprocessing = 0.0;
        Validator<LocalDate> validator = new Validator<>(path + "/error_locations.txt");
        for (int i = 0; i < amount; i++) {
            System.out.println("Run " + i);
            ExperimentsGait rd = new ExperimentsGait(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);

            rd.readDatasetFromPath(";");
            rd.readRulesFromPath();
            double durationPreprocessing = rd.initialize(baseline, new HashSet<>(), NullBehavior.NO_REPAIR);
            double duration = rd.run(numAnchors, validator, new HashSet<>());

            int numCells = rd.fullDataset.getSize() * rd.fullDataset.getContract().getAttributes().size();
            validator.setPartitionedLocations(rd.convertRepairLocations(validator.getPartitionedLocations()));
            Map<String, List<Validator.Location<LocalDate>>> convertedLocations = rd.convertRepairLocations();
            List<Double> metrics = validator.validate(rd.validationPath, convertedLocations, numCells);
            precision += metrics.get(0);
            recall += metrics.get(1);
            f1 += metrics.get(2);
            executionTime += duration;
            executionTimePreprocessing += durationPreprocessing;
            System.out.println(durationPreprocessing + " + " + duration + ": " + metrics);
        }
        System.out.println("Avg precision: " + precision / amount);
        System.out.println("Avg recall: " + recall / amount);
        System.out.println("Avg f1: " + f1 / amount);
        System.out.println("Avg execution time preprocessing: " + executionTimePreprocessing / amount);
        System.out.println("Avg execution time: " + executionTime / amount);

        long endTotal = System.currentTimeMillis();
        System.out.println("Total runtime: " + (endTotal - startTotal)/1000.0/60.0 + " minutes");
    }


}
