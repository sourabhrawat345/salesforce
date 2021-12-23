/*
 * Copyright © 2022 Cask Data, Inc.
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
package io.cdap.plugin.salesforce.plugin.sink.batch;

import com.sforce.async.OperationEnum;
import com.sforce.ws.ConnectionException;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.plugin.PluginProperties;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.salesforce.InvalidConfigException;
import io.cdap.plugin.salesforce.SObjectDescriptor;
import io.cdap.plugin.salesforce.SObjectsDescribeResult;
import io.cdap.plugin.salesforce.plugin.OAuthInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.FieldSetter;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Tests for SalesforceSinkConfig
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SalesforceSinkConfig.class, SObjectDescriptor.class, SObjectsDescribeResult.class})
public class SalesforceSinkConfigTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  SalesforceSinkConfig salesforceSinkConfig;
  OAuthInfo oAuthInfo;

  @Before
  public void setUp() {
    oAuthInfo = new OAuthInfo("token", "https://d5j000001ufckeay.lightning.force.com");

    salesforceSinkConfig = new SalesforceSinkConfig("Reference Name", "42", "Client Secret",
      "username", "password", "https://login.salesforce.com/services/oauth2/token", "S Object", "Operation",
      "External Id Field",
      "Max Bytes Per Batch", "Max Records Per Batch", "An error occurred", "token", oAuthInfo);
  }

  @Test
  public void testConfig() {
    assertEquals("Reference Name", salesforceSinkConfig.referenceName);
    assertEquals("42", salesforceSinkConfig.getConsumerKey());
    assertEquals("External Id Field", salesforceSinkConfig.getExternalIdField());
    assertEquals("passwordtoken", salesforceSinkConfig.getPassword());
    assertEquals("https://login.salesforce.com/services/oauth2/token", salesforceSinkConfig.getLoginUrl());
    PluginProperties properties = salesforceSinkConfig.getProperties();
    PluginProperties rawProperties = salesforceSinkConfig.getRawProperties();
    assertEquals(properties, rawProperties);
    assertEquals("S Object", salesforceSinkConfig.getSObject());
    assertEquals("Client Secret", salesforceSinkConfig.getConsumerSecret());
    assertSame(oAuthInfo, salesforceSinkConfig.getOAuthInfo());
    assertEquals("username", salesforceSinkConfig.getUsername());
    assertEquals("Operation", salesforceSinkConfig.getOperation());
  }

  @Test
  public void testGetOperationEnum() {
    thrown.expect(InvalidConfigException.class);
    salesforceSinkConfig.getOperationEnum();
  }

  @Test
  public void testGetMaxBytesPerBatch() {
    thrown.expect(InvalidConfigException.class);
    salesforceSinkConfig.getMaxBytesPerBatch();
  }

  @Test
  public void testGetMaxRecordsPerBatch() {
    thrown.expect(InvalidConfigException.class);
    salesforceSinkConfig.getMaxRecordsPerBatch();
  }

  @Test
  public void testGetErrorHandling() {
    thrown.expect(InvalidConfigException.class);
    salesforceSinkConfig.getErrorHandling();
  }

  @Test
  public void testValidate() {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    Schema schema = Schema.of(Schema.LogicalType.DATE);
    thrown.expect(InvalidConfigException.class);
    salesforceSinkConfig.validate(schema, mockFailureCollector);
  }

  @Test
  public void testValidateWithZeroMaxBytesAndZeroMaxRecords() throws NoSuchFieldException {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    Schema schema = Schema.of(Schema.LogicalType.DATE);
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxBytesPerBatch"), "0");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxRecordsPerBatch"), "0");
    thrown.expect(ValidationException.class);
    salesforceSinkConfig.validate(schema, mockFailureCollector);
  }

  @Test
  public void testValidateSchemaWithNullFields() throws NoSuchFieldException, ConnectionException {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxBytesPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxRecordsPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("errorHandling"),
      ErrorHandling.SKIP.getValue());
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("operation"),
      OperationEnum.insert.name());
    Schema schema = Schema.recordOf("output");
    try {
      salesforceSinkConfig.validate(schema, mockFailureCollector);
    } catch (ValidationException e) {
      assertEquals(1, mockFailureCollector.getValidationFailures().size());
      assertEquals("Errors were encountered during validation. Sink schema must contain at least one field",
        e.getMessage());
    }
  }

  @Test
  public void testValidateSchemaWithInsertOperation() throws NoSuchFieldException, ConnectionException {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxBytesPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxRecordsPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("errorHandling"),
      ErrorHandling.SKIP.getValue());
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("operation"),
      OperationEnum.insert.name());
    mockStatic(SObjectDescriptor.class);
    mockStatic(SObjectsDescribeResult.class);
    SObjectsDescribeResult sObjectsDescribeResult = mock(SObjectsDescribeResult.class);
    SObjectDescriptor sObjectDescriptor = spy(new SObjectDescriptor("test", new ArrayList<>()));
    PowerMockito.when(SObjectDescriptor.fromName(any(), any())).thenReturn(sObjectDescriptor);
    PowerMockito.when(SObjectsDescribeResult.of(any(), anyString(), any())).thenReturn(sObjectsDescribeResult);
    Schema schema = Schema.recordOf("output",
      Schema.Field.of("Name", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("NumberOfEmployees", Schema.of(Schema.Type.INT)),
      Schema.Field.of("ShippingLatitude", Schema.of(Schema.Type.DOUBLE)),
      Schema.Field.of("ShippingLongitude", Schema.of(Schema.Type.DOUBLE))
    );

    salesforceSinkConfig.validate(schema, mockFailureCollector);
    assertEquals(5, mockFailureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateSchemaWithUpsertOperation() throws NoSuchFieldException, ConnectionException {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxBytesPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxRecordsPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("errorHandling"),
      ErrorHandling.SKIP.getValue());
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("operation"),
      OperationEnum.upsert.name());
    mockStatic(SObjectDescriptor.class);
    mockStatic(SObjectsDescribeResult.class);
    SObjectsDescribeResult sObjectsDescribeResult = mock(SObjectsDescribeResult.class);
    SObjectDescriptor sObjectDescriptor = spy(new SObjectDescriptor("test", new ArrayList<>()));
    PowerMockito.when(SObjectDescriptor.fromName(any(), any())).thenReturn(sObjectDescriptor);
    PowerMockito.when(SObjectsDescribeResult.of(any(), anyString(), any())).thenReturn(sObjectsDescribeResult);
    Schema schema = Schema.recordOf("output",
      Schema.Field.of("Name", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("NumberOfEmployees", Schema.of(Schema.Type.INT)),
      Schema.Field.of("ShippingLatitude", Schema.of(Schema.Type.DOUBLE)),
      Schema.Field.of("ShippingLongitude", Schema.of(Schema.Type.DOUBLE))
    );
    salesforceSinkConfig.validate(schema, mockFailureCollector);
    assertEquals(6, mockFailureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateSchemaWithUpdateOperation() throws NoSuchFieldException, ConnectionException {
    MockFailureCollector mockFailureCollector = new MockFailureCollector("Stage Name");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxBytesPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("maxRecordsPerBatch"), "1");
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("errorHandling"),
      ErrorHandling.SKIP.getValue());
    FieldSetter.setField(salesforceSinkConfig, SalesforceSinkConfig.class.getDeclaredField("operation"),
      OperationEnum.update.name());
    mockStatic(SObjectDescriptor.class);
    mockStatic(SObjectsDescribeResult.class);
    SObjectsDescribeResult sObjectsDescribeResult = mock(SObjectsDescribeResult.class);
    SObjectDescriptor sObjectDescriptor = spy(new SObjectDescriptor("test", new ArrayList<>()));
    PowerMockito.when(SObjectDescriptor.fromName(any(), any())).thenReturn(sObjectDescriptor);
    PowerMockito.when(SObjectsDescribeResult.of(any(), anyString(), any())).thenReturn(sObjectsDescribeResult);
    Schema schema = Schema.recordOf("output",
      Schema.Field.of("Name", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("NumberOfEmployees", Schema.of(Schema.Type.INT)),
      Schema.Field.of("ShippingLatitude", Schema.of(Schema.Type.DOUBLE)),
      Schema.Field.of("ShippingLongitude", Schema.of(Schema.Type.DOUBLE))
    );
    salesforceSinkConfig.validate(schema, mockFailureCollector);
    assertEquals(6, mockFailureCollector.getValidationFailures().size());
  }
}

