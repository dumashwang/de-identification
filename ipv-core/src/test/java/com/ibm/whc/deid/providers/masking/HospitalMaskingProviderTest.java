/*
 * (C) Copyright IBM Corp. 2016,2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.providers.masking;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.whc.deid.models.Hospital;
import com.ibm.whc.deid.providers.identifiers.HospitalIdentifier;
import com.ibm.whc.deid.providers.identifiers.Identifier;
import com.ibm.whc.deid.shared.localization.Resource;
import com.ibm.whc.deid.shared.pojo.config.masking.HospitalMaskingProviderConfig;
import com.ibm.whc.deid.util.HospitalManager;
import com.ibm.whc.deid.util.ManagerFactory;
import com.ibm.whc.deid.util.localization.LocalizationManager;

public class HospitalMaskingProviderTest extends TestLogSetUp implements MaskingProviderTest {

  /*
   * Tests for hospital.mask.preserveCountry option and its boolean values (true and false). It also
   * tests for the localization of the hospital.
   */

  @Test
  public void testMask() {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    configuration.setMaskPreserveCountry(false);

    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);
    String hospitalName = "York Hospital";
    String maskedValue = maskingProvider.mask(hospitalName);

    assertFalse(hospitalName.equalsIgnoreCase(maskedValue));
  }

  @Test
  public void testMaskPreserveCountry() {
    // the preserve country option by default is true.
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();

    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);
    String hospitalName = "York Hospital";

    int randomizationOK = 0;
    for (int i = 0; i < 100; i++) {
      String maskedValue = maskingProvider.mask(hospitalName);

      if (!maskedValue.toUpperCase().equals(hospitalName.toUpperCase())) {
        randomizationOK++;
      }

      HospitalManager hospitalManager =
          (HospitalManager) ManagerFactory.getInstance().getManager(tenantId,
              Resource.HOSPITAL_NAMES, null, LocalizationManager.DEFAULT_LOCALIZATION_PROPERTIES);
      Hospital original = hospitalManager.getValue(hospitalName);
      Hospital masked = hospitalManager.getValue(maskedValue);

      assertTrue(original.getNameCountryCode().equals(masked.getNameCountryCode()));
    }

    assertTrue(randomizationOK > 0);
  }

  @Test
  public void testMaskNullHospitalInputReturnNull() throws Exception {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);

    String invalidHospital = null;
    String maskedHospital = maskingProvider.mask(invalidHospital);

    assertEquals(null, maskedHospital);
    assertThat(outContent.toString(), containsString("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidHospitalInputValidHandlingReturnNull() throws Exception {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    configuration.setUnspecifiedValueHandling(1);
    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);

    String invalidHospital = "Invalid Hospital";
    String maskedHospital = maskingProvider.mask(invalidHospital);

    assertEquals(null, maskedHospital);
    assertThat(outContent.toString(), containsString("WARN - WPH1011W"));
  }

  @Test
  public void testMaskInvalidHospitalInputValidHandlingReturnRandom() throws Exception {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    configuration.setUnspecifiedValueHandling(2);
    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);
    Identifier identifier = new HospitalIdentifier(tenantId, localizationProperty);

    String invalidHospital = "Invalid Hospital";
    String maskedHospital = maskingProvider.mask(invalidHospital);

    assertFalse(maskedHospital.equals(invalidHospital));
    assertTrue(identifier.isOfThisType(maskedHospital));
    assertThat(outContent.toString(), containsString("WARN - WPH1011W"));
  }

  @Test
  public void testMaskInvalidHospitalInputValidHandlingReturnDefaultCustomValue() throws Exception {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    configuration.setUnspecifiedValueHandling(3);
    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);

    String invalidHospital = "Invalid Hospital";
    String maskedHospital = maskingProvider.mask(invalidHospital);

    assertEquals("OTHER", maskedHospital);
    assertThat(outContent.toString(), containsString("WARN - WPH1011W"));
  }

  @Test
  public void testMaskInvalidHospitalInputValidHandlingReturnNonDefaultCustomValue()
      throws Exception {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    configuration.setUnspecifiedValueHandling(3);
    configuration.setUnspecifiedValueReturnMessage("Test Hospital");
    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);

    String invalidHospital = "Invalid Hospital";
    String maskedHospital = maskingProvider.mask(invalidHospital);

    assertEquals("Test Hospital", maskedHospital);
    assertThat(outContent.toString(), containsString("WARN - WPH1011W"));
  }

  @Test
  public void testMaskInvalidHospitalInputInvalidHandlingReturnNull() throws Exception {
    HospitalMaskingProviderConfig configuration = new HospitalMaskingProviderConfig();
    configuration.setUnspecifiedValueHandling(4);
    MaskingProvider maskingProvider =
        new HospitalMaskingProvider(configuration, tenantId, localizationProperty);

    String invalidHospital = "Invalid Hospital";
    String maskedHospital = maskingProvider.mask(invalidHospital);

    assertEquals(null, maskedHospital);
    assertThat(outContent.toString(), containsString("WARN - WPH1011W"));
  }
}
