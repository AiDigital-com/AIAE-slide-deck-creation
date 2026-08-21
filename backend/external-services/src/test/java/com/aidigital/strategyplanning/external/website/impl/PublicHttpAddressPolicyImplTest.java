package com.aidigital.strategyplanning.external.website.impl;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the server-side request forgery guard on user-supplied website URLs.
 *
 * <p>These assertions are the control itself: every case that returns false here is an address
 * the server must refuse to connect to on a user's behalf.
 */
class PublicHttpAddressPolicyImplTest {

	@Test
	void shouldRejectPrivateAndNonHttpHostsTest() {
		// Given: the URL a user typed into the builder
		PublicHttpAddressPolicyImpl policy = new PublicHttpAddressPolicyImpl();

		// When-Then: only a public http(s) address is fetchable; every internal address,
		// IPv6 literal, and non-http scheme is refused before any connection is made
		assertThat(policy.isFetchableUrl("https://example.com")).isTrue();
		assertThat(policy.isFetchableUrl("http://localhost:8080")).isFalse();
		assertThat(policy.isFetchableUrl("http://127.0.0.1")).isFalse();
		assertThat(policy.isFetchableUrl("http://192.168.1.10")).isFalse();
		assertThat(policy.isFetchableUrl("http://10.0.0.5")).isFalse();
		assertThat(policy.isFetchableUrl("http://169.254.169.254")).isFalse();
		assertThat(policy.isFetchableUrl("http://[::1]")).isFalse();
		assertThat(policy.isFetchableUrl("http://0.0.0.0")).isFalse();
		assertThat(policy.isFetchableUrl("http://[fc00::1]")).isFalse();
		assertThat(policy.isFetchableUrl("ftp://example.com")).isFalse();
		assertThat(policy.isFetchableUrl("not a url")).isFalse();
		assertThat(policy.isFetchableUrl(null)).isFalse();
	}

	@Test
	void shouldRejectAHostThatDoesNotResolveTest() {
		// Given: a hostname that cannot be resolved
		PublicHttpAddressPolicyImpl policy = new PublicHttpAddressPolicyImpl();

		// When-Then: an unresolvable host is refused rather than attempted
		assertThat(policy.isFetchableUrl("https://no-such-host.invalid")).isFalse();
	}

	@Test
	void shouldRejectAUrlWithNoHostTest() {
		// Given: input that parses but names no host
		PublicHttpAddressPolicyImpl policy = new PublicHttpAddressPolicyImpl();

		// When-Then: there is nothing safe to connect to
		assertThat(policy.isFetchableUrl("https:///path")).isFalse();
		assertThat(policy.isFetchableUrl("   ")).isFalse();
	}

	@Test
	void shouldAllowPubliclyRoutableAddressesTest() throws UnknownHostException {
		// Given: public IPv4 and IPv6 addresses
		PublicHttpAddressPolicyImpl policy = new PublicHttpAddressPolicyImpl();

		// When-Then: the guard blocks internal ranges only, not the public internet
		assertThat(policy.isBlockedAddress(InetAddress.getByName("8.8.8.8"))).isFalse();
		assertThat(policy.isBlockedAddress(InetAddress.getByName("[2001:4860:4860::8888]"))).isFalse();
	}

	@Test
	void shouldRejectTheUniqueLocalIpv6RangeTest() throws UnknownHostException {
		// Given: addresses inside IPv6 fc00::/7, which Java reports as neither
		// site-local nor link-local
		PublicHttpAddressPolicyImpl policy = new PublicHttpAddressPolicyImpl();

		// When-Then: the explicit prefix check catches what the JDK predicates miss
		assertThat(policy.isBlockedAddress(InetAddress.getByName("[fc00::1]"))).isTrue();
		assertThat(policy.isBlockedAddress(InetAddress.getByName("[fd12:3456::1]"))).isTrue();
	}
}
