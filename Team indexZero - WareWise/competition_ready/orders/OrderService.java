package orders;
import java.util.*;
import warehouse.WarehouseLayout;

public class OrderService {
  
    
	private static final int DEMAND_THRESHOLD = 3;
	private final Map<String, Integer> orderCount = new HashMap<>();

   
    private final Map<String, Order>          orderStore;       
    private final Map<String, List<Order>>    customerHistory;  
    private final OrderPriorityQueue          queue;

    
    private final Map<String, List<String>>   splitRegistry;    

    
    private final Set<String>                 mergedIds;        

    
    public OrderService() {
        this.orderStore      = new HashMap<>();
        this.customerHistory = new HashMap<>();
        this.queue           = new OrderPriorityQueue();
        this.splitRegistry   = new HashMap<>();
        this.mergedIds       = new HashSet<>();
    }


    public Order createOrder(String orderId, String customerId, List<OrderItem> items,
                             int priority, String warehouseId,
                             Map<String, Integer> stock) {

        if (orderStore.containsKey(orderId))
            throw new IllegalArgumentException("Order already exists: " + orderId);

        if (!checkAvailability(items, stock)) {
            System.out.println("[OrderService] Stock insufficient for order: " + orderId);
            return null;
        }

        Order order = new Order(orderId, customerId, items, priority, warehouseId);
        orderStore.put(orderId, order);

        customerHistory
            .computeIfAbsent(customerId, k -> new ArrayList<>())
            .add(order);

        queue.enqueue(order);

        
        for (OrderItem oi : items)
            orderCount.merge(oi.getItemId(), 1, Integer::sum);

        return order;
    }
   
    public List<Order> getAllOrders() {
        return new ArrayList<>(orderStore.values());
    }

    
    public boolean orderExists(String orderId) {
        return orderStore.containsKey(orderId);
    }

    public boolean checkAvailability(List<OrderItem> items, Map<String, Integer> stock) {
        
        Map<String, Integer> requested = new HashMap<>();
        for (OrderItem oi : items)
            requested.merge(oi.getItemId(), oi.getQuantity(), Integer::sum);

        for (Map.Entry<String, Integer> e : requested.entrySet()) {
            int available = stock.getOrDefault(e.getKey(), 0);
            if (available < e.getValue()) return false;
        }
        return true;
    }


    public boolean cancelOrder(String orderId) {
        Order order = getOrThrow(orderId);

        if (order.getStatus() != Order.Status.PENDING) {
            System.out.println("[OrderService] Cannot cancel order in state: " + order.getStatus());
            return false;
        }

        order.setStatus(Order.Status.CANCELLED);
        
        queue.reprioritize(orderId, order.getPriority()); 
            
        
        return true;
    }

  
    public String trackOrder(String orderId) {
        Order order = getOrThrow(orderId);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "Order   : %s%nCustomer: %s%nStatus  : %s%nPriority: %d%nWeight  : %.2f kg%nTracking: %s%n",
            order.getOrderId(), order.getCustomerId(), order.getStatus(),
            order.getPriority(), order.getTotalWeight(), order.getTrackingId()
        ));
        sb.append("%n── Items ──────────────────────────────%n".formatted());
        for (OrderItem oi : order.getItems()) {
            sb.append(String.format("  • %-12s  %-20s  qty: %d  wt: %.1f kg  price: ₹%.0f%n",
                oi.getItemId(), oi.getName(), oi.getQuantity(),
                oi.getWeightPerUnit(), oi.getPricePerUnit()));
        }
        sb.append(String.format("── Total: %d item(s), %.2f kg ──", order.getItems().size(), order.getTotalWeight()));
        return sb.toString();
    }

 
    public List<Order> getOrderHistory(String customerId) {
        return Collections.unmodifiableList(
            customerHistory.getOrDefault(customerId, Collections.emptyList())
        );
    }

 
    public Order processNext() {
        while (!queue.isEmpty()) {
            Order next = queue.dequeue();
            if (next == null) break;
            if (next.getStatus() == Order.Status.CANCELLED) continue; 
            next.setStatus(Order.Status.PROCESSING);
            return next;
        }
        return null;
    }

 
    public List<Order> splitByWeight(String orderId, double maxKg) {
        Order parent = getOrThrow(orderId);

        if (parent.getTotalWeight() <= maxKg) return Collections.emptyList();

        List<Order>     children  = new ArrayList<>();
        List<OrderItem> currentBin = new ArrayList<>();
        double          binWeight  = 0;
        int             childIdx   = 1;

        for (OrderItem item : parent.getItems()) {
            double itemW = item.totalWeight();

            
            if (itemW > maxKg && currentBin.isEmpty()) {
                Order child = buildChildOrder(parent, List.of(item), childIdx++);
                registerChild(parent.getOrderId(), child, children);
                continue;
            }

            if (binWeight + itemW > maxKg) {
                
                Order child = buildChildOrder(parent, new ArrayList<>(currentBin), childIdx++);
                registerChild(parent.getOrderId(), child, children);
                currentBin.clear();
                binWeight = 0;
            }

            currentBin.add(item);
            binWeight += itemW;
        }

        
        if (!currentBin.isEmpty()) {
            Order child = buildChildOrder(parent, new ArrayList<>(currentBin), childIdx++);
            registerChild(parent.getOrderId(), child, children);
        }

        
        parent.setStatus(Order.Status.SPLIT);
        return children;
    }

  

    public List<Order> splitByZone(String orderId, Map<String, String> itemToZone) {
        Order parent = getOrThrow(orderId);

        
        Map<String, List<OrderItem>> byZone = new LinkedHashMap<>();
        for (OrderItem item : parent.getItems()) {
            String zone = itemToZone.getOrDefault(item.getItemId(), "UNKNOWN");
            byZone.computeIfAbsent(zone, k -> new ArrayList<>()).add(item);
        }

        if (byZone.size() <= 1) return Collections.emptyList(); 

        List<Order> children = new ArrayList<>();
        int childIdx = 1;
        for (List<OrderItem> zoneItems : byZone.values()) {
            Order child = buildChildOrder(parent, zoneItems, childIdx++);
            registerChild(parent.getOrderId(), child, children);
        }

        parent.setStatus(Order.Status.SPLIT);
        return children;
    }

    


    public List<String> getChildOrders(String parentOrderId) {
        return Collections.unmodifiableList(
            splitRegistry.getOrDefault(parentOrderId, Collections.emptyList())
        );
    }

    

    private Order buildChildOrder(Order parent, List<OrderItem> items, int childIdx) {
        String childId = parent.getOrderId() + "-S" + childIdx;
        Order child = new Order(childId, parent.getCustomerId(), items,
                                parent.getPriority(), parent.getWarehouseId());
        child.markAsSplit(parent.getOrderId());
        return child;
    }

    private void registerChild(String parentId, Order child, List<Order> resultList) {
        orderStore.put(child.getOrderId(), child);
        customerHistory
            .computeIfAbsent(child.getCustomerId(), k -> new ArrayList<>())
            .add(child);
        queue.enqueue(child);
        splitRegistry
            .computeIfAbsent(parentId, k -> new ArrayList<>())
            .add(child.getOrderId());
        resultList.add(child);
    }

    public Map<String, List<String>> indexOrders(List<String> orderIds) {
        Map<String, List<String>> itemIndex = new HashMap<>();
        for (String oid : orderIds) {
            if (!orderStore.containsKey(oid)) continue;
            Order order = orderStore.get(oid);
            for (OrderItem item : order.getItems())
                itemIndex
                    .computeIfAbsent(item.getItemId(), k -> new ArrayList<>())
                    .add(oid);
        }
        return itemIndex;
    }

    public List<List<String>> findCommonItems(List<String> orderIds) {
        Map<String, List<String>> itemIndex = indexOrders(orderIds);

        
        Map<String, String> parent = new HashMap<>();
        for (String oid : orderIds) parent.put(oid, oid);

        for (List<String> sharingOrders : itemIndex.values()) {
            
            String root = find(parent, sharingOrders.get(0));
            for (int i = 1; i < sharingOrders.size(); i++)
                union(parent, root, sharingOrders.get(i));
        }

        
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String oid : orderIds) {
            String root = find(parent, oid);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(oid);
        }

        
        List<List<String>> result = new ArrayList<>();
        for (List<String> group : groups.values())
            if (group.size() > 1) result.add(group);
        return result;
    }

    public boolean validateMerge(List<String> orderIds) {
        if (orderIds == null || orderIds.size() < 2) return false;
        String warehouseId = null;
        for (String oid : orderIds) {
            if (!orderStore.containsKey(oid)) return false;
            Order o = orderStore.get(oid);
            if (o.getStatus() != Order.Status.PENDING) return false;
            if (warehouseId == null) warehouseId = o.getWarehouseId();
            else if (!warehouseId.equals(o.getWarehouseId())) return false;
        }
        return true;
    }

    

    public Order mergeBatch(List<String> orderIds, String mergedOrderId) {
        if (!validateMerge(orderIds)) {
            System.out.println("[OrderService] Merge validation failed for: " + orderIds);
            return null;
        }

        
        Map<String, OrderItem> merged = new LinkedHashMap<>();
        String warehouseId = null;
        String customerId  = null;
        int    maxPriority = 1;

        for (String oid : orderIds) {
            Order o = orderStore.get(oid);
            if (warehouseId == null) { warehouseId = o.getWarehouseId(); customerId = o.getCustomerId(); }
            maxPriority = Math.max(maxPriority, o.getPriority());

            for (OrderItem item : o.getItems()) {
                merged.merge(item.getItemId(), item, (existing, incoming) -> {
                    existing.setQuantity(existing.getQuantity() + incoming.getQuantity());
                    return existing;
                });
            }

            
            o.setStatus(Order.Status.CANCELLED);
            mergedIds.add(oid);
        }

        Order mergedOrder = new Order(mergedOrderId, customerId,
                                      new ArrayList<>(merged.values()),
                                      maxPriority, warehouseId);
        orderStore.put(mergedOrderId, mergedOrder);
        customerHistory
            .computeIfAbsent(customerId, k -> new ArrayList<>())
            .add(mergedOrder);
        queue.enqueue(mergedOrder);
        return mergedOrder;
    }

    

    private String find(Map<String, String> parent, String x) {
        if (!parent.get(x).equals(x))
            parent.put(x, find(parent, parent.get(x))); 
        return parent.get(x);
    }

    private void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a);
        String rb = find(parent, b);
        if (!ra.equals(rb)) parent.put(rb, ra);
    }

    public boolean completeOrder(String orderId) {
        Order order = getOrThrow(orderId);
        if (order.getStatus() != Order.Status.PROCESSING) {
            System.out.println("[OrderService] Cannot complete order in state: " + order.getStatus());
            return false;
        }
        order.setStatus(Order.Status.COMPLETED);
        return true;
    }

    

    public void deductStock(Order order, inventory.InventoryManager inv) {
        if (order == null || inv == null) return;
        for (OrderItem oi : order.getItems()) {
            model.Item item = inv.searchById(oi.getItemId());
            if (item != null) {
                int newQty = Math.max(0, item.getQuantity() - oi.getQuantity());
                inv.updateItem(oi.getItemId(), "quantity", String.valueOf(newQty));
            }
        }
    }

    
    public void restoreStock(Order order, inventory.InventoryManager inv) {
        if (order == null || inv == null) return;
        for (OrderItem oi : order.getItems()) {
            model.Item item = inv.searchById(oi.getItemId());
            if (item != null) {
                int newQty = item.getQuantity() + oi.getQuantity();
                inv.updateItem(oi.getItemId(), "quantity", String.valueOf(newQty));
            }
        }
    }

    public Order getOrder(String orderId) {
        return orderStore.get(orderId);
    }

    public int queueSize() {
        return queue.size();
    }

    private Order getOrThrow(String orderId) {
        Order o = orderStore.get(orderId);
        if (o == null) throw new IllegalArgumentException("Order not found: " + orderId);
        return o;
    }

   
    public void recordDemand(Order order, WarehouseLayout layout) {
        if (order == null || layout == null) return;
        for (OrderItem oi : order.getItems()) {
            String itemId = oi.getItemId();
            int count = orderCount.getOrDefault(itemId, 0);
            if (count >= DEMAND_THRESHOLD) {
                String result = layout.relocateItem(itemId, "HOT");
                if (result != null)
                    System.out.println("[OrderService] Item " + itemId
                        + " reached " + count + " orders — auto-moved to HOT zone at " + result);
            }
        }
    }

   

    public void reverseDemand(Order order, WarehouseLayout layout) {
        if (order == null || layout == null) return;
        for (OrderItem oi : order.getItems()) {
            String itemId = oi.getItemId();
            int count = orderCount.getOrDefault(itemId, 0);
            if (count > 0) orderCount.put(itemId, count - 1);
            if (orderCount.getOrDefault(itemId, 0) <= DEMAND_THRESHOLD) {
                String result = layout.relocateItem(itemId, "GENERAL");
                if (result != null)
                    System.out.println("[OrderService] Demand dropped for " + itemId
                    		+ " (orders=" + orderCount.get(itemId) + ") — moved back to GENERAL at " + result);
            }
        }
    }

    
    public int getDemand(String itemId) {
    	return orderCount.getOrDefault(itemId, 0);
    }

    
    public Map<String, Integer> getDemandMap() {
        return Collections.unmodifiableMap(orderCount);
    
    }
}
