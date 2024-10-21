package com.probely.plugin;

import com.probely.api.FindingSeverity;
import com.probely.api.Scan;

public class ScanRules {
  private FindingSeverity severityThreshold;

  public ScanRules(FindingSeverity threshold) {
    severityThreshold = threshold;
  }

  public boolean isVulnerable(Scan scan) {
    int count = 0;
    if (severityThreshold == FindingSeverity.LOW) {
      count = scan.lows + scan.mediums + scan.highs;
    } else if (severityThreshold == FindingSeverity.MEDIUM) {
      count = scan.mediums + scan.highs;
    } else if (severityThreshold == FindingSeverity.HIGH) {
      count = scan.highs;
    }
    return count > 0;
  }
}
