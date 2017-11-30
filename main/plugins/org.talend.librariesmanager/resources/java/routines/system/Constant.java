package routines.system;

/**
 * store some global constant
 * 
 * @author Administrator
 *
 */
public abstract class Constant {

    private static final String version_studio = org.talend.commons.utils.VersionUtils.getDisplayVersion();
    
    /**
     * the default pattern for date parse and format
     */
    public static final String dateDefaultPattern = "dd-MM-yyyy";

    /**
     * the default user agent string for AWS and Azure components
     */
    public static final String TALEND_USER_AGENT = "APN/1.0 Talend/" + version_studio + " Studio/" + version_studio;
    
    /**
     * the default user agent string for GCS components
     */
    public static final String TALEND_USER_AGENT_GCS = "Studio/" + version_studio + " (GPN:Talend) DataIntegration/" + version_studio + " Jets3t/0.9.1";
}
