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

package io.cdap.plugin.salesforcebatchsource.stepsdesign;

import io.cdap.e2e.utils.CdfHelper;
import io.cdap.plugin.salesforcebatchsource.actions.SalesforcePropertiesPageActions;
import io.cdap.plugin.utils.SchemaTable;
import io.cdap.plugin.utils.enums.SOQLQueryType;
import io.cdap.plugin.utils.enums.SObjects;
import io.cdap.plugin.utils.enums.SalesforceBatchSourceProperty;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Design-time steps of Salesforce plugins.
 */
public class DesignTimeSteps implements CdfHelper {
  String invalidSobjectName = "blahblah";

  @When("fill Reference Name property")
  public void fillReferenceNameProperty() {
    String referenceName = "TestSF" + RandomStringUtils.randomAlphanumeric(7);
    SalesforcePropertiesPageActions.fillReferenceName(referenceName);
  }

  @When("fill Authentication properties with invalid values")
  public void fillAuthenticationPropertiesWithInvalidValues() {
    SalesforcePropertiesPageActions.fillAuthenticationPropertiesWithInvalidValues();
  }

  @When("fill Authentication properties for Salesforce Admin user")
  public void fillAuthenticationPropertiesForSalesforceAdminUser() {
    SalesforcePropertiesPageActions.fillAuthenticationPropertiesForSalesforceAdminUser();
  }

  @When("configure Salesforce source for an SOQL Query of type: {string}")
  public void configureSalesforceForSoqlQuery(String queryType) {
    SalesforcePropertiesPageActions.configureSalesforcePluginForSoqlQuery(SOQLQueryType.valueOf(queryType));
  }

  @When("click on the Get Schema button")
  public void clickOnGetSchemaButton() {
    SalesforcePropertiesPageActions.clickOnGetSchemaButton();
  }

  @When("configure Salesforce source for an SObject Query of SObject: {string}")
  public void configureSalesforceForSObjectQuery(String sObjectName) {
    SalesforcePropertiesPageActions.configureSalesforcePluginForSObjectQuery(SObjects.valueOf(sObjectName));
  }

  @When("click on the Validate button")
  public void clickOnValidateButton() {
    SalesforcePropertiesPageActions.clickOnValidateButton();
  }

  @Then("verify No errors found success message")
  public void verifyNoErrorsFoundSuccessMessage() {
    SalesforcePropertiesPageActions.verifyNoErrorsFoundSuccessMessage();
  }

  @Then("verify the Output Schema table for an SOQL query of type: {string}")
  public void verifyOutputSchemaTableForSoqlQuery(String queryType) {
    SchemaTable schemaTable = SalesforcePropertiesPageActions.
      getExpectedSchemaTableForSOQLQuery(SOQLQueryType.valueOf(queryType));
    SalesforcePropertiesPageActions.verifyOutputSchemaTable(schemaTable);
  }

  @Then("verify the Output Schema table for an SObject Query of SObject: {string}")
  public void verifyOutputSchemaTableForSObjectQuery(String sObjectName) {
    SchemaTable schemaTable = SalesforcePropertiesPageActions.
      getExpectedSchemaTableForSObjectQuery(SObjects.valueOf(sObjectName));
    SalesforcePropertiesPageActions.verifyOutputSchemaTable(schemaTable);
  }

  @When("close plugin properties page")
  public void closePluginPropertiesPage() {
    SalesforcePropertiesPageActions.clickOnClosePropertiesPageButton();
  }

  @Then("verify required fields missing validation message for Reference Name property")
  public void verifyRequiredFieldsMissingValidationMessageForReferenceName() {
    SalesforcePropertiesPageActions.verifyRequiredFieldsMissingValidationMessage(
      SalesforceBatchSourceProperty.REFERENCE_NAME);
  }

  @Then("verify validation message for blank Authentication properties")
  public void verifyValidationMessageForBlankAuthenticationProperties() {
    SalesforcePropertiesPageActions.verifyValidationMessageForBlankAuthenticationProperty();
  }

  @Then("verify validation message for invalid Authentication properties")
  public void verifyValidationMessageForInvalidAuthenticationProperties() {
    SalesforcePropertiesPageActions.verifyValidationMessageForInvalidAuthenticationProperty();
  }

  @Then("verify validation message for missing SOQL or SObject Name property")
  public void verifyValidationMessageForMissingSoqlOrSobjectNameProperty() {
    SalesforcePropertiesPageActions.verifyValidationMessageForMissingSoqlOrSobjectNameProperty();
  }

  @When("fill SOQL Query field with a Star Query")
  public void fillSoqlQueryFieldWithStarQuery() {
    SalesforcePropertiesPageActions.fillSOQLPropertyField(SOQLQueryType.STAR);
  }

  @Then("verify validation message for invalid soql query with Star")
  public void verifyInvalidSoqlQueryErrorMessageForStarQueries() {
    SalesforcePropertiesPageActions.verifyInvalidSoqlQueryErrorMessageForStarQueries();
  }

  @When("fill SObject Name property with an invalid value")
  public void fillSObjectNameFieldWithInvalidValue() {
    SalesforcePropertiesPageActions.fillSObjectName(invalidSobjectName);
  }

  @Then("verify validation message for invalid SObject name")
  public void verifyValidationMessageForInvalidSObjectName() {
    SalesforcePropertiesPageActions.verifyValidationMessageForInvalidSObjectName(invalidSobjectName);
  }
}
