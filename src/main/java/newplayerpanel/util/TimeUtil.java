package newplayerpanel.util;

import newplayerpanel.messages.MessageManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");
    
    public static String formatTimeLocalized(long seconds, MessageManager messageManager) {
        if (seconds == -1) {
            return messageManager.get("time-permanent");
        }
        if (seconds < 60) {
            return messageManager.get("time-seconds", "time", seconds);
        }
        if (seconds < 3600) {
            return messageManager.get("time-minutes", "time", seconds / 60);
        }
        if (seconds < 86400) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return messageManager.get("time-hours", "hours", hours, "minutes", minutes);
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        return messageManager.get("time-days", "days", days, "hours", hours);
    }
    
    
    public static long parseTimeString(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }
        
        Matcher matcher = TIME_PATTERN.matcher(timeString.trim());
        long totalSeconds = 0;
        boolean found = false;
        
        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "s":
                    totalSeconds += value;
                    break;
                case "m":
                    totalSeconds += value * 60;
                    break;
                case "h":
                    totalSeconds += value * 3600;
                    break;
                case "d":
                    totalSeconds += value * 86400;
                    break;
                case "w":
                    totalSeconds += value * 604800;
                    break;
                case "M":
                    totalSeconds += value * 2592000;
                    break;
                case "y":
                    totalSeconds += value * 31536000;
                    break;
                default:
                    return -1;
            }
        }
        
        return found ? totalSeconds : -1;
    }
    
    public static long parseTimeInput(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }
        
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return parseTimeString(input);
        }
    }
}
