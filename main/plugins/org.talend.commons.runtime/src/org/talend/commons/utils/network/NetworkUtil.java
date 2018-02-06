// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.commons.utils.network;

import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.talend.commons.runtime.utils.io.FileCopyUtils;

/**
 * ggu class global comment. Detailled comment
 */
public class NetworkUtil {

    private static final String[] windowsCommand = { "ipconfig", "/all" }; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String[] linuxCommand = { "/sbin/ifconfig", "-a" }; //$NON-NLS-1$ //$NON-NLS-2$

    private static final Pattern macPattern = Pattern
            .compile(".*((:?[0-9a-f]{2}[-:]){5}[0-9a-f]{2}).*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String TALEND_DISABLE_INTERNET = "talend.disable.internet";//$NON-NLS-1$

    public static boolean isNetworkValid() {
        String disableInternet = System.getProperty(TALEND_DISABLE_INTERNET);
        if ("true".equals(disableInternet)) { //$NON-NLS-1$
            return false;
        }
        try {
            URL url = new URL("https://www.talend.com"); //$NON-NLS-1$
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            conn.setRequestMethod("HEAD"); //$NON-NLS-1$
            String strMessage = conn.getResponseMessage();
            if (strMessage.compareTo("Not Found") == 0) { //$NON-NLS-1$
                return false;
            }
            if (strMessage.equals("OK")) { //$NON-NLS-1$
                return true;
            }
            conn.disconnect();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static Authenticator getDefaultAuthenticator() {
        try {
            Field theAuthenticatorField = Authenticator.class.getDeclaredField("theAuthenticator");
            if (theAuthenticatorField != null) {
                theAuthenticatorField.setAccessible(true);
                Authenticator setAuthenticator = (Authenticator) theAuthenticatorField.get(null);
                return setAuthenticator;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void loadAuthenticator() {
        // get parameter from System.properties.
        if (Boolean.getBoolean("http.proxySet")) {//$NON-NLS-1$
            // authentification for the url by using username and password
            Authenticator.setDefault(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String httpProxyUser = System.getProperty("http.proxyUser"); //$NON-NLS-1$
                    String httpProxyPassword = System.getProperty("http.proxyPassword"); //$NON-NLS-1$
                    String httpsProxyUser = System.getProperty("https.proxyUser"); //$NON-NLS-1$
                    String httpsProxyPassword = System.getProperty("https.proxyPassword"); //$NON-NLS-1$
                    String proxyUser = null;
                    char[] proxyPassword = new char[0];
                    if (StringUtils.isNotEmpty(httpProxyUser)) {
                        proxyUser = httpProxyUser;
                        if (StringUtils.isNotEmpty(httpProxyPassword)) {
                            proxyPassword = httpProxyPassword.toCharArray();
                        }
                    } else if (StringUtils.isNotEmpty(httpsProxyUser)) {
                        proxyUser = httpsProxyUser;
                        if (StringUtils.isNotEmpty(httpsProxyPassword)) {
                            proxyPassword = httpsProxyPassword.toCharArray();
                        }
                    }
                    return new PasswordAuthentication(proxyUser, proxyPassword);
                }

            });
        } else {
            Authenticator.setDefault(null);
        }
    }

    public static void updateSvnkitConfigureFile(String srcFilePath, String destFilePath) {
        // SVNFileUtil getSystemApplicationDataPath C:\ProgramData\\Application Data
        // Note:ProgramData:Starting with Windows 10,this setting can no longer be used in provisioning packages.
        String osName = System.getProperty("os.name");//$NON-NLS-1$
        String osNameLC = osName == null ? null : osName.toLowerCase();
        boolean windows = osName != null && osNameLC.indexOf("windows") >= 0;//$NON-NLS-1$
        if (windows && Boolean.getBoolean("http.proxySet")) {//$NON-NLS-1$
            FileCopyUtils.copy(srcFilePath + "\\servers", destFilePath + "\\servers");//$NON-NLS-1$//$NON-NLS-2$
        }
    }

    /**
     * encode url
     * 
     * @param urlStr url not encoded yet!
     * @return
     * @throws Exception
     */
    public static URL encodeUrl(String urlStr) throws Exception {
        try {
            // String decodedURL = URLDecoder.decode(urlStr, "UTF-8"); //$NON-NLS-1$
            URL url = new URL(urlStr);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(),
                    url.getRef());
            return uri.toURL();
        } catch (Exception e) {
            throw e;
        }
    }

    public static boolean isSelfAddress(String addr) {
        if (addr == null || addr.isEmpty()) {
            return false; // ?
        }

        try {
            final InetAddress sourceAddress = InetAddress.getByName(addr);
            if (sourceAddress.isLoopbackAddress()) {
                // final String hostAddress = sourceAddress.getHostAddress();
                // // if addr is localhost, will be 127.0.0.1 also
                // if (hostAddress.equals("127.0.0.1") || hostAddress.equals("localhost") ) {
                return true;
                // }
            } else {
                // check all ip configs
                InetAddress curAddr = null;
                Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface ni = netInterfaces.nextElement();
                    Enumeration<InetAddress> address = ni.getInetAddresses();
                    while (address.hasMoreElements()) {
                        curAddr = address.nextElement();
                        if (addr.equals(curAddr.getHostAddress())) {
                            return true;
                        }
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }
}
