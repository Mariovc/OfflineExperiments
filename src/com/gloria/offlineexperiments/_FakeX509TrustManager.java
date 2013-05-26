package com.gloria.offlineexperiments;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class _FakeX509TrustManager implements javax.net.ssl.X509TrustManager {
	private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};

	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
	}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		return (_AcceptedIssuers);
	}
}
