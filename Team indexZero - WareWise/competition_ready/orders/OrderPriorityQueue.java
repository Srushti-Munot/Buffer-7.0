package orders;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


public class OrderPriorityQueue {

 	
	private static final Comparator<Order> ORDER_COMPARATOR =
	        Comparator.comparingInt(Order::getPriority)
	                  .thenComparingLong(Order::getTimestamp);

    private final PriorityQueue<Order>  heap;
    private final Map<String, Order>    lookup;   

    
    public OrderPriorityQueue() {
        this.heap   = new PriorityQueue<>(ORDER_COMPARATOR);
        this.lookup = new HashMap<>();
    }

    public void enqueue(Order order) {
        if (order == null) throw new IllegalArgumentException("order cannot be null");
        if (lookup.containsKey(order.getOrderId())) return; 
        heap.offer(order);
        lookup.put(order.getOrderId(), order);
    }


    public Order dequeue() {
        Order top = heap.poll();
        if (top != null) lookup.remove(top.getOrderId());
        return top;
    }

  
    public Order peek() {
        return heap.peek();
    }


    public void reprioritize(String orderId, int newPriority) {
        if (!lookup.containsKey(orderId))
            throw new IllegalArgumentException("Order not found: " + orderId);

        Order order = lookup.get(orderId);

        
        heap.remove(order);
        order.setPriority(newPriority);    
        heap.offer(order);
        
    }
   

    public boolean contains(String orderId) {
        return lookup.containsKey(orderId);
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public String toString() {
        
        return String.format("OrderPriorityQueue{size=%d, head=%s}",
                heap.size(), peek());
    }
}
