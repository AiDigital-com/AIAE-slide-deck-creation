package com.aidigital.strategyplanning.external.website.impl;

import com.aidigital.strategyplanning.external.website.PublicHttpAddressPolicy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Default implementation of {@link PublicHttpAddressPolicy}.
 */
@Service
public class PublicHttpAddressPolicyImpl implements PublicHttpAddressPolicy {

	private static final String SCHEME_HTTP = "http";
	private static final String SCHEME_HTTPS = "https";
	private static final int IPV6_ADDRESS_BYTES = 16;
	private static final int UNIQUE_LOCAL_MASK = 0xfe;
	private static final int UNIQUE_LOCAL_PREFIX = 0xfc;
	private static final int BYTE_MASK = 0xff;

	/**
	 * Validates that a URL is a public http(s) address safe to fetch server-side. The host is
	 * resolved to every IP it maps to and each address is rejected if it is loopback, link-local,
	 * site-local (private), unique-local (IPv6 fc00::/7), multicast, or a wildcard address. This
	 * blocks server-side request forgery via hostnames, IPv6 literals such as {@code [::1]}, and
	 * DNS names that resolve to internal ranges.
	 *
	 * @param url candidate URL
	 * @return true when the URL is a public http(s) address that resolves only to public IPs
	 */
	@Override
	public boolean isFetchableUrl(String url) {
		if (!StringUtils.hasText(url)) {
			return false;
		}
		try {
			URI uri = URI.create(url.trim());
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if (scheme == null || host == null
					|| !(scheme.equalsIgnoreCase(SCHEME_HTTP) || scheme.equalsIgnoreCase(SCHEME_HTTPS))) {
				return false;
			}
			InetAddress[] resolved = InetAddress.getAllByName(host);
			if (resolved.length == 0) {
				return false;
			}
			for (InetAddress address : resolved) {
				if (isBlockedAddress(address)) {
					return false;
				}
			}
			return true;
		} catch (IllegalArgumentException | UnknownHostException e) {
			return false;
		}
	}

	/**
	 * Determines whether an IP address is outside the publicly routable space and must not be
	 * fetched server-side.
	 *
	 * @param address resolved IP address
	 * @return true when the address is loopback, private, link-local, unique-local, multicast, or
	 * a wildcard/any-local address
	 */
	boolean isBlockedAddress(InetAddress address) {
		if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
				|| address.isMulticastAddress() || address.isAnyLocalAddress()) {
			return true;
		}
		byte[] bytes = address.getAddress();
		if (bytes.length == IPV6_ADDRESS_BYTES) {
			int first = bytes[0] & BYTE_MASK;
			return (first & UNIQUE_LOCAL_MASK) == UNIQUE_LOCAL_PREFIX;
		}
		return false;
	}
}
