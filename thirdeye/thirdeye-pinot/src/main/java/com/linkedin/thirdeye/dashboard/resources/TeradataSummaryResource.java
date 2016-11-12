package com.linkedin.thirdeye.dashboard.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.thirdeye.client.ThirdEyeCacheRegistry;
import com.linkedin.thirdeye.client.diffsummary.Cube;
import com.linkedin.thirdeye.client.diffsummary.Dimensions;
import com.linkedin.thirdeye.client.diffsummary.OLAPDataBaseClient;
import com.linkedin.thirdeye.client.diffsummary.teradata.QueryTera;
import com.linkedin.thirdeye.client.diffsummary.teradata.TeradataThirdEyeSummaryClient;
import com.linkedin.thirdeye.dashboard.views.diffsummary.Summary;
import com.linkedin.thirdeye.dashboard.views.diffsummary.SummaryResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path(value = "/teradashboard")
public class TeradataSummaryResource {
  private static final ThirdEyeCacheRegistry CACHE_REGISTRY_INSTANCE = ThirdEyeCacheRegistry.getInstance();

  private static final Logger LOG = LoggerFactory.getLogger(TeradataSummaryResource.class);
  private static final String DEFAULT_TIMEZONE_ID = "UTC";
  private static final String DEFAULT_TOP_DIMENSIONS = "3";
  private static final String DEFAULT_HIERARCHIES = "[]";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String DEFAULT_ONE_SIDE_ERROR = "false";

  @GET
  @Path(value = "/summary/autoDimensionOrder")
  @Produces(MediaType.APPLICATION_JSON)
  public String buildSummary(@QueryParam("table") String table,
      @QueryParam("currentStart") Long currentStartInclusive,
      @QueryParam("currentEnd") Long currentEndExclusive,
      @QueryParam("baselineStart") Long baselineStartInclusive,
      @QueryParam("baselineEnd") Long baselineEndExclusive,
      @QueryParam("dimensions") String groupByDimensions,
      @QueryParam("summarySize") int summarySize,
      @QueryParam("topDimensions") @DefaultValue(DEFAULT_TOP_DIMENSIONS) int topDimensions,
      @QueryParam("hierarchies") @DefaultValue(DEFAULT_HIERARCHIES) String hierarchiesPayload,
      @QueryParam("oneSideError") @DefaultValue(DEFAULT_ONE_SIDE_ERROR) boolean doOneSideError,
      @QueryParam("timeZone") @DefaultValue(DEFAULT_TIMEZONE_ID) String timeZone) throws Exception {
    if (summarySize < 1) summarySize = 1;

    SummaryResponse response = null;
    QueryTera queryTera = CACHE_REGISTRY_INSTANCE.getQueryTera();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    try {
      OLAPDataBaseClient olapClient = new TeradataThirdEyeSummaryClient(queryTera, executorService);
      olapClient.setCollection(table);
      olapClient.setCurrentStartInclusive(new DateTime(currentStartInclusive, DateTimeZone.forID(timeZone)));
      olapClient.setCurrentEndExclusive(new DateTime(currentEndExclusive, DateTimeZone.forID(timeZone)));
      olapClient.setBaselineStartInclusive(new DateTime(baselineStartInclusive, DateTimeZone.forID(timeZone)));
      olapClient.setBaselineEndExclusive(new DateTime(baselineEndExclusive, DateTimeZone.forID(timeZone)));

      Dimensions dimensions;
      if (groupByDimensions == null || groupByDimensions.length() == 0 || groupByDimensions.equals("undefined")) {
        List<String> allColumns = queryTera.getColumnNames(table).stream()
            .filter(
                e -> (!e.toUpperCase().equals("METRIC")) && (!e.toUpperCase().equals("DATETIME_FLAG")))
            .collect(Collectors.toList());
        dimensions = new Dimensions(allColumns);
      } else {
        dimensions = new Dimensions(Arrays.asList(groupByDimensions.trim().split(",")));
      }

      List<List<String>> hierarchies =
          OBJECT_MAPPER.readValue(hierarchiesPayload, new TypeReference<List<List<String>>>() {
          });

      Cube cube = new Cube();
      cube.buildWithAutoDimensionOrder(olapClient, dimensions, topDimensions, hierarchies);

      Summary summary = new Summary(cube);
      response = summary.computeSummary(summarySize, doOneSideError, topDimensions);

    } catch (Exception e) {
      LOG.error("Exception while generating difference summary", e);
      response = SummaryResponse.buildNotAvailableResponse();
    } finally {
      if (!executorService.isShutdown()) {
        executorService.shutdown();
      }
    }
    return OBJECT_MAPPER.writeValueAsString(response);
  }

  @GET
  @Path(value = "/summary/manualDimensionOrder")
  @Produces(MediaType.APPLICATION_JSON)
  public String buildSummaryManualDimensionOrder(@QueryParam("table") String table,
      @QueryParam("metric") String metric,
      @QueryParam("currentStart") Long currentStartInclusive,
      @QueryParam("currentEnd") Long currentEndExclusive,
      @QueryParam("baselineStart") Long baselineStartInclusive,
      @QueryParam("baselineEnd") Long baselineEndExclusive,
      @QueryParam("dimensions") String groupByDimensions,
      @QueryParam("summarySize") int summarySize,
      @QueryParam("oneSideError") @DefaultValue(DEFAULT_ONE_SIDE_ERROR) boolean doOneSideError,
      @QueryParam("timeZone") @DefaultValue(DEFAULT_TIMEZONE_ID) String timeZone) throws Exception {
    if (summarySize < 1) summarySize = 1;

    SummaryResponse response = null;
    QueryTera queryTera = CACHE_REGISTRY_INSTANCE.getQueryTera();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    try {
      OLAPDataBaseClient olapClient = new TeradataThirdEyeSummaryClient(queryTera, executorService);
      olapClient.setCollection(table);
      olapClient.setCurrentStartInclusive(new DateTime(currentStartInclusive, DateTimeZone.forID(timeZone)));
      olapClient.setCurrentEndExclusive(new DateTime(currentEndExclusive, DateTimeZone.forID(timeZone)));
      olapClient.setBaselineStartInclusive(new DateTime(baselineStartInclusive, DateTimeZone.forID(timeZone)));
      olapClient.setBaselineEndExclusive(new DateTime(baselineEndExclusive, DateTimeZone.forID(timeZone)));

      List<String> allDimensions;
      if (groupByDimensions == null || groupByDimensions.length() == 0 || groupByDimensions.equals("undefined")) {
        allDimensions = queryTera.getColumnNames(table).stream()
            .filter(
                e -> (!e.toUpperCase().equals("METRIC")) && (!e.toUpperCase().equals("DATETIME_FLAG")))
            .collect(Collectors.toList());
      } else {
        allDimensions = Arrays.asList(groupByDimensions.trim().split(","));
      }
      if (allDimensions.size() > Integer.parseInt(DEFAULT_TOP_DIMENSIONS)) {
        allDimensions = allDimensions.subList(0, Integer.parseInt(DEFAULT_TOP_DIMENSIONS));
      }
      Dimensions dimensions = new Dimensions(allDimensions);


      Cube cube = new Cube();
      cube.buildWithManualDimensionOrder(olapClient, dimensions);

      Summary summary = new Summary(cube);
      response = summary.computeSummary(summarySize, doOneSideError);
      response.setMetricName(metric);
    } catch (Exception e) {
      LOG.error("Exception while generating difference summary", e);
      response = SummaryResponse.buildNotAvailableResponse();
    } finally {
      if (!executorService.isShutdown()) {
        executorService.shutdown();
      }
    }
    return OBJECT_MAPPER.writeValueAsString(response);
  }
}

