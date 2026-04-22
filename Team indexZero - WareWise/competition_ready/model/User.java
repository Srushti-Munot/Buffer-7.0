package model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId, name, email, role, warehouseId;
    private List<String> activityLog;
    private boolean isActive;
    private String lastLogin;

    public User(String userId, String name, String email, String role, String warehouseId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.warehouseId = warehouseId;
        this.activityLog = new ArrayList<>();
        this.isActive = true;
        this.lastLogin = "NEVER";
    }

    
    public String getUserId()                        { return userId; }
    public void setUserId(String userId)             { this.userId = userId; }

    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }

    public String getEmail()                         { return email; }
    public void setEmail(String email)               { this.email = email; }

    public String getRole()                          { return role; }
    public void setRole(String role)                 { this.role = role; }

    public String getWarehouseId()                   { return warehouseId; }
    public void setWarehouseId(String warehouseId)   { this.warehouseId = warehouseId; }

    public List<String> getActivityLog()             { return activityLog; }
    public void setActivityLog(List<String> log)     { this.activityLog = log; }

    public boolean isActive()                        { return isActive; }
    public void setActive(boolean isActive)          { this.isActive = isActive; }

    public String getLastLogin()                     { return lastLogin; }
    public void setLastLogin(String lastLogin)       { this.lastLogin = lastLogin; }

    

    public void logAction(String action) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date());
        activityLog.add("[" + timestamp + "] " + action);
    }
}
