package com.aidigital.strategyplanning.external.google.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GoogleWorkspaceClientProperties} defaults and constraint enforcement.
 */
class GoogleWorkspaceClientPropertiesTest {

	private static Validator validator;

	@BeforeAll
	static void buildValidator() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			validator = factory.getValidator();
		}
	}

	@Test
	void shouldApplyDefaultValuesThatPassValidationTest() {
		// Given: untouched properties
		GoogleWorkspaceClientProperties props = new GoogleWorkspaceClientProperties();

		// When: they are validated
		Set<ConstraintViolation<GoogleWorkspaceClientProperties>> violations = validator.validate(props);

		// Then: the defaults are a two-minute transfer ceiling with three bounded attempts
		assertThat(violations).isEmpty();
		assertThat(props.getTransferResponseTimeout()).isEqualTo(Duration.ofSeconds(120));
		assertThat(props.getTransientRetryAttempts()).isEqualTo(3);
		assertThat(props.getTransientRetryBackoff()).isEqualTo(Duration.ofMillis(500));
	}

	@Test
	void shouldApplyCustomValuesTest() {
		// Given: properties set from configuration
		GoogleWorkspaceClientProperties props = new GoogleWorkspaceClientProperties();

		// When: each value is overridden
		props.setTransferResponseTimeout(Duration.ofSeconds(30));
		props.setTransientRetryAttempts(1);
		props.setTransientRetryBackoff(Duration.ofSeconds(2));

		// Then: the overrides are what the client will read
		assertThat(props.getTransferResponseTimeout()).isEqualTo(Duration.ofSeconds(30));
		assertThat(props.getTransientRetryAttempts()).isEqualTo(1);
		assertThat(props.getTransientRetryBackoff()).isEqualTo(Duration.ofSeconds(2));
	}

	@Test
	void shouldRejectZeroAttemptsAndMissingDurationsTest() {
		// Given: zero attempts would never send the call, and a null duration cannot be waited
		GoogleWorkspaceClientProperties props = new GoogleWorkspaceClientProperties();
		props.setTransientRetryAttempts(0);
		props.setTransferResponseTimeout(null);
		props.setTransientRetryBackoff(null);

		// When: they are validated
		Set<ConstraintViolation<GoogleWorkspaceClientProperties>> violations = validator.validate(props);

		// Then: every one of the three is refused at startup
		assertThat(violations).extracting(v -> v.getPropertyPath().toString())
				.containsExactlyInAnyOrder("transientRetryAttempts", "transferResponseTimeout",
						"transientRetryBackoff");
	}
}
