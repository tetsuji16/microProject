/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
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
