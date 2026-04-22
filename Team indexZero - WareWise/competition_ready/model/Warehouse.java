package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class Warehouse {

    public static final String MODE_DAY      = "DAY";

    private String warehouseId, name, location, operatingMode;
    private ArrayList<Zone>  zones;
    private int rows, cols, staffCount;
    private String[][]       grid;

    
    private final HashMap<String, String[][]> layoutProfiles = new HashMap<>();

    public Warehouse(String warehouseId, String name, String location, int rows, int cols) {
        this.warehouseId   = warehouseId;
        this.name          = name;
        this.location      = location;
        this.rows          = rows;
        this.cols          = cols;
        this.grid          = new String[rows][cols];
        this.operatingMode = MODE_DAY;
        this.zones         = new ArrayList<>();
        this.staffCount    = 0;
    }
   

    public void addZone(Zone zone) {
        zones.add(zone);
        
        if (zone.getRow() >= 0 && zone.getRow() < rows &&
            zone.getCol() >= 0 && zone.getCol() < cols) {
            grid[zone.getRow()][zone.getCol()] = zone.getZoneId();
        }
    }

    
    public Optional<Zone> findZone(String zoneId) {
        return zones.stream().filter(z -> z.getZoneId().equals(zoneId)).findFirst();
    }

    
    public Zone getAvailableZoneByType(String type) {
        return zones.stream()
                    .filter(z -> z.getType().equalsIgnoreCase(type) &&
                                 !z.isFull() && !z.isMaintenanceFlag())
                    .findFirst()
                    .orElse(null);
    }
   

    public void addStaff(String staffId) { staffCount++; }
    public void removeStaff()            { if (staffCount > 0) staffCount--; }

      
    public void saveProfile(String name) {
        String[][] copy = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            copy[r] = grid[r].clone();
        layoutProfiles.put(name, copy);
    }

    
    public boolean applyProfile(String name) {
        String[][] saved = layoutProfiles.get(name);
        if (saved == null) return false;
        for (int r = 0; r < rows; r++)
            grid[r] = saved[r].clone();
        return true;
    }
    
    public int getTotalCapacity() {
        return zones.stream().mapToInt(Zone::getCapacity).sum();
    }

    
    public int getCurrentLoad() {
        return zones.stream().mapToInt(Zone::getCurrentCount).sum();
    }

    
    public double utilizationRate() {
        int total = getTotalCapacity();
        return total > 0 ? (double) getCurrentLoad() / total : 0.0;
    }

    

    public void setLayoutCell(int row, int col, String val) {
        if (row >= 0 && row < rows && col >= 0 && col < cols)
            grid[row][col] = val;
    }

    public String getLayoutCell(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols)
            return grid[row][col];
        return null;
    }

    
    public Zone getZoneAt(int r, int c) {
        String cellId = getLayoutCell(r, c);
        if (cellId == null) return null;
        return findZone(cellId).orElse(null);
    }

    public String getWarehouseId()                       { return warehouseId; }
    public void   setWarehouseId(String warehouseId)     { this.warehouseId = warehouseId; }

    public String getName()                              { return name; }
    public void   setName(String name)                   { this.name = name; }

    public String getLocation()                          { return location; }
    public void   setLocation(String location)           { this.location = location; }

    public String getOperatingMode()                     { return operatingMode; }
    public void   setOperatingMode(String operatingMode) { this.operatingMode = operatingMode; }

    public ArrayList<Zone> getZones()                    { return zones; }
    public void            setZones(ArrayList<Zone> zones) { this.zones = zones; }

    public int  getRows()                                { return rows; }
    public void setRows(int rows)                        { this.rows = rows; }

    public int  getCols()                                { return cols; }
    public void setCols(int cols)                        { this.cols = cols; }

    public int  getStaffCount()                          { return staffCount; }
    public void setStaffCount(int staffCount)            { this.staffCount = staffCount; }

    public String[][] getGrid()                          { return grid; }
    public void       setGrid(String[][] grid)           { this.grid = grid; }

    @Override
    public String toString() {
        return "Warehouse[" + warehouseId + " | " + name + " | " + location +
               " | Mode: " + operatingMode + " | Load: " + getCurrentLoad() +
               "/" + getTotalCapacity() + "]";
    }
}