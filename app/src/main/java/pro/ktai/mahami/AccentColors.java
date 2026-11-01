package pro.ktai.mahami;

/** ألوان الثيمات — تُزامن مع الواجهة */
public class AccentColors {
    public static int statusBar(String accent, boolean dark) {
        if (dark) {
            switch (accent == null ? "teal" : accent) {
                case "blue":   return 0xFF121417;
                case "violet": return 0xFF121417;
                case "rose":   return 0xFF121417;
                case "amber":  return 0xFF121417;
                default:       return 0xFF121417;
            }
        }
        switch (accent == null ? "teal" : accent) {
            case "blue":   return 0xFFF4F6F5;
            case "violet": return 0xFFF4F6F5;
            case "rose":   return 0xFFF4F6F5;
            case "amber":  return 0xFFF4F6F5;
            default:       return 0xFFF4F6F5;
        }
    }
}
