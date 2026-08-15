package com.microproject.menu.testsupport;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microproject.util.UiLinkTargets;

public final class RibbonLinkAuditSupport {
	private RibbonLinkAuditSupport() {
	}

	public static Map<String, String> majorLinks() {
		Map<String, String> links = new LinkedHashMap<>();
		links.put("projectHome", UiLinkTargets.PROJECT_HOME);
		links.put("documentation", UiLinkTargets.DOCUMENTATION_HOME);
		links.put("trial", UiLinkTargets.TRIAL_HOME);
		links.put("login", UiLinkTargets.LOGIN_HOME);
		return links;
	}

	public static int fetchStatus(String url) throws IOException, InterruptedException, URISyntaxException {
		HttpClient client = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		HttpRequest request = HttpRequest.newBuilder(new URI(url))
			.timeout(Duration.ofSeconds(20))
			.GET()
			.build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
		return response.statusCode();
	}
}
