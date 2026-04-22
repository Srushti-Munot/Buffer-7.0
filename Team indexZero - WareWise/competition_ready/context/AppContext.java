package context;

import inventory.InventoryManager;
import orders.OrderService;
import model.Item;
import warehouse.WarehouseLayout;
import warehouse.PathFinder;
import java.util.ArrayList;
import java.util.List;



public class AppContext {

    private final InventoryManager inventory;
    private final OrderService orderService;
    private final WarehouseLayout warehouseLayout;
    private final PathFinder pathFinder;

    public static final int GRID_ROWS = 8;
    public static final int GRID_COLS = 10;

    
    private final List<Runnable> refreshListeners = new ArrayList<>();

   

    public void addRefreshListener(Runnable listener) {
        if (listener != null) refreshListeners.add(listener);
    }

    
    public void removeRefreshListener(Runnable listener) {
        refreshListeners.remove(listener);
    }


    public void notifyGlobalRefresh() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            for (Runnable r : new ArrayList<>(refreshListeners)) r.run();
        });
    }

    public AppContext() {
        inventory = new InventoryManager("WH-001");
        orderService = new OrderService();
        warehouseLayout = new WarehouseLayout(GRID_ROWS, GRID_COLS);
        pathFinder = new PathFinder();
        pathFinder.buildGraphFromGrid(warehouseLayout.getGrid());
        seedInventory();
    }

   
    public InventoryManager getInventory()         { return inventory;       }
    public OrderService      getOrderService()      { return orderService;    }
    public WarehouseLayout   getWarehouseLayout()   { return warehouseLayout; }
    public PathFinder        getPathFinder()        { return pathFinder;      }

   
    private void seedInventory() {
        Object[][] data = {
                
                {"ITM-S01", "Notebooks", "Stationery", 0.3, false, false, 200, 30, 120.0, "GENERAL"},
                {"ITM-S02", "Pens", "Stationery", 0.1, false, false, 500, 50, 20.0, "GENERAL"},
                {"ITM-S03", "Markers", "Stationery", 0.2, false, false, 300, 40, 45.0, "GENERAL"},
                {"ITM-S04", "Paper Reams", "Stationery", 2.5, false, false, 150, 25, 200.0, "GENERAL"},

                
                {"ITM-E01", "Mouse", "Electronics", 0.2, false, false, 80, 15, 599.0, "HOT"},
                {"ITM-E02", "Keyboard", "Electronics", 0.6, false, false, 60, 10, 1299.0, "HOT"},
                {"ITM-E03", "Monitors", "Electronics", 4.5, false, true, 20, 5, 12999.0, "FRAGILE"},
                {"ITM-E04", "USB Drives", "Electronics", 0.05, false, false, 200, 40, 299.0, "HOT"},

                
                {"ITM-C01", "T-Shirts", "Clothes", 0.3, false, false, 150, 20, 399.0, "GENERAL"},
                {"ITM-C02", "Shirts", "Clothes", 0.4, false, false, 100, 15, 699.0, "GENERAL"},
                {"ITM-C03", "Jeans", "Clothes", 0.8, false, false, 80, 10, 1499.0, "GENERAL"},
                {"ITM-C04", "Jackets", "Clothes", 1.2, false, false, 50, 8, 2499.0, "GENERAL"},

                
                {"ITM-N01", "Rice 25kg", "Non-Perishable", 25.0, false, false, 200, 30, 1200.0, "GENERAL"},
                {"ITM-N02", "Flour 10kg", "Non-Perishable", 10.0, false, false, 180, 25, 650.0, "GENERAL"},
                {"ITM-N03", "Cooking Oil", "Non-Perishable", 2.0, false, false, 120, 20, 280.0, "GENERAL"},
                {"ITM-N04", "Canned Food", "Non-Perishable", 0.5, false, false, 300, 50, 150.0, "GENERAL"},

                
                {"ITM-P01", "Milk 1L", "Perishable", 1.0, true, false, 100, 40, 80.0, "PERISHABLE"},
                {"ITM-P02", "Bread", "Perishable", 0.5, true, false, 80, 30, 60.0, "PERISHABLE"},
                {"ITM-P03", "Fruits", "Perishable", 1.5, true, false, 60, 20, 200.0, "PERISHABLE"},
                {"ITM-P04", "Yogurt", "Perishable", 0.4, true, false, 90, 35, 90.0, "PERISHABLE"},
                {"ITM-P05", "Juice 1L", "Perishable", 1.0, true, false, 70, 25, 120.0, "PERISHABLE"},

                
                {"ITM-F01", "Glass Bottles", "Fragile", 0.5, false, true, 40, 10, 350.0, "FRAGILE"},
                {"ITM-F02", "Ceramics", "Fragile", 0.8, false, true, 25, 5, 800.0, "FRAGILE"},
                {"ITM-F03", "Light Bulbs", "Fragile", 0.2, false, true, 60, 15, 85.0, "FRAGILE"},
                {"ITM-F04", "Lab Equipment", "Fragile", 3.0, false, true, 10, 2, 15000.0, "FRAGILE"},

                
                {"ITM-001", "Laptop", "Electronics", 2.5, false, true, 15, 5, 89900.0, "FRAGILE"},
                {"ITM-002", "Rice 25kg", "Non-Perishable", 25.0, true, false, 200, 30, 1200.0, "GENERAL"},
                {"ITM-003", "Glass Vase", "Fragile", 0.8, false, true, 8, 3, 3500.0, "FRAGILE"},
                {"ITM-004", "Cement Bag", "Non-Perishable", 50.0, false, false, 80, 10, 450.0, "GENERAL"},
                {"ITM-005", "Mobile Phone", "Electronics", 0.4, false, true, 45, 10, 24999.0, "HOT"},
                {"ITM-006", "Wheat Flour", "Non-Perishable", 10.0, true, false, 12, 20, 650.0, "GENERAL"},
                {"ITM-007", "Headphones", "Electronics", 0.3, false, true, 30, 8, 12000.0, "HOT"},
                {"ITM-008", "Milk 1L", "Perishable", 1.0, true, false, 100, 40, 80.0, "PERISHABLE"},
                {"ITM-009", "Ceramic Mug", "Fragile", 0.4, false, true, 25, 5, 450.0, "FRAGILE"},
                {"ITM-010", "Steel Rod", "Non-Perishable", 8.0, false, false, 60, 15, 750.0, "GENERAL"},
        };

        for (Object[] r : data) {
            
            String id = (String) r[0];
            if (inventory.searchById(id) != null) continue;

            Item it = new Item(id, (String) r[1], (String) r[2],
                    (Double) r[3], (Boolean) r[4], (Boolean) r[5],
                    (Integer) r[6], (Integer) r[7]);
            it.setPrice((Double) r[8]);
            it.setDemandScore(Math.random() * 100);

            String zoneType = (String) r[9];
            
            String locId = warehouseLayout.placeItem(id, zoneType);
            it.setLocation(locId != null ? locId : zoneType);

            inventory.addItem(it);
        }
    }
}

