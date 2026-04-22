package warehouse;

public class Cell {

    public static final String ZONE  = "ZONE";
    public static final String AISLE = "AISLE";
    public static final String WALL  = "WALL";
    public static final String EMPTY = "EMPTY";
    public static final String DOCK  = "DOCK";

    private int    row, col;
    private String cellType;
    private String zoneId;
    private boolean occupied;
    private String  label;

    public Cell(int row, int col, String cellType) {
        this.row = row; this.col = col; this.cellType = cellType;
        this.zoneId = null; this.occupied = false;
        this.label = row + "," + col;
    }

    
    public boolean isWalkable() {
        return AISLE.equals(cellType) || ZONE.equals(cellType) || DOCK.equals(cellType);
    }

    
    public boolean isZoneCell() { return ZONE.equals(cellType) && zoneId != null; }

    
    public void assignZone(String zoneId) {
        this.zoneId = zoneId; this.cellType = ZONE; this.label = zoneId;
    }

    
    public void clearZone() {
        this.zoneId = null; this.cellType = EMPTY;
        this.label = row + "," + col; this.occupied = false;
    }

    public void occupy()  { occupied = true; }
    public void vacate()  { occupied = false; }

    public int     getRow()                       { return row; }
    public void    setRow(int r)                  { this.row = r; }
    public int     getCol()                       { return col; }
    public void    setCol(int c)                  { this.col = c; }
    public String  getCellType()                  { return cellType; }
    public void    setCellType(String t)          { this.cellType = t; }
    public String  getZoneId()                    { return zoneId; }
    public void    setZoneId(String z)            { this.zoneId = z; }
    public boolean isOccupied()                   { return occupied; }
    public void    setOccupied(boolean o)         { this.occupied = o; }
    public String  getLabel()                     { return label; }
    public void    setLabel(String l)             { this.label = l; }

    @Override public String toString() {
        return "Cell[" + row + "," + col + "|" + cellType +
               (zoneId != null ? "|zone=" + zoneId : "") +
               (occupied ? "|OCC" : "") + "]";
    }
}
