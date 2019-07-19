package com.senzing.api.services;

import com.senzing.g2.engine.G2Config;
import com.senzing.g2.engine.G2Engine;
import com.senzing.g2.engine.G2Product;
import com.senzing.util.AccessToken;
import com.senzing.util.WorkerThreadPool;

import java.util.Set;

/**
 * This interface abstracts the various functions required by the API services
 * to run.
 */
public interface SzApiProvider {
  /**
   * Until we come up with a better method, use a simple factory to make
   * the global provider instance available.
   */
  class Factory {
    /**
     * The installed {@link SzApiProvider}.
     */
    private static SzApiProvider PROVIDER = null;

    /**
     * The {@link AccessToken} to authorizing uninstalling the provider.
     */
    private static AccessToken ACCESS_TOKEN = null;

    /**
     * Install a provider.  This fails if a provider is already installed.
     *
     * @param provider The non-null provider to install.
     *
     * @throws NullPointerException If the specified parameter is <tt>null</tt>.
     *
     * @throws IllegalStateException If a provider is already installed.
     */
    public static synchronized AccessToken installProvider(SzApiProvider provider) {
      if (provider == null) {
        throw new NullPointerException(
            "The specified provider cannot be null.");
      }
      if (PROVIDER != null) {
        throw new IllegalStateException(
            "An SzApiProvider is already installed: "
            + PROVIDER.getClass().getName());
      }
      PROVIDER = provider;
      ACCESS_TOKEN = new AccessToken();
      return ACCESS_TOKEN;
    }

    /**
     * Uninstalls the provider.  This does nothing if no provider is installed.
     *
     * @param token The {@link AccessToken} with which the provider was
     *              installed.
     *
     * @throws IllegalStateException If the specifid token is not the expected
     *                               token.
     */
    public static synchronized void uninstallProvider(AccessToken token)
      throws IllegalStateException
    {
      if (ACCESS_TOKEN != null && ACCESS_TOKEN != token) {
        throw new IllegalStateException(
            "The specified access token was not the expected access token.");
      }
      PROVIDER = null;
      ACCESS_TOKEN = null;
    }

    /**
     * Returns the installed {@link SzApiProvider}.  If no provider is installed
     * then an exception is thrown.
     *
     * @return The installed {@link SzApiProvider}.
     *
     * @throws IllegalStateException If no provider is installed.
     */
    public static synchronized SzApiProvider getProvider()
      throws IllegalStateException
    {
      if (PROVIDER == null) {
        IllegalStateException e = new IllegalStateException(
            "No SzApiProvider has been installed.");
        e.printStackTrace();
        throw e;
      }
      return PROVIDER;
    }
  }

  /**
   * Returns the associated {@link G2Product} API implementation.
   *
   * @return The associated {@link G2Product} API implementation.
   */
  G2Product getProductApi();

  /**
   * Returns the associated {@link G2Engine} API implementation.
   *
   * @return The associated {@link G2Engine} API implementation.
   */
  G2Engine getEngineApi();

  /**
   * Returns the associated {@link G2Config} API implementation.
   *
   * @return The associated {@link G2Config} API implementation.
   */
  G2Config getConfigApi();

  /**
   * Executes the specified task with the proper thread for utilizing the
   * various G2 API implementations.
   *
   * @param task The Task to execute.
   * @param <T> The return value for the task.
   * @param <E> The exception type that may be thrown by the task.
   * @return Returns an instance of type <tt>T</tt> as obtained from the
   *         specified task.
   * @throws E If the specified task fails with an exception.
   */
  <T, E extends Exception> T executeInThread(WorkerThreadPool.Task<T, E> task)
      throws E;

  /**
   * Gets the <b>unmodifiable</b> {@Link Set} of Data Source codes that
   * are configured.
   *
   * @return The <b>unmodifiable</b> {@Link Set} of Data Source codes that
   *         are configured.
   */
  Set<String> getDataSources();

  /**
   * Gets the attribute class associated with a feature type code.
   *
   * @param featureType The feature type code.
   *
   * @return The attribute class for the specified feature type, or
   *         <tt>null</tt> if the specified feature type code is <tt>null</tt>
   *         or not recognized.
   */
  String getAttributeClassForFeature(String featureType);
}