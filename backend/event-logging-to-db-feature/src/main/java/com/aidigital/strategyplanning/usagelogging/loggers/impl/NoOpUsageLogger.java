// NoOpUsageLogger — silent sink wired when app.usage-logging.enabled=false.
// Keeps the @LogUsage call sites cheap (assemble + drop) when usage
// logging is intentionally disabled (tests, local dev without DB).

package com.aidigital.strategyplanning.usagelogging.loggers.impl;

import com.aidigital.strategyplanning.usagelogging.loggers.UsageLogger;
import com.aidigital.strategyplanning.usagelogging.models.UsageEvent;

/**
 * Drops usage events when usage logging is intentionally disabled.
 */
public class NoOpUsageLogger implements UsageLogger {

	@Override
	public void record(UsageEvent event) {
		// no-op
	}
}
