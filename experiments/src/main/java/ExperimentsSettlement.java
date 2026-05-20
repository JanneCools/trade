import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.cost.AggregateCostFunction;
import be.ugent.ledc.sigma.datastructures.contracts.DateTimeContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.bounding.PartitionedBounding;
import be.ugent.ledc.sigma.repair.bounding.PartitionedTemporalBounding;
import be.ugent.ledc.sigma.repair.cost.functions.*;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BinaryOperator;

public class ExperimentsSettlement extends Experiments<LocalDateTime> {

    public ExperimentsSettlement(
            String path, String queryFilename, String rulesetFilename,
            String timeAttribute, String partitionAttribute, boolean earlyStop) {
        super(path, queryFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
    }

    public ExperimentsSettlement(ExperimentsSettlement other) {
        super(other);
    }

    private <T extends Comparable<? super T>> Map<Integer, T> getMedians(
            String attribute, BinaryOperator<T> averager
    ) throws ChronosException {
        Map<Integer, T> medians = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<LocalDateTime> dataset = this.partitionedDatasets.get(key);
            Signal<LocalDateTime, T> signal = dataset.getAttributeSignal(attribute);

            List<T> values = new ArrayList<>();
            LocalDateTime curr = signal.start();
            while (curr != null) {
                T value = signal.get(curr);
                if (value != null)
                    values.add(value);
                curr = signal.nextIndex(curr);
            }

            values.sort(T::compareTo);
            T median = null;
            if (!values.isEmpty()) {
                median = values.get(values.size()/2);
                if (values.size() % 2 == 0) {
                    median = averager.apply(values.get(values.size()/2), values.get(values.size()/2 - 1));
                }
            }

            medians.put(Integer.parseInt(key), median);
        }

        return medians;
    }

    @Override
    public void createTemporalDatasets() throws ChronosException {
        DateTimeContractor contractor = new DateTimeContractor(LocalDateTime::plusSeconds, ChronoUnit.SECONDS);
        this.partitionedDatasets = TemporalDataset.create(fullDataset, timeAttribute, contractor, partitionAttribute);
    }

    @Override
    protected NonConstantCostModel getNonConstantCostModel(Set<String> exclude) throws ChronosException {
        Map<String, IterableCostFunction<?>> costFunctions = new HashMap<>();

        costFunctions.put("page_title", new IterableCostFunctionWrapper<>(new EditDistanceCostFunction<String>()));

        costFunctions.put("population_total", new IterableCostFunctionWrapper<>(new RangedDistanceCostFunction<>(
                SigmaContractorFactory.LONG, 0L, 2000000000L
        )));
        costFunctions.put("population_density_km2", new IterableCostFunctionWrapper<>(new RangedDistanceCostFunction<>(
                SigmaContractorFactory.LONG, 0L, 50000L
        )));

        BigDecimal zero = new BigDecimal("0");
        BigDecimal two = new BigDecimal("2");
        BigDecimal maxArea = new BigDecimal("1000000");
        BinaryOperator<BigDecimal> averagerBD = (a, b) -> a.add(b).divide(two, RoundingMode.HALF_UP);
        Map<Integer, BigDecimal> medians = getMedians("area_total_km2", averagerBD);
        RangedDistanceCostFunction<BigDecimal,?> distanceCost = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, zero, maxArea);
        PartitionTargetCostFunction<BigDecimal,?,?,?> targetCost = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.INTEGER,
                "page_id#curr", medians
        );
        costFunctions.put("area_total_km2", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(distanceCost, 1, targetCost, 1)
        )));

        this.costFunctions = new LinkedHashMap<>();
        this.costFunctions.putAll(costFunctions);

        return new NonConstantCostModel(TRuleset.unfold(costFunctions), Map.of(
                "area_total_km2#curr", new PartitionedBounding<>(
                        this.fullDataset, "page_id",
                        SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "area_total_km2",
                        1000, zero, new BigDecimal("1000000")),
                "area_total_km2#next", new PartitionedBounding<>(
                        this.fullDataset, "page_id",
                        SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "area_total_km2",
                        1000, zero, new BigDecimal("1000000")),
                "population_total#curr", new PartitionedTemporalBounding<>(
                        this.fullDataset, "page_id", SigmaContractorFactory.DATETIME_SECONDS, "value_valid_from",
                        SigmaContractorFactory.LONG, "population_total",
                        5, 50000, 0L, 2000000000L),
                "population_total#next", new PartitionedTemporalBounding<>(
                        this.fullDataset, "page_id", SigmaContractorFactory.DATETIME_SECONDS, "value_valid_from",
                        SigmaContractorFactory.LONG, "population_total",
                        5, 5000, 0L, 2000000000L),
                "population_density_km2#curr", new PartitionedTemporalBounding<>(
                        this.fullDataset, "page_id", SigmaContractorFactory.DATETIME_SECONDS, "value_valid_from",
                        SigmaContractorFactory.LONG, "population_density_km2",
                        5, 5000, 0L, 50000L),
                "population_density_km2#next", new PartitionedTemporalBounding<>(
                        this.fullDataset, "page_id", SigmaContractorFactory.DATETIME_SECONDS, "value_valid_from",
                        SigmaContractorFactory.LONG, "population_density_km2",
                        5, 5000, 0L, 50000L)
        ));
    }

    public static void main(String[] args) throws ChronosException, IOException, ParseException, RepairException, DataReadException {
        long startTotal = System.currentTimeMillis();

        String path = "data/settlement";
        String datasetFilename = "dataset.csv";
        String rulesetFilename = "rules.rbx";
        String timeAttribute = "value_valid_from";
        String partitionAttribute = "page_id";

        boolean baseline = true;
        int numAnchors = 1;
        boolean earlyStop = true;

        System.out.println("Running ExperimentsSettlement with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");

        int amount = 10;
        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;
        double executionTime = 0.0;
        double executionTimePreprocessing = 0.0;
        Validator<LocalDateTime> validator = new Validator<>(path + "/error_locations.txt");
        for (int i = 0; i < amount; i++) {
            ExperimentsSettlement rw = new ExperimentsSettlement(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);

            rw.readDatasetFromPath(";");
            rw.readRulesFromPath();
            double durationPreprocessing = rw.initialize(baseline, new HashSet<>(), NullBehavior.NO_REPAIR);
            double duration = rw.run(numAnchors, validator, new HashSet<>());

            int numCells = rw.fullDataset.getSize() * rw.fullDataset.getContract().getAttributes().size();
            validator.setPartitionedLocations(rw.convertRepairLocations(validator.getPartitionedLocations()));
            Map<String, List<Validator.Location<LocalDateTime>>> convertedLocations = rw.convertRepairLocations();
            List<Double> metrics = validator.validate(rw.validationPath, convertedLocations, numCells);
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

    /**
     * This main function runs the scalability tests. It only executes the preprocessing phase once,
     * as this is not influenced by the dataset size
     */
//    public static void main(String[] args) throws ChronosException, IOException, ParseException, RepairException, DataReadException {
//        long startTotal = System.currentTimeMillis();
//
//        String tuples = "5000";
//
//        String path = "data/settlement";
//        String datasetFilename = "dataset_" + tuples + ".csv";
//        String rulesetFilename = "rules.rbx";
//        String timeAttribute = "value_valid_from";
//        String partitionAttribute = "page_id";
//        boolean earlyStop = true;
//        boolean baseline = true;
//        int numAnchors = 1;
//
//        ExperimentsSettlement rw = new ExperimentsSettlement(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
//
//        System.out.println("Running Scalability tests with " + tuples + " tuples, "
//                + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");
//
//        // read dataset and rules
//        long start = System.currentTimeMillis();
//        rw.readDatasetFromPath(";");
//        rw.readRulesFromPath();
//        long stop = System.currentTimeMillis();
//        System.out.println("Time for reading dataset and rules: " + (stop - start)/1000.0 + " seconds");
//
//        int numCells = rw.fullDataset.getSize() * rw.fullDataset.getContract().getAttributes().size();
//        System.out.println("Total cells: " + numCells);
//
//        double dur = rw.initialize(baseline, new HashSet<>(), NullBehavior.NO_REPAIR);
//        System.out.println("Time for initialization: " + dur + " minutes");
//
//        // execute repair
//        int amount = 10;
//        double execution_time = 0.0;
//        for (int i = 0; i < amount; i++) {
//            ExperimentsSettlement copy = new ExperimentsSettlement(rw);
//            Validator<LocalDateTime> validator = new Validator<>(copy.groundTruthPath);
//            double duration = copy.run(numAnchors, validator, new HashSet<>());
//
//            execution_time += duration;
//            System.out.println(duration);
//        }
//        System.out.println("Avg execution_time: " + execution_time / amount);
//
//        long endTotal = System.currentTimeMillis();
//        System.out.println("Total runtime: " + (endTotal - startTotal)/1000.0/60.0 + " minutes");
//    }

}
