package gui;

import context.AppContext;
import orders.Order;
import orders.OrderItem;
import orders.OrderService;
import orders.OrderPriorityQueue;
import warehouse.PathFinder;
import model.Item;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class OrderGUI extends JFrame {
 
    private static final Color BG       = new Color(18,  18,  30);
    private static final Color PANEL_BG = new Color(28,  28,  45);
    private static final Color CARD_BG  = new Color(38,  38,  60);
    private static final Color ACCENT   = new Color(99,  179, 237);
    private static final Color ACCENT2  = new Color(104, 211, 145);
    private static final Color WARN     = new Color(251, 191,  36);
    private static final Color DANGER   = new Color(252,  92,  92);
    private static final Color PURPLE   = new Color(167, 139, 250);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color TEXT_DIM = new Color(148, 163, 184);

    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font BODY_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font MONO_FONT   = new Font("Consolas",  Font.PLAIN, 12);

    private final OrderService            service;
    private final context.AppContext      ctx;
      
    private final List<Order>             allOrders = new ArrayList<>(); 
    
    private DefaultTableModel orderTableModel;
    private DefaultTableModel queueTableModel;
    private DefaultTableModel historyTableModel;
    
    private JLabel sideTotalOrders;
    private JLabel sideStockItems;
    private JLabel kpiTotal;
    private JLabel kpiPending;
    private JLabel kpiProcessing;
    private JLabel kpiCancelled;

    private JLabel statusLabel;
    
    private JTable orderTable;
    private JTable queueTable;
   
    public OrderGUI(AppContext ctx) {
        this.ctx = ctx;
        service = ctx.getOrderService();
        
        seedOrders();

        buildUI();
        setTitle("Order Management System");
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);

           
        Runnable myRefresh = () -> refreshAll();
        ctx.addRefreshListener(myRefresh);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                ctx.removeRefreshListener(myRefresh);
            }
        });
    }
  
    private void seedOrders() {
        
        allOrders.addAll(service.getAllOrders());

        
        createOrderInternal("ORD-001", "CUST-A", 4, "WH-001",
            new OrderItem("ITM-001", "Laptop",       2, 89900, 2.5),
            new OrderItem("ITM-005", "Mobile Phone", 3, 24999, 0.4));

        createOrderInternal("ORD-002", "CUST-B", 2, "WH-001",
            new OrderItem("ITM-002", "Rice 25kg", 5, 1200, 25.0));

        createOrderInternal("ORD-003", "CUST-A", 5, "WH-001",
            new OrderItem("ITM-003", "Glass Vase",  4, 3500, 0.8),
            new OrderItem("ITM-004", "Cement Bag",  2,  450, 50.0));

        createOrderInternal("ORD-004", "CUST-C", 3, "WH-001",
            new OrderItem("ITM-001", "Laptop",      1, 89900, 2.5),
            new OrderItem("ITM-006", "Wheat Flour", 3,   650, 10.0));

        createOrderInternal("ORD-005", "CUST-B", 1, "WH-001",
            new OrderItem("ITM-004", "Cement Bag",  6, 450, 50.0));
    }

    private void createOrderInternal(String orderId, String customerId,
                                     int priority, String warehouseId,
                                     OrderItem... items) {
        
        if (service.orderExists(orderId)) return;
        
        Map<String, Integer> liveStock = ctx.getInventory().getStockSnapshot();
        Order o = service.createOrder(orderId, customerId,
                                      Arrays.asList(items), priority,
                                      warehouseId, liveStock);
        if (o != null) {
            
            service.deductStock(o, ctx.getInventory());
            allOrders.add(o);
        }
    }
    
    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildSidebar(),   BorderLayout.WEST);
        add(buildMain(),      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }
   
    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(200, 0));
        side.setBackground(PANEL_BG);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel logo = new JLabel("Orders");
        logo.setFont(TITLE_FONT);
        logo.setForeground(ACCENT);
        logo.setBorder(new EmptyBorder(0, 0, 24, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logo);

        side.add(sideStatCard("Total Orders", String.valueOf(allOrders.size()),  lbl -> sideTotalOrders = lbl));
        side.add(sideStatCard("Queue Size",      String.valueOf(service.queueSize())));
        side.add(sideStatCard("Stock Items",  String.valueOf(ctx.getInventory().size()), lbl -> sideStockItems  = lbl));
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel sideStatCard(String label, String value, java.util.function.Consumer<JLabel> reg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setMaximumSize(new Dimension(200, 62));
        card.setBorder(new CompoundBorder(
            new EmptyBorder(4, 8, 4, 8),
            new CompoundBorder(new LineBorder(ACCENT.darker(), 1, true),
                               new EmptyBorder(6, 10, 6, 10))));
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lbl.setForeground(TEXT_DIM);
        JLabel val = new JLabel(value); val.setFont(HEADER_FONT); val.setForeground(ACCENT);
        if (reg != null) reg.accept(val);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.SOUTH);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel sideStatCard(String label, String value) {
        return sideStatCard(label, value, null);
    }
    
    private JPanel buildMain() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.setFont(HEADER_FONT);

        tabs.addTab("Dashboard",    buildDashboard());
        tabs.addTab("Orders",       buildOrdersPanel());
        tabs.addTab("Split / Merge", buildSplitMergePanel());
        tabs.addTab("History",       buildHistoryPanel());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG);
        wrap.add(tabs);
        return wrap;
    }

      
    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        
        JPanel kpi = new JPanel(new GridLayout(1, 4, 12, 0));
        kpi.setBackground(BG);

        long pending    = allOrders.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count();
        long processing = allOrders.stream().filter(o -> o.getStatus() == Order.Status.PROCESSING).count();
        long cancelled  = allOrders.stream().filter(o -> o.getStatus() == Order.Status.CANCELLED).count();

        kpi.add(kpiCardLive("Total Orders",   String.valueOf(allOrders.size()), ACCENT,  lbl -> kpiTotal      = lbl));
        kpi.add(kpiCardLive("Pending",        String.valueOf(pending),          WARN,    lbl -> kpiPending    = lbl));
        kpi.add(kpiCardLive("Processing",     String.valueOf(processing),       ACCENT2, lbl -> kpiProcessing = lbl));
        kpi.add(kpiCardLive("Cancelled",      String.valueOf(cancelled),        DANGER,  lbl -> kpiCancelled  = lbl));
        p.add(kpi, BorderLayout.NORTH);
       
        p.add(buildQueuePanel(), BorderLayout.CENTER);

        
        p.add(buildStatusBreakdown(), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildQueuePanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);

        JLabel heading = new JLabel("Priority Queue — Next-to-Process");
        heading.setFont(HEADER_FONT); heading.setForeground(TEXT);
        outer.add(heading, BorderLayout.NORTH);

        String[] cols = {"Order ID", "Customer", "Priority", "Items", "Weight (kg)", "Status", "Tracking"};
        queueTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshQueueTable();

        
        queueTable = new JTable(queueTableModel);
        styleTable(queueTable);
        outer.add(new JScrollPane(queueTable), BorderLayout.CENTER);

        JButton processBtn = accentButton("Process Next", ACCENT2);
        processBtn.addActionListener(e -> {
            Order next = service.processNext();
            if (next == null) { setStatus("Queue is empty — nothing to process."); return; }
            refreshAll();
            setStatus("Now processing: " + next.getOrderId() + " (Priority " + next.getPriority() + ") — see Orders tab to mark complete.");
        });
    
        
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.setBackground(BG);
        btnRow.add(processBtn);
        outer.add(btnRow, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildStatusBreakdown() {
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);
        outer.setPreferredSize(new Dimension(0, 100));

        JLabel heading = new JLabel("Status Breakdown");
        heading.setFont(HEADER_FONT); heading.setForeground(TEXT);
        outer.add(heading, BorderLayout.NORTH);

        JPanel bars = new JPanel(new GridLayout(1, Order.Status.values().length, 12, 0));
        bars.setBackground(BG);

        Color[] barColors = {WARN, ACCENT, ACCENT2, DANGER, PURPLE};
        int total = Math.max(allOrders.size(), 1);
        int ci = 0;
        for (Order.Status s : Order.Status.values()) {
            long count = allOrders.stream().filter(o -> o.getStatus() == s).count();
            int pct    = (int)((double) count / total * 100);
            JPanel card = new JPanel(new BorderLayout(0, 4));
            card.setBackground(CARD_BG);
            card.setBorder(new EmptyBorder(8, 10, 8, 10));

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(pct);
            bar.setForeground(barColors[ci % barColors.length]);
            bar.setBackground(BG);
            bar.setStringPainted(true);
            bar.setString(count + " (" + pct + "%)");

            JLabel lbl = new JLabel(s.name());
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(TEXT_DIM);

            card.add(lbl, BorderLayout.NORTH);
            card.add(bar, BorderLayout.CENTER);
            bars.add(card);
            ci++;
        }
        outer.add(bars, BorderLayout.CENTER);
        return outer;
    }

      
    private JPanel buildOrdersPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"Order ID", "Customer", "Warehouse", "Priority", "Items",
                         "Weight (kg)", "Status", "Tracking ID"};
        orderTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshOrderTable();

        
        orderTable = new JTable(orderTableModel);
        styleTable(orderTable);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        
        orderTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    String status = (String) orderTableModel.getValueAt(row, 6);
                    c.setBackground(switch (status) {
                        case "PENDING"    -> CARD_BG;
                        case "PROCESSING" -> new Color(30, 60, 40);
                        case "CANCELLED"  -> new Color(55, 25, 25);
                        case "SPLIT"      -> new Color(40, 35, 60);
                        default           -> CARD_BG;
                    });
                    c.setForeground(TEXT);
                }
                return c;
            }
        });

        p.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        p.add(buildOrderButtons(orderTable), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildOrderButtons(JTable table) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(BG);

        JButton createBtn   = accentButton("Create Order",   ACCENT2);
        JButton cancelBtn   = accentButton("Cancel",         DANGER);
        JButton completeBtn = accentButton("Mark Complete",   new Color(104, 211, 145));
        JButton reprioBtn   = accentButton("Reprioritize",    WARN);
        JButton trackBtn    = accentButton("Track",          ACCENT);
        JButton stockBtn    = accentButton("View Stock",     PURPLE);

        createBtn.addActionListener(e  -> showCreateOrderDialog());
        cancelBtn.addActionListener(e  -> cancelSelectedOrder(table));
        reprioBtn.addActionListener(e  -> showReprioritizeDialog(table));
        trackBtn.addActionListener(e   -> trackSelectedOrder(table));
        stockBtn.addActionListener(e   -> showStockDialog());

        
        
        completeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { setStatus("Select a PROCESSING order first."); return; }
            String oid    = (String) orderTableModel.getValueAt(row, 0);
            String status = (String) orderTableModel.getValueAt(row, 6);
            if (!"PROCESSING".equals(status)) {
                setStatus("Only PROCESSING orders (green rows) can be marked complete.");
                return;
            }
            boolean ok = service.completeOrder(oid);
            if (ok) { refreshAll(); setStatus("Order " + oid + " marked COMPLETED."); }
        });

        bar.add(createBtn); bar.add(cancelBtn); bar.add(completeBtn);
        bar.add(reprioBtn); bar.add(trackBtn);  bar.add(stockBtn);
        return bar;
    }

    
    private void showCreateOrderDialog() {
        JDialog d = styledDialog("Create New Order", 600, 720);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(5, 5, 5, 5);
        g.fill    = GridBagConstraints.HORIZONTAL;

        JTextField fOid  = styledTF(18); fOid.setText(String.format("ORD-%03d", allOrders.size()+1));
        JTextField fCust = styledTF(18); fCust.setText("CUST-X");
        JTextField fWh   = styledTF(18); fWh.setText("WH-001");
        JSpinner   fPrio = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        fPrio.getEditor().getComponent(0).setBackground(CARD_BG);

        
        JPanel itemsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        itemsPanel.setBackground(CARD_BG);
        List<JTextField[]> itemRows = new ArrayList<>();

        
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        headerRow.setBackground(CARD_BG);
        String[] colHeaders = {"Item ID", "Name", "Qty", "Price (₹)", "Wt (kg)"};
        int[]    colWidths  = {8, 10, 4, 6, 5};
        for (int c = 0; c < colHeaders.length; c++) {
            JLabel lh = new JLabel(colHeaders[c]);
            lh.setPreferredSize(new java.awt.Dimension(
                new JTextField(colWidths[c]).getPreferredSize().width, 16));
            lh.setForeground(ACCENT); lh.setFont(BODY_FONT.deriveFont(java.awt.Font.BOLD, 11f));
            headerRow.add(lh);
        }
        itemsPanel.add(headerRow);

        
        String[][] hints = {
            {"e.g. ITM-001", "e.g. Laptop",    "e.g. 2", "e.g. 89900", "e.g. 2.5"},
            {"e.g. ITM-002", "e.g. Rice 25kg", "e.g. 5", "e.g. 1200",  "e.g. 25.0"},
            {"e.g. ITM-003", "e.g. Glass Vase","e.g. 1", "e.g. 3500",  "e.g. 0.8"},
        };
        for (int i = 0; i < 3; i++) {
            JTextField[] row = {
                placeholderTF(8,  hints[i][0]),
                placeholderTF(10, hints[i][1]),
                placeholderTF(4,  hints[i][2]),
                placeholderTF(6,  hints[i][3]),
                placeholderTF(5,  hints[i][4])
            };
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            rowPanel.setBackground(CARD_BG);
            for (JTextField tf : row) rowPanel.add(tf);
            itemRows.add(row);
            itemsPanel.add(rowPanel);
        }

        int r = 0;
        addFormRow(form, g, r++, "Order ID",   fOid);
        addFormRow(form, g, r++, "Customer ID",fCust);
        addFormRow(form, g, r++, "Warehouse",  fWh);
        addFormRow(form, g, r++, "Priority (1-5)", fPrio);

        g.gridx=0; g.gridy=r; g.weightx=0.3;
        JLabel itemLbl = new JLabel("Items:");
        itemLbl.setForeground(TEXT); itemLbl.setFont(BODY_FONT);
        form.add(itemLbl, g);
        g.gridx=1; g.weightx=0.7;
        form.add(itemsPanel, g); r++;

        JButton save = accentButton("Create", ACCENT2);
        save.addActionListener(e -> {
            try {
                List<OrderItem> items = new ArrayList<>();
                for (JTextField[] row : itemRows) {
                    String id   = row[0].getText().trim();
                    if (id.isEmpty()) continue;  
                    String name = row[1].getText().trim();
                    String qtyS = row[2].getText().trim();
                    String prcS = row[3].getText().trim();
                    String wgtS = row[4].getText().trim();
                    
                    if (name.isEmpty()) { showErrorDialog("Row with ID \"" + id + "\": Name cannot be blank."); return; }
                    if (qtyS.isEmpty()) { showErrorDialog("Row with ID \"" + id + "\": Quantity is missing (e.g. 2)."); return; }
                    if (prcS.isEmpty()) { showErrorDialog("Row with ID \"" + id + "\": Price is missing (e.g. 89900)."); return; }
                    if (wgtS.isEmpty()) { showErrorDialog("Row with ID \"" + id + "\": Weight is missing in kg (e.g. 2.5)."); return; }
                    int    qty;
                    double price, weight;
                    try { qty = Integer.parseInt(qtyS); if (qty <= 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { showErrorDialog("Row \"" + id + "\": Quantity must be a whole number > 0."); return; }
                    try { price = Double.parseDouble(prcS); if (price < 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { showErrorDialog("Row \"" + id + "\": Price must be a number ≥ 0."); return; }
                    try { weight = Double.parseDouble(wgtS); if (weight <= 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { showErrorDialog("Row \"" + id + "\": Weight must be a number > 0 kg."); return; }
                    items.add(new OrderItem(id, name, qty, price, weight));
                }
                if (items.isEmpty()) { showErrorDialog("Add at least one item."); return; }
                Order o = service.createOrder(
                    fOid.getText().trim(), fCust.getText().trim(),
                    items, (Integer) fPrio.getValue(),
                    fWh.getText().trim(), ctx.getInventory().getStockSnapshot());
                if (o == null) { showErrorDialog("Stock check failed — insufficient quantity."); return; }
                
                
                service.deductStock(o, ctx.getInventory());

                
                service.recordDemand(o, ctx.getWarehouseLayout());
                
                for (orders.OrderItem oi : o.getItems()) {
                    model.Item it = ctx.getInventory().searchById(oi.getItemId());
                    if (it != null) {
                        String newLoc = ctx.getWarehouseLayout().getItemLocation(oi.getItemId());
                        if (!newLoc.equals("UNASSIGNED")) it.setLocation(newLoc);
                    }
                }

                
                
                allOrders.add(o);
                refreshAll();
                ctx.notifyGlobalRefresh();
                setStatus("Order " + o.getOrderId() + " created (priority " + o.getPriority() + ").");
                d.dispose();
            } catch (Exception ex) { showErrorDialog("Invalid input: " + ex.getMessage()); }
        });

        g.gridx=0; g.gridy=r; g.gridwidth=2;
        form.add(save, g);
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(12);
        d.add(formScroll);
        d.setVisible(true);
    }

    
    private void cancelSelectedOrder(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { showInfoDialog("Select", "Please select an order first."); return; }
        String oid = (String) orderTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Cancel order " + oid + "?",
            "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Order o = service.getOrder(oid);
            boolean ok = service.cancelOrder(oid);
            if (ok && o != null) {
                service.restoreStock(o, ctx.getInventory());
                service.reverseDemand(o, ctx.getWarehouseLayout());
                
                for (orders.OrderItem oi : o.getItems()) {
                    model.Item it = ctx.getInventory().searchById(oi.getItemId());
                    if (it != null) {
                        String newLoc = ctx.getWarehouseLayout().getItemLocation(oi.getItemId());
                        if (!newLoc.equals("UNASSIGNED")) it.setLocation(newLoc);
                    }
                }
            }
            refreshAll();
            ctx.notifyGlobalRefresh();
            setStatus(ok ? "Order " + oid + " cancelled — stock restored." : "Could not cancel order " + oid + ".");
        }
    }

    
    private void showReprioritizeDialog(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { showInfoDialog("Select", "Please select an order first."); return; }
        String oid = (String) orderTableModel.getValueAt(row, 0);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{"New priority for " + oid + ":", spinner},
            "Reprioritize", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                service.getOrder(oid);   
                
                
                queueReprioritize(oid, (Integer) spinner.getValue());
                refreshAll();
                setStatus("Order " + oid + " reprioritized to " + spinner.getValue() + ".");
            } catch (Exception ex) { showErrorDialog(ex.getMessage()); }
        }
    }

    
    private void queueReprioritize(String orderId, int newPriority) {
        Order o = service.getOrder(orderId);
        if (o == null) throw new IllegalArgumentException("Order not found: " + orderId);
        o.setPriority(newPriority);  
        
    }

    
    private void trackSelectedOrder(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { showInfoDialog("Select", "Please select an order first."); return; }
        String oid = (String) orderTableModel.getValueAt(row, 0);
        try { showInfoDialog("Tracking — " + oid, service.trackOrder(oid)); }
        catch (Exception ex) { showErrorDialog(ex.getMessage()); }
    }

    
    private void showStockDialog() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Live inventory stock:\n\n");
        ctx.getInventory().getStockSnapshot()
            .forEach((k, v) -> sb.append(String.format("  %-12s : %d units%n", k, v)));
        showInfoDialog("Stock Snapshot", sb.toString());
    }

    
    
    
    private JPanel buildSplitMergePanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        p.add(buildSplitCard());
        p.add(buildMergeCard());
        return p;
    }

    
    private JPanel buildSplitCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(ACCENT.darker(), 1, true),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel title = new JLabel("Split Order");
        title.setFont(TITLE_FONT); title.setForeground(ACCENT);
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.fill = GridBagConstraints.HORIZONTAL;

        JTextField fOid     = styledTF(14);
        JTextField fMaxKg   = styledTF(8);  fMaxKg.setText("50.0");
        JTextField fZoneMap = styledTF(14); fZoneMap.setText("ITM-001:Z-HOT,ITM-002:Z-COLD");

        String[] splitModes = {"By Weight", "By Zone"};
        JComboBox<String> modeBox = new JComboBox<>(splitModes);
        modeBox.setBackground(CARD_BG); modeBox.setForeground(TEXT);

        int r = 0;
        addFormRow(form, g, r++, "Order ID",            fOid);
        addFormRow(form, g, r++, "Split Mode",          modeBox);
        addFormRow(form, g, r++, "Max Weight (kg)",     fMaxKg);
        addFormRow(form, g, r++, "Zone Map (id:zone,…)", fZoneMap);

        card.add(form, BorderLayout.CENTER);

        JTextArea output = styledTextArea();
        JScrollPane scroll = new JScrollPane(output);
        scroll.setPreferredSize(new Dimension(0, 160));

        JButton splitBtn = accentButton("Split", WARN);
        splitBtn.addActionListener(e -> {
            String oid = fOid.getText().trim();
            if (oid.isEmpty()) { output.setText("Enter an Order ID."); return; }
            try {
                List<Order> children;
                if (modeBox.getSelectedIndex() == 0) {
                    double maxKg = Double.parseDouble(fMaxKg.getText().trim());
                    children = service.splitByWeight(oid, maxKg);
                } else {
                    Map<String, String> zoneMap = parseZoneMap(fZoneMap.getText().trim());
                    children = service.splitByZone(oid, zoneMap);
                }
                if (children.isEmpty()) {
                    output.setText("No split needed — order fits within constraint.");
                } else {
                    allOrders.addAll(children);
                    StringBuilder sb = new StringBuilder("Created " + children.size() + " child orders:\n\n");
                    for (Order c : children) sb.append("  ").append(c).append("\n");
                    output.setText(sb.toString());
                    setStatus("Order " + oid + " split into " + children.size() + " parts.");
                    refreshAll();
                }
            } catch (Exception ex) { output.setText("Error: " + ex.getMessage()); }
        });

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setBackground(CARD_BG);
        south.add(splitBtn, BorderLayout.NORTH);
        south.add(scroll,   BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    
    private JPanel buildMergeCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(ACCENT2.darker(), 1, true),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel title = new JLabel("Merge Orders");
        title.setFont(TITLE_FONT); title.setForeground(ACCENT2);
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.fill = GridBagConstraints.HORIZONTAL;

        JTextField fIds      = styledTF(18); fIds.setText("ORD-001,ORD-004");
        JTextField fMergedId = styledTF(14); fMergedId.setText("ORD-MERGED-1");

        int r = 0;
        addFormRow(form, g, r++, "Order IDs (comma-sep)",  fIds);
        addFormRow(form, g, r++, "New Merged Order ID",    fMergedId);

        
        JTextArea output = styledTextArea();
        JScrollPane scroll = new JScrollPane(output);
        scroll.setPreferredSize(new Dimension(0, 160));

        JButton findBtn  = accentButton("Find Overlaps", ACCENT);
        JButton mergeBtn = accentButton("Merge Batch",   ACCENT2);

        findBtn.addActionListener(e -> {
            List<String> ids = parseIds(fIds.getText());
            if (ids.size() < 2) { output.setText("Enter at least 2 order IDs."); return; }
            List<List<String>> groups = service.findCommonItems(ids);
            if (groups.isEmpty()) {
                output.setText("No overlapping items found across these orders.");
            } else {
                StringBuilder sb = new StringBuilder("Overlap groups:\n");
                for (List<String> grp : groups) sb.append("  ").append(grp).append("\n");
                output.setText(sb.toString());
            }
        });

        mergeBtn.addActionListener(e -> {
            List<String> ids = parseIds(fIds.getText());
            String mid       = fMergedId.getText().trim();
            if (ids.size() < 2 || mid.isEmpty()) {
                output.setText("Enter at least 2 order IDs and a merged order ID.");
                return;
            }
            try {
                Order merged = service.mergeBatch(ids, mid);
                if (merged == null) {
                    output.setText("Merge failed — validation error.\n" +
                        "(Orders must all be PENDING and in the same warehouse.)");
                } else {
                    allOrders.add(merged);
                    refreshAll();
                    output.setText("Merged order created:\n\n" + merged);
                    setStatus("Orders " + ids + " merged into " + merged.getOrderId() + ".");
                }
            } catch (Exception ex) { output.setText("Error: " + ex.getMessage()); }
        });

        form.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(form, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnRow.setBackground(CARD_BG);
        btnRow.add(findBtn); btnRow.add(mergeBtn);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setBackground(CARD_BG);
        south.add(btnRow, BorderLayout.NORTH);
        south.add(scroll, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    
    
    
    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setBackground(BG);
        JLabel lbl = new JLabel("Customer ID:"); lbl.setForeground(TEXT); lbl.setFont(BODY_FONT);
        JTextField custField = styledTF(16); custField.setText("CUST-A");
        JButton lookupBtn = accentButton("Lookup", ACCENT);

        String[] cols = {"Order ID", "Priority", "Status", "Items", "Weight (kg)", "Split", "Tracking"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable histTable = new JTable(historyTableModel);
        styleTable(histTable);

        lookupBtn.addActionListener(e -> {
            String cid = custField.getText().trim();
            List<Order> history = service.getOrderHistory(cid);
            historyTableModel.setRowCount(0);
            for (Order o : history) {
                historyTableModel.addRow(new Object[]{
                    o.getOrderId(), o.getPriority(), o.getStatus().name(),
                    o.getItems().size(), String.format("%.2f", o.getTotalWeight()),
                    o.isSplit() ? "YES (" + o.getParentOrderId() + ")" : "No",
                    o.getTrackingId().substring(0, 8) + "…"
                });
            }
            setStatus("History for " + cid + ": " + history.size() + " orders.");
        });

        toolbar.add(lbl); toolbar.add(custField); toolbar.add(lookupBtn);
        p.add(toolbar, BorderLayout.NORTH);
        p.add(new JScrollPane(histTable), BorderLayout.CENTER);

        
        SwingUtilities.invokeLater(lookupBtn::doClick);
        return p;
    }

    
    
    
    private boolean refreshing = false; 

    private void refreshAll() {
        
        
        int orderSel = (orderTable != null) ? orderTable.getSelectedRow() : -1;
        int queueSel = (queueTable != null) ? queueTable.getSelectedRow() : -1;

        refreshOrderTable();
        refreshQueueTable();

        
        if (orderTable != null && orderSel >= 0 && orderSel < orderTableModel.getRowCount())
            orderTable.setRowSelectionInterval(orderSel, orderSel);
        if (queueTable != null && queueSel >= 0 && queueSel < queueTableModel.getRowCount())
            queueTable.setRowSelectionInterval(queueSel, queueSel);

        
        if (kpiTotal != null) {
            kpiTotal.setText(String.valueOf(allOrders.size()));
            kpiPending.setText(String.valueOf(
                allOrders.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count()));
            kpiProcessing.setText(String.valueOf(
                allOrders.stream().filter(o -> o.getStatus() == Order.Status.PROCESSING).count()));
            kpiCancelled.setText(String.valueOf(
                allOrders.stream().filter(o -> o.getStatus() == Order.Status.CANCELLED).count()));
            if (sideTotalOrders != null) sideTotalOrders.setText(String.valueOf(allOrders.size()));
            if (sideStockItems  != null) sideStockItems.setText(String.valueOf(ctx.getInventory().size()));
        }
        
        if (!refreshing) {
            refreshing = true;
            ctx.notifyGlobalRefresh();
            refreshing = false;
        }
    }

    private void refreshOrderTable() {
        orderTableModel.setRowCount(0);
        for (Order o : allOrders) {
            orderTableModel.addRow(new Object[]{
                o.getOrderId(), o.getCustomerId(), o.getWarehouseId(),
                o.getPriority(), o.getItems().size(),
                String.format("%.2f", o.getTotalWeight()),
                o.getStatus().name(),
                o.getTrackingId().substring(0, 8) + "…"
            });
        }
    }

    private void refreshQueueTable() {
        queueTableModel.setRowCount(0);
        
        allOrders.stream()
            .filter(o -> o.getStatus() == Order.Status.PENDING)
            .sorted(Comparator.comparingInt(Order::getPriority).reversed()
                              .thenComparingLong(Order::getTimestamp))
            .limit(8)
            .forEach(o -> queueTableModel.addRow(new Object[]{
                o.getOrderId(), o.getCustomerId(), o.getPriority(),
                o.getItems().size(), String.format("%.2f", o.getTotalWeight()),
                o.getStatus().name(),
                o.getTrackingId().substring(0, 8) + "…"
            }));
    }

    
    
    
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL_BG);
        bar.setBorder(new EmptyBorder(4, 12, 4, 12));
        statusLabel = new JLabel("Ready — " + allOrders.size() + " orders loaded.");
        statusLabel.setFont(MONO_FONT);
        statusLabel.setForeground(TEXT_DIM);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }

    
    
    
    private JPanel kpiCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(accent, 2, true), new EmptyBorder(16, 20, 16, 20)));
        JLabel lbl = new JLabel(title); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lbl.setForeground(TEXT_DIM);
        JLabel val = new JLabel(value); val.setFont(new Font("Segoe UI", Font.BOLD, 28)); val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return card;
    }

    
    private JPanel kpiCardLive(String title, String value, Color accent,
                                java.util.function.Consumer<JLabel> capture) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(accent, 2, true), new EmptyBorder(16, 20, 16, 20)));
        JLabel lbl = new JLabel(title); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lbl.setForeground(TEXT_DIM);
        JLabel val = new JLabel(value); val.setFont(new Font("Segoe UI", Font.BOLD, 28)); val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        capture.accept(val);
        return card;
    }

    private void styleTable(JTable t) {
        t.setBackground(CARD_BG); t.setForeground(TEXT); t.setFont(BODY_FONT);
        t.setRowHeight(26); t.setGridColor(BG);
        t.setSelectionBackground(ACCENT.darker()); t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(PANEL_BG); t.getTableHeader().setForeground(ACCENT);
        t.getTableHeader().setFont(HEADER_FONT); t.setFillsViewportHeight(true);
    }

    private JTextField styledTF(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG); f.setForeground(TEXT); f.setCaretColor(ACCENT);
        f.setFont(BODY_FONT);
        f.setBorder(new CompoundBorder(new LineBorder(ACCENT.darker(), 1, true), new EmptyBorder(4, 6, 4, 6)));
        return f;
    }

    



    private JTextField placeholderTF(int cols, String hint) {
        JTextField f = new JTextField(cols) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setColor(TEXT_DIM);
                    g2.setFont(getFont().deriveFont(java.awt.Font.ITALIC));
                    java.awt.Insets ins = getInsets();
                    g2.drawString(hint, ins.left + 2,
                        getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                    g2.dispose();
                }
            }
        };
        f.setBackground(BG); f.setForeground(TEXT); f.setCaretColor(ACCENT);
        f.setFont(BODY_FONT);
        f.setBorder(new CompoundBorder(new LineBorder(ACCENT.darker(), 1, true), new EmptyBorder(4, 6, 4, 6)));
        f.setToolTipText(hint);
        return f;
    }

    private JTextArea styledTextArea() {
        JTextArea ta = new JTextArea();
        ta.setBackground(BG); ta.setForeground(ACCENT2); ta.setFont(MONO_FONT);
        ta.setEditable(false); ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        return ta;
    }

    private JButton accentButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color.darker()); b.setForeground(Color.WHITE);
        b.setFont(HEADER_FONT); b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(color, 1, true), new EmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(color); }
            public void mouseExited(MouseEvent e)  { b.setBackground(color.darker()); }
        });
        return b;
    }

    private JDialog styledDialog(String title, int w, int h) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(w, h); d.setLocationRelativeTo(this);
        d.getContentPane().setBackground(CARD_BG);
        d.setLayout(new BorderLayout());
        return d;
    }

    private void addFormRow(JPanel form, GridBagConstraints g, int row, String label, Component comp) {
        g.gridx=0; g.gridy=row; g.gridwidth=1; g.weightx=0.38;
        JLabel lbl = new JLabel(label); lbl.setForeground(TEXT); lbl.setFont(BODY_FONT);
        form.add(lbl, g);
        g.gridx=1; g.weightx=0.62;
        form.add(comp, g);
    }

    private void showInfoDialog(String title, String msg) {
        JTextArea ta = new JTextArea(msg);
        ta.setEditable(false); ta.setBackground(CARD_BG); ta.setForeground(TEXT);
        ta.setFont(MONO_FONT); ta.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(420, 160));
        JOptionPane.showMessageDialog(this, sp, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorDialog(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }


    
    private List<String> parseIds(String raw) {
        List<String> ids = new ArrayList<>();
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) ids.add(t);
        }
        return ids;
    }

    private Map<String, String> parseZoneMap(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String entry : raw.split(",")) {
            String[] kv = entry.trim().split(":");
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Panel.background",            new Color(28,  28,  45));
        UIManager.put("OptionPane.background",        new Color(38,  38,  60));
        UIManager.put("OptionPane.messageForeground", new Color(226, 232, 240));
        SwingUtilities.invokeLater(() -> new OrderGUI(new context.AppContext()));
    }
}