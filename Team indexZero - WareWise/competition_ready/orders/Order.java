package orders;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {

    
    public enum Status {
        PENDING, PROCESSING, COMPLETED, CANCELLED, SPLIT
    }

    
    private final String       orderId;
    private final String       customerId;
    private final String       warehouseId;
    private final long         timestamp;          
    private final String       trackingId;         

    private List<OrderItem>    items;
    private int                priority;           
    private Status             status;
    private double             totalWeight;        

    
    private boolean            isSplit;
    private String             parentOrderId;      

    
    public Order(String orderId, String customerId,
                 List<OrderItem> items, int priority, String warehouseId) {

        if (orderId == null || orderId.isBlank())
            throw new IllegalArgumentException("orderId cannot be null/blank");
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId cannot be null/blank");
        if (priority < 1 || priority > 5)
            throw new IllegalArgumentException("priority must be between 1 and 5");

        this.orderId     = orderId;
        this.customerId  = customerId;
        this.warehouseId = warehouseId;
        this.priority    = priority;
        this.status      = Status.PENDING;
        this.timestamp   = System.currentTimeMillis();
        this.trackingId  = UUID.randomUUID().toString();
        this.isSplit     = false;
        this.parentOrderId = null;

        
        this.items = new ArrayList<>(items != null ? items : Collections.emptyList());
        this.totalWeight = calculateTotalWeight();
    }
   
    public void addItem(OrderItem item) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        items.add(item);
        totalWeight += item.totalWeight();
    }

    
    private double calculateTotalWeight() {
        return items.stream().mapToDouble(OrderItem::totalWeight).sum();
    }

    
    public void markAsSplit(String parentOrderId) {
        this.isSplit       = true;
        this.parentOrderId = parentOrderId;
        this.status        = Status.SPLIT;
    }

    
    public String          getOrderId()       { return orderId; }
    public String          getCustomerId()    { return customerId; }
    public String          getWarehouseId()   { return warehouseId; }
    public long            getTimestamp()     { return timestamp; }
    public String          getTrackingId()    { return trackingId; }
    public int             getPriority()      { return priority; }
    public Status          getStatus()        { return status; }
    public double          getTotalWeight()   { return totalWeight; }
    public boolean         isSplit()          { return isSplit; }
    public String          getParentOrderId() { return parentOrderId; }

    
    public List<OrderItem> getItems()         { return Collections.unmodifiableList(items); }

    
    public void setPriority(int priority) {
        if (priority < 1 || priority > 5)
            throw new IllegalArgumentException("priority must be between 1 and 5");
        this.priority = priority;
    }

    public void setStatus(Status status) {
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format(
            "Order{id=%s, customer=%s, warehouse=%s, priority=%d, status=%s, " +
            "items=%d, weight=%.2fkg, split=%b, tracking=%s}",
            orderId, customerId, warehouseId, priority, status,
            items.size(), totalWeight, isSplit, trackingId
        );
    }
}
