package multiwarehouse;

import model.Item;
import model.Warehouse;

import java.util.*;
import java.util.stream.Collectors;

class StockBalancer {

    private final Map<String, Warehouse>       warehouses;       
    private final Map<String, List<String>>    itemLocations;    
    private final TreeMap<String, Double>      utilizationScores;

    public StockBalancer() {
        this.warehouses        = new HashMap<>();
        this.itemLocations     = new HashMap<>();
        this.utilizationScores = new TreeMap<>();
    }

    
    public void registerWarehouse(Warehouse w) {
        warehouses.put(w.getWarehouseId(), w);
        utilizationScores.put(w.getWarehouseId(), w.utilizationRate());
    }

  
    public boolean detectImbalance(double threshold) {
        if (warehouses.size() < 2) return false;
        double avg = warehouses.values().stream()
                .mapToDouble(Warehouse::utilizationRate)
                .average().orElse(0.0);
        return warehouses.values().stream()
                .anyMatch(w -> w.utilizationRate() < avg * threshold);
    }

    public String redirectOrder(List<String> itemIds) {
        
        PriorityQueue<Warehouse> heap = new PriorityQueue<>(
            Comparator.comparingDouble(Warehouse::utilizationRate));
        heap.addAll(warehouses.values());

        while (!heap.isEmpty()) {
            Warehouse w = heap.poll();
            
            boolean carries = itemIds.stream()
                    .anyMatch(id -> itemLocations.getOrDefault(id, Collections.emptyList())
                                                 .contains(w.getWarehouseId()));
            if (carries) return w.getWarehouseId();
        }
        return null;
    }

    public String getSuggestedSource(String itemId) {
        List<String> carriers = itemLocations.getOrDefault(itemId, Collections.emptyList());
        return carriers.stream()
                .filter(warehouses::containsKey)
                .min(Comparator.comparingDouble(
                    wid -> warehouses.get(wid).utilizationRate()))
                .orElse(null);
    }

    
    public void registerItemLocation(String itemId, String warehouseId) {
        itemLocations
            .computeIfAbsent(itemId, k -> new ArrayList<>())
            .add(warehouseId);
    }

    public Map<String, Warehouse>    getWarehouses()    { return Collections.unmodifiableMap(warehouses); }
    public Map<String, List<String>> getItemLocations() { return Collections.unmodifiableMap(itemLocations); }
}


class SyncManager {

    private static class Transfer {
        final String src, dest, itemId;
        final int qty;
        Transfer(String src, String dest, String itemId, int qty) {
            this.src = src; this.dest = dest; this.itemId = itemId; this.qty = qty;
        }
        @Override public String toString() {
            return "Transfer[" + src + "→" + dest + " | " + itemId + " x" + qty + "]";
        }
    }

    
    private final Queue<Transfer>             pendingTransfers;
    
    private final LinkedList<String>          auditLog;
    
    private final Map<String, Integer>        lockMap;
    
    private final TreeMap<Long, String>       timedEvents;

    public SyncManager() {
        this.pendingTransfers = new LinkedList<>();
        this.auditLog         = new LinkedList<>();
        this.lockMap          = new HashMap<>();
        this.timedEvents      = new TreeMap<>();
    }

    public void initiateTransfer(String src, String dest, String itemId, int qty) {
        if (lockMap.containsKey(itemId))
            throw new IllegalStateException("Item " + itemId + " is already locked in a transfer.");
        lockMap.put(itemId, qty);
        Transfer t = new Transfer(src, dest, itemId, qty);
        pendingTransfers.offer(t);
        log("INITIATED: " + t);
    }

 
    public String processNextTransfer(Map<String, Map<String, Integer>> warehouseStock) {
        Transfer t = pendingTransfers.poll();
        if (t == null) return null;

        
        Map<String, Integer> srcStock = warehouseStock.get(t.src);
        if (srcStock != null) {
            srcStock.merge(t.itemId, -t.qty, Integer::sum);
        }
        
        warehouseStock
            .computeIfAbsent(t.dest, k -> new HashMap<>())
            .merge(t.itemId, t.qty, Integer::sum);

        lockMap.remove(t.itemId);
        log("COMPLETED: " + t);
        return t.toString();
    }

    
    public boolean rollbackOnFailure(String itemId) {
        if (!lockMap.containsKey(itemId)) return false;
        lockMap.remove(itemId);
        
        pendingTransfers.removeIf(t -> t.itemId.equals(itemId));
        log("ROLLBACK: " + itemId);
        return true;
    }

    public void syncOnTransfer(String src, String dest, String itemId, int qty) {
        log("SYNCED: Transfer[" + src + "→" + dest + " | " + itemId + " x" + qty + "]");
        lockMap.remove(itemId);
    }

    private void log(String msg) {
        String entry = "[" + System.currentTimeMillis() + "] " + msg;
        auditLog.addLast(entry);
        timedEvents.put(System.currentTimeMillis(), msg);
    }

    public LinkedList<String>       getAuditLog()       { return new LinkedList<>(auditLog); }
    public Map<String, Integer>     getLockMap()        { return Collections.unmodifiableMap(lockMap); }
    public TreeMap<Long, String>    getTimedEvents()    { return new TreeMap<>(timedEvents); }
    public int                      queueSize()         { return pendingTransfers.size(); }
}



class ItemTracking {

    
    private final Map<String, String>         trackingLocation;
    
    private final Map<String, List<String[]>> transferHistory;
    
    private final Set<String>                 inTransit;
    
    private final Queue<String>               completed;
    
    private final TreeMap<String, List<String>> batchRegistry;
    
    private final Map<String, String>           trackingToItem;

    public ItemTracking() {
        this.trackingLocation = new HashMap<>();
        this.transferHistory  = new HashMap<>();
        this.inTransit        = new HashSet<>();
        this.completed        = new LinkedList<>();
        this.batchRegistry    = new TreeMap<>();
        this.trackingToItem   = new HashMap<>();
    }

    public String assignTrackingId(String itemId, String batchId, String srcWarehouseId) {
        String trackingId = "TRK-" + batchId + "-" + itemId + "-" + System.currentTimeMillis();
        trackingLocation.put(trackingId, srcWarehouseId);
        trackingToItem.put(trackingId, itemId);
        inTransit.add(trackingId);
        batchRegistry
            .computeIfAbsent(batchId, k -> new ArrayList<>())
            .add(itemId);
        logMove(itemId, srcWarehouseId);
        return trackingId;
    }

    public void updateLocation(String trackingId, String newWarehouseId) {
        if (!trackingLocation.containsKey(trackingId))
            throw new NoSuchElementException("Tracking ID not found: " + trackingId);
        trackingLocation.put(trackingId, newWarehouseId);
        String itemId = trackingToItem.get(trackingId);
        if (itemId != null) logMove(itemId, newWarehouseId);
    }

  
    public void markDelivered(String trackingId) {
        inTransit.remove(trackingId);
        completed.offer(trackingId);
    }

 
    public List<String> getTransferHistory(String itemId) {
        List<String[]> events = transferHistory.getOrDefault(itemId, Collections.emptyList());
        return events.stream()
                .map(e -> e[0] + " → " + e[1])
                .collect(Collectors.toList());
    }


    public List<String> getByBatchId(String batchId) {
        return Collections.unmodifiableList(
            batchRegistry.getOrDefault(batchId, Collections.emptyList()));
    }

 
    public String getCurrentLocation(String trackingId) {
        return trackingLocation.get(trackingId);
    }

    private void logMove(String itemId, String warehouseId) {
        transferHistory
            .computeIfAbsent(itemId, k -> new ArrayList<>())
            .add(new String[]{String.valueOf(System.currentTimeMillis()), warehouseId});
    }

    public Set<String>  getInTransit()       { return Collections.unmodifiableSet(inTransit); }
    public Queue<String> getCompleted()      { return new LinkedList<>(completed); }
    public TreeMap<String, List<String>> getBatchRegistry() { return new TreeMap<>(batchRegistry); }
    public int          trackedCount()       { return trackingLocation.size(); }
}
