/*
 * Copyright 2011 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package betamax.proxy.jetty

import betamax.Recorder
import betamax.proxy.RecordAndPlaybackProxyInterceptor
import static betamax.Recorder.DEFAULT_PROXY_PORT
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.eclipse.jetty.server.Server

class ProxyServer extends SimpleServer {

	ProxyServer() {
		super(DEFAULT_PROXY_PORT)
	}

	void start(Recorder recorder) {
		def handler = new ProxyHandler()
		handler.interceptor = new RecordAndPlaybackProxyInterceptor(recorder)
		handler.timeout = recorder.proxyTimeout

		super.start(handler)
	}

    @Override
    protected Server createServer(int port) {
        def server = super.createServer(port)
        server.addConnector(createSSLConnector(port + 1))
        server
    }

    private Connector createSSLConnector(int port) {
        def sslConnector = new SslSelectChannelConnector()
        sslConnector.port = port // TODO: separate property
        sslConnector.keystore = new File("src/main/resources/keystore").absolutePath // TODO: need to make this a classpath resource
        sslConnector.password = "password"
        sslConnector.keyPassword = "password"
        return sslConnector
    }
}
