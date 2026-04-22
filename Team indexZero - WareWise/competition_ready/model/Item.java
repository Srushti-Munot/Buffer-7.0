package model;

import java.time.LocalDateTime;

public class Item {

    private String itemId, name, category;
    private double weight, demandScore, price;
    private boolean isPerishable, isFragile;
    private int quantity, restockThreshold, fragility;
    private String location, status, warehouseId;
    private LocalDateTime lastUpdated;

    public Item(String itemId, String name, String category,
                double weight, boolean isPerishable, boolean isFragile,
                int quantity, int restockThreshold) {
        this.itemId          = itemId;
        this.name            = name;
        this.category        = category;
        this.weight          = weight;
        this.isPerishable    = isPerishable;
        this.isFragile       = isFragile;
        this.quantity        = quantity;
        this.restockThreshold = restockThreshold;
        this.demandScore     = 0.0;
        this.price           = 0.0;
        this.fragility       = isFragile ? 7 : 2;   
        this.location        = "UNASSIGNED";
        this.status          = "AVAILABLE";
        this.warehouseId     = "";
        this.lastUpdated     = LocalDateTime.now();
    }

    

    
    public void adjustQuantity(int delta) {
        this.quantity = Math.max(0, this.quantity + delta);
        refreshStatus();
        this.lastUpdated = LocalDateTime.now();
    }

    
    public void refreshStatus() {
        if (quantity == 0)                  this.status = "OUT_OF_STOCK";
        else if (quantity < restockThreshold) this.status = "LOW_STOCK";
        else                                  this.status = "AVAILABLE";
    }

    
    public boolean needsRestock() {
        return quantity <= restockThreshold;
    }

    
    public boolean isOutOfStock() {
        return quantity == 0;
    }

    
    public String getItemId()                          { return itemId; }
    public void   setItemId(String itemId)             { this.itemId = itemId; }

    public String getName()                            { return name; }
    public void   setName(String name)                 { this.name = name; this.lastUpdated = LocalDateTime.now(); }

    public String getCategory()                        { return category; }
    public void   setCategory(String category)         { this.category = category; }

    public double getWeight()                          { return weight; }
    public void   setWeight(double weight)             { this.weight = weight; }

    public double getDemandScore()                     { return demandScore; }
    public void   setDemandScore(double demandScore)   { this.demandScore = demandScore; }

    public double getPrice()                           { return price; }
    public void   setPrice(double price)               { this.price = price; }

    public boolean isPerishable()                      { return isPerishable; }
    public void    setPerishable(boolean isPerishable) { this.isPerishable = isPerishable; }

    public boolean isFragile()                         { return isFragile; }
    public void    setFragile(boolean isFragile)       { this.isFragile = isFragile; this.fragility = isFragile ? 7 : 2; }

    public int  getFragility()                         { return fragility; }
    public void setFragility(int fragility)            { this.fragility = fragility; }

    public int  getQuantity()                          { return quantity; }
    public void setQuantity(int quantity)              { this.quantity = quantity; refreshStatus(); this.lastUpdated = LocalDateTime.now(); }

    public int  getRestockThreshold()                  { return restockThreshold; }
    public void setRestockThreshold(int restockThreshold) { this.restockThreshold = restockThreshold; }

    public String getLocation()                        { return location; }
    public void   setLocation(String location)         { this.location = location; }

    public String getStatus()                          { return status; }
    public void   setStatus(String status)             { this.status = status; }

    public String getWarehouseId()                     { return warehouseId; }
    public void   setWarehouseId(String warehouseId)   { this.warehouseId = warehouseId; }

    public LocalDateTime getLastUpdated()              { return lastUpdated; }

    @Override
    public String toString() {
        return "Item[" + itemId + " | " + name + " | Qty: " + quantity + " | " + status + " | " + location + "]";
    }
}
