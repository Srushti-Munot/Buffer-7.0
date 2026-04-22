package warehouse;

import java.util.*;


class EfficiencyCalculator {

    
    private final Map<String, Long>   orderTimes;
    
    private final Map<String, Double> orderDistances;
    
    private final List<Double>        scoreHistory;
    
    private final TreeMap<Long, Double> chronoScores;

    
    private double baseline;

    public EfficiencyCalculator() {
        this.orderTimes     = new HashMap<>();
        this.orderDistances = new HashMap<>();
        this.scoreHistory   = new ArrayList<>();
        this.chronoScores   = new TreeMap<>();
        this.baseline       = -1;   
    }

    
    public void recordOrderTime(String orderId, long startMs, long endMs) {
        if (endMs < startMs) throw new IllegalArgumentException("endMs must be >= startMs");
        orderTimes.put(orderId, endMs - startMs);
    }

    
    public void recordDistance(String orderId, double units) {
        orderDistances.put(orderId, units);
    }

    
    public double timePerOrder() {
        if (orderTimes.isEmpty()) return 0.0;
        return orderTimes.values().stream()
                .mapToLong(Long::longValue)
                .average().orElse(0.0);
    }

    
    public double distanceTraveled() {
        if (orderDistances.isEmpty()) return 0.0;
        return orderDistances.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
    }

    
    public double getScore() {
        double targetTime = 60_000.0;
        double targetDist = 200.0;

        double timePenalty = Math.min(timePerOrder() / targetTime, 1.0);
        double distPenalty = Math.min(distanceTraveled() / targetDist, 1.0);

        double score = 100.0 - (50.0 * timePenalty + 50.0 * distPenalty);
        score = Math.max(0, Math.min(100, score));

        
        scoreHistory.add(score);
        chronoScores.put(System.currentTimeMillis(), score);

        
        if (baseline < 0) baseline = score;

        return score;
    }

    public double compareWithBaseline() {
        if (baseline <= 0) return 0.0;
        double current = scoreHistory.isEmpty() ? getScore() : scoreHistory.get(scoreHistory.size() - 1);
        return ((current - baseline) / baseline) * 100.0;
    }

    
    public void setBaseline(double score) { this.baseline = score; }

    public List<Double>            getScoreHistory() { return Collections.unmodifiableList(scoreHistory); }
    public TreeMap<Long, Double>   getChronoScores() { return new TreeMap<>(chronoScores); }
    public int                     ordersTracked()   { return orderTimes.size(); }
}


class ContextEngine {

    public static final String RUSH_HOUR    = "RUSH_HOUR";
    public static final String LOW_LOAD     = "LOW_LOAD";
    public static final String NORMAL       = "NORMAL";
    public static final String SURGE        = "SURGE";
    public static final String MAINTENANCE  = "MAINTENANCE";

    
    private final Map<String, Runnable> strategies;

    
    private final Queue<String> modeChangeLog;

    private String currentMode;

    public ContextEngine() {
        this.strategies    = new HashMap<>();
        this.modeChangeLog = new LinkedList<>();
        this.currentMode   = NORMAL;

        
        for (String mode : new String[]{RUSH_HOUR, LOW_LOAD, NORMAL, SURGE, MAINTENANCE})
            strategies.put(mode, () -> {});
    }

    
    public void registerStrategy(String modeName, Runnable action) {
        strategies.put(modeName, action);
    }

    public String detectMode(int queueSize, int staffCount, int hour) {
        if (staffCount == 0)                                      return MAINTENANCE;
        if (queueSize > 100 && (hour == 8 || hour == 9 || hour == 17 || hour == 18)) return RUSH_HOUR;
        if (queueSize > 150)                                      return SURGE;
        if (queueSize < 10)                                       return LOW_LOAD;
        return NORMAL;
    }

    
    public void applyStrategy(String mode) {
        if (!mode.equals(currentMode)) {
            String entry = String.format("[%d] %s → %s",
                System.currentTimeMillis(), currentMode, mode);
            modeChangeLog.offer(entry);
            if (modeChangeLog.size() > 1000) modeChangeLog.poll();  
            currentMode = mode;
        }
        Runnable strategy = strategies.getOrDefault(mode, () -> {});
        strategy.run();
    }

    
    public String detectAndApply(int queueSize, int staffCount, int hour) {
        String mode = detectMode(queueSize, staffCount, hour);
        applyStrategy(mode);
        return mode;
    }

    public String        getCurrentMode()  { return currentMode; }
    public Queue<String> getModeChangeLog(){ return new LinkedList<>(modeChangeLog); }
    public int           logSize()         { return modeChangeLog.size(); }
}
