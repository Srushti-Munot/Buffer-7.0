package demand;

import model.Item;

import java.util.*;
import java.util.stream.Collectors;

class DemandEngine {
    
    private final Map<String, TreeMap<Long, Double>> demandHistory;
 
    private final Map<String, Map<String, Double>> regionScores;
  
    private final Queue<String[]>  slidingWindow;
    private final int              windowSize;
  
    private final Map<String, Double> windowScores;

    private PriorityQueue<Map.Entry<String, Double>> topHeap;

    public DemandEngine(int windowSize) {
        this.windowSize    = windowSize;
        this.demandHistory = new HashMap<>();
        this.regionScores  = new HashMap<>();
        this.slidingWindow = new LinkedList<>();
        this.windowScores  = new HashMap<>();
    }

    public void recordDemandEvent(String itemId, String region, double value) {
        long now = System.currentTimeMillis();

        
        demandHistory
            .computeIfAbsent(itemId, k -> new TreeMap<>())
            .put(now, value);

        
        regionScores
            .computeIfAbsent(region, k -> new HashMap<>())
            .merge(itemId, value, Double::sum);

        
        if (slidingWindow.size() >= windowSize) {
            String[] evicted = slidingWindow.poll();
            
            double evictedVal = Double.parseDouble(evicted[1]);
            windowScores.merge(evicted[0], -evictedVal, Double::sum);
        }
        slidingWindow.offer(new String[]{itemId, String.valueOf(value)});
        windowScores.merge(itemId, value, Double::sum);
    }

    
    public boolean detectSurge(String itemId) {
        TreeMap<Long, Double> history = demandHistory.get(itemId);
        if (history == null || history.size() < 3) return false;

        List<Double> values = new ArrayList<>(history.values());
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        double latest = history.lastEntry().getValue();
        return latest > mean + 2 * stdDev;
    }


    public double forecastNextPeriod(String itemId, int periods) {
        TreeMap<Long, Double> history = demandHistory.get(itemId);
        if (history == null || history.isEmpty()) return 0.0;

        List<Double> values = new ArrayList<>(history.values());
        int start = Math.max(0, values.size() - periods);
        return values.subList(start, values.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
    }
   
    public List<String> getTopDemandItems(int n) {
        return windowScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
  
    public List<Map.Entry<String, Double>> getRegionDemand(String region) {
        Map<String, Double> scores = regionScores.getOrDefault(region, Collections.emptyMap());
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    
    public void syncDemandScore(Item item) {
        double score = windowScores.getOrDefault(item.getItemId(), item.getDemandScore());
        item.setDemandScore(score);
    }

    public Map<String, TreeMap<Long, Double>> getDemandHistory() { return Collections.unmodifiableMap(demandHistory); }
    public Map<String, Double>                getWindowScores()  { return Collections.unmodifiableMap(windowScores); }
}


class ShortageDetector {

    private static class Threshold {
        final int minQty;
        final int maxQty;
        Threshold(int minQty, int maxQty) { this.minQty = minQty; this.maxQty = maxQty; }
    }

    private final Map<String, Threshold> thresholds;   

    
    private PriorityQueue<Item> shortageHeap;

    private final List<String> shortageAlerts;
    private final List<String> surplusAlerts;

    public ShortageDetector() {
        this.thresholds    = new HashMap<>();
        this.shortageHeap  = new PriorityQueue<>(Comparator.comparingInt(Item::getQuantity));
        this.shortageAlerts = new ArrayList<>();
        this.surplusAlerts  = new ArrayList<>();
    }

    
    public void setThreshold(String itemId, int minQty, int maxQty) {
        thresholds.put(itemId, new Threshold(minQty, maxQty));
    }

    
    public void checkStockLevels(Collection<Item> items) {
        shortageAlerts.clear();
        surplusAlerts.clear();
        shortageHeap = new PriorityQueue<>(Comparator.comparingInt(Item::getQuantity));

        for (Item item : items) {
            Threshold t = thresholds.get(item.getItemId());
            if (t == null) continue;

            if (item.getQuantity() < t.minQty) {
                shortageAlerts.add(item.getItemId());
                shortageHeap.offer(item);
            } else if (item.getQuantity() > t.maxQty) {
                surplusAlerts.add(item.getItemId());
            }
        }
    }
    
    public void triggerAlert(String itemId, String reason) {
        shortageAlerts.add(itemId + ": " + reason);
    }

    
    public List<Item> getShortageItems() {
        List<Item> result  = new ArrayList<>();
        List<Item> drained = new ArrayList<>();
        while (!shortageHeap.isEmpty()) {
            Item item = shortageHeap.poll();
            result.add(item);
            drained.add(item);
        }
        shortageHeap.addAll(drained);   
        return result;
    }

    public List<String> getSurplusItems()  { return Collections.unmodifiableList(surplusAlerts); }
    public List<String> getShortageAlerts(){ return Collections.unmodifiableList(shortageAlerts); }
}

class ReorderSystem {

    private static class ReorderPolicy {
        final int reorderPoint;   
        final int reorderQty;     
        ReorderPolicy(int point, int qty) { this.reorderPoint = point; this.reorderQty = qty; }
    }

    private final Map<String, ReorderPolicy> policies;   

    
    private final PriorityQueue<String[]> pendingReorders;   

    
    private final Queue<String> notificationQueue;

    
    private final Set<String> reorderSet;

    public ReorderSystem() {
        this.policies          = new HashMap<>();
        this.pendingReorders   = new PriorityQueue<>(
            Comparator.comparingInt(arr -> -Integer.parseInt(arr[1])));  
        this.notificationQueue = new LinkedList<>();
        this.reorderSet        = new HashSet<>();
    }

    
    public void setReorderPolicy(String itemId, int reorderPoint, int reorderQty) {
        policies.put(itemId, new ReorderPolicy(reorderPoint, reorderQty));
    }

   
    public void checkAndNotify(Collection<Item> items) {
        for (Item item : items) {
            ReorderPolicy policy = policies.get(item.getItemId());
            if (policy == null) continue;

            if (item.getQuantity() <= policy.reorderPoint && !reorderSet.contains(item.getItemId())) {
                String msg = String.format(
                    "REORDER: %s (%s) — qty=%d at/below reorder point=%d. Order qty=%d",
                    item.getItemId(), item.getName(),
                    item.getQuantity(), policy.reorderPoint, policy.reorderQty);
                notificationQueue.offer(msg);
                pendingReorders.offer(new String[]{item.getItemId(), String.valueOf(policy.reorderQty)});
                reorderSet.add(item.getItemId());
            }
        }
    }

    
    public String pollNotification() {
        return notificationQueue.poll();
    }
 
    public List<String> getReorderList() {
        return new ArrayList<>(reorderSet);
    }

    
    public void fulfilReorder(String itemId) {
        reorderSet.remove(itemId);
    }

    public int pendingCount()       { return pendingReorders.size(); }
    public int notificationCount()  { return notificationQueue.size(); }
}
