package gui;

import context.AppContext;
import inventory.InventoryManager;
import model.Item;
import model.Warehouse;
import model.Zone;
import warehouse.PathFinder;
import warehouse.WarehouseLayout;








import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class WarehouseGUI extends JFrame {
 
    private static final Color BG       = new Color(18,  18,  30);
    private static final Color PANEL_BG = new Color(28,  28,  45);
    private static final Color CARD_BG  = new Color(38,  38,  60);
    private static final Color ACCENT   = new Color(99, 179, 237);
    private static final Color ACCENT2  = new Color(104, 211, 145);
    private static final Color WARN     = new Color(251, 191,  36);
    private static final Color DANGER   = new Color(252,  92,  92);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color TEXT_DIM = new Color(148, 163, 184);

    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font BODY_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font MONO_FONT   = new Font("Consolas",  Font.PLAIN, 12);

    
    private final context.AppContext ctx;
    private final Warehouse        warehouse;
    private final InventoryManager inventory;
    private final WarehouseLayout  layout;

    private final PathFinder       pathFinder;
    
    private DefaultTableModel tableModel;
    private JTable            itemTable;
    private JTextField        searchField;
    private JComboBox<String> itemBox3D;
    
    private DefaultTableModel zoneTableModel;

    
    private JLabel statusLabel;
  
    public WarehouseGUI(AppContext ctx) {
        this.ctx    = ctx;
        this.layout = ctx.getWarehouseLayout();
        warehouse   = new Warehouse("WH-001", "Main Warehouse", "Pune, Maharashtra",
                context.AppContext.GRID_ROWS, context.AppContext.GRID_COLS);
        inventory   = ctx.getInventory();

        String[] types   = {Zone.HOT, Zone.PERISHABLE, Zone.FRAGILE, Zone.GENERAL, Zone.DISPATCH};
        String[] zoneIds = {"Z-HOT", "Z-PERI", "Z-FRAG", "Z-GEN", "Z-DISP"};
        int[]    caps    = {60, 50, 30, 120, 20};
        for (int i = 0; i < types.length; i++) {
            warehouse.addZone(new Zone(zoneIds[i], types[i], caps[i], i, 0));
        }

        pathFinder = ctx.getPathFinder();

        buildUI();
        setTitle("Warehouse Management System");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);

        Runnable myRefresh = this::refreshInventoryTable;
        ctx.addRefreshListener(myRefresh);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                ctx.removeRefreshListener(myRefresh);
            }
        });
    }
  
    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildSidebar(),    BorderLayout.WEST);
        add(buildMain(),       BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);
    }
    
    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(200, 0));
        side.setBackground(PANEL_BG);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel logo = new JLabel("  📦 WMS");
        logo.setFont(TITLE_FONT);
        logo.setForeground(ACCENT);
        logo.setBorder(new EmptyBorder(0, 0, 30, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logo);

        JLabel whName = new JLabel("  " + warehouse.getName());
        whName.setFont(BODY_FONT);
        whName.setForeground(TEXT_DIM);
        whName.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(whName);

        JLabel whLoc = new JLabel("  📍 " + warehouse.getLocation());
        whLoc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        whLoc.setForeground(TEXT_DIM);
        whLoc.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(whLoc);
        side.add(Box.createVerticalStrut(20));

        side.add(sideStatCard("Total Items", String.valueOf(inventory.size())));
        side.add(sideStatCard("Zones",        String.valueOf(warehouse.getZones().size())));
        side.add(sideStatCard("Mode",          warehouse.getOperatingMode()));

        return side;
    }

    private JPanel sideStatCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setMaximumSize(new Dimension(200, 60));
        card.setBorder(new CompoundBorder(
                new EmptyBorder(4, 8, 4, 8),
                new CompoundBorder(
                        new LineBorder(ACCENT.darker(), 1, true),
                        new EmptyBorder(6, 10, 6, 10)
                )
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_DIM);

        JLabel val = new JLabel(value);
        val.setFont(HEADER_FONT);
        val.setForeground(ACCENT);

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.SOUTH);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }
    
    private JTabbedPane buildTabBar() { return new JTabbedPane(); }

    private JPanel buildMain() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.setFont(HEADER_FONT);
        tabs.setTabPlacement(JTabbedPane.TOP);
        UIManager.put("TabbedPane.selected", CARD_BG);

        tabs.addTab("Dashboard",   buildDashboard());
        tabs.addTab("Inventory",   buildInventoryPanel());
        tabs.addTab("Zones",       buildZonesPanel());
        tabs.addTab("Warehouse",   buildWarehousePanel());
        tabs.addTab("Path Finder", buildPathFinderPanel());
        tabs.addTab("3D Layout",   build3DLayoutPanel());
        tabs.addTab("Maintenance", buildMaintenancePanel());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(tabs, BorderLayout.CENTER);
        return wrapper;
    }

    
    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setBackground(BG);
        kpiRow.add(kpiCard("Total Items",    String.valueOf(inventory.size()),                    ACCENT));
        kpiRow.add(kpiCard("Low Stock",      String.valueOf(inventory.getLowStockItems(20).size()), WARN));
        kpiRow.add(kpiCard("Zones",          String.valueOf(warehouse.getZones().size()),          ACCENT2));
        kpiRow.add(kpiCard("Operating Mode", warehouse.getOperatingMode(),                        new Color(167,139,250)));

        p.add(kpiRow, BorderLayout.NORTH);
        p.add(buildZoneGrid(), BorderLayout.CENTER);
        p.add(buildLowStockTable(), BorderLayout.SOUTH);
        return p;
    }

    private JPanel kpiCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(accent, 2, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_DIM);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 28));
        val.setForeground(accent);

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildZoneGrid() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG);

        JLabel heading = new JLabel("Zone Utilisation  (2D Overview)");
        heading.setFont(HEADER_FONT);
        heading.setForeground(TEXT);
        outer.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 0, 12, 0));
        grid.setBackground(BG);

        for (Zone z : warehouse.getZones()) {
            int inUse = countItemsInZone(z.getType());
            double pct = z.getCapacity() > 0 ? (double) inUse / z.getCapacity() : 0;
            Color barColor = pct > 0.8 ? DANGER : pct > 0.5 ? WARN : ACCENT2;

            JPanel card = new JPanel(new BorderLayout(0, 4));
            card.setBackground(CARD_BG);
            card.setBorder(new CompoundBorder(
                    new LineBorder(CARD_BG.brighter(), 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            JLabel id  = new JLabel(z.getZoneId());
            id.setFont(HEADER_FONT);
            id.setForeground(TEXT);

            JLabel type = new JLabel(z.getType()
                    + "  ·  max stack: " + model.Zone.resolveMaxLevels(z.getType()));
            type.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            type.setForeground(TEXT_DIM);

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue((int)(pct * 100));
            bar.setForeground(barColor);
            bar.setBackground(BG);
            bar.setStringPainted(true);
            bar.setString(inUse + "/" + z.getCapacity());

            JLabel maint = new JLabel(z.isMaintenanceFlag() ? "⚠ MAINTENANCE" : "✓ OK");
            maint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            maint.setForeground(z.isMaintenanceFlag() ? WARN : ACCENT2);

            card.add(id,    BorderLayout.NORTH);
            card.add(type,  BorderLayout.CENTER);
            card.add(bar,   BorderLayout.SOUTH);
            card.add(maint, BorderLayout.EAST);
            grid.add(card);
        }

        outer.add(grid, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildLowStockTable() {
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);
        outer.setPreferredSize(new Dimension(0, 180));

        JLabel heading = new JLabel("Low-Stock Alerts  (threshold < 20)");
        heading.setFont(HEADER_FONT);
        heading.setForeground(WARN);
        outer.add(heading, BorderLayout.NORTH);

        List<Item> low = inventory.getLowStockItems(20);
        String[] cols = {"ID", "Name", "Category", "Qty", "Threshold", "Status"};
        Object[][] rows = low.stream().map(i -> new Object[]{
                i.getItemId(), i.getName(), i.getCategory(),
                i.getQuantity(), i.getRestockThreshold(), i.getStatus()
        }).toArray(Object[][]::new);

        JTable t = new JTable(new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int r, int c) { return false; }
        });
        styleTable(t);
        outer.add(new JScrollPane(t), BorderLayout.CENTER);
        return outer;
    }

    
    private JPanel buildInventoryPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        p.add(buildInventoryToolbar(), BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Category", "Qty", "Weight", "Price", "Location", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshTableData(inventory.viewAll());

        itemTable = new JTable(tableModel);
        styleTable(itemTable);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        p.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        p.add(buildInventoryButtons(), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildInventoryToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setBackground(BG);

        JLabel lbl = new JLabel("Search:");
        lbl.setForeground(TEXT);
        lbl.setFont(BODY_FONT);

        searchField = styledTextField(22);
        searchField.addActionListener(e -> runSearch());

        JButton searchBtn = accentButton("Search", ACCENT);
        searchBtn.addActionListener(e -> runSearch());

        JButton clearBtn = accentButton("Clear", TEXT_DIM);
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTableData(inventory.viewAll());
        });

        bar.add(lbl);
        bar.add(searchField);
        bar.add(searchBtn);
        bar.add(clearBtn);
        return bar;
    }

    private JPanel buildInventoryButtons() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setBackground(BG);
        bar.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton addBtn    = accentButton("Add Item",  ACCENT2);
        JButton editBtn   = accentButton("Edit Item", ACCENT);
        JButton removeBtn = accentButton("Remove",   DANGER);
        JButton undoBtn   = accentButton("Undo",      WARN);
        JButton statsBtn  = accentButton("Summary",  new Color(167,139,250));

        addBtn.addActionListener(e    -> showAddItemDialog());
        editBtn.addActionListener(e   -> showEditItemDialog());
        removeBtn.addActionListener(e -> removeSelectedItem());
        undoBtn.addActionListener(e   -> {
            String msg = inventory.undoLastAction();
            refreshTableData(inventory.viewAll());
            setStatus(msg);
        });
        statsBtn.addActionListener(e  -> showInfoDialog("Summary", inventory.getSummary()));

        bar.add(addBtn);
        bar.add(editBtn);
        bar.add(removeBtn);
        bar.add(undoBtn);
        bar.add(statsBtn);
        return bar;
    }

    private void runSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) { refreshTableData(inventory.viewAll()); return; }
        List<Item> results = inventory.searchByName(q);
        refreshTableData(results);
        setStatus("Found " + results.size() + " result(s) for \"" + q + "\"");
    }

    private void refreshInventoryTable() {
        if (tableModel == null) return;
        String q = searchField.getText().trim();
        if (!q.isEmpty())
            refreshTableData(inventory.searchByName(q));
        else
            refreshTableData(inventory.viewAll());
        if (itemBox3D != null) {
            itemBox3D.removeAllItems();
            inventory.viewAll().forEach(it -> itemBox3D.addItem(it.getItemId() + " – " + it.getName()));
        }
        getContentPane().repaint();
    }

    private void refreshTableData(List<Item> items) {
        int selectedRow = (itemTable != null) ? itemTable.getSelectedRow() : -1;
        String selectedId = null;
        if (selectedRow >= 0)
            selectedId = (String) tableModel.getValueAt(selectedRow, 0);

        tableModel.setRowCount(0);
        for (Item i : items) {
            tableModel.addRow(new Object[]{
                    i.getItemId(), i.getName(), i.getCategory(),
                    i.getQuantity(), String.format("%.1f kg", i.getWeight()),
                    String.format("₹%.2f", i.getPrice()),
                    i.getLocation(), i.getStatus()
            });
        }

        if (selectedId != null && itemTable != null) {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                if (selectedId.equals(tableModel.getValueAt(r, 0))) {
                    itemTable.setRowSelectionInterval(r, r);
                    break;
                }
            }
        }
    }

    
    private void showAddItemDialog() {
        JDialog d = styledDialog("Add New Item", 440, 480);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        JTextField fId       = styledTextField(20);
        JTextField fName     = styledTextField(20);
        JTextField fCat      = styledTextField(20);
        JTextField fQty      = styledTextField(10);
        JTextField fWeight   = styledTextField(10);
        JTextField fPrice    = styledTextField(10);
        JTextField fThresh   = styledTextField(10);
        JCheckBox  cbPerish  = styledCheckbox("Perishable");
        JCheckBox  cbFragile = styledCheckbox("Fragile");

        Object[][] fields = {
                {"Item ID",    fId},     {"Name",           fName},
                {"Category",   fCat},    {"Quantity",        fQty},
                {"Weight (kg)",fWeight}, {"Price (₹)",       fPrice},
                {"Restock Threshold", fThresh}, {null, cbPerish},
                {null, cbFragile}
        };

        int row = 0;
        for (Object[] f : fields) {
            if (f[0] != null) {
                g.gridx=0; g.gridy=row; g.weightx=0.3;
                JLabel lbl = new JLabel((String)f[0]);
                lbl.setForeground(TEXT); lbl.setFont(BODY_FONT);
                form.add(lbl, g);
            }
            g.gridx=1; g.gridy=row; g.weightx=0.7;
            form.add((Component)f[1], g);
            row++;
        }

        JButton save = accentButton("💾 Save", ACCENT2);
        save.addActionListener(e -> {
            try {
                String category   = fCat.getText().trim();
                boolean isPerish  = cbPerish.isSelected();
                boolean isFragile = cbFragile.isSelected();

                String zoneType;
                if (isFragile) {
                    zoneType = model.Zone.FRAGILE;
                } else if (isPerish) {
                    zoneType = model.Zone.PERISHABLE;
                } else {
                    switch (category.toLowerCase()) {
                        case "electronics": zoneType = model.Zone.HOT;        break;
                        case "perishable":  zoneType = model.Zone.PERISHABLE; break;
                        case "fragile":     zoneType = model.Zone.FRAGILE;    break;
                        default:            zoneType = model.Zone.GENERAL;    break;
                    }
                }

                Item it = new Item(
                        fId.getText().trim(), fName.getText().trim(), category,
                        Double.parseDouble(fWeight.getText().trim()),
                        isPerish, isFragile,
                        Integer.parseInt(fQty.getText().trim()),
                        Integer.parseInt(fThresh.getText().trim())
                );
                it.setPrice(Double.parseDouble(fPrice.getText().trim()));

                String locId = layout.placeItem(it.getItemId(), zoneType);
                it.setLocation(locId != null ? locId : zoneType);

                inventory.addItem(it);
                refreshTableData(inventory.viewAll());
                ctx.notifyGlobalRefresh();
                setStatus("Item " + it.getItemId() + " added → " + it.getLocation());
                d.dispose();
            } catch (Exception ex) {
                showErrorDialog("Invalid input: " + ex.getMessage());
            }
        });

        g.gridx=0; g.gridy=row; g.gridwidth=2;
        form.add(save, g);

        d.add(form);
        d.setVisible(true);
    }

    
    private void showEditItemDialog() {
        int row = itemTable.getSelectedRow();
        if (row < 0) { showInfoDialog("Select Item", "Please select an item first."); return; }
        String itemId = (String) tableModel.getValueAt(row, 0);
        Item item = inventory.searchById(itemId);
        if (item == null) return;

        JDialog d = styledDialog("Edit Item: " + itemId, 380, 340);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill   = GridBagConstraints.HORIZONTAL;

        String[] editFields = {"name", "category", "quantity", "price", "location"};
        String[] labels     = {"Name", "Category", "Quantity", "Price (₹)", "Location"};
        String[] defaults   = {item.getName(), item.getCategory(),
                String.valueOf(item.getQuantity()),
                String.valueOf(item.getPrice()), item.getLocation()};

        JTextField[] tfs = new JTextField[editFields.length];
        for (int i = 0; i < editFields.length; i++) {
            g.gridx=0; g.gridy=i; g.weightx=0.35;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setForeground(TEXT); lbl.setFont(BODY_FONT);
            form.add(lbl, g);
            tfs[i] = styledTextField(18);
            tfs[i].setText(defaults[i]);
            g.gridx=1; g.weightx=0.65;
            form.add(tfs[i], g);
        }

        JButton save = accentButton("💾 Update", ACCENT);
        save.addActionListener(e -> {
            try {
                for (int i = 0; i < editFields.length; i++) {
                    String newVal = tfs[i].getText().trim();
                    if (!newVal.equals(defaults[i]))
                        inventory.updateItem(itemId, editFields[i], newVal);
                }
                refreshTableData(inventory.viewAll());
                ctx.notifyGlobalRefresh();
                setStatus("Item " + itemId + " updated.");
                d.dispose();
            } catch (Exception ex) {
                showErrorDialog("Update error: " + ex.getMessage());
            }
        });

        g.gridx=0; g.gridy=editFields.length; g.gridwidth=2;
        form.add(save, g);

        d.add(form);
        d.setVisible(true);
    }

    private void removeSelectedItem() {
        int row = itemTable.getSelectedRow();
        if (row < 0) { showInfoDialog("Select Item", "Please select an item first."); return; }
        String itemId = (String) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove item " + itemId + "?", "Confirm Remove",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            inventory.removeItem(itemId);
            ctx.getWarehouseLayout().removeItem(itemId);
            refreshTableData(inventory.viewAll());
            ctx.notifyGlobalRefresh();
            setStatus("Item " + itemId + " removed.");
        }
    }

    
    private JPanel buildZonesPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"Zone ID", "Type", "Capacity", "In Use", "Free", "Activity", "Maintenance"};
        zoneTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshZoneTable();

        JTable zt = new JTable(zoneTableModel);
        styleTable(zt);

        p.add(new JScrollPane(zt), BorderLayout.CENTER);
        p.add(buildZoneButtons(zt), BorderLayout.SOUTH);
        return p;
    }

    private void refreshZoneTable() {
    zoneTableModel.setRowCount(0);
    int rows = context.AppContext.GRID_ROWS;
    int cols = context.AppContext.GRID_COLS;

    for (Zone z : warehouse.getZones()) {
        
        int inUse = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (WarehouseLayout.resolveZoneType(r, c, rows, cols).equals(z.getType())) {
                    inUse += layout.getItemsAt(WarehouseLayout.toLocationId(r, c)).size();
                }
            }
        }
        zoneTableModel.addRow(new Object[]{
                z.getZoneId(), z.getType(), z.getCapacity(),
                inUse, z.getCapacity() - inUse,
                String.format("%.1f", (double) inUse),
                z.isMaintenanceFlag() ? "⚠ YES" : "✓ No"
        });
    }
}

    private int countItemsInZone(String zoneType) {
    int rows = context.AppContext.GRID_ROWS;
    int cols = context.AppContext.GRID_COLS;
    int count = 0;
    for (int r = 0; r < rows; r++)
        for (int c = 0; c < cols; c++)
            if (WarehouseLayout.resolveZoneType(r, c, rows, cols).equals(zoneType))
                count += layout.getItemsAt(WarehouseLayout.toLocationId(r, c)).size();
    return count;
}

    private JPanel buildZoneButtons(JTable zt) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(BG);

        JButton addZoneBtn  = accentButton("Add Zone",     ACCENT2);
        JButton toggleMaint = accentButton("Toggle Maint", WARN);
        JButton refreshBtn  = accentButton("Refresh",      ACCENT);

        addZoneBtn.addActionListener(e  -> showAddZoneDialog());
        toggleMaint.addActionListener(e -> {
            int row = zt.getSelectedRow();
            if (row < 0) { showInfoDialog("Select", "Select a zone first."); return; }
            String zid = (String) zoneTableModel.getValueAt(row, 0);
            warehouse.findZone(zid).ifPresent(z -> {
                z.setMaintenanceFlag(!z.isMaintenanceFlag());
                refreshZoneTable();
                setStatus("Zone " + zid + " maintenance: " + z.isMaintenanceFlag());
            });
        });
        refreshBtn.addActionListener(e -> refreshZoneTable());

        bar.add(addZoneBtn);
        bar.add(toggleMaint);
        bar.add(refreshBtn);
        return bar;
    }

    private void showAddZoneDialog() {
        JDialog d = styledDialog("Add Zone", 360, 280);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill   = GridBagConstraints.HORIZONTAL;

        String[] zTypes = {Zone.HOT, Zone.COLD, Zone.FRAGILE, Zone.BULK, Zone.DISPATCH};
        JTextField fId  = styledTextField(15);
        JComboBox<String> fType = new JComboBox<>(zTypes);
        fType.setBackground(CARD_BG); fType.setForeground(TEXT);
        JTextField fCap = styledTextField(10);
        JTextField fRow = styledTextField(10);
        JTextField fCol = styledTextField(10);

        addFormRow(form, g, 0, "Zone ID",  fId);
        addFormRow(form, g, 1, "Type",     fType);
        addFormRow(form, g, 2, "Capacity", fCap);
        addFormRow(form, g, 3, "Grid Row", fRow);
        addFormRow(form, g, 4, "Grid Col", fCol);

        JButton save = accentButton("💾 Add", ACCENT2);
        save.addActionListener(e -> {
            try {
                Zone zone = new Zone(
                        fId.getText().trim(), (String) fType.getSelectedItem(),
                        Integer.parseInt(fCap.getText().trim()),
                        Integer.parseInt(fRow.getText().trim()),
                        Integer.parseInt(fCol.getText().trim())
                );
                warehouse.addZone(zone);
                refreshZoneTable();
                setStatus("Zone " + zone.getZoneId() + " added.");
                d.dispose();
            } catch (Exception ex) {
                showErrorDialog("Error: " + ex.getMessage());
            }
        });

        g.gridx=0; g.gridy=5; g.gridwidth=2;
        form.add(save, g);
        d.add(form);
        d.setVisible(true);
    }

    
    private JPanel buildWarehousePanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel info = new JPanel(new GridLayout(0, 2, 8, 6));
        info.setBackground(CARD_BG);
        info.setBorder(new CompoundBorder(
                new LineBorder(ACCENT.darker(), 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        String[][] rows = {
                {"Warehouse ID",  warehouse.getWarehouseId()},
                {"Name",          warehouse.getName()},
                {"Location",      warehouse.getLocation()},
                {"Grid Size",     warehouse.getRows() + " × " + warehouse.getCols()},
                {"Staff Count",   String.valueOf(warehouse.getStaffCount())},
                {"Total Capacity",String.valueOf(warehouse.getTotalCapacity())},
                {"Current Load",  warehouse.getCurrentLoad() + " / " + warehouse.getTotalCapacity()},
                {"Utilisation",   String.format("%.1f%%", warehouse.utilizationRate() * 100)},
                {"Operating Mode",warehouse.getOperatingMode()},
        };
        for (String[] row : rows) {
            JLabel k = new JLabel(row[0]);
            k.setFont(BODY_FONT); k.setForeground(TEXT_DIM);
            JLabel v = new JLabel(row[1]);
            v.setFont(HEADER_FONT); v.setForeground(ACCENT);
            info.add(k); info.add(v);
        }
        p.add(info, BorderLayout.CENTER);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        ctrl.setBackground(BG);

        String[] modes = {"DAY"};
        JComboBox<String> modeBox = new JComboBox<>(modes);
        modeBox.setSelectedItem(warehouse.getOperatingMode());
        modeBox.setBackground(CARD_BG); modeBox.setForeground(TEXT);

        JButton applyMode = accentButton("Set Mode", ACCENT);
        applyMode.addActionListener(e -> {
            warehouse.setOperatingMode((String) modeBox.getSelectedItem());
            setStatus("Operating mode → " + warehouse.getOperatingMode());
        });

        JButton addStaff = accentButton("Add Staff", ACCENT2);
        addStaff.addActionListener(e -> {
            warehouse.addStaff("S-" + System.currentTimeMillis());
            setStatus("Staff count: " + warehouse.getStaffCount());
        });

        JLabel modeLbl = new JLabel("Mode:");
        modeLbl.setForeground(TEXT); modeLbl.setFont(BODY_FONT);

        ctrl.add(modeLbl);
        ctrl.add(modeBox);
        ctrl.add(applyMode);
        ctrl.add(addStaff);

        p.add(ctrl, BorderLayout.SOUTH);
        return p;
    }

    
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL_BG);
        bar.setBorder(new EmptyBorder(4, 12, 4, 12));

        statusLabel = new JLabel("Ready — " + warehouse.getName() + " | " + inventory.size() + " items loaded.");
        statusLabel.setFont(MONO_FONT);
        statusLabel.setForeground(TEXT_DIM);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    
    private void styleTable(JTable t) {
        t.setBackground(CARD_BG);
        t.setForeground(TEXT);
        t.setFont(BODY_FONT);
        t.setRowHeight(26);
        t.setGridColor(BG);
        t.setSelectionBackground(ACCENT.darker());
        t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(PANEL_BG);
        t.getTableHeader().setForeground(ACCENT);
        t.getTableHeader().setFont(HEADER_FONT);
        t.setFillsViewportHeight(true);
    }

    private JTextField styledTextField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(BODY_FONT);
        f.setBorder(new CompoundBorder(
                new LineBorder(ACCENT.darker(), 1, true),
                new EmptyBorder(4, 6, 4, 6)
        ));
        return f;
    }

    private JCheckBox styledCheckbox(String label) {
        JCheckBox cb = new JCheckBox(label);
        cb.setBackground(CARD_BG);
        cb.setForeground(TEXT);
        cb.setFont(BODY_FONT);
        return cb;
    }

    private JButton accentButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color.darker());
        b.setForeground(Color.WHITE);
        b.setFont(HEADER_FONT);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(color); }
            public void mouseExited(MouseEvent e)  { b.setBackground(color.darker()); }
        });
        return b;
    }

    private JDialog styledDialog(String title, int w, int h) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        d.getContentPane().setBackground(CARD_BG);
        d.setLayout(new BorderLayout());
        return d;
    }

    private void addFormRow(JPanel form, GridBagConstraints g, int row, String label, Component comp) {
        g.gridx=0; g.gridy=row; g.gridwidth=1; g.weightx=0.35;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT); lbl.setFont(BODY_FONT);
        form.add(lbl, g);
        g.gridx=1; g.weightx=0.65;
        form.add(comp, g);
    }

    private void showInfoDialog(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorDialog(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private JPanel buildPathFinderPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("🗺  Shortest Path Finder  —  Dijkstra's Algorithm");
        title.setFont(TITLE_FONT);
        title.setForeground(ACCENT);
        p.add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        controls.setBackground(PANEL_BG);

        java.util.List<String> zoneList = new java.util.ArrayList<>();
        for (int r = 0; r < context.AppContext.GRID_ROWS; r++)
            for (int c = 0; c < context.AppContext.GRID_COLS; c++)
                zoneList.add(WarehouseLayout.toLocationId(r, c));
        String[] zones = zoneList.toArray(new String[0]);
        JComboBox<String> fromBox = new JComboBox<>(zones);
        JComboBox<String> toBox   = new JComboBox<>(zones);
        toBox.setSelectedIndex(zones.length - 1); 

        styleCombo(fromBox); styleCombo(toBox);

        JButton findBtn   = styledButton("Find Shortest Path", ACCENT);
        JLabel  distLabel = new JLabel("  Distance: —");
        distLabel.setForeground(ACCENT2); distLabel.setFont(HEADER_FONT);

        controls.add(label("From:")); controls.add(fromBox);
        controls.add(label("To:"));   controls.add(toBox);
        controls.add(findBtn);        controls.add(distLabel);

        JTextArea resultArea = new JTextArea(6, 50);
        resultArea.setBackground(CARD_BG);
        resultArea.setForeground(TEXT);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        resultArea.setEditable(false);
        resultArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        resultArea.setText("Select source and destination zones, then click Find Shortest Path.\n\n" +
                "The algorithm will run Dijkstra's on the warehouse graph and show\n" +
                "the exact sequence of zones/aisles to walk through.\n\n" +
                "DSA: HashMap adjacency list + PriorityQueue MinHeap + dist[] + prev[]");

        JTextArea gridArea = new JTextArea(10, 40);
        gridArea.setBackground(new Color(15, 15, 25));
        gridArea.setForeground(new Color(80, 200, 120));
        gridArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        gridArea.setEditable(false);
        gridArea.setBorder(new EmptyBorder(10,12,10,12));
        gridArea.setText(buildLayoutAscii(null));

        
        
        
        
        
        
        
        
        findBtn.addActionListener(e -> {
            String src  = (String) fromBox.getSelectedItem();
            String dest = (String) toBox.getSelectedItem();
            if (src.equals(dest)) {
                resultArea.setText("Source and destination are the same zone.");
                return;
            }
            java.util.List<String> path = pathFinder.dijkstra(src, dest);
            if (path.isEmpty()) {
                resultArea.setText("No path found between " + src + " and " + dest +
                        ".\nCheck that both zones exist in the graph.");
                distLabel.setText("  Distance: no path");
            } else {
                double dist = pathFinder.estimateDistance(path);
                distLabel.setText(String.format("  Distance: %.0f steps", dist));
                StringBuilder sb = new StringBuilder();
                sb.append("Shortest path from ").append(src).append(" → ").append(dest).append("\n");
                sb.append("─".repeat(50)).append("\n");
                for (int i = 0; i < path.size(); i++) {
                    sb.append(String.format("  Step %2d : %s", i + 1, path.get(i)));
                    if (i < path.size() - 1) sb.append("  →");
                    sb.append("\n");
                }
                sb.append("─".repeat(50)).append("\n");
                sb.append(String.format("Total steps: %.0f", dist));
                resultArea.setText(sb.toString());
                gridArea.setText(buildLayoutAscii(path));
            }
        });

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setBackground(BG);
        JScrollPane resSp  = new JScrollPane(resultArea); resSp.getViewport().setBackground(CARD_BG);
        JScrollPane gridSp = new JScrollPane(gridArea);   gridSp.getViewport().setBackground(new Color(15,15,25));
        center.add(resSp); center.add(gridSp);

        p.add(controls, BorderLayout.NORTH);
        p.add(center,   BorderLayout.CENTER);
        return p;
    }

    








    private String buildLayoutAscii(java.util.List<String> highlightPath) {
        java.util.Set<String> onPath = highlightPath != null
                ? new java.util.HashSet<>(highlightPath) : new java.util.HashSet<>();

        int rows = context.AppContext.GRID_ROWS;
        int cols = context.AppContext.GRID_COLS;

        StringBuilder sb = new StringBuilder();
        sb.append("  Warehouse Layout 8×10  (*** = on shortest path)\n");
        sb.append("      ");
        for (int c = 0; c < cols; c++) sb.append(String.format("  %-4d", c + 1));
        sb.append("\n  ┌");
        sb.append("───────".repeat(cols)).append("┐\n");

        for (int r = 0; r < rows; r++) {
            sb.append(String.format("  %c │", (char)('A' + r)));
            for (int c = 0; c < cols; c++) {
            	String locId = WarehouseLayout.toLocationId(r, c);
                String zType = WarehouseLayout.resolveZoneType(r, c, rows, cols);
                String abbr;
                switch (zType) {
                    case "DISPATCH":   abbr = "DISP "; break;
                    case "HOT":        abbr = "HOT  "; break;
                    case "PERISHABLE": abbr = "PERI "; break;
                    case "FRAGILE":    abbr = "FRAG "; break;
                    default:           abbr = "GEN  "; break;
                }
                String cell = onPath.contains(locId) ? "[***]" : abbr;
                sb.append(String.format(" %-5s", cell));
            }
            sb.append("│\n");
        }
        sb.append("  └");
        sb.append("───────".repeat(cols)).append("┘\n");
        sb.append("  DISP=Dispatch  HOT=Hot  PERI=Perishable  FRAG=Fragile  GEN=General\n");
        sb.append("  [***] = on shortest path\n");
        return sb.toString();
    }

    
    







    private JPanel build3DLayoutPanel() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Warehouse 3D Isometric Layout");
        title.setFont(TITLE_FONT); title.setForeground(ACCENT);
        outer.add(title, BorderLayout.NORTH);

        
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBackground(PANEL_BG);
        legend.setBorder(new EmptyBorder(12, 12, 12, 12));
        legend.setPreferredSize(new Dimension(170, 0));

        JLabel legTitle = new JLabel("Zone Legend");
        legTitle.setFont(HEADER_FONT); legTitle.setForeground(TEXT);
        legend.add(legTitle);
        legend.add(Box.createVerticalStrut(10));

        String[][] legendData = {
                {"DISPATCH",   "#E57373"},
                {"HOT",        "#FF8A65"},
                {"PERISHABLE", "#4FC3F7"},
                {"FRAGILE",    "#CE93D8"},
                {"GENERAL",    "#81C784"},
        };
        for (String[] ld : legendData) {
            Color c = Color.decode(ld[1]);
            JLabel lbl = new JLabel("  " + ld[0]);
            lbl.setOpaque(true);
            lbl.setBackground(c);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setBorder(new EmptyBorder(4, 6, 4, 6));
            lbl.setMaximumSize(new Dimension(150, 24));
            legend.add(lbl);
            legend.add(Box.createVerticalStrut(4));
        }
        legend.add(Box.createVerticalStrut(16));

        JLabel stackTitle = new JLabel("Max Stack Levels");
        stackTitle.setFont(HEADER_FONT); stackTitle.setForeground(TEXT_DIM);
        legend.add(stackTitle);
        legend.add(Box.createVerticalStrut(6));
        String[][] stackInfo = {
                {"FRAGILE / DISPATCH", "1 level"},
                {"HOT / PERISHABLE",   "2 levels"},
                {"GENERAL",            "4 levels"},
        };
        for (String[] si : stackInfo) {
            JLabel l1 = new JLabel(si[0]); l1.setFont(new Font("Segoe UI", Font.BOLD, 10));   l1.setForeground(TEXT);
            JLabel l2 = new JLabel("  → " + si[1]); l2.setFont(new Font("Segoe UI", Font.PLAIN, 10)); l2.setForeground(ACCENT2);
            legend.add(l1); legend.add(l2);
            legend.add(Box.createVerticalStrut(4));
        }

        outer.add(legend, BorderLayout.EAST);

        
        JTextArea infoArea = new JTextArea(3, 40);
        infoArea.setBackground(CARD_BG); infoArea.setForeground(TEXT);
        infoArea.setFont(MONO_FONT); infoArea.setEditable(false);
        infoArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        infoArea.setText("Click any cell to see items stored there.\n" +
                "Use the move buttons to dynamically relocate a selected item.\n" +
                "Stack height = number of items in that location (taller = more items).");

        
        JPanel controls = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        controls.setBackground(PANEL_BG);

        itemBox3D = new JComboBox<>();
        inventory.viewAll().forEach(it -> itemBox3D.addItem(it.getItemId() + " – " + it.getName()));
        styleCombo(itemBox3D);
        JComboBox<String> itemBox = itemBox3D;

        JButton movePeriBtn = styledButton("Move to PERISHABLE", new Color(79,  195, 247));
        JButton moveFragBtn = styledButton("Move to FRAGILE",    new Color(160, 100, 200));
        JButton moveGenBtn  = styledButton("Move to GENERAL",    new Color(129, 199, 132));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row1.setBackground(PANEL_BG);
        row1.add(new JLabel("Item:") {{ setForeground(TEXT_DIM); setFont(HEADER_FONT); }});
        row1.add(itemBox);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row2.setBackground(PANEL_BG);
        row2.add(movePeriBtn);
        row2.add(moveFragBtn);
        row2.add(moveGenBtn);

        controls.add(row1);
        controls.add(row2);

        
        int rows = context.AppContext.GRID_ROWS;
        int cols = context.AppContext.GRID_COLS;

        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int cellW = 64, cellH = 32, levelH = 18;
                int originX = getWidth() / 2;
                int originY = 60;

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        String zType = WarehouseLayout.resolveZoneType(r, c, rows, cols);
                        int stackH   = layout.getStackHeightAt(r, c);
                        int maxLvl   = model.Zone.resolveMaxLevels(zType);

                        int sx = originX + (c - r) * (cellW / 2);
                        int sy = originY + (c + r) * (cellH / 2);

                        drawIsoCell(g2, sx, sy, cellW, cellH, levelH,
                                stackH, maxLvl, zType,
                                WarehouseLayout.toLocationId(r, c));
                    }
                }
            }

            private void drawIsoCell(Graphics2D g2,
                                     int sx, int sy,
                                     int cw, int ch, int lvlH,
                                     int stackH, int maxLvl,
                                     String zoneType, String locId) {
                Color base = zoneColor(zoneType);
                Color top  = base.brighter();
                Color side = base.darker().darker();

                int levels = Math.max(1, stackH);

                for (int lv = 0; lv < levels; lv++) {
                    int elevation = lv * lvlH;

                    int[] topX = { sx,          sx + cw/2, sx,          sx - cw/2 };
                    int[] topY = { sy - elevation - ch/2,
                            sy - elevation,
                            sy - elevation + ch/2,
                            sy - elevation };
                    g2.setColor(lv == levels - 1 ? top : base.darker());
                    g2.fillPolygon(topX, topY, 4);
                    g2.setColor(new Color(0,0,0,60));
                    g2.drawPolygon(topX, topY, 4);

                    int[] rightX = { sx + cw/2, sx + cw/2, sx, sx };
                    int[] rightY = { sy - elevation,
                            sy - elevation + lvlH,
                            sy - elevation + ch/2 + lvlH,
                            sy - elevation + ch/2 };
                    g2.setColor(side);
                    g2.fillPolygon(rightX, rightY, 4);
                    g2.setColor(new Color(0,0,0,80));
                    g2.drawPolygon(rightX, rightY, 4);

                    int[] leftX = { sx - cw/2, sx - cw/2, sx, sx };
                    int[] leftY = { sy - elevation,
                            sy - elevation + lvlH,
                            sy - elevation + ch/2 + lvlH,
                            sy - elevation + ch/2 };
                    g2.setColor(side.brighter());
                    g2.fillPolygon(leftX, leftY, 4);
                    g2.setColor(new Color(0,0,0,80));
                    g2.drawPolygon(leftX, leftY, 4);
                }

                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int txtW = fm.stringWidth(locId);
                int topElevation = levels * lvlH;
                g2.drawString(locId, sx - txtW/2, sy - topElevation + 4);

                if (stackH > 0) {
                    String badge = stackH + "/" + maxLvl;
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    g2.setColor(stackH >= maxLvl ? new Color(252, 92, 92) : new Color(104, 211, 145));
                    g2.drawString(badge, sx - fm.stringWidth(badge)/2, sy - topElevation - 2);
                }
            }

            private Color zoneColor(String zType) {
                switch (zType) {
                    case "DISPATCH":   return new Color(200, 80,  80);
                    case "HOT":        return new Color(210, 110, 60);
                    case "PERISHABLE": return new Color(50,  150, 200);
                    case "FRAGILE":    return new Color(160, 100, 200);
                    default:           return new Color(70,  150, 90);
                }
            }
        };
        canvas.setBackground(BG);
        canvas.setPreferredSize(new Dimension(900, 520));

        
        Runnable refreshCanvas = () -> { canvas.revalidate(); canvas.repaint(); };


        movePeriBtn.addActionListener(e -> {
            String sel = (String) itemBox.getSelectedItem();
            if (sel == null) return;
            String itemId = sel.split(" – ")[0].trim();
            String newLoc = layout.relocateItem(itemId, "PERISHABLE");
            if (newLoc != null) {
                inventory.searchById(itemId).setLocation(newLoc);
                infoArea.setText(" " + itemId + " moved to PERISHABLE zone → " + newLoc +
                        "\nPERISHABLE zone max stack: 2 levels (near Door 2)");
            } else {
                infoArea.setText("⚠ PERISHABLE zone is full!");
            }
            refreshCanvas.run();
        });

        moveFragBtn.addActionListener(e -> {
            String sel = (String) itemBox.getSelectedItem();
            if (sel == null) return;
            String itemId = sel.split(" – ")[0].trim();
            String newLoc = layout.relocateItem(itemId, "FRAGILE");
            if (newLoc != null) {
                inventory.searchById(itemId).setLocation(newLoc);
                infoArea.setText(" " + itemId + " moved to FRAGILE zone → " + newLoc +
                        "\nFRAGILE zone max stack: 1 level (no stacking)");
            } else {
                infoArea.setText("⚠ FRAGILE zone is full!");
            }
            refreshCanvas.run();
        });

        moveGenBtn.addActionListener(e -> {
            String sel = (String) itemBox.getSelectedItem();
            if (sel == null) return;
            String itemId = sel.split(" – ")[0].trim();
            String newLoc = layout.relocateItem(itemId, "GENERAL");
            if (newLoc != null) {
                inventory.searchById(itemId).setLocation(newLoc);
                infoArea.setText(" " + itemId + " moved to GENERAL zone → " + newLoc +
                        "\nGENERAL zone max stack: 4 levels");
            } else {
                infoArea.setText("⚠ GENERAL zone is full!");
            }
            refreshCanvas.run();
        });

        
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int cellW = 64, cellH = 32;
                int originX = canvas.getWidth() / 2;
                int originY = 60;
                int bestR = 0, bestC = 0;
                double bestDist = Double.MAX_VALUE;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int sx = originX + (c - r) * (cellW / 2);
                        int sy = originY + (c + r) * (cellH / 2);
                        double dist = Math.hypot(e.getX() - sx, e.getY() - sy);
                        if (dist < bestDist) { bestDist = dist; bestR = r; bestC = c; }
                    }
                }
                String locId  = WarehouseLayout.toLocationId(bestR, bestC);
                String zType  = WarehouseLayout.resolveZoneType(bestR, bestC, rows, cols);
                java.util.List<String> items = layout.getItemsAt(locId);
                int stackH    = layout.getStackHeightAt(bestR, bestC);
                int maxLvl    = model.Zone.resolveMaxLevels(zType);
                StringBuilder sb = new StringBuilder();
                sb.append(" Location: ").append(locId)
                        .append("  |  Zone: ").append(zType)
                        .append("  |  Stack: ").append(stackH).append("/").append(maxLvl).append("\n");
                if (items.isEmpty()) sb.append("No items stored here.");
                else {
                    sb.append("Items stored:\n");
                    for (String id : items) {
                        model.Item it = inventory.searchById(id);
                        sb.append("  • ").append(id);
                        if (it != null) sb.append(" – ").append(it.getName());
                        sb.append("\n");
                    }
                }
                infoArea.setText(sb.toString());
            }
        });

        JPanel south = new JPanel(new BorderLayout(6, 6));
        south.setBackground(BG);
        south.add(controls,                  BorderLayout.NORTH);
        south.add(new JScrollPane(infoArea), BorderLayout.CENTER);

        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBackground(BG);
        canvasScroll.getViewport().setBackground(BG);
        canvasScroll.setBorder(null);

        outer.add(canvasScroll, BorderLayout.CENTER);
        outer.add(south,        BorderLayout.SOUTH);
        return outer;
    }

    private String shortName(String zoneId) {
        if (zoneId.startsWith("AISLE")) return "aisle";
        return zoneId.replace("Z-","").replace("AISLE-","a");
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text); l.setForeground(TEXT_DIM); l.setFont(HEADER_FONT);
        return l;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(CARD_BG); cb.setForeground(TEXT); cb.setFont(HEADER_FONT);
    }

    private JButton styledButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color); b.setForeground(Color.WHITE);
        b.setFont(HEADER_FONT); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    
    
    
    
    
    
    
    
    private JPanel buildMaintenancePanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(BG);
        p.setBorder(new javax.swing.border.EmptyBorder(16, 16, 16, 16));

        
        JLabel title = new JLabel("⚠  Maintenance Intelligence Dashboard");
        title.setFont(TITLE_FONT); title.setForeground(WARN);
        p.add(title, BorderLayout.NORTH);

        
        JTextArea out = new JTextArea();
        out.setBackground(new Color(15, 15, 25));
        out.setForeground(new Color(200, 220, 255));
        out.setFont(MONO_FONT);
        out.setEditable(false);
        out.setLineWrap(false);
        out.setBorder(new javax.swing.border.EmptyBorder(10, 14, 10, 14));
        out.setText("Click  「 Run Maintenance Scan 」  to analyse all zones.");

        JScrollPane scroll = new JScrollPane(out);
        scroll.getViewport().setBackground(new Color(15, 15, 25));
        scroll.setBorder(BorderFactory.createLineBorder(WARN.darker(), 1));
        p.add(scroll, BorderLayout.CENTER);

        
        JPanel btnRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 4));
        btnRow.setBackground(BG);

        JButton scanBtn = accentButton("Run Maintenance Scan", WARN);
        JLabel  modeLabel = new JLabel();
        modeLabel.setFont(HEADER_FONT);

        scanBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();

            
            int hour      = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            int queueSize = inventory.size();
            int staff     = warehouse.getStaffCount();

            
            String mode;
            if (staff == 0)                                                          mode = "MAINTENANCE";
            else if (queueSize > 100 && (hour==8||hour==9||hour==17||hour==18))      mode = "RUSH_HOUR";
            else if (queueSize > 150)                                                mode = "SURGE";
            else if (queueSize < 10)                                                 mode = "LOW_LOAD";
            else                                                                     mode = "NORMAL";

            String modeColor = mode.equals("MAINTENANCE") ? "⛔" :
                               mode.equals("RUSH_HOUR")   ? "🔴" :
                               mode.equals("SURGE")       ? "🟠" :
                               mode.equals("LOW_LOAD")    ? "🟢" : "🔵";

            sb.append("═══════════════════════════════════════════════════════\n");
            sb.append("  CURRENT MODE : ").append(modeColor).append("  ").append(mode).append("\n");
            sb.append("  Staff: ").append(staff)
              .append("  |  Total items loaded: ").append(queueSize)
              .append("  |  Hour: ").append(hour).append(":xx\n");
            sb.append("═══════════════════════════════════════════════════════\n\n");

            
            
            
            
            
            
            
            
            for (model.Zone z : warehouse.getZones()) {
                int live = countItemsInZone(z.getType());
                z.setCurrentCount(live);
                z.setActivityScore(live * 2.0);
            }
            java.util.PriorityQueue<model.Zone> heap = new java.util.PriorityQueue<>(
                (a, b) -> Double.compare(riskScore(b), riskScore(a))  
            );
            heap.addAll(warehouse.getZones());

            sb.append("  ┌─ MinHeap Risk Ranking (MaxHeap: highest risk first) ─────────┐\n");
            sb.append(String.format("  │  %-8s  %-12s  %6s  %8s  %9s  %8s│\n",
                "Zone", "Type", "Used%", "Activity", "RiskScore", "Action"));
            sb.append("  ├──────────────────────────────────────────────────────────────┤\n");

            java.util.List<model.Zone> ranked = new java.util.ArrayList<>();
            int rank = 1;
            while (!heap.isEmpty()) {
                model.Zone z = heap.poll();
                ranked.add(z);
                double pct  = z.getCapacity() > 0 ? (double)z.getCurrentCount()/z.getCapacity()*100 : 0;
                double risk = riskScore(z);
                String action = risk > 70 ? "⚠ SERVICE NOW" :
                                risk > 40 ? "  MONITOR    " :
                                            "  OK         ";
                sb.append(String.format("  │  #%-2d %-6s  %-12s  %5.1f%%  %8.1f  %9.1f  %s│\n",
                    rank++, z.getZoneId(), z.getType(), pct,
                    z.getActivityScore(), risk, action));
            }
            sb.append("  └──────────────────────────────────────────────────────────────┘\n\n");

            
            
            
            sb.append("  ┌─ Greedy Rebalance Suggestions ──────────────────────────────┐\n");
            java.util.List<model.Zone> overloaded  = new java.util.ArrayList<>();
            java.util.List<model.Zone> underloaded = new java.util.ArrayList<>();
            for (model.Zone z : ranked) {
                double pct = z.getCapacity() > 0 ? (double)z.getCurrentCount()/z.getCapacity() : 0;
                if (pct > 0.80 && !z.isMaintenanceFlag()) overloaded.add(z);
                if (pct < 0.30 && z.freeSlots() > 5)      underloaded.add(z);
            }
            if (overloaded.isEmpty()) {
                sb.append("  │  ✓ No zones overloaded — warehouse is balanced.             │\n");
            } else {
                for (model.Zone src : overloaded) {
                    if (underloaded.isEmpty()) break;
                    model.Zone dst = underloaded.get(0);
                    int canMove = Math.min(src.getCurrentCount() / 4, dst.freeSlots());
                    sb.append(String.format("  │  Move ~%-2d items : %-6s → %-6s  (%d free slots)          │\n",
                        canMove, src.getZoneId(), dst.getZoneId(), dst.freeSlots()));
                }
            }
            sb.append("  └──────────────────────────────────────────────────────────────┘\n\n");

            
            
            
            
            java.util.TreeMap<Long, String> timeline = new java.util.TreeMap<>();
            
            timeline.put(System.currentTimeMillis(), mode);
            
            if (!mode.equals("NORMAL")) {
                timeline.put(System.currentTimeMillis() - 300_000L, "NORMAL");
            }

            sb.append("  ┌─ TreeMap Mode Timeline (sorted by timestamp) ───────────────┐\n");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
            for (java.util.Map.Entry<Long, String> entry : timeline.entrySet()) {
                sb.append(String.format("  │  %s  →  %-12s                                    │\n",
                    sdf.format(new java.util.Date(entry.getKey())), entry.getValue()));
            }
            sb.append("  └──────────────────────────────────────────────────────────────┘\n\n");

            
            if (!ranked.isEmpty()) {
                model.Zone top = ranked.get(0);
                sb.append("  ► SERVICE FIRST: ").append(top.getZoneId())
                  .append(" (").append(top.getType()).append(")")
                  .append("  Risk: ").append(String.format("%.1f", riskScore(top)))
                  .append("  |  Activity: ").append(String.format("%.1f", top.getActivityScore()))
                  .append("\n");
            }

            out.setText(sb.toString());
            out.setCaretPosition(0);
            modeLabel.setText("  Mode: " + mode);
            modeLabel.setForeground(mode.equals("MAINTENANCE") ? DANGER :
                                    mode.equals("RUSH_HOUR")   ? WARN   : ACCENT2);
        });

        btnRow.add(scanBtn);
        btnRow.add(modeLabel);
        p.add(btnRow, BorderLayout.SOUTH);
        return p;
    }

    
    private double riskScore(model.Zone z) {
        double util     = z.getCapacity() > 0 ? (double)z.getCurrentCount()/z.getCapacity() : 0;
        double activity = Math.min(z.getActivityScore() / 20.0, 1.0); 
        return util * 60.0 + activity * 40.0;
    }

    
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Panel.background",            new Color(28, 28, 45));
        UIManager.put("OptionPane.background",        new Color(38, 38, 60));
        UIManager.put("OptionPane.messageForeground", new Color(226, 232, 240));

        SwingUtilities.invokeLater(() -> new WarehouseGUI(new context.AppContext()));
    }
}