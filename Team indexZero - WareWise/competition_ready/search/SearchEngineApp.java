package search;

import context.AppContext;
import inventory.InventoryManager;
import model.Item;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


public class SearchEngineApp extends JFrame {

    private static final Color BG       = new Color(18,  18,  30);
    private static final Color PANEL_BG = new Color(28,  28,  45);
    private static final Color CARD_BG  = new Color(38,  38,  60);
    private static final Color ACCENT   = new Color(99,  179, 237);
    private static final Color ACCENT2  = new Color(104, 211, 145);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color TEXT_DIM = new Color(148, 163, 184);
    private static final Font  HEADER_FONT = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font  BODY_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
 
    
    private final HashMap<String, Item>       itemStore  = new HashMap<>();
    private final TreeMap<String, List<Item>> categories = new TreeMap<>();
    private final TrieNode                    trieRoot   = new TrieNode();
    private InventoryManager inventory;

    private JTextField        searchField;
    private DefaultListModel<String> suggModel = new DefaultListModel<>();
    private DefaultTableModel resultModel;
    private JLabel            statsLabel;

    
    private static class TrieNode {
        HashMap<Character, TrieNode> children = new HashMap<>();
        boolean isEnd  = false;
        String  itemId = null;
    }


    private void trieInsert(String word, String itemId) {
        TrieNode node = trieRoot;
        for (char c : word.toLowerCase().toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd  = true;
        node.itemId = itemId;
    }

   

    private List<String> trieSearch(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        TrieNode node = trieRoot;
        for (char c : prefix.toLowerCase().toCharArray()) {
            node = node.children.get(c);
            if (node == null) return results;
        }
        dfsSuggest(node, new StringBuilder(prefix.toLowerCase()), results, limit);
        return results;
    }

    private void dfsSuggest(TrieNode node, StringBuilder cur,
                             List<String> out, int limit) {
        if (out.size() >= limit) return;
        if (node.isEnd) out.add(cur.toString());
        for (Map.Entry<Character, TrieNode> e : node.children.entrySet()) {
            cur.append(e.getKey());
            dfsSuggest(e.getValue(), cur, out, limit);
            cur.deleteCharAt(cur.length() - 1); 
        }
    }

    
    private void seedData() {
        for (Item it : inventory.viewAll()) {
            itemStore.put(it.getItemId(), it);
            trieInsert(it.getName(), it.getItemId());
            categories.computeIfAbsent(it.getCategory(), k -> new ArrayList<>()).add(it);
        }
    }

    
    public SearchEngineApp(AppContext ctx) {
        this.inventory = ctx.getInventory();
        seedData();

        
        
        ctx.getInventory().addChangeObserver(() ->
            javax.swing.SwingUtilities.invokeLater(this::rebuildTrie));

        
        
        Runnable myRefresh = this::rebuildTrie;
        ctx.addRefreshListener(myRefresh);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                ctx.removeRefreshListener(myRefresh);
            }
        });

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildStatsBar(),  BorderLayout.SOUTH);
        setTitle("WMS — Smart Search Engine");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private JPanel buildSearchBar() {
        JPanel p = new JPanel(new BorderLayout(10,0));
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        JLabel lbl = new JLabel("🔍  Search:");
        lbl.setForeground(TEXT); lbl.setFont(HEADER_FONT);
        searchField = new JTextField();
        searchField.setBackground(CARD_BG); searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT); searchField.setFont(BODY_FONT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT,1),
            BorderFactory.createEmptyBorder(6,10,6,10)));
        
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e)  { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        searchField.addActionListener(e -> performSearch(searchField.getText().trim()));
        JButton btn = new JButton("Search");
        btn.setBackground(ACCENT); btn.setForeground(Color.WHITE);
        btn.setFont(HEADER_FONT); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.addActionListener(e -> performSearch(searchField.getText().trim()));
        p.add(lbl,BorderLayout.WEST); p.add(searchField,BorderLayout.CENTER); p.add(btn,BorderLayout.EAST);
        return p;
    }

    private JSplitPane buildCenter() {
        JList<String> suggList = new JList<>(suggModel);
        suggList.setBackground(CARD_BG); suggList.setForeground(ACCENT2); suggList.setFont(BODY_FONT);
        suggList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String sel = suggList.getSelectedValue();
                if (sel != null) { searchField.setText(sel); performSearch(sel); }
            }
        });
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(PANEL_BG);
        JLabel sl = new JLabel("  💡 Suggestions"); sl.setForeground(TEXT_DIM); sl.setFont(HEADER_FONT);
        sl.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        left.add(sl, BorderLayout.NORTH);
        left.add(new JScrollPane(suggList), BorderLayout.CENTER);

        String[] cols = {"ID","Name","Category","Qty","Status","Price","Demand"};
        resultModel = new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} };
        JTable table = new JTable(resultModel);
        table.setBackground(CARD_BG); table.setForeground(TEXT); table.setFont(BODY_FONT);
        table.setRowHeight(26); table.getTableHeader().setBackground(PANEL_BG);
        table.getTableHeader().setForeground(TEXT_DIM); table.setGridColor(new Color(50,50,70));

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(PANEL_BG);
        JLabel rl = new JLabel("  📋 Results"); rl.setForeground(TEXT_DIM); rl.setFont(HEADER_FONT);
        rl.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        right.add(rl, BorderLayout.NORTH); right.add(new JScrollPane(table), BorderLayout.CENTER);

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        sp.setDividerLocation(200); return sp;
    }

    private JPanel buildStatsBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(PANEL_BG);
        statsLabel = new JLabel("  " + itemStore.size() + " items in Trie  |  "
                                + categories.size() + " categories in TreeMap");
        statsLabel.setForeground(TEXT_DIM); statsLabel.setFont(BODY_FONT);
        p.add(statsLabel);
        
        JButton refreshBtn = new JButton("⟳ Sync Inventory");
        refreshBtn.setFont(BODY_FONT);
        refreshBtn.setBackground(new Color(38, 38, 60));
        refreshBtn.setForeground(new Color(99, 179, 237));
        refreshBtn.setFocusPainted(false);
        
        refreshBtn.addActionListener(e -> rebuildTrie());
        p.add(refreshBtn);
        return p;
    }

    
    
    private void rebuildTrie() {
        itemStore.clear();
        categories.clear();
        trieRoot.children.clear();
        for (Item it : inventory.viewAll()) {
            itemStore.put(it.getItemId(), it);
            trieInsert(it.getName(), it.getItemId());
            categories.computeIfAbsent(it.getCategory(), k -> new ArrayList<>()).add(it);
        }
        if (statsLabel != null) {
            statsLabel.setText("  " + itemStore.size() + " items in Trie  |  "
                               + categories.size() + " categories  |  synced ✓");
        }
    }

    

    private void performSearch(String query) {
        resultModel.setRowCount(0);
        if (query.isEmpty()) return;
        List<Item> results = new ArrayList<>();
        Item exact = itemStore.get(query.toUpperCase());
        if (exact != null) results.add(exact);
        for (Item item : itemStore.values())
            if (!results.contains(item) && item.getName().toLowerCase().contains(query.toLowerCase()))
                results.add(item);
        List<Item> catItems = categories.getOrDefault(query, new ArrayList<>());
        for (Item item : catItems) if (!results.contains(item)) results.add(item);
        results.sort(Comparator.comparingDouble(Item::getDemandScore).reversed());
        for (Item it : results)
            resultModel.addRow(new Object[]{it.getItemId(),it.getName(),it.getCategory(),
                it.getQuantity(),it.getStatus(),String.format("₹%.0f",it.getPrice()),
                String.format("%.1f",it.getDemandScore())});
        statsLabel.setText("  Found "+results.size()+" result(s) for \""+query+"\"");
    }

    private void updateSuggestions() {
        String q = searchField.getText().trim();
        suggModel.clear();
        if (q.isEmpty()) return;
        for (String s : trieSearch(q, 8)) suggModel.addElement(s);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchEngineApp(new context.AppContext()));
    }
}
