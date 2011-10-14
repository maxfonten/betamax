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

package betamax.proxy.owasp

import betamax.Recorder
import org.owasp.proxy.daemon.Server
import org.owasp.proxy.http.server.BufferedMessageInterceptor.Action
import static betamax.Recorder.DEFAULT_PROXY_PORT
import org.owasp.proxy.http.*
import org.owasp.proxy.http.server.*
import java.util.logging.Logger
import java.util.concurrent.CountDownLatch
import betamax.proxy.VetoingProxyInterceptor
import betamax.proxy.RecordAndPlaybackProxyInterceptor

class ProxyServer {

	private final String host
	private int port

	private Server proxy
	private CountDownLatch startedLatch
	private CountDownLatch stoppedLatch

	private final log = Logger.getLogger(ProxyServer.name)

	ProxyServer() {
		this(DEFAULT_PROXY_PORT)
	}

	ProxyServer(int port) {
		def localAddress = InetAddress.localHost
		if (localAddress.loopbackAddress) {
			log.info "local address is loopback, using hostname $localAddress.hostName"
			host = localAddress.hostName
		} else {
			host = localAddress.hostAddress
		}
		this.port = port
	}

	void start(Recorder recorder) {
		startedLatch = new CountDownLatch(1)
		stoppedLatch = new CountDownLatch(1)

		def requestHandler = new DefaultHttpRequestHandler()
		def interceptor = new BufferedMessageInterceptor() {

			private final VetoingProxyInterceptor interceptor = new RecordAndPlaybackProxyInterceptor(recorder)

			@Override
			Action directRequest(MutableRequestHeader request) {
				boolean proceed = interceptor.interceptRequest()
			}

			@Override
			Action directResponse(RequestHeader request, MutableResponseHeader response) {
				println "directResponse..."
		        Action.BUFFER
		    }

			@Override
			void processRequest(MutableBufferedRequest request) {
				super.processRequest(request)
			}

			@Override
			void processResponse(BufferedRequest request, MutableBufferedResponse response) {
				println "processResponse..."
		        try {
					response.addHeader("Via", "Betamax")
					println "$request.resource : $response.decodedContent.length"
		        } catch (MessageFormatException mfe) {
		            mfe.printStackTrace()
		        }
		    }

			@Override
			void responseContentSizeExceeded(BufferedRequest request, ResponseHeader response, int size) {
				println "responseContentSizeExceeded..."
				super.responseContentSizeExceeded(request, response, size)
			}
		}
		int maxContentSize = 10240
		requestHandler = new BufferingHttpRequestHandler(requestHandler, interceptor, maxContentSize)
		def httpProxy = new HttpProxyConnectionHandler(requestHandler)
		def listen = new InetSocketAddress(host, port)
		proxy = new Server(listen, httpProxy)
		proxy.start()
		sleep 1000
		startedLatch.countDown()
		println "it's alive..."
	}

	void stop() {
		proxy?.stop()
		stoppedLatch?.countDown()
	}

	boolean isRunning() {
		startedLatch?.count == 0 && stoppedLatch?.count > 0
	}

	void setPort(int port) {
		if (running) {
			throw new IllegalStateException("Cannot set port once the server is already started")
		}
		this.port = port
	}

}
