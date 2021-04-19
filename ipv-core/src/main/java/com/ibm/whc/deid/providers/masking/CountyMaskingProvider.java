/*
 * (C) Copyright IBM Corp. 2016,2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.providers.masking;

import com.ibm.whc.deid.models.County;
import com.ibm.whc.deid.shared.localization.Resource;
import com.ibm.whc.deid.shared.pojo.config.masking.CountyMaskingProviderConfig;
import com.ibm.whc.deid.util.CountyManager;
import com.ibm.whc.deid.util.ManagerFactory;

public class CountyMaskingProvider extends AbstractMaskingProvider {

  private static final long serialVersionUID = 7519844993295356265L;

  protected final boolean getPseudorandom;
  protected final int unspecifiedValueHandling;
  protected final String unspecifiedValueReturnMessage;

  protected transient volatile CountyManager countyResourceManager = null;

  /**
   * Instantiates a new Country masking provider.
   *
   * @param configuration the configuration of the provider
   * @param tenantId the tenant associated with the current request
   * @param localizationProperty location of the localization property file
   */
  public CountyMaskingProvider(CountyMaskingProviderConfig configuration, String tenantId,
      String localizationProperty) {
    super(tenantId, localizationProperty);
    this.getPseudorandom = configuration.isMaskPseudorandom();
    this.unspecifiedValueHandling = configuration.getUnspecifiedValueHandling();
    this.unspecifiedValueReturnMessage = configuration.getUnspecifiedValueReturnMessage();
  }

  protected CountyManager getCountyManager() {
    if (countyResourceManager == null) {
      countyResourceManager = (CountyManager) ManagerFactory.getInstance().getManager(tenantId,
          Resource.COUNTY, null, localizationProperty);
    }
    return countyResourceManager;
  }

  @Override
  public String mask(String identifier) {
    if (identifier == null) {
      debugFaultyInput("identifier");
      return null;
    }

    CountyManager countyManager = getCountyManager();

    if (this.getPseudorandom) {
      return countyManager.getPseudorandom(identifier);
    }

    County county = countyManager.getValue(identifier);

    if (county == null) {
      debugFaultyInput("county");
      if (unspecifiedValueHandling == 2) {
        return countyManager.getRandomKey();
      } else if (unspecifiedValueHandling == 3) {
        return unspecifiedValueReturnMessage;
      } else {
        return null;
      }
    }

    return countyManager.getRandomKey(county.getNameCountryCode());
  }
}
