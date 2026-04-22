package inventory;

import model.Item;

import java.util.*;
import java.util.stream.Collectors;

public class InventoryManager {

    private final String warehouseId;
  
    private final HashMap<String, Item> itemMap      = new HashMap<>();
 
    private final TreeMap<String, Item> sortedByName = new TreeMap<>();

    private final HashMap<String, List<Item>> categoryMap = new HashMap<>();
 
    private final Stack<String[]> undoStack = new Stack<>();

    
    private final HashMap<String, Item> snapshots = new HashMap<>();

    private final List<Runnable> changeObservers = new ArrayList<>();

    public void addChangeObserver(Runnable observer) {
        if (observer != null) changeObservers.add(observer);
    }

    private void notifyObservers() {
        for (Runnable r : changeObservers) r.run();
    }

    public InventoryManager(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void addItem(Item item) {
        if (item == null) throw new IllegalArgumentException("Item must not be null.");
        if (itemMap.containsKey(item.getItemId()))
            throw new IllegalStateException("Item " + item.getItemId() + " already exists.");

        item.setWarehouseId(warehouseId);
        item.refreshStatus();

        itemMap.put(item.getItemId(), item);
        sortedByName.put(item.getName().toLowerCase(), item);
        categoryMap.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);

        undoStack.push(new String[]{"ADD", item.getItemId()});
        notifyObservers(); 
    }

    public void removeItem(String itemId) {
        Item item = requireItem(itemId);

        
        snapshots.put(itemId, cloneItem(item));

        itemMap.remove(itemId);
        sortedByName.values().removeIf(i -> i.getItemId().equals(itemId));
        List<Item> catList = categoryMap.get(item.getCategory());
        if (catList != null) catList.removeIf(i -> i.getItemId().equals(itemId));

        undoStack.push(new String[]{"REMOVE", itemId});
        notifyObservers(); 
    }

    public void updateItem(String itemId, String field, String newValue) {
        Item item = requireItem(itemId);

        
        snapshots.put(itemId + "_pre", cloneItem(item));

        switch (field.toLowerCase()) {
            case "name" -> {
                sortedByName.remove(item.getName().toLowerCase());
                item.setName(newValue);
                sortedByName.put(newValue.toLowerCase(), item);
            }
            case "category" -> {
                List<Item> old = categoryMap.get(item.getCategory());
                if (old != null) old.remove(item);
                item.setCategory(newValue);
                categoryMap.computeIfAbsent(newValue, k -> new ArrayList<>()).add(item);
            }
            case "quantity"          -> item.setQuantity(Integer.parseInt(newValue));
            case "weight"            -> item.setWeight(Double.parseDouble(newValue));
            case "price"             -> item.setPrice(Double.parseDouble(newValue));
            case "location"          -> item.setLocation(newValue);
            case "status"            -> item.setStatus(newValue);
            case "demandscore"       -> item.setDemandScore(Double.parseDouble(newValue));
            case "restockthreshold"  -> item.setRestockThreshold(Integer.parseInt(newValue));
            case "fragility"         -> item.setFragility(Integer.parseInt(newValue));
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        }

        undoStack.push(new String[]{"UPDATE", itemId});
        notifyObservers(); 
    }

    public String undoLastAction() {
        if (undoStack.isEmpty()) return "Nothing to undo.";

        String[] op = undoStack.pop();
        String opType = op[0];
        String itemId = op[1];

        switch (opType) {
            case "ADD" -> {
                Item toRemove = itemMap.remove(itemId);
                if (toRemove != null) {
                    sortedByName.values().removeIf(i -> i.getItemId().equals(itemId));
                    List<Item> cat = categoryMap.get(toRemove.getCategory());
                    if (cat != null) cat.remove(toRemove);
                }
                notifyObservers(); 
                return "Undone ADD of item " + itemId;
            }
            case "REMOVE" -> {
                Item snapshot = snapshots.remove(itemId);
                if (snapshot != null) {
                    itemMap.put(snapshot.getItemId(), snapshot);
                    sortedByName.put(snapshot.getName().toLowerCase(), snapshot);
                    categoryMap.computeIfAbsent(snapshot.getCategory(), k -> new ArrayList<>()).add(snapshot);
                }
                notifyObservers(); 
                return "Undone REMOVE of item " + itemId;
            }
            case "UPDATE" -> {
                String snapshotKey = itemId + "_pre";
                Item pre = snapshots.remove(snapshotKey);
                if (pre != null) {
                    Item current = itemMap.get(itemId);
                    if (current != null) {
                        
                        sortedByName.remove(current.getName().toLowerCase());
                        current.setName(pre.getName());
                        current.setCategory(pre.getCategory());
                        current.setQuantity(pre.getQuantity());
                        current.setWeight(pre.getWeight());
                        current.setPrice(pre.getPrice());
                        current.setLocation(pre.getLocation());
                        current.setStatus(pre.getStatus());
                        current.setDemandScore(pre.getDemandScore());
                        current.setRestockThreshold(pre.getRestockThreshold());
                        current.setFragility(pre.getFragility());
                        sortedByName.put(current.getName().toLowerCase(), current);
                    }
                }
                notifyObservers(); 
                return "Undone UPDATE of item " + itemId;
            }
            default -> { return "Unknown operation in undo stack."; }
        }
    }

  
    public Item searchById(String itemId) {
        return itemMap.get(itemId);
    }

    
    public List<Item> searchByName(String query) {
        String q = query.toLowerCase();
        return itemMap.values().stream()
                .filter(i -> i.getName().toLowerCase().contains(q))
                .sorted(Comparator.comparing(Item::getName))
                .collect(Collectors.toList());
    }

    
    public List<Item> getByCategory(String category) {
        return categoryMap.getOrDefault(category, Collections.emptyList());
    }

    
    public List<Item> getLowStockItems(int threshold) {
        return itemMap.values().stream()
                .filter(i -> i.getQuantity() < threshold)
                .sorted(Comparator.comparingInt(Item::getQuantity))
                .collect(Collectors.toList());
    }

    
    public List<Item> viewAll() {
        return new ArrayList<>(sortedByName.values());
    }

    
    public List<Item> getByDemand() {
        return itemMap.values().stream()
                .sorted(Comparator.comparingDouble(Item::getDemandScore).reversed())
                .collect(Collectors.toList());
    }

    
    public String getSummary() {
        long lowStock  = itemMap.values().stream().filter(Item::needsRestock).count();
        long outOfStock = itemMap.values().stream().filter(Item::isOutOfStock).count();
        return "Warehouse: " + warehouseId +
               " | Total items: " + itemMap.size() +
               " | Low stock: " + lowStock +
               " | Out of stock: " + outOfStock;
    }

    

    private Item requireItem(String itemId) {
        Item item = itemMap.get(itemId);
        if (item == null) throw new NoSuchElementException("Item not found: " + itemId);
        return item;
    }

    
    private Item cloneItem(Item src) {
        Item copy = new Item(
            src.getItemId(), src.getName(), src.getCategory(),
            src.getWeight(), src.isPerishable(), src.isFragile(),
            src.getQuantity(), src.getRestockThreshold()
        );
        copy.setDemandScore(src.getDemandScore());
        copy.setPrice(src.getPrice());
        copy.setLocation(src.getLocation());
        copy.setStatus(src.getStatus());
        copy.setWarehouseId(src.getWarehouseId());
        copy.setFragility(src.getFragility());
        return copy;
    }

    public String getWarehouseId() { return warehouseId; }
    public int    size()           { return itemMap.size(); }



    public Map<String, Integer> getStockSnapshot() {
        Map<String, Integer> snapshot = new HashMap<>();
        for (Map.Entry<String, model.Item> e : itemMap.entrySet())
            snapshot.put(e.getKey(), e.getValue().getQuantity());
        return snapshot;
    }
    


    public java.util.List<String> shiftToHotZone(warehouse.WarehouseLayout layout, int topN) {
        java.util.List<String> moved = new java.util.ArrayList<>();

        
        java.util.List<Item> ranked = new java.util.ArrayList<>(itemMap.values());
        ranked.sort((a, b) -> Double.compare(b.getDemandScore(), a.getDemandScore()));

        int count = 0;
        for (Item item : ranked) {
            if (count >= topN) break;

            
            String currentLoc = item.getLocation();
            if (currentLoc != null && currentLoc.startsWith("R0") || 
                currentLoc != null && currentLoc.startsWith("R1")) {
                
                count++;
                continue;
            }
            
            if (currentLoc != null && currentLoc.equalsIgnoreCase("HOT")) {
                count++;
                continue;
            }

            
            String newLoc = layout.relocateItem(item.getItemId(), model.Zone.HOT);
            if (newLoc != null) {
                item.setLocation(newLoc);
                moved.add(item.getName() + " → " + newLoc);
            }
            count++;
        }
        notifyObservers();
        return moved;
    }
}
