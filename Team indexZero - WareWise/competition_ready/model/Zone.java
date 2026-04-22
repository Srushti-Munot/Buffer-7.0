package model;

import java.util.ArrayList;

public class Zone {
    
    public static final String HOT        = "HOT";
    public static final String COLD       = "COLD";
    public static final String FRAGILE    = "FRAGILE";
    public static final String BULK       = "BULK";
    public static final String DISPATCH   = "DISPATCH";
    
    public static final String GENERAL    = "GENERAL";
    public static final String PERISHABLE = "PERISHABLE";

    private String zoneId, type;
    private double activityScore;
    private int currentCount, row, col, capacity;
    private boolean maintenanceFlag;
    private ArrayList<String> itemIds = new ArrayList<>();

    
    private int maxLevels;    
    private int currentLoad;  

    public Zone(String zoneId, String type, int capacity, int row, int col) {
        this.zoneId          = zoneId;
        this.type            = type;
        this.capacity        = capacity;
        this.row             = row;
        this.col             = col;
        this.currentCount    = 0;
        this.currentLoad     = 0;
        this.activityScore   = 0;
        this.maintenanceFlag = false;
        this.maxLevels       = resolveMaxLevels(type);
    }

 
    public static int resolveMaxLevels(String type) {
        if (type == null) return 4;
        switch (type.toUpperCase()) {
            case "FRAGILE":    return 1;
            case "HOT":        return 2;
            case "PERISHABLE": return 2;
            case "COLD":       return 2;
            case "DISPATCH":   return 1;
            default:           return 4;  
        }
    }

    
    public boolean canStack() {
        return currentLoad < maxLevels;
    }

    
    public boolean addLevel() {
        if (!canStack()) return false;
        currentLoad++;
        return true;
    }

    
    public boolean removeLevel() {
        if (currentLoad <= 0) return false;
        currentLoad--;
        return true;
    }


    public void addItem(String itemId) {
        if (maintenanceFlag)
            throw new IllegalStateException("Zone " + zoneId + " is under maintenance.");
        if (isFull())
            throw new IllegalStateException("Zone " + zoneId + " is at capacity (" + capacity + ").");
        if (itemIds.contains(itemId))
            throw new IllegalArgumentException("Item " + itemId + " already in zone " + zoneId + ".");
        itemIds.add(itemId);
        currentCount++;
        activityScore += 1.0;
    }

    public boolean removeItem(String itemId) {
        boolean removed = itemIds.remove(itemId);
        if (removed) {
            currentCount--;
            activityScore += 0.5;   
        }
        return removed;
    }

    
    public boolean isFull() {
        return currentCount >= capacity;
    }

    
    public double utilizationRate() {
        return capacity > 0 ? (double) currentCount / capacity : 0.0;
    }

    
    public void recordAccess() {
        activityScore += 1.0;
    }

    
    public int freeSlots() {
        return capacity - currentCount;
    }

    

    public String getZoneId()                        { return zoneId; }
    public void   setZoneId(String zoneId)           { this.zoneId = zoneId; }

    public String getType()                          { return type; }
    public void   setType(String type)               { this.type = type; }

    public int  getCapacity()                        { return capacity; }
    public void setCapacity(int capacity)            { this.capacity = capacity; }

    public double getActivityScore()                 { return activityScore; }
    public void   setActivityScore(double score)     { this.activityScore = score; }

    public int  getCurrentCount()                    { return currentCount; }
    public void setCurrentCount(int currentCount)    { this.currentCount = currentCount; }

    public int  getRow()                             { return row; }
    public void setRow(int row)                      { this.row = row; }

    public int  getCol()                             { return col; }
    public void setCol(int col)                      { this.col = col; }

    public boolean isMaintenanceFlag()               { return maintenanceFlag; }
    public void    setMaintenanceFlag(boolean flag)  { this.maintenanceFlag = flag; }

    public ArrayList<String> getItemIds()            { return itemIds; }
    public void setItemIds(ArrayList<String> ids)    { this.itemIds = ids; this.currentCount = ids.size(); }

    public int  getMaxLevels()                        { return maxLevels; }
    public void setMaxLevels(int maxLevels)           { this.maxLevels = maxLevels; }

    public int  getCurrentLoad()                      { return currentLoad; }
    public void setCurrentLoad(int load)              { this.currentLoad = load; }

    @Override
    public String toString() {
        return "Zone[" + zoneId + " | " + type +
                " | Cap: " + capacity + " | Used: " + currentCount +
                (maintenanceFlag ? " | MAINT" : "") + "]";
    }
}
