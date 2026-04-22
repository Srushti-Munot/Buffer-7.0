package search;

import model.Item;

import java.util.*;
import java.util.stream.Collectors;

class TrieNode {
    final Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;
    String  word  = null;    
    String  itemId = null;   
}


class Trie {

    private final TrieNode              root;
    private final Map<String, TrieNode> wordIndex;   

    public Trie() {
        this.root      = new TrieNode();
        this.wordIndex = new HashMap<>();
    }

    public void insert(String word, String itemId) {
        if (word == null || word.isBlank()) return;
        TrieNode node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd  = true;
        node.word   = word;
        node.itemId = itemId;
        wordIndex.put(word.toLowerCase(), node);
    }


    public boolean search(String word) {
        if (word == null) return false;
        TrieNode node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            if (!node.children.containsKey(c)) return false;
            node = node.children.get(c);
        }
        return node.isEnd;
    }


    public boolean startsWith(String prefix) {
        if (prefix == null) return false;
        TrieNode node = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            if (!node.children.containsKey(c)) return false;
            node = node.children.get(c);
        }
        return true;
    }


    public List<String> getSuggestions(String prefix, int max) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) return results;

        TrieNode node = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            if (!node.children.containsKey(c)) return results;
            node = node.children.get(c);
        }
        collectWords(node, results, max);
        return results;
    }

    public boolean delete(String word) {
        if (word == null || !search(word)) return false;
        wordIndex.remove(word.toLowerCase());
        deleteHelper(root, word.toLowerCase(), 0);
        return true;
    }

  
    private boolean deleteHelper(TrieNode node, String word, int depth) {
        if (depth == word.length()) {
            node.isEnd  = false;
            node.word   = null;
            node.itemId = null;
            return node.children.isEmpty();   
        }
        char c = word.charAt(depth);
        TrieNode child = node.children.get(c);
        if (child == null) return false;
        boolean shouldDelete = deleteHelper(child, word, depth + 1);
        if (shouldDelete) {
            node.children.remove(c);
            return !node.isEnd && node.children.isEmpty();
        }
        return false;
    }

    private void collectWords(TrieNode node, List<String> out, int max) {
        if (out.size() >= max) return;
        if (node.isEnd && node.word != null) out.add(node.word);
        for (TrieNode child : node.children.values()) {
            collectWords(child, out, max);
            if (out.size() >= max) return;
        }
    }

    public TrieNode getRoot() { return root; }
}

class SearchEngine {

    private final Trie                            trie;
    private final Map<String, Item>               itemIndex;
    private final Map<String, List<Item>>         categoryIndex;
    private final TreeMap<String, List<Item>>     sortedCategories;
    private final Set<String>                     recentQueries;

    public SearchEngine() {
        this.trie             = new Trie();
        this.itemIndex        = new HashMap<>();
        this.categoryIndex    = new HashMap<>();
        this.sortedCategories = new TreeMap<>();
        this.recentQueries    = new HashSet<>();
    }

    

    public void indexItem(Item item) {
        trie.insert(item.getName(), item.getItemId());
        itemIndex.put(item.getItemId(), item);
        categoryIndex
            .computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
            .add(item);
        sortedCategories
            .computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
            .add(item);
    }

    public void removeItem(String itemId) {
        Item item = itemIndex.remove(itemId);
        if (item == null) return;
        trie.delete(item.getName());
        List<Item> catList = categoryIndex.get(item.getCategory());
        if (catList != null) catList.removeIf(i -> i.getItemId().equals(itemId));
        List<Item> sortList = sortedCategories.get(item.getCategory());
        if (sortList != null) sortList.removeIf(i -> i.getItemId().equals(itemId));
    }

    public List<Item> search(String query) {
        recentQueries.add(query);
        return itemIndex.values().stream()
                .filter(i -> i.getName().toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparingDouble(Item::getDemandScore).reversed())
                .collect(Collectors.toList());
    }

 
    public List<String> autoSuggest(String prefix, int max) {
        return trie.getSuggestions(prefix, max);
    }

   

    public TreeMap<String, List<Item>> groupByCategory() {
        return sortedCategories;
    }

  
    public List<Item> getRankedResults(String query) {
        recentQueries.add(query);
        List<Item> exact   = new ArrayList<>();
        List<Item> partial = new ArrayList<>();

        for (Item item : itemIndex.values()) {
            if (item.getName().equalsIgnoreCase(query))
                exact.add(item);
            else if (item.getName().toLowerCase().contains(query.toLowerCase()))
                partial.add(item);
        }
        partial.sort(Comparator.comparingDouble(Item::getDemandScore).reversed());

        List<Item> results = new ArrayList<>(exact);
        results.addAll(partial);
        return results;
    }

    
    public Set<String>      getRecentQueries() { return Collections.unmodifiableSet(recentQueries); }
    public Collection<Item> getAllItems()       { return Collections.unmodifiableCollection(itemIndex.values()); }
    public int              size()             { return itemIndex.size(); }
}
