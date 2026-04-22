package inventory;

import model.Item;
import model.Zone;

import java.util.*;
import java.util.stream.Collectors;


class HotZoneOptimizer {
  
    private PriorityQueue<Item> demandHeap;
    private final Map<String, String> itemZoneMap;   

    public HotZoneOptimizer() {
        this.demandHeap  = new PriorityQueue<>(
            Comparator.comparingDouble(Item::getDemandScore).reversed());
        this.itemZoneMap = new HashMap<>();
    }

  
    public void rebuildHeap(Collection<Item> allItems) {
        demandHeap = new PriorityQueue<>(
            Comparator.comparingDouble(Item::getDemandScore).reversed());
        demandHeap.addAll(allItems);
    }

    public List<Item> getHotItems(int n) {
        List<Item> drained  = new ArrayList<>();
        List<Item> topItems = new ArrayList<>();
        while (!demandHeap.isEmpty() && topItems.size() < n) {
            Item top = demandHeap.poll();
            topItems.add(top);
            drained.add(top);
        }
        demandHeap.addAll(drained);   
        return topItems;
    }


    public String assignZone(Item item) {
        String zone;
        if (item.getFragility() >= 7)        zone = Zone.FRAGILE;
        else if (item.getDemandScore() >= 70) zone = Zone.HOT;
        else if (item.getDemandScore() >= 40) zone = Zone.COLD;
        else if (item.getWeight() >= 20)      zone = Zone.BULK;
        else                                  zone = Zone.DISPATCH;

        itemZoneMap.put(item.getItemId(), zone);
        return zone;
    }

    public Map<String, String> rebalanceZones(int topN) {
        List<Item>       topItems   = getHotItems(topN);
        Set<String>      hotItemIds = topItems.stream()
                                              .map(Item::getItemId)
                                              .collect(Collectors.toSet());
        Map<String, String> moves = new LinkedHashMap<>();

        for (Map.Entry<String, String> e : itemZoneMap.entrySet()) {
            if (Zone.HOT.equals(e.getValue()) && !hotItemIds.contains(e.getKey())) {
                moves.put(e.getKey(), Zone.COLD);   
            }
        }
        for (Item item : topItems) {
            if (!Zone.HOT.equals(itemZoneMap.get(item.getItemId()))) {
                moves.put(item.getItemId(), Zone.HOT);   
            }
        }
        return moves;
    }

    public Map<String, String> getItemZoneMap() { return Collections.unmodifiableMap(itemZoneMap); }
}


class AffinityMapper {

    
    private final Map<String, Map<String, Integer>> graph;

    public AffinityMapper() {
        this.graph = new HashMap<>();
    }

    public void updateAffinityOnPurchase(List<String> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            for (int j = i + 1; j < itemIds.size(); j++) {
                String a = itemIds.get(i);
                String b = itemIds.get(j);
                graph.computeIfAbsent(a, k -> new HashMap<>()).merge(b, 1, Integer::sum);
                graph.computeIfAbsent(b, k -> new HashMap<>()).merge(a, 1, Integer::sum);
            }
        }
    }

  
    public List<String> getRelatedItems(String itemId, int topK) {
        Map<String, Integer> neighbours = graph.getOrDefault(itemId, Collections.emptyMap());
        return neighbours.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

   
    public List<String[]> getStorageSuggestions(int threshold) {
        List<String[]> suggestions = new ArrayList<>();
        Set<String>    visited     = new HashSet<>();

        for (Map.Entry<String, Map<String, Integer>> outer : graph.entrySet()) {
            for (Map.Entry<String, Integer> inner : outer.getValue().entrySet()) {
                String key = outer.getKey() + "|" + inner.getKey();
                String rev = inner.getKey() + "|" + outer.getKey();
                if (!visited.contains(rev) && inner.getValue() >= threshold) {
                    suggestions.add(new String[]{outer.getKey(), inner.getKey()});
                    visited.add(key);
                }
            }
        }
        return suggestions;
    }

    
    public void buildAffinityGraph(List<List<String>> historicalOrders) {
        for (List<String> order : historicalOrders) {
            updateAffinityOnPurchase(order);
        }
    }

    public Map<String, Map<String, Integer>> getGraph() { return Collections.unmodifiableMap(graph); }
}

class MultiDimPlacer {

    
    private final Map<String, Double>       scoreCache;
    
    private final TreeMap<Double, Item>     scoredItems;

    
    private static final double W_DEMAND    = 0.4;
    private static final double W_WEIGHT    = 0.2;   
    private static final double W_FRAGILITY = 0.2;   
    private static final double W_ACCESS    = 0.2;

    
    private static final double MAX_DEMAND    = 100.0;
    private static final double MAX_WEIGHT    = 100.0;  
    private static final double MAX_FRAGILITY = 10.0;
    private static final double MAX_ACCESS    = 1000.0;

    public MultiDimPlacer() {
        this.scoreCache  = new HashMap<>();
        this.scoredItems = new TreeMap<>();
    }

    public double scoreItem(Item item, double accessFreq) {
        double normDemand    = item.getDemandScore() / MAX_DEMAND;
        double normWeight    = item.getWeight()      / MAX_WEIGHT;
        double normFragility = item.getFragility()   / MAX_FRAGILITY;
        double normAccess    = Math.min(accessFreq, MAX_ACCESS) / MAX_ACCESS;

        double score = W_DEMAND    * normDemand
                     + W_WEIGHT    * (1.0 - normWeight)
                     + W_FRAGILITY * (1.0 - normFragility)
                     + W_ACCESS    * normAccess;

        scoreCache.put(item.getItemId(), score);
        scoredItems.put(score, item);
        return score;
    }


    public String getPlacementSuggestion(Item item, double accessFreq) {
        double score = scoreItem(item, accessFreq);
        if      (score >= 0.7) return Zone.HOT;
        else if (score >= 0.5) return Zone.COLD;
        else if (score >= 0.3) return Zone.BULK;
        else                   return Zone.DISPATCH;
    }


    public List<Item> getPlacementOrder(Collection<Item> items) {
        return items.stream()
                .sorted(Comparator.comparingDouble(
                    i -> -scoreCache.getOrDefault(i.getItemId(), 0.0)))
                .collect(Collectors.toList());
    }

    public Map<String, Double> getScoreCache() { return Collections.unmodifiableMap(scoreCache); }
}

class RealTimeTracker {

    
    private final Map<String, Item>   liveInventory;
    
    private final Set<String>         flaggedItems;
    
    
    private final PriorityQueue<QualityIssue> urgentQueue;

    public RealTimeTracker() {
        this.liveInventory = new HashMap<>();
        this.flaggedItems  = new HashSet<>();
        this.urgentQueue   = new PriorityQueue<>(
            Comparator.comparingInt(QualityIssue::getUrgency).reversed());
    }

    
    public static class QualityIssue {
        private final String itemId;
        private final String reason;
        private final int    urgency;

        public QualityIssue(String itemId, String reason, int urgency) {
            this.itemId  = itemId;
            this.reason  = reason;
            this.urgency = urgency;
        }

        public String getItemId()  { return itemId; }
        public String getReason()  { return reason; }
        public int    getUrgency() { return urgency; }

        @Override
        public String toString() {
            return "QualityIssue[" + itemId + " | " + reason + " | urgency=" + urgency + "]";
        }
    }

    
    public void register(Item item) {
        liveInventory.put(item.getItemId(), item);
    }

        public void updateQuantity(String itemId, int delta) {
        Item item = requireItem(itemId);
        item.adjustQuantity(delta);
    }

    
    public void flagQualityIssue(String itemId, String reason) {
        requireItem(itemId);
        flaggedItems.add(itemId);
        Item item = liveInventory.get(itemId);
        int urgency = item.isOutOfStock() ? 2 : 1;
        urgentQueue.offer(new QualityIssue(itemId, reason, urgency));
    }

    
    public void resolveQualityIssue(String itemId) {
        flaggedItems.remove(itemId);
    }

    
    public String getStatus(String itemId) {
        Item item = requireItem(itemId);
        return String.format("Item[%s | %s | qty=%d | status=%s | flagged=%b]",
            item.getItemId(), item.getName(), item.getQuantity(),
            item.getStatus(), flaggedItems.contains(itemId));
    }

    
    public Set<String> getFlaggedItems() {
        return Collections.unmodifiableSet(flaggedItems);
    }

    
    public QualityIssue peekUrgentIssue() {
        return urgentQueue.peek();
    }

    
    public QualityIssue pollUrgentIssue() {
        return urgentQueue.poll();
    }

    private Item requireItem(String itemId) {
        Item item = liveInventory.get(itemId);
        if (item == null) throw new NoSuchElementException("Item not tracked: " + itemId);
        return item;
    }

    public Map<String, Item> getLiveInventory() { return Collections.unmodifiableMap(liveInventory); }
    public int size()                            { return liveInventory.size(); }
}
