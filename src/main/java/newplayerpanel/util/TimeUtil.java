package newplayerpanel.util;

public class TimeUtil {
    
    public static String formatTime(long seconds) {
        if (seconds == -1) return "перманентно";
        if (seconds < 60) return seconds + " сек";
        if (seconds < 3600) return (seconds / 60) + " мин";
        return (seconds / 3600) + " ч " + ((seconds % 3600) / 60) + " мин";
    }
}

