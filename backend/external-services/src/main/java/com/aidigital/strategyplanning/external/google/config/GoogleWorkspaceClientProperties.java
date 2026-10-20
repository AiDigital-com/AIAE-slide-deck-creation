package com.aidigital.strategyplanning.external.google.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Runtime-tunable properties for the Google Workspace client's file transfer calls.
 *
 * <p>Ordinary Drive, Slides, and Docs calls are small JSON exchanges and share the module-wide
 * response timeout. Exporting a presentation as a PPTX and uploading one back for conversion
 * move whole files and are far slower, so they get their own ceiling here rather than relaxing
 * the global one. Google's upload and export endpoints also occasionally answer a transient
 * {@code 502}/{@code 503} HTML page; the retry settings bound how often such a call is resent.
 *
 * <p>All fields bind from the {@code app.external.google.*} namespace:
 * <pre>
 * app:
 *   external:
 *     google:
 *       transfer-response-timeout: 120s
 *       transient-retry-attempts: 3
 *       transient-retry-backoff: 500ms
 * </pre>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.external.google")
public class GoogleWorkspaceClientProperties {

	/** Maximum wait for the first response byte of a file export or upload. */
	@NotNull
	private Duration transferResponseTimeout = Duration.ofSeconds(120);

	/** Total attempts for a file export or upload that keeps answering 5xx, including the first. */
	@Positive
	private int transientRetryAttempts = 3;

	/** Base pause before a retry; the pause grows linearly with the attempt number. */
	@NotNull
	private Duration transientRetryBackoff = Duration.ofMillis(500);
}
