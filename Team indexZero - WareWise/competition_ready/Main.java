import context.AppContext;
import gui.WarehouseGUI;
import gui.OrderGUI;
import search.SearchEngineApp;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main {

    
    private static final Color BG       = new Color(18,  18,  30);
    private static final Color PANEL_BG = new Color(28,  28,  45);
    private static final Color CARD_BG  = new Color(38,  38,  60);
    private static final Color ACCENT   = new Color(99,  179, 237);
    private static final Color ACCENT2  = new Color(104, 211, 145);
    private static final Color WARN     = new Color(251, 191,  36);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color TEXT_DIM = new Color(148, 163, 184);
    private static final Font  TITLE    = new Font("Segoe UI", Font.BOLD,  24);
    private static final Font  HEADER   = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font  BODY     = new Font("Segoe UI", Font.PLAIN, 13);

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(Main::showLauncher);
    }

    private static void showLauncher() {
        AppContext ctx = new AppContext();

        
    
        final WarehouseGUI[]    warehouseWin = {null};
        final OrderGUI[]        orderWin     = {null};
        final SearchEngineApp[] searchWin    = {null};

        JFrame frame = new JFrame("WareWise");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL_BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel title = new JLabel("—== { WareWise } ==—");
        title.setFont(TITLE); title.setForeground(ACCENT);
        JLabel subtitle = new JLabel("Optimizing Every Inch, Every Second");
        subtitle.setFont(BODY); subtitle.setForeground(TEXT_DIM);

        JPanel titleBlock = new JPanel(new BorderLayout());
        titleBlock.setBackground(PANEL_BG);
        titleBlock.add(title,    BorderLayout.CENTER);
        titleBlock.add(subtitle, BorderLayout.SOUTH);

        header.add(titleBlock, BorderLayout.NORTH);
        JPanel searchCardRow = new JPanel(new BorderLayout());
        searchCardRow.setBackground(PANEL_BG);
        searchCardRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        searchCardRow.add(moduleCard(
            "Smart Search",
            "Search items instantly across your warehouse",
            WARN,
            e -> {
                if (searchWin[0] == null || !searchWin[0].isDisplayable()) {
                    searchWin[0] = new SearchEngineApp(ctx);
                } else {
                    searchWin[0].toFront();
                    searchWin[0].requestFocus();
                }
            }
        ), BorderLayout.CENTER);
        header.add(searchCardRow, BorderLayout.SOUTH);
        frame.add(header, BorderLayout.NORTH);

     
        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 16));
        grid.setBackground(BG);
        grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        grid.add(moduleCard(
            "Warehouse & Inventory",
            "Manage stock, zones and picking routes",
            ACCENT,
            e -> {
                if (warehouseWin[0] == null || !warehouseWin[0].isDisplayable()) {
                    warehouseWin[0] = new WarehouseGUI(ctx);
                } else {
                    warehouseWin[0].toFront();
                    warehouseWin[0].requestFocus();
                }
            }
        ));

        grid.add(moduleCard(
            "Order Management",
            "Create, track and fulfil orders",
            ACCENT2,
            e -> {
                if (orderWin[0] == null || !orderWin[0].isDisplayable()) {
                    orderWin[0] = new OrderGUI(ctx);
                } else {
                    orderWin[0].toFront();
                    orderWin[0].requestFocus();
                }
            }
        ));

        frame.add(grid, BorderLayout.CENTER);

        
        JLabel footer = new JLabel(
            " ",
            SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(TEXT_DIM);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        frame.add(footer, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

  
    private static JPanel moduleCard(String title, String desc,
                                      Color accent, ActionListener onClick) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent.darker(), 1, true),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel t = new JLabel(title); t.setFont(HEADER); t.setForeground(accent);
        JLabel d = new JLabel("<html>" + desc.replace("\n","<br>") + "</html>");
        d.setFont(BODY); d.setForeground(TEXT_DIM);

        card.add(t, BorderLayout.NORTH);
        card.add(d, BorderLayout.CENTER);

        
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { onClick.actionPerformed(null); }
            public void mouseEntered(MouseEvent e) { card.setBackground(new Color(50,50,75)); }
            public void mouseExited(MouseEvent e)  { card.setBackground(CARD_BG); }
        });

        return card;
    }
}
