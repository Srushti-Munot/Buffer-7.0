# WareWise — Warehouse Management System

> *"Optimizing Every Inch, Every Second"*

---

## Problem Statement

Small and mid-scale warehouses bleed money through inefficient picking routes, poor stock placement, scattered order tracking, and zero cross-location visibility — yet enterprise WMS software costs hundreds of thousands to deploy. WareWise delivers a full-featured, DSA-backed warehouse management system in pure Java, purpose-built to solve these problems without any database, cloud dependency, or licensing cost.

---

## Feature Modules

---

### 1. Authentication & Access Control

A four-tier role system (Viewer → Picker → Manager → Admin) where each role inherits the permissions of the one below it. Admins can assign roles, revoke access, and view a sorted user directory. All permission checks are O(1) — no scanning through user lists. An activity monitor logs login/logout events and flags repeated failed attempts.

---

### 2. Inventory Management

The core of the system. Every item is indexed two ways — by ID for instant lookup, and by name for sorted display. Items are also pre-grouped by category so fetching all items in a category doesn't require scanning the whole inventory.

Key capabilities:
- **Undo** — every add, remove, or update can be reversed in one step
- **Hot-Zone Optimizer** — automatically identifies the highest-demand items and moves them to the closest picking zone, reducing picker travel time
- **Affinity Mapper** — tracks which items are frequently ordered together and suggests co-placement to speed up multi-item picks
- **Smart Placement** — scores each item on demand, weight, fragility, and access frequency, then recommends the best zone for it
- **Quality Tracker** — flags items with quality issues and surfaces the most urgent ones first

---

### 3. Smart Search & Autocomplete

A Trie-based search engine that gives live autocomplete as the user types. Searching by prefix finds all matching items without scanning the full inventory — it walks the Trie directly to the prefix and collects results from there. Results are automatically grouped by category in alphabetical order. The search index rebuilds itself whenever inventory changes, so results are always up to date.

---

### 4. Order Management

Handles the full lifecycle of an order from creation to fulfillment.

- Orders are always processed highest-priority-first, with equal-priority orders served in the order they were created
- Any order can be fetched, updated, or cancelled instantly by ID
- **Splitting** — one order can be broken into sub-orders for partial fulfillment across locations
- **Merging** — multiple orders with overlapping items can be combined into one picker trip
- Orders follow a strict status flow (Pending → Processing → Fulfilled / Cancelled) — illegal jumps are rejected
- When an item gets ordered frequently enough, it's automatically flagged for relocation to the HOT zone

---

### 5. Demand Forecasting & Analytics

Tracks demand over time per item and uses that history to forecast and react:

- **Surge Detection** — flags an item as surging if its latest demand is statistically abnormal compared to its history
- **Moving Average Forecast** — predicts next-period demand using recent data points
- **Reorder Triggers** — automatically generates a reorder list when any item's stock drops below its configured threshold
- **Shortage Alerts** — three-tier severity system (Critical / Warning / Watch) that fires alerts as stock crosses each level
- Regional demand tracking lets managers see which items are moving fastest in which locations

---

### 6. Warehouse Layout & Zone Management

The warehouse is modeled as a 2D grid where each cell belongs to a zone (HOT, PERISHABLE, FRAGILE, GENERAL, DISPATCH). Zone types are assigned automatically based on grid position — high-traffic zones near the front, cold/perishable zones in designated areas.

- Each zone type enforces its own stacking limit (e.g., FRAGILE zones allow only 1 level, GENERAL zones allow 4)
- Managers can save named layout profiles (e.g., Day Shift, Night Shift) and switch between them instantly
- A Context Engine can automatically switch profiles based on time of day or current order queue load
- An Efficiency Calculator tracks per-zone utilization and produces an overall warehouse efficiency score

---

### 7. Pathfinding & Pick-Route Optimization

The warehouse grid is converted into a weighted graph, and Dijkstra's algorithm finds the shortest path between any two zones. For multi-item picks, the system generates a full route that visits all required zones in the optimal order (Bulk → Cold → HOT → Fragile → Dispatch), minimizing total walking distance. New zone connections can be added at runtime when the layout changes — no need to rebuild the full graph.

---

### 8. Multi-Warehouse Operations

Manages inventory and orders across multiple warehouse locations:

- When an order can't be fulfilled locally, the system automatically redirects it to the best available warehouse — one that carries the required items and has the most spare capacity
- A transfer queue handles inter-warehouse stock moves in order, with locks to prevent the same stock being dispatched twice
- Every transfer is logged with a timestamp for a full audit trail
- Each item gets a unique tracking ID, can be grouped into batches, and has a complete movement history showing every warehouse it has passed through

---

## DSA Summary

| Data Structure | Primary Use |
|----------------|-------------|
| `HashMap` | O(1) lookup everywhere — items, orders, users, zones, demand scores |
| `TreeMap` | Sorted iteration — item names, layout profiles, demand history, audit log |
| `HashSet` | O(1) membership checks — roles, flagged items, in-transit IDs |
| `Stack` | Undo log (LIFO) |
| `PriorityQueue` (min-heap) | Order priority queue, Dijkstra, warehouse selection by utilization |
| `PriorityQueue` (max-heap) | Hot-zone optimizer, quality issue ranking, top-demand items |
| `Queue` | Sliding demand window, FIFO transfer queue |
| `Trie` | O(L) prefix search for autocomplete |
| `2D Array` | Warehouse grid — O(1) cell access by position |
| `Graph` (adjacency list) | Zone graph for Dijkstra pathfinding |
| Dijkstra's Algorithm | Shortest pick route, O((V+E) log V) |

---

*Built for competition — every data structure choice is deliberate and optimized for the operation it serves.*
