package auth;

import model.User;

import java.util.*;
import java.util.stream.Collectors;

class AccessControl {

    public static final String ROLE_VIEWER  = "VIEWER";
    public static final String ROLE_PICKER  = "PICKER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_ADMIN   = "ADMIN";

    private final Map<String, User>          userStore;          
    private final Map<String, Set<String>>   rolePermissions;    
    private final TreeMap<String, String>    userRoleSorted;     
    private final Set<String>                validRoles;

    public AccessControl() {
        this.userStore       = new HashMap<>();
        this.rolePermissions = new HashMap<>();
        this.userRoleSorted  = new TreeMap<>();
        this.validRoles      = new HashSet<>();

        initDefaultRoles();
    }

    private void initDefaultRoles() {
        
        Set<String> viewer = new HashSet<>(Arrays.asList(
            "VIEW_INVENTORY", "VIEW_ORDERS", "SEARCH"));
        rolePermissions.put(ROLE_VIEWER, viewer);
        validRoles.add(ROLE_VIEWER);

        
        Set<String> picker = new HashSet<>(viewer);
        picker.addAll(Arrays.asList("UPDATE_STOCK", "UPDATE_ORDER_STATUS"));
        rolePermissions.put(ROLE_PICKER, picker);
        validRoles.add(ROLE_PICKER);

        
        Set<String> manager = new HashSet<>(picker);
        manager.addAll(Arrays.asList(
            "ADD_ITEM", "REMOVE_ITEM", "CREATE_ORDER", "CANCEL_ORDER", "VIEW_ANALYTICS"));
        rolePermissions.put(ROLE_MANAGER, manager);
        validRoles.add(ROLE_MANAGER);

        
        Set<String> admin = new HashSet<>(manager);
        admin.addAll(Arrays.asList(
            "MANAGE_USERS", "MANAGE_WAREHOUSES", "SYSTEM_CONFIG", "ASSIGN_ROLE"));
        rolePermissions.put(ROLE_ADMIN, admin);
        validRoles.add(ROLE_ADMIN);
    }

 
    public void registerUser(User user) {
        if (!validRoles.contains(user.getRole()))
            throw new IllegalArgumentException("Unknown role: " + user.getRole());
        if (userStore.containsKey(user.getUserId()))
            throw new IllegalStateException("User already exists: " + user.getUserId());
        userStore.put(user.getUserId(), user);
        userRoleSorted.put(user.getUserId(), user.getRole());
    }

        public void assignRole(String callerId, String targetId, String newRole) {
        if (!hasPermission(callerId, "ASSIGN_ROLE"))
            throw new SecurityException("Caller " + callerId + " lacks ASSIGN_ROLE permission.");
        if (!validRoles.contains(newRole))
            throw new IllegalArgumentException("Unknown role: " + newRole);
        User target = requireUser(targetId);
        target.setRole(newRole);
        userRoleSorted.put(targetId, newRole);
    }

 
    public boolean hasPermission(String userId, String action) {
        User user = userStore.get(userId);
        if (user == null || !user.isActive()) return false;
        Set<String> perms = rolePermissions.getOrDefault(user.getRole(), Collections.emptySet());
        return perms.contains(action);
    }


    public void revokeAccess(String callerId, String targetId) {
        if (!hasPermission(callerId, "MANAGE_USERS"))
            throw new SecurityException("Caller " + callerId + " lacks MANAGE_USERS permission.");
        requireUser(targetId).setActive(false);
    }


    public List<User> getUsersByRole(String role) {
        return userRoleSorted.entrySet().stream()
                .filter(e -> role.equals(e.getValue()))
                .map(e -> userStore.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public void addPermissionToRole(String role, String permission) {
        if (!validRoles.contains(role))
            throw new IllegalArgumentException("Unknown role: " + role);
        rolePermissions.get(role).add(permission);
    }

    private User requireUser(String userId) {
        User user = userStore.get(userId);
        if (user == null) throw new NoSuchElementException("User not found: " + userId);
        return user;
    }

    public Map<String, User>         getAllUsers()        { return Collections.unmodifiableMap(userStore); }
    public Map<String, Set<String>>  getRolePermissions() { return Collections.unmodifiableMap(rolePermissions); }
    public Set<String>               getValidRoles()     { return Collections.unmodifiableSet(validRoles); }
}



class ActivityMonitor {

    private static final int ROLLING_AUDIT_SIZE = 50;

    
    private final Map<String, List<String>>  userLogs;
    
    private final Stack<String>              globalRecent;
    
    private final Map<String, Integer>       actionCounts;
    
    private final Queue<String>              rollingAudit;
    
    private final TreeMap<Long, String>      chronoLog;

    public ActivityMonitor() {
        this.userLogs      = new HashMap<>();
        this.globalRecent  = new Stack<>();
        this.actionCounts  = new HashMap<>();
        this.rollingAudit  = new LinkedList<>();
        this.chronoLog     = new TreeMap<>();
    }

    public void logAction(String userId, String action) {
        long now   = System.currentTimeMillis();
        String entry = "[" + now + "] " + userId + ": " + action;

        
        userLogs.computeIfAbsent(userId, k -> new ArrayList<>()).add(0, entry);

        
        globalRecent.push(entry);

        
        actionCounts.merge(userId, 1, Integer::sum);

        
        if (rollingAudit.size() >= ROLLING_AUDIT_SIZE) rollingAudit.poll();
        rollingAudit.offer(entry);

        
        chronoLog.put(now, entry);
    }

    
    public List<String> getLog(String userId) {
        return Collections.unmodifiableList(
            userLogs.getOrDefault(userId, Collections.emptyList()));
    }

   
    public List<String> getTopPerformers(int n) {
        
        PriorityQueue<Map.Entry<String, Integer>> heap = new PriorityQueue<>(
            Map.Entry.<String, Integer>comparingByValue().reversed());
        heap.addAll(actionCounts.entrySet());

        List<String> result = new ArrayList<>();
        while (!heap.isEmpty() && result.size() < n) {
            result.add(heap.poll().getKey());
        }
        return result;
    }

 

    public List<String> flagAnomaly(int threshold, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;

        
        Map<String, Integer> windowCounts = new HashMap<>();
        for (Map.Entry<Long, String> e : chronoLog.tailMap(cutoff).entrySet()) {
            
            String entry = e.getValue();
            int start = entry.indexOf("] ") + 2;
            int end   = entry.indexOf(":", start);
            if (start > 1 && end > start) {
                String userId = entry.substring(start, end).trim();
                windowCounts.merge(userId, 1, Integer::sum);
            }
        }
        return windowCounts.entrySet().stream()
                .filter(e -> e.getValue() > threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    
    public String getLastAction() {
        return globalRecent.isEmpty() ? null : globalRecent.peek();
    }

    

    public List<String> getRollingAudit() {
        return new ArrayList<>(rollingAudit);
    }

    
    public List<String> getChronoLog() {
        return new ArrayList<>(chronoLog.values());
    }

    public Map<String, Integer> getActionCounts() { return Collections.unmodifiableMap(actionCounts); }
    public int                  totalActions()    { return actionCounts.values().stream().mapToInt(Integer::intValue).sum(); }
}
