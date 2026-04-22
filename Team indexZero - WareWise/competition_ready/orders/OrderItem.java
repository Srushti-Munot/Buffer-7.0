package orders;

public class OrderItem {

    private String itemId;
    private String name;
    private int quantity;
    private double pricePerUnit;   
    private double weightPerUnit;  

    
    public OrderItem(String itemId, String name, int quantity,
                     double pricePerUnit, double weightPerUnit) {
        if (itemId == null || itemId.isBlank())
            throw new IllegalArgumentException("itemId cannot be null/blank");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be > 0");
        if (pricePerUnit < 0 || weightPerUnit < 0)
            throw new IllegalArgumentException("price and weight must be non-negative");

        this.itemId        = itemId;
        this.name          = name;
        this.quantity      = quantity;
        this.pricePerUnit  = pricePerUnit;
        this.weightPerUnit = weightPerUnit;
    }

    
    
    public double totalWeight() {
        return weightPerUnit * quantity;
    }

    
    public double totalPrice() {
        return pricePerUnit * quantity;
    }

    
    public String getItemId()         { return itemId; }
    public String getName()           { return name; }
    public int    getQuantity()       { return quantity; }
    public double getPricePerUnit()   { return pricePerUnit; }
    public double getWeightPerUnit()  { return weightPerUnit; }

    
    public void setQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        this.quantity = quantity;
    }
    public void setName(String name)              { this.name = name; }
    public void setPricePerUnit(double price)     { this.pricePerUnit = price; }
    public void setWeightPerUnit(double weight)   { this.weightPerUnit = weight; }

    @Override
    public String toString() {
        return String.format("OrderItem{id=%s, name=%s, qty=%d, price=%.2f, weight=%.2fkg}",
                itemId, name, quantity, pricePerUnit, totalWeight());
    }
}
