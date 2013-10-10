package edu.one.core.infra.mustache;

import edu.one.core.infra.http.Renders;

public class StaticResourceTemplateFunction extends VertxTemplateFunction {

	private final String infraPort;

	private final String publicDir;
	private final String protocol;

	public StaticResourceTemplateFunction(String publicDir) {
		this.publicDir = publicDir;
		this.infraPort = null;
		this.protocol = "http://";
	}

	public StaticResourceTemplateFunction(String publicDir, String infraPort) {
		this.publicDir = publicDir;
		this.infraPort = infraPort;
		this.protocol = "http://";
	}

	public StaticResourceTemplateFunction(String publicDir, String infraPort, boolean https) {
		this.publicDir = publicDir;
		this.infraPort = infraPort;
		this.protocol = https ? "https://" : "http://";
	}

	@Override
	public String apply(String path) {
		String host = Renders.getHost(request);
		if (infraPort != null && request.headers().get("X-Forwarded-For") == null) {
			host = host.split(":")[0] + ":" + infraPort;
		}
		return protocol
				+ host
				+ ((publicDir != null && publicDir.startsWith("/")) ? publicDir : "/" + publicDir)
				+ "/" + path;
	}
}
