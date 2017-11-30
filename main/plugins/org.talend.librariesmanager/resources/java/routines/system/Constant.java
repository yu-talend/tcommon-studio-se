package routines.system;

/**
 * store some global constant
 * 
 * @author Administrator
 *
 */
public class Constant {

    /**
     * the default pattern for date parse and format
     */
    public static final String dateDefaultPattern = "dd-MM-yyyy";

    /**
     * the default user agent string for AWS and Azure components
     */
    public static String getUserAgent(String version_studio) {
        return  "APN/1.0 Talend/" + version_studio + " Studio/" + version_studio;
    }
    
    /**
     * the default user agent string for GCS components
     */
    public static String getUserAgentGCS(String version_studio) {
        return "Studio/" + version_studio + " (GPN:Talend) DataIntegration/" + version_studio + " Jets3t/0.9.1";
    }
}
