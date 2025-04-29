import java.util.Map;

public class EnvCheck {
    public static void main(String[] args) {
        System.out.println("==== Checking Environment Variables ====");
        
        // Check specific variables
        System.out.println("SPOTIFY_CLIENT_ID: " + (System.getenv("SPOTIFY_CLIENT_ID") != null ? "FOUND" : "MISSING"));
        System.out.println("SPOTIFY_CLIENT_SECRET: " + (System.getenv("SPOTIFY_CLIENT_SECRET") != null ? "FOUND" : "MISSING"));
        
        // Print all environment variables for debugging
        System.out.println("\n==== All Environment Variables ====");
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            String value = env.get(key);
            // Mask sensitive values
            if (key.contains("SECRET") || key.contains("KEY") || key.contains("TOKEN") || key.contains("PASSWORD")) {
                value = "[REDACTED]";
            }
            System.out.println(key + ": " + value);
        }
    }
}