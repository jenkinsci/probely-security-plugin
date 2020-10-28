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
        switch (severityThreshold) {
            case LOW:
                count += scan.lows;
            case MEDIUM:
                count += scan.mediums;
            case HIGH:
                count += scan.highs;
        }
        return count > 0;
    }
}
