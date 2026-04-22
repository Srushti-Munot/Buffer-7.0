package warehouse;

import model.Item;
import java.util.*;

public class PathFinder {

    private final HashMap<String, List<Edge>> adjacencyList;
    private HashMap<String, Double>           dist;
    private HashMap<String, String>           prev;
    private List<String>                      categoryOrder;

    
    private static class Edge {
        final String neighbourId;
        final double distance;
        Edge(String neighbourId, double distance) {
            this.neighbourId = neighbourId;
            this.distance    = distance;
        }
    }

    private static class Node implements Comparable<Node> {
        final String zoneId;
        final double cost;
        Node(String zoneId, double cost) { this.zoneId = zoneId; this.cost = cost; }

        
        @Override
        public int compareTo(Node other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    
    public PathFinder() {
        this.adjacencyList = new HashMap<>();
        this.dist          = new HashMap<>();
        this.prev          = new HashMap<>();
        this.categoryOrder = new ArrayList<>(
            Arrays.asList("BULK", "COLD", "HOT", "FRAGILE", "DISPATCH")
        );
    }

  
    public void addEdge(String zoneA, String zoneB, double distance) {
        adjacencyList.computeIfAbsent(zoneA, k -> new ArrayList<>())
                     .add(new Edge(zoneB, distance));
        adjacencyList.computeIfAbsent(zoneB, k -> new ArrayList<>())
                     .add(new Edge(zoneA, distance));
    }

   


    public void buildGraphFromGrid(String[][] grid) {
        if (grid == null) return;
        int rows = grid.length;
        int cols = grid[0].length;

        
        int[] dr = {-1, 1, 0, 0};
        int[] dc = { 0, 0,-1, 1};

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String cellId = grid[r][c];
                if (cellId == null || cellId.equals("EMPTY") || cellId.equals("WALL")) continue;

                
                adjacencyList.computeIfAbsent(cellId, k -> new ArrayList<>());

                
                for (int d = 0; d < 4; d++) {
                    int nr = r + dr[d];
                    int nc = c + dc[d];
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                    String neighbourId = grid[nr][nc];
                    if (neighbourId == null || neighbourId.equals("EMPTY")
                            || neighbourId.equals("WALL")) continue;

                    
                    
                    addEdge(cellId, neighbourId, 1.0);
                }
            }
        }
    }

    
    public void seedDemoLayout() {
        
        addEdge("Z-HOT",  "AISLE-0-1", 1.0);
        addEdge("AISLE-0-1", "Z-COLD", 1.0);
        
        addEdge("Z-HOT",  "AISLE-1-0", 1.0);
        addEdge("AISLE-0-1", "AISLE-1-1", 1.0);
        addEdge("Z-COLD", "AISLE-1-2", 1.0);
        addEdge("AISLE-1-0","AISLE-1-1",1.0);
        addEdge("AISLE-1-1","AISLE-1-2",1.0);
        
        addEdge("AISLE-1-0","Z-FRAG", 1.0);
        addEdge("AISLE-1-1","AISLE-2-1",1.0);
        addEdge("AISLE-1-2","Z-DISP", 1.0);
        addEdge("Z-FRAG","AISLE-2-1",1.0);
        addEdge("AISLE-2-1","Z-DISP",1.0);
        
        addEdge("Z-FRAG","AISLE-3-0",1.0);
        addEdge("AISLE-2-1","AISLE-3-1",1.0);
        addEdge("Z-DISP","AISLE-3-2",1.0);
        addEdge("AISLE-3-0","AISLE-3-1",1.0);
        addEdge("AISLE-3-1","AISLE-3-2",1.0);
        
        addEdge("AISLE-3-0","Z-BULK",1.0);
        addEdge("AISLE-3-1","AISLE-4-1",1.0);
        addEdge("AISLE-3-2","Z-BULK2",1.0);
        addEdge("Z-BULK","AISLE-4-1",1.0);
        addEdge("AISLE-4-1","Z-BULK2",1.0);
    }

    public List<String> dijkstra(String src, String dest) {
        if (!adjacencyList.containsKey(src) || !adjacencyList.containsKey(dest))
            return Collections.emptyList();

        dist = new HashMap<>();
        prev = new HashMap<>();

        
        for (String node : adjacencyList.keySet()) {
            dist.put(node, Double.MAX_VALUE);
            prev.put(node, null);
        }
        dist.put(src, 0.0);

        PriorityQueue<Node> heap = new PriorityQueue<>();
        heap.offer(new Node(src, 0.0));

        Set<String> visited = new HashSet<>();

        while (!heap.isEmpty()) {
            Node current = heap.poll();
            if (visited.contains(current.zoneId)) continue;
            visited.add(current.zoneId);

            if (current.zoneId.equals(dest)) break;  

            List<Edge> neighbours = adjacencyList.getOrDefault(current.zoneId, Collections.emptyList());
            for (Edge edge : neighbours) {
                if (visited.contains(edge.neighbourId)) continue;
                double newDist = dist.get(current.zoneId) + edge.distance;
                if (newDist < dist.getOrDefault(edge.neighbourId, Double.MAX_VALUE)) {
                    dist.put(edge.neighbourId, newDist);
                    prev.put(edge.neighbourId, current.zoneId);
                    heap.offer(new Node(edge.neighbourId, newDist));
                }
            }
        }

        return reconstructPath(src, dest);
    }

 
    public List<String> getPathForOrder(List<Item> items, Map<String, String> itemToZone) {
        if (items == null || items.isEmpty()) return Collections.emptyList();

        
        Map<String, List<String>> byCategory = new LinkedHashMap<>();
        for (String cat : categoryOrder) byCategory.put(cat, new ArrayList<>());

        for (Item item : items) {
            String zone = itemToZone.get(item.getItemId());
            if (zone == null) continue;
            String cat = item.getCategory().toUpperCase();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(zone);
        }

        
        List<String> orderedStops = new ArrayList<>();
        String lastZone = null;

        for (String cat : categoryOrder) {
            List<String> zones = byCategory.getOrDefault(cat, Collections.emptyList());
            if (zones.isEmpty()) continue;

            
            List<String> remaining = new ArrayList<>(zones);
            while (!remaining.isEmpty()) {
                String next = nearestZone(lastZone, remaining);
                orderedStops.add(next);
                remaining.remove(next);
                lastZone = next;
            }
        }

        
        if (orderedStops.size() <= 1) return orderedStops;

        List<String> fullRoute = new ArrayList<>();
        fullRoute.add(orderedStops.get(0));

        for (int i = 0; i < orderedStops.size() - 1; i++) {
            List<String> leg = dijkstra(orderedStops.get(i), orderedStops.get(i + 1));
            
            for (int j = 1; j < leg.size(); j++)
                fullRoute.add(leg.get(j));
            
            if (leg.isEmpty()) fullRoute.add(orderedStops.get(i + 1));
        }

        return fullRoute;
    }

    


    public double estimateDistance(List<String> route) {
        if (route == null || route.size() < 2) return 0.0;
        double total = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            List<String> leg = dijkstra(route.get(i), route.get(i + 1));
            if (leg.size() >= 2) {
                
                for (int j = 0; j < leg.size() - 1; j++) {
                    total += edgeCost(leg.get(j), leg.get(j + 1));
                }
            }
        }
        return total;
    }

    
    public void setCategoryOrder(List<String> order) {
        this.categoryOrder = new ArrayList<>(order);
    }

    

    private List<String> reconstructPath(String src, String dest) {
        if (!dist.containsKey(dest) || dist.get(dest) == Double.MAX_VALUE)
            return Collections.emptyList();

        LinkedList<String> path = new LinkedList<>();
        String current = dest;
        while (current != null) {
            path.addFirst(current);
            current = prev.get(current);
        }
        return (path.getFirst().equals(src)) ? new ArrayList<>(path) : Collections.emptyList();
    }

    
    private String nearestZone(String current, List<String> candidates) {
        if (current == null || !adjacencyList.containsKey(current))
            return candidates.get(0);

        String nearest = null;
        double best    = Double.MAX_VALUE;
        for (String candidate : candidates) {
            List<String> path = dijkstra(current, candidate);
            double d = estimateDistance(path);
            if (d < best) { best = d; nearest = candidate; }
        }
        return nearest != null ? nearest : candidates.get(0);
    }

    
    private double edgeCost(String from, String to) {
        for (Edge e : adjacencyList.getOrDefault(from, Collections.emptyList()))
            if (e.neighbourId.equals(to)) return e.distance;
        return 0.0;
    }

    
    public Set<String>   getZones()          { return adjacencyList.keySet(); }
    public List<String>  getCategoryOrder()  { return Collections.unmodifiableList(categoryOrder); }
}
