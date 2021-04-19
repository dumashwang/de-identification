/*
 * (C) Copyright IBM Corp. 2016,2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import com.ibm.whc.deid.models.ICD;
import com.ibm.whc.deid.models.ICDFormat;
import com.ibm.whc.deid.models.ICDWithoutFormat;
import com.ibm.whc.deid.shared.localization.Resource;
import com.ibm.whc.deid.shared.localization.Resources;
import com.ibm.whc.deid.util.localization.LocalizationManager;
import com.ibm.whc.deid.util.localization.ResourceEntry;
import com.ibm.whc.deid.utils.log.LogCodes;
import com.ibm.whc.deid.utils.log.LogManager;

/** The type ICDv9Manager. */
public class ICDv9Manager implements Manager {

  private static final LogManager logger = LogManager.getInstance();

  protected static final Resources resourceType = Resource.ICDV9;

  // The number of codes expected to be loaded from the resource file.
  // Modify this value if the resource file is modified.
  private static final int EXPECTED_COUNT = 15000;

  private final SecureRandom random = new SecureRandom();

  private final ArrayList<ICDWithoutFormat> icdList;
  private final HashMap<String, ICDWithoutFormat> icdByCodeMap;
  private final HashMap<String, ICDWithoutFormat> icdByNameMap;
  private final HashMap<String, ICDWithoutFormat> icdByShortMap;

  protected ICDv9Manager() {
    icdList = new ArrayList<>(EXPECTED_COUNT);
    int hashMapInitialSize = Math.round(EXPECTED_COUNT / 0.75f) + 1;
    icdByCodeMap = new HashMap<>(hashMapInitialSize, 0.75f);
    icdByNameMap = new HashMap<>(hashMapInitialSize, 0.75f);
    icdByShortMap = new HashMap<>(hashMapInitialSize, 0.75f);
  }

  protected void add(ICDWithoutFormat icdCode) {
    icdList.add(icdCode);
    icdByCodeMap.put(icdCode.getCode(), icdCode);
    icdByNameMap.put(icdCode.getFullName().toUpperCase(), icdCode);
    String shortName = icdCode.getShortName();
    if (!shortName.trim().isEmpty()) {
      icdByShortMap.put(icdCode.getShortName().toUpperCase(), icdCode);
    }
  }

  /**
   * Creates a new ICDv10Manager instance from the definitions in the given properties file.
   * 
   * @param localizationProperty path and file name of a properties file consumed by the
   *        LocalizationManager to find the resources for this manager instance.
   * 
   * @return a ICDv10Manager instance
   * 
   * @see LocalizationManager
   */
  public static ICDv9Manager buildICDv9Manager(String localizationProperty) {
    ICDv9Manager manager = new ICDv9Manager();

    Collection<ResourceEntry> resourceEntries =
        LocalizationManager.getInstance(localizationProperty).getResources(resourceType);
    for (ResourceEntry entry : resourceEntries) {

      try (InputStream inputStream = entry.createStream()) {

        try (CSVParser parser = Readers.createCSVReaderFromStream(inputStream, ';', '"')) {
          for (CSVRecord record : parser) {

            String code = record.get(0);
            String shortName = record.get(1);
            String fullName = record.get(2);

            if (!code.trim().isEmpty() && !fullName.trim().isEmpty()) {
              String chapterCode = record.get(3);
              String chapterName = record.get(4);
              String categoryCode = record.get(5);
              String categoryName = record.get(6);

              manager.add(new ICDWithoutFormat(code, shortName, fullName, chapterCode, chapterName,
                  categoryCode, categoryName));
            }
          }
        }

      } catch (IOException | NullPointerException e) {
        logger.logError(LogCodes.WPH1013E, e);
      }
    }

    return manager;
  }

  @Override
  public String getRandomKey() {
    String key = null;
    int count = this.icdList.size();
    if (count == 1) {
      key = this.icdList.get(0).getCode();
    } else {
      key = this.icdList.get(random.nextInt(count)).getCode();
    }
    return key;
  }

  /**
   * Retrieve an ICD code by its code or name.
   *
   * @param codeOrName the code or name
   * @return the corresponding ICD code or <i>null</i> if no such code is loaded.
   */
  public ICD lookupICD(String codeOrName) {
    String key = codeOrName.toUpperCase();

    ICDWithoutFormat icd = this.icdByCodeMap.get(key);
    if (icd != null) {
      return new ICD(icd, ICDFormat.CODE);
    }

    icd = this.icdByNameMap.get(key);
    if (icd != null) {
      return new ICD(icd, ICDFormat.NAME);
    }

    icd = this.icdByShortMap.get(key);
    if (icd != null) {
      return new ICD(icd, ICDFormat.NAME);
    }

    return null;
  }

  @Override
  public boolean isValidKey(String codeOrName) {
    return lookupICD(codeOrName) != null;
  }
}
