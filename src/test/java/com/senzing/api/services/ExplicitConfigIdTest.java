package com.senzing.api.services;

import com.senzing.api.model.SzConfigResponse;
import com.senzing.api.model.SzDataSourcesResponse;
import com.senzing.api.model.SzErrorResponse;
import com.senzing.api.server.SzApiServer;
import com.senzing.api.server.SzApiServerOptions;
import com.senzing.g2.engine.*;
import com.senzing.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;

import static com.senzing.api.model.SzHttpMethod.*;
import static com.senzing.util.LoggingUtilities.formatError;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class ExplicitConfigIdTest extends AutoReinitializeTest
{
  /**
   * Sets the desired options for the {@link SzApiServer} during server
   * initialization.
   *
   * @param options The {@link SzApiServerOptions} to initialize.
   */
  protected void initializeServerOptions(SzApiServerOptions options) {
    super.initializeServerOptions(options);
    if (NATIVE_API_AVAILABLE) {
      Result<Long> result = new Result<>();
      int returnCode = this.configMgrApi.getDefaultConfigID(result);
      if (returnCode != 0) {
        fail(formatError("G2ConfigMgr.getDefaultConfigID",
                         this.configMgrApi));
      }
      options.setConfigurationId(result.getValue());
    }
  }

  @Test public void getDataSourcesTest() {
    this.assumeNativeApiAvailable();
    final String newDataSource = "FOO";
    String  uriText = this.formatServerUri("data-sources");
    UriInfo uriInfo = this.newProxyUriInfo(uriText);

    this.addDataSource(newDataSource);

    // now sleep for 1 second longer than the config refresh period
    try {
      Thread.sleep(SzApiServer.CONFIG_REFRESH_PERIOD + 1000L);
    } catch (InterruptedException ignore) {
      fail("Interrupted while sleeping and waiting for config refresh.");
    }

    // now retry the request to get the data sources
    long before = System.currentTimeMillis();
    SzDataSourcesResponse response
        = this.configServices.getDataSources(false, uriInfo);
    response.concludeTimers();
    long after = System.currentTimeMillis();

    synchronized (this.expectedDataSources) {
      this.validateDataSourcesResponse(response,
                                       before,
                                       after,
                                       null,
                                       INITIAL_DATA_SOURCES);
    }
  }

  @Test public void getCurrentConfigTest() {
    final String newDataSource = "PHOO";
    this.assumeNativeApiAvailable();
    String  uriText = this.formatServerUri("config/current");
    UriInfo uriInfo = this.newProxyUriInfo(uriText);

    this.addDataSource(newDataSource);

    // now sleep for 1 second longer than the config refresh period
    try {
      Thread.sleep(SzApiServer.CONFIG_REFRESH_PERIOD + 1000L);
    } catch (InterruptedException ignore) {
      fail("Interrupted while sleeping and waiting for config refresh.");
    }

    long before = System.currentTimeMillis();
    SzConfigResponse response
        = this.configServices.getCurrentConfig(uriInfo);
    response.concludeTimers();
    long after = System.currentTimeMillis();

    synchronized (this.expectedDataSources) {
      this.validateConfigResponse(response,
                                  uriText,
                                  before,
                                  after,
                                  INITIAL_DATA_SOURCES);
    }
  }


  @Test public void postRecordTest() {
    final String newDataSource = "FOOX";
    this.assumeNativeApiAvailable();
    String  uriText = this.formatServerUri(
        "data-sources/" + newDataSource + "/records");
    UriInfo uriInfo = this.newProxyUriInfo(uriText);

    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("NAME_FIRST", "Joe");
    job.add("NAME_LAST", "Schmoe");
    job.add("PHONE_NUMBER", "702-555-1212");
    job.add("ADDR_FULL", "101 Main Street, Las Vegas, NV 89101");
    JsonObject  jsonObject  = job.build();
    String      jsonText    = JsonUtils.toJsonText(jsonObject);

    // add the data source (so it is there for retry)
    this.addDataSource(newDataSource);

    // now add the record -- this should succeed on retry
    long before = System.currentTimeMillis();
    try {
      this.entityDataServices.loadRecord(
          newDataSource, null, uriInfo, jsonText);
      fail("Expected for data source \"" + newDataSource
           + "\" to trigger a NotFoundException");
    } catch (NotFoundException expected) {
      SzErrorResponse response
          = (SzErrorResponse) expected.getResponse().getEntity();
      response.concludeTimers();
      long after = System.currentTimeMillis();

      this.validateBasics(
          response, 404, POST, uriText, before, after);
    }
  }

  @Test public void putRecordTest() {
    final String recordId = "ABC123";
    final String newDataSource = "PHOOX";

    this.assumeNativeApiAvailable();
    String  uriText = this.formatServerUri(
        "data-sources/" + newDataSource + "/records/" + recordId);
    UriInfo uriInfo = this.newProxyUriInfo(uriText);

    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("NAME_FIRST", "John");
    job.add("NAME_LAST", "Doe");
    job.add("PHONE_NUMBER", "818-555-1313");
    job.add("ADDR_FULL", "100 Main Street, Los Angeles, CA 90012");
    JsonObject  jsonObject  = job.build();
    String      jsonText    = JsonUtils.toJsonText(jsonObject);

    // add the data source (so it is there for retry)
    this.addDataSource(newDataSource);

    // now add the record -- this should succeed on retry
    long before = System.currentTimeMillis();
    try {
      this.entityDataServices.loadRecord(
          newDataSource, recordId, null, uriInfo, jsonText);
      fail("Expected for data source \"" + newDataSource
               + "\" to trigger a NotFoundException");
    } catch (NotFoundException expected) {
      SzErrorResponse response
          = (SzErrorResponse) expected.getResponse().getEntity();
      response.concludeTimers();
      long after = System.currentTimeMillis();

      this.validateBasics(
          response, 404, PUT, uriText, before, after);
    }
  }
}