package com.probely.plugin;

import com.probely.api.FindingSeverity;
import com.probely.api.Scan;

public class ScanRules {
    private FindingSeverity severityThreshold;

    public ScanRules(FindingSeverity threshold) {
        severityThreshold = threshold;
    }

    public boolean isVulnerable(Scan scan) {
        if (severityThreshold == FindingSeverity.HIGH) {
            return scan.highs > 0;
        } else if (severityThreshold == FindingSeverity.MEDIUM) {
            return scan.mediums + scan.highs > 0;
        } else if (severityThreshold == FindingSeverity.LOW) {
            return scan.lows + scan.mediums + scan.highs > 0;
        } else {
            return false;
        }
    }
}
