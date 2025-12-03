package newplayerpanel.restrictions;

import java.util.*;

public class Restriction {
    
    private final String name;
    private final RestrictionType type;
    private final Set<String> actions;
    private final List<String> items;
    private final List<String> entities;
    private final List<String> commands;
    private final int timeSeconds;
    private final boolean isDefault;
    
    public Restriction(String name, RestrictionType type, Set<String> actions,
                      List<String> items, List<String> entities, List<String> commands,
                      int timeSeconds, boolean isDefault) {
        this.name = name;
        this.type = type;
        this.actions = actions;
        this.items = items != null ? items : new ArrayList<>();
        this.entities = entities != null ? entities : new ArrayList<>();
        this.commands = commands != null ? commands : new ArrayList<>();
        this.timeSeconds = timeSeconds;
        this.isDefault = isDefault;
    }
    
    public static Restriction fromMap(Map<String, Object> map) {
        String name = (String) map.get("name");
        String typeStr = (String) map.get("type");
        RestrictionType type = RestrictionType.valueOf(typeStr);
        
        Set<String> actions = new HashSet<>();
        Object actionsObj = map.get("actions");
        if (actionsObj instanceof String) {
            String[] actionArray = ((String) actionsObj).split(",");
            for (String action : actionArray) {
                actions.add(action.trim().toUpperCase());
            }
        } else if (actionsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> actionList = (List<String>) actionsObj;
            for (String action : actionList) {
                actions.add(action.trim().toUpperCase());
            }
        }
        
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) map.get("item");
        
        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) map.get("entity");
        
        @SuppressWarnings("unchecked")
        List<String> commands = (List<String>) map.get("command");
        
        int timeSeconds = -1;
        Object timeObj = map.get("time");
        if (timeObj != null) {
            if (timeObj instanceof Integer) {
                timeSeconds = (Integer) timeObj;
            } else if (timeObj instanceof String) {
                try {
                    timeSeconds = Integer.parseInt((String) timeObj);
                } catch (NumberFormatException e) {
                    timeSeconds = -1;
                }
            }
        }
        
        boolean isDefault = false;
        Object defaultObj = map.get("default");
        if (defaultObj != null) {
            if (defaultObj instanceof Boolean) {
                isDefault = (Boolean) defaultObj;
            } else if (defaultObj instanceof String) {
                isDefault = Boolean.parseBoolean((String) defaultObj);
            }
        }
        
        return new Restriction(name, type, actions, items, entities, commands, timeSeconds, isDefault);
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        
        map.put("name", name);
        map.put("type", type.name());
        
        List<String> actionsList = new ArrayList<>(actions);
        actionsList.sort(String::compareToIgnoreCase);
        map.put("actions", actionsList);
        
        switch (type) {
            case EQUIPMENT:
            case ITEM:
                if (!items.isEmpty()) {
                    List<String> sortedItems = new ArrayList<>(items);
                    sortedItems.sort(String::compareToIgnoreCase);
                    map.put("item", sortedItems);
                }
                break;
            case ENTITY:
                if (!entities.isEmpty()) {
                    List<String> sortedEntities = new ArrayList<>(entities);
                    sortedEntities.sort(String::compareToIgnoreCase);
                    map.put("entity", sortedEntities);
                }
                break;
            case COMMAND:
                if (!commands.isEmpty()) {
                    List<String> sortedCommands = new ArrayList<>(commands);
                    sortedCommands.sort(String::compareToIgnoreCase);
                    map.put("command", sortedCommands);
                }
                break;
        }
        
        map.put("time", timeSeconds);
        map.put("default", isDefault);
        
        return map;
    }
    
    public String getName() {
        return name;
    }
    
    public RestrictionType getType() {
        return type;
    }
    
    public Set<String> getActions() {
        return new HashSet<>(actions);
    }
    
    public List<String> getItems() {
        return new ArrayList<>(items);
    }
    
    public List<String> getEntities() {
        return new ArrayList<>(entities);
    }
    
    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }
    
    public int getTimeSeconds() {
        return timeSeconds;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public enum RestrictionType {
        EQUIPMENT,
        ITEM,
        ENTITY,
        COMMAND
    }
}
