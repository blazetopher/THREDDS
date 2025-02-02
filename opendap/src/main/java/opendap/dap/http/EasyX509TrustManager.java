/*
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package opendap.dap.http;


import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

//import com.sun.net.ssl.TrustManagerFactory;
//import com.sun.net.ssl.TrustManager;
//import com.sun.net.ssl.X509TrustManager;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * <p/>
 * EasyX509TrustManager unlike default {@link X509TrustManager} accepts
 * self-signed certificates.
 * </p>
 * <p/>
 * This trust manager SHOULD NOT be used for productive systems
 * due to security reasons, unless it is a concious decision and
 * you are perfectly aware of security implications of accepting
 * self-signed certificates
 * </p>
 *
 * @author <a href="mailto:adrian.sutton@ephox.com">Adrian Sutton</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *         <p/>
 *         <p/>
 *         DISCLAIMER: AbstractHttpClient developers DO NOT actively support this component.
 *         The component is provided as a reference material, which may be inappropriate
 *         for use without additional customization.
 *         </p>
 */

public class EasyX509TrustManager implements X509TrustManager {
  private X509TrustManager standardTrustManager = null;

  /**
   * Log object for this class.
   */
/* FIX
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EasyX509TrustManager.class);
  // private static final Log LOG = LogFactory.getLog(EasyX509TrustManager.class);
*/

  /**
   * Constructor for EasyX509TrustManager.
   */
  public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
    super();
    TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
    factory.init(keystore);
    TrustManager[] trustmanagers = factory.getTrustManagers();
    if (trustmanagers.length == 0) {
      throw new NoSuchAlgorithmException("SunX509 trust manager not supported");
    }
    this.standardTrustManager = (X509TrustManager) trustmanagers[0];
  }

  /**
   * see com.sun.net.ssl.X509TrustManager#getAcceptedIssuers()
   */
  public X509Certificate[] getAcceptedIssuers() {
    return this.standardTrustManager.getAcceptedIssuers();
  }

  /**
   * see com.sun.net.ssl.X509TrustManager#isClientTrusted(X509Certificate[])
   */
  public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    this.standardTrustManager.checkClientTrusted(certificates, authType);
  }

  /**
   * see com.sun.net.ssl.X509TrustManager#isServerTrusted(X509Certificate[])
   */
  public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
/*
    if ((certificates != null) && logger.isDebugEnabled()) {
      logger.debug("Server certificate chain:");
      for (int i = 0; i < certificates.length; i++) {
        logger.debug("X509Certificate[" + i + "]=" + certificates[i]);
      }
    }
*/

    if ((certificates != null) && (certificates.length == 1)) {
      X509Certificate certificate = certificates[0];
        certificate.checkValidity();
    } else {
      this.standardTrustManager.checkServerTrusted(certificates, authType);
    }
      return;
      
  }

}