package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.website.WebsiteContentClient;
import com.aidigital.strategyplanning.service.categoryanalysis.services.ClientWebsiteReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ClientWebsiteReader}.
 *
 * <p>The fetch itself is deliberately guarded against server-side request forgery, and that guard
 * lives with the outbound client; what remains here is turning a page of HTML into the plain text
 * a prompt can carry.
 */
@Service
@RequiredArgsConstructor
public class ClientWebsiteReaderImpl implements ClientWebsiteReader {

	private static final int MAX_WEBSITE_CHARS = 4000;

	private final WebsiteContentClient websiteContentClient;

	@Override
	public String fetchWebsiteText(String clientWebsite) {
		String html = websiteContentClient.fetchHtml(clientWebsite);
		if (html == null) {
			return null;
		}
		String text = extractReadableText(html);
		return StringUtils.hasText(text) ? text : null;
	}

	/**
	 * Strips scripts, styles, and markup from raw HTML and returns collapsed readable text,
	 * truncated to a prompt-friendly length.
	 *
	 * @param html raw HTML body
	 * @return readable text, truncated to a prompt-friendly length
	 */
	public String extractReadableText(String html) {
		String noScript = html.replaceAll("(?is)<script.*?</script>", " ")
				.replaceAll("(?is)<style.*?</style>", " ")
				.replaceAll("(?is)<!--.*?-->", " ");
		String noTags = noScript.replaceAll("(?s)<[^>]+>", " ");
		String unescaped = noTags
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&#39;", "'");
		String collapsed = unescaped.replaceAll("\\s+", " ").trim();
		return collapsed.length() <= MAX_WEBSITE_CHARS ? collapsed : collapsed.substring(0, MAX_WEBSITE_CHARS);
	}
}
