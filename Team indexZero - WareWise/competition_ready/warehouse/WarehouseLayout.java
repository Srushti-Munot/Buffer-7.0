package warehouse;

import model.Zone;
import java.util.*;


public class WarehouseLayout {
  
    private String[][] grid;

    
    private Cell[][] cellGrid;

    
    private HashMap<String, Zone> zoneMap;

    
    private HashMap<String, Cell> locationIndex;

    
    private HashMap<String, String> itemLocationMap;

    
    private TreeMap<String, String[][]> profiles;

    private int rows;
    private int cols;

    
    private String activeProfile;

  
    public WarehouseLayout(int rows, int cols) {
        this.rows           = rows;
        this.cols           = cols;
        this.grid           = new String[rows][cols];
        this.cellGrid       = new Cell[rows][cols];
        this.zoneMap        = new HashMap<>();
        this.locationIndex  = new HashMap<>();
        this.itemLocationMap = new HashMap<>();
        this.profiles       = new TreeMap<>();
        this.activeProfile  = null;

        
        for (int r = 0; r < rows; r++) {
            Arrays.fill(grid[r], "EMPTY");
            for (int c = 0; c < cols; c++) {
                String locId = toLocationId(r, c);          
                String zType = resolveZoneType(r, c, rows, cols);
                Cell cell    = new Cell(r, c, Cell.ZONE);
                cell.setLabel(locId);
                cell.assignZone(locId);                      
                cellGrid[r][c]  = cell;
                locationIndex.put(locId, cell);
                grid[r][c]      = locId;
            }
        }
    }

    
    public static String resolveZoneType(int r, int c, int rows, int cols) {
        
        if ((r == 0 && c == 0) || (r == rows - 1 && c == cols - 1))
            return Zone.DISPATCH;
        
        if (r <= 1)
            return Zone.HOT;
        
        if (r >= rows - 2)
            return Zone.PERISHABLE;
        
        int midRowStart = rows / 2 - 1;
        int midRowEnd   = rows / 2 + 1;
        int midColStart = cols / 2 - 2;
        int midColEnd   = cols / 2 + 2;
        if (r >= midRowStart && r <= midRowEnd && c >= midColStart && c <= midColEnd)
            return Zone.FRAGILE;
        
        return Zone.GENERAL;
    }

    


    public String getZoneTypeAt(int r, int c) {
        return resolveZoneType(r, c, rows, cols);
    }

    public String placeItem(String itemId, String zoneType) {
        if (itemLocationMap.containsKey(itemId))
            return itemLocationMap.get(itemId);     

        int maxLevels = Zone.resolveMaxLevels(zoneType);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!resolveZoneType(r, c, rows, cols).equals(zoneType)) continue;
                Cell cell = cellGrid[r][c];
                
                if (getCellLoad(r, c) < maxLevels) {
                    String locId = toLocationId(r, c);
                    itemLocationMap.put(itemId, locId);
                    incrementCellLoad(r, c);
                    return locId;
                }
            }
        }
        return null;  
    }

    



    public String relocateItem(String itemId, String newZoneType) {
        String oldLoc = itemLocationMap.remove(itemId);
        if (oldLoc != null) {
            Cell old = locationIndex.get(oldLoc);
            if (old != null) decrementCellLoad(old.getRow(), old.getCol());
        }
        return placeItem(itemId, newZoneType);
    }

    
    public void removeItem(String itemId) {
        String loc = itemLocationMap.remove(itemId);
        if (loc == null) return;
        Cell cell = locationIndex.get(loc);
        if (cell != null) decrementCellLoad(cell.getRow(), cell.getCol());
    }

    
    public String getItemLocation(String itemId) {
        return itemLocationMap.getOrDefault(itemId, "UNASSIGNED");
    }

    
    public List<String> getItemsAt(String locationId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> e : itemLocationMap.entrySet())
            if (e.getValue().equals(locationId)) result.add(e.getKey());
        return result;
    }

    
    
    private int[][] stackLoad = null;

    private int[][] getStackLoad() {
        if (stackLoad == null) stackLoad = new int[rows][cols];
        return stackLoad;
    }

    private int getCellLoad(int r, int c)       { return getStackLoad()[r][c]; }
    private void incrementCellLoad(int r, int c) { getStackLoad()[r][c]++; }
    private void decrementCellLoad(int r, int c) { if (getStackLoad()[r][c] > 0) getStackLoad()[r][c]--; }

    
    public int getStackHeightAt(int r, int c) { return getCellLoad(r, c); }

   
    public static String toLocationId(int row, int col) {
        return String.valueOf((char)('A' + row)) + (col + 1);
    }

    
    public Cell getCellByLocation(String locationId) {
        return locationIndex.get(locationId);
    }

    
    public Cell getCellAt(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return null;
        return cellGrid[r][c];
    }

    public void saveProfile(String name) {
        profiles.put(name, deepCopyGrid(grid));
    }

    public void applyProfile(String name) {
        String[][] snapshot = profiles.get(name);
        if (snapshot != null) {
            this.grid = deepCopyGrid(snapshot);
            this.activeProfile = name;
        }
    }

    public void updateForTimeOfDay(int hour) {
        if (hour >= 6 && hour < 18) {
            applyProfile("DAY");
        } else if (hour >= 18 && hour <= 21) {
            applyProfile("PEAK");
        } else {
            applyProfile("NIGHT");
        }
    }
  public void updateForLoad(int count, int threshold) {
        if (count >= threshold) {
            applyProfile("PEAK");
        } else {
            applyProfile("OFF_PEAK");
        }
    }

   
    public Zone getZoneAt(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return null;
        String cellValue = grid[r][c];
        return zoneMap.get(cellValue); 
    }

    

    public void setCell(int r, int c, String val) {
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            grid[r][c] = val;
        }
    }

    
    public void addZone(Zone zone) {
        zoneMap.put(zone.getZoneId(), zone);
    }

    

    public String[][] getGrid() {
        return grid;
    }

    public HashMap<String, Zone> getZoneMap() {
        return zoneMap;
    }

    public TreeMap<String, String[][]> getProfiles() {
        return profiles;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    

    private String[][] deepCopyGrid(String[][] source) {
        String[][] copy = new String[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }
}
