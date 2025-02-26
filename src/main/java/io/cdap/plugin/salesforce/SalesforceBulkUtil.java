/*
 * Copyright © 2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.salesforce;

import com.google.common.base.Preconditions;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.CSVReader;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import io.cdap.plugin.salesforce.plugin.source.batch.util.SalesforceSourceConstants;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Class which provides functions to submit jobs to bulk api and read resulting batches
 */
public final class SalesforceBulkUtil {
  private static final Logger LOG = LoggerFactory.getLogger(SalesforceBulkUtil.class);


  /**
   * Create a new job using the Bulk API.
   *
   * @return The JobInfo for the new job.
   * @throws AsyncApiException if there is an issue creating the job
   */
  public static JobInfo createJob(BulkConnection bulkConnection,
                                  String sObject, OperationEnum operationEnum,
                                  @Nullable String externalIdField) throws AsyncApiException {
    JobInfo job = new JobInfo();
    job.setObject(sObject);
    job.setOperation(operationEnum);
    job.setConcurrencyMode(ConcurrencyMode.Parallel);
    job.setContentType(ContentType.CSV);
    if (externalIdField != null) {
      job.setExternalIdFieldName(externalIdField);
    }

    job = bulkConnection.createJob(job);
    Preconditions.checkState(job.getId() != null, "Couldn't get job ID. There was a problem in creating the " +
      "batch job");
    return bulkConnection.getJobStatus(job.getId());
  }

  /**
   * Close a job in Salesforce
   *
   * @param bulkConnection bulk connection instance
   * @param jobId a job id
   * @throws AsyncApiException  if there is an issue creating the job
   */
  public static void closeJob(BulkConnection bulkConnection, String jobId) throws AsyncApiException {
    JobInfo job = new JobInfo();
    job.setId(jobId);
    job.setState(JobStateEnum.Closed);
    bulkConnection.updateJob(job);
  }

  /**
   * Gets the results of the insert operation for every batch and checks them for errors.
   *
   * @param bulkConnection bulk connection instance
   * @param job a Salesforce job
   * @param batchInfoList a list of batches to check
   * @param ignoreFailures if true, unsuccessful row insertions do not cause an exception
   * @throws AsyncApiException if there is an issue checking for batch results
   * @throws IOException reading csv from Salesforce failed
   */
  public static void checkResults(BulkConnection bulkConnection, JobInfo job,
                                  List<BatchInfo> batchInfoList, boolean ignoreFailures)
    throws AsyncApiException, IOException {

    for (BatchInfo batchInfo : batchInfoList) {
      /*
      The response is a CSV with the following headers:
      Id,Success,Created,Error
       */
      CSVReader rdr = new CSVReader(bulkConnection.getBatchResultStream(job.getId(), batchInfo.getId()));
      List<String> resultHeader = rdr.nextRecord();
      int resultCols = resultHeader.size();

      List<String> row;
      while ((row = rdr.nextRecord()) != null) {
        Map<String, String> resultInfo = new HashMap<>();
        for (int i = 0; i < resultCols; i++) {
          resultInfo.put(resultHeader.get(i), row.get(i));
        }
        boolean success = Boolean.parseBoolean(resultInfo.get("Success"));
        if (!success) {
          String error = resultInfo.get("Error");
          String errorMessage = String.format("Failed to create row with error: '%s'. BatchId='%s'",
                                              error, batchInfo.getId());
          if (ignoreFailures) {
            LOG.error(errorMessage);
          } else {
            throw new RuntimeException(errorMessage);
          }
        }
      }
    }
  }

  /**
   * Wait for a job to complete by polling the Bulk API.
   *
   * @param bulkConnection BulkConnection used to check results.
   * @param job The job awaiting completion.
   * @param batchInfoList List of batches for this job.
   */
  public static void awaitCompletion(BulkConnection bulkConnection, JobInfo job,
                                     List<BatchInfo> batchInfoList) {
    Set<String> incomplete = batchInfoList
      .stream()
      .map(BatchInfo::getId)
      .collect(Collectors.toSet());

    Awaitility.await()
      .atMost(SalesforceSourceConstants.GET_BATCH_WAIT_TIME_SECONDS, TimeUnit.SECONDS)
      .pollInterval(SalesforceSourceConstants.GET_BATCH_RESULTS_SLEEP_MS, TimeUnit.MILLISECONDS)
      .until(() -> {
        BatchInfo[] statusList =
          bulkConnection.getBatchInfoList(job.getId()).getBatchInfo();

        for (BatchInfo b : statusList) {
          if (b.getState() == BatchStateEnum.Failed) {
            throw new BulkAPIBatchException("Batch failed", b);
          } else if (b.getState() == BatchStateEnum.Completed) {
            incomplete.remove(b.getId());
          }
        }

        return incomplete.isEmpty();
      });
  }
}
