import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.cost.AggregateCostFunction;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.contracts.DateTimeContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.bounding.PartitionedBounding;
import be.ugent.ledc.sigma.repair.cost.functions.*;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ExperimentsElection extends Experiments<LocalDateTime> {

    public ExperimentsElection(
            String path, String queryFilename, String rulesetFilename, String timeAttribute, String partitionAttribute, boolean earlyStop) {
        super(path, queryFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
    }

    public ExperimentsElection(ExperimentsElection other) {
        super(other);
    }

    @Override
    public void createTemporalDatasets() throws ChronosException {
        DateTimeContractor contractor = new DateTimeContractor(LocalDateTime::plusSeconds, ChronoUnit.SECONDS);
        this.partitionedDatasets = TemporalDataset.create(fullDataset, timeAttribute, contractor, partitionAttribute);
    }

    private <T extends Comparable<? super T>> Map<Integer, T> getMostFrequent(String attribute) throws ChronosException {
        Map<Integer, T> mapping = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<LocalDateTime> dataset = this.partitionedDatasets.get(key);
            Signal<LocalDateTime, T> signal = dataset.getAttributeSignal(attribute);

            Map<T, Integer> frequencies = new HashMap<>();
            LocalDateTime curr = signal.start();
            while (curr != null) {
                T value = signal.get(curr);
                if (value != null) {
                    int count = frequencies.getOrDefault(value, 0);
                    frequencies.put(value, count + 1);
                }
                curr = signal.nextIndex(curr);
            }

            int max = 0;
            T maxValue = null;
            for (Map.Entry<T, Integer> entry: frequencies.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    maxValue = entry.getKey();
                }
            }
            mapping.put(Integer.parseInt(key), maxValue);
        }

        return mapping;
    }

    private Map<Integer, Long> getMostFrequentBins(String attribute, long binSize) throws ChronosException {
        Map<Integer, Long> mapping = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<LocalDateTime> dataset = this.partitionedDatasets.get(key);
            Signal<LocalDateTime, Long> signal = dataset.getAttributeSignal(attribute);

            Map<Long, Integer> frequencies = new HashMap<>();
            LocalDateTime curr = signal.start();
            while (curr != null) {
                Long value = signal.get(curr);
                if (value != null) {
                    long bin = value / binSize;
                    Long index = bin * binSize + (binSize / 2);
                    frequencies.merge(index, 1, Integer::sum);
                }
                curr = signal.nextIndex(curr);
            }

            int max = 0;
            Long maxValue = null;
            for (Map.Entry<Long, Integer> entry: frequencies.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    maxValue = entry.getKey();
                }
            }
            mapping.put(Integer.parseInt(key), maxValue);
        }

        return mapping;
    }

    @Override
    protected NonConstantCostModel getNonConstantCostModel(Set<String> exclude) throws ChronosException {
        Map<String, IterableCostFunction<?>> costFunctions = new HashMap<>();

        BigDecimal zero = new BigDecimal("0");
        BigDecimal hundred = new BigDecimal("100");

        costFunctions.put("page_title", new IterableCostFunctionWrapper<>(new EditDistanceCostFunction<String>()));
        costFunctions.put("type", new IterableCostFunctionWrapper<>(new EditDistanceCostFunction<String>()));

        Map<Integer, Long> frequenciesVotes = getMostFrequent("votes_for_election");
        costFunctions.put("votes_for_election", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesVotes
        )));

        Map<Integer, Long> frequenciesNeededVotes = getMostFrequent("needed_votes");
        costFunctions.put("needed_votes", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesNeededVotes
        )));

        Map<Integer, BigDecimal> frequenciesTurnout = getMostFrequent("turnout");
        RangedDistanceCostFunction<BigDecimal,?> turnoutCost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, zero, hundred);
        PartitionTargetCostFunction<BigDecimal,?,?,?> turnoutCost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesTurnout
        );
        RoundingCostFunction<BigDecimal,?> turnoutCost3 = new RoundingCostFunction<>(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, new Pair<>(zero, hundred));
        costFunctions.put("turnout", new IterableCostFunctionWrapper<>(
                AggregateCostFunction.createAggregateByProduct(turnoutCost3, AggregateCostFunction.createAggregateBySum(Map.of(turnoutCost1, 1, turnoutCost2, 1)))
        ));

        Map<Integer, Long> frequenciesElectoral1 = getMostFrequent("electoral_vote1");
        RangedDistanceCostFunction<Long,?> electoral1Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 1000L);
        PartitionTargetCostFunction<Long,?,?,?> electoral1Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesElectoral1
        );
        costFunctions.put("electoral_vote1", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(electoral1Cost1, 1, electoral1Cost2, 1)
        )));

        Map<Integer, Long> frequenciesPopular1 = getMostFrequentBins("popular_vote1", 500000L);
        RangedDistanceCostFunction<Long,?> popular1Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 2000000000L);
        PartitionTargetCostFunction<Long,?,?,?> popular1Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPopular1
        );
        costFunctions.put("popular_vote1", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(popular1Cost1, 1, popular1Cost2, 1)
        )));

        Map<Integer, BigDecimal> frequenciesPerc1 = getMostFrequent("percentage1");
        costFunctions.put("percentage1", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPerc1
        )));

        Map<Integer, Long> frequenciesElectoral2 = getMostFrequent("electoral_vote2");
        RangedDistanceCostFunction<Long,?> electoral2Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 1000L);
        PartitionTargetCostFunction<Long,?,?,?> electoral2Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesElectoral2
        );
        costFunctions.put("electoral_vote2", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(electoral2Cost1, 1, electoral2Cost2, 1)
        )));

        Map<Integer, Long> frequenciesPopular2 = getMostFrequentBins("popular_vote2", 500000L);
        RangedDistanceCostFunction<Long,?> popular2Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 2000000000L);
        PartitionTargetCostFunction<Long,?,?,?> popular2Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPopular2
        );
        costFunctions.put("popular_vote2", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(popular2Cost1, 1, popular2Cost2, 1)
        )));

        Map<Integer, BigDecimal> frequenciesPerc2 = getMostFrequent("percentage2");
        costFunctions.put("percentage2", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPerc2
        )));

        this.costFunctions = new LinkedHashMap<>();
        this.costFunctions.putAll(costFunctions);

        return new NonConstantCostModel(TRuleset.unfold(costFunctions), Map.of(
                "popular_vote1#curr", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote1", 40000),
                "popular_vote1#next", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote1", 40000),
                "popular_vote2#curr", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote2", 40000),
                "popular_vote2#next", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote2", 40000)
        ));
    }

    /**
     * This main function creates a new instance of the RepairCustomGAIT class in each run,
     * and thus executes the preprocessing/initialization phase each run.
     */
    public static void main(String[] args) throws ChronosException, IOException, ParseException, RepairException, DataReadException {
        long startTotal = System.currentTimeMillis();

        String path = "data/election";
        String datasetFilename = "dataset.csv";
        String rulesetFilename = "rules.rbx";
        String timeAttribute = "value_valid_from";
        String partitionAttribute = "page_id";

        boolean earlyStop = true;
        boolean baseline = true;
        int numAnchors = 1;

        System.out.println("Running ExperimentsElection with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");

        int amount = 10;
        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;
        double executionTime = 0.0;
        double executionTimePreprocessing = 0.0;
        Validator<LocalDateTime> validator = new Validator<>(path + "/error_locations.txt");
        for (int i = 0; i < amount; i++) {
            System.out.println("Run " + i);
            ExperimentsElection rw = new ExperimentsElection(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);

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
     * This main function takes a copy of the RepairCustomGAIT class in each run,
     * so only executes the preprocessing/initialization once.
     */
//    public static void main(String[] args) throws ChronosException, IOException, ParseException, RepairException, DataReadException {
//        long startTotal = System.currentTimeMillis();
//
//        String path = "data/election";
//        String datasetFilename = "dataset.csv";
//        String rulesetFilename = "rules.rbx";
//        String timeAttribute = "value_valid_from";
//        String partitionAttribute = "page_id";
//
//        boolean earlyStop = true;
//        boolean baseline = true;
//        int numAnchors = 1;
//
//        ExperimentsElection rw = new ExperimentsElection(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
//
//        System.out.println("Running RepairWikipediaElection with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");
//
//        // read dataset and rules
//        long start = System.currentTimeMillis();
//        rw.readDatasetFromPath(";");
//        System.out.println(rw.fullDataset.getSize() * rw.fullDataset.getContract().getAttributes().size());
//        rw.readRulesFromPath();
//        long stop = System.currentTimeMillis();
//        System.out.println("Time for reading dataset and rules: " + (stop - start)/1000.0 + " seconds");
//
//        double dur = rw.initialize(baseline, new HashSet<>(), NullBehavior.NO_REPAIR);
//        System.out.println("Time for initialization: " + dur + " minutes");
//
//        int numCells = rw.fullDataset.getSize() * rw.fullDataset.getContract().getAttributes().size();
//        System.out.println("Total cells: " + numCells);
//
//        // execute repair
//        int amount = 10;
//        double precision = 0.0;
//        double recall = 0.0;
//        double f1 = 0.0;
//        double execution_time = 0.0;
//        for (int i = 0; i < amount; i++) {
//            ExperimentsElection copy = new ExperimentsElection(rw);
//            Validator<LocalDateTime> validator = new Validator<>(copy.groundTruthPath);
//            double duration = copy.run(numAnchors, validator, new HashSet<>());
//
//            validator.setPartitionedLocations(copy.convertRepairLocations(validator.getPartitionedLocations()));
//            Map<String, List<Validator.Location<LocalDateTime>>> convertedLocations = copy.convertRepairLocations();
//            List<Double> metrics = validator.validate(copy.validationPath, convertedLocations, numCells);
//            precision += metrics.get(0);
//            recall += metrics.get(1);
//            f1 += metrics.get(2);
//            execution_time += duration;
//            System.out.println(duration + " ; " + metrics);
//        }
//        System.out.println("Avg precision: " + precision / amount);
//        System.out.println("Avg recall: " + recall / amount);
//        System.out.println("Avg f1: " + f1 / amount);
//        System.out.println("Avg execution_time: " + execution_time / amount);
//
//        long endTotal = System.currentTimeMillis();
//        System.out.println("Total runtime: " + (endTotal - startTotal)/1000.0/60.0 + " minutes");
//    }

}
