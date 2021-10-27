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

package io.cdap.plugin.salesforce.plugin.source.batch;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.sforce.async.BulkConnection;
import io.cdap.cdap.api.data.batch.InputFormatProvider;
import io.cdap.plugin.salesforce.SalesforceConstants;
import io.cdap.plugin.salesforce.plugin.OAuthInfo;
import io.cdap.plugin.salesforce.plugin.source.batch.util.SalesforceSourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * InputFormatProvider used by cdap to provide configurations to mapreduce job
 */
public class SalesforceInputFormatProvider implements InputFormatProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SalesforceWideRecordReader.class);

  private static final Gson GSON = new Gson();

  private final Map<String, String> conf;

  public SalesforceInputFormatProvider(SalesforceBaseSourceConfig config,
                                       List<String> queries,
                                       Map<String, String> schemas,
                                       BulkConnection bulkConnection,
                                       String jobId,
                                       @Nullable String sObjectNameField) {
    try {
      ImmutableMap.Builder<String, String> configBuilder = new ImmutableMap.Builder<String, String>()
        .put(SalesforceSourceConstants.CONFIG_QUERIES, GSON.toJson(queries))
        .put(SalesforceSourceConstants.CONFIG_SCHEMAS, GSON.toJson(schemas))
        .put(SalesforceSourceConstants.PROPERTY_BULK_CONNECTION, GSON.toJson(bulkConnection))
        .put(SalesforceSourceConstants.PROPERTY_JOB_ID, jobId);
      OAuthInfo oAuthInfo = config.getOAuthInfo();
      if (oAuthInfo != null) {
        configBuilder
          .put(SalesforceConstants.CONFIG_OAUTH_TOKEN, oAuthInfo.getAccessToken())
          .put(SalesforceConstants.CONFIG_OAUTH_INSTANCE_URL, oAuthInfo.getInstanceURL());
      } else {
        configBuilder
          .put(SalesforceConstants.CONFIG_USERNAME, Objects.requireNonNull(config.getUsername()))
          .put(SalesforceConstants.CONFIG_PASSWORD, Objects.requireNonNull(config.getPassword()))
          .put(SalesforceConstants.CONFIG_CONSUMER_KEY, Objects.requireNonNull(config.getConsumerKey()))
          .put(SalesforceConstants.CONFIG_CONSUMER_SECRET, Objects.requireNonNull(config.getConsumerSecret()))
          .put(SalesforceConstants.CONFIG_LOGIN_URL, Objects.requireNonNull(config.getLoginUrl()));
      }

      if (sObjectNameField != null) {
        configBuilder.put(SalesforceSourceConstants.CONFIG_SOBJECT_NAME_FIELD, sObjectNameField);
      }

      if (config instanceof SalesforceSourceConfig) {
        SalesforceSourceConfig sourceConfig = (SalesforceSourceConfig) config;
        configBuilder
          .put(SalesforceSourceConstants.CONFIG_PK_CHUNK_ENABLE, String.valueOf(sourceConfig.getEnablePKChunk()))
          .put(SalesforceSourceConstants.CONFIG_CHUNK_SIZE, String.valueOf(sourceConfig.getChunkSize()))
          .put(SalesforceSourceConstants.CONFIG_CHUNK_PARENT, sourceConfig.getParent());
      }

      this.conf = configBuilder.build();
    } catch (Exception e) {
      LOG.error("Something went wrong in input format provider");
      LOG.error(e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public Map<String, String> getInputFormatConfiguration() {
    return conf;
  }

  @Override
  public String getInputFormatClassName() {
    return SalesforceInputFormat.class.getName();
  }
}
