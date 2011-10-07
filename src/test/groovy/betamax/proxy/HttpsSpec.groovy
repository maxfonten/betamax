package betamax.proxy

import betamax.Recorder
import betamax.util.server.EchoHandler
import java.security.KeyStore
import java.security.cert.X509Certificate
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.ProxySelectorRoutePlanner
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import betamax.proxy.jetty.*
import javax.net.ssl.*
import static org.apache.http.HttpHeaders.VIA
import static org.apache.http.HttpVersion.HTTP_1_1
import org.apache.http.conn.scheme.*
import org.apache.http.conn.ssl.*
import org.apache.http.params.*
import static org.apache.http.protocol.HTTP.UTF_8
import org.eclipse.jetty.server.*
import spock.lang.*

@Issue("https://github.com/robfletcher/betamax/issues/34")
class HttpsSpec extends Specification {

	@Shared @AutoCleanup("deleteDir") File tapeRoot = new File(System.properties."java.io.tmpdir", "tapes")
	@Shared @AutoCleanup("stop") SimpleServer endpoint = new SimpleSecureServer()
	@AutoCleanup("ejectTape") Recorder recorder = new Recorder(tapeRoot: tapeRoot)
	@AutoCleanup("stop") ProxyServer proxy = new ProxyServer()
	HttpClient http

	def setupSpec() {
		endpoint.start(EchoHandler)
	}

	def setup() {
		def trustStore = KeyStore.getInstance(KeyStore.defaultType)
		trustStore.load(null, null)

		def sslSocketFactory = new DummySSLSocketFactory(trustStore)
		sslSocketFactory.hostnameVerifier = new X509HostnameVerifier() {
			void verify(String host, SSLSocket ssl) {
				//To change body of implemented methods use File | Settings | File Templates.
			}

			void verify(String host, X509Certificate cert) {
				//To change body of implemented methods use File | Settings | File Templates.
			}

			void verify(String host, String[] cns, String[] subjectAlts) {
				//To change body of implemented methods use File | Settings | File Templates.
			}

			boolean verify(String s, SSLSession sslSession) {
				true
			}
		}

		def params = new BasicHttpParams()
		HttpProtocolParams.setVersion(params, HTTP_1_1)
		HttpProtocolParams.setContentCharset(params, UTF_8)

		def registry = new SchemeRegistry()
		registry.register new Scheme("http", PlainSocketFactory.socketFactory, 80)
		registry.register new Scheme("https", sslSocketFactory, 443)

		def connectionManager = new ThreadSafeClientConnManager(params, registry)

		http = new DefaultHttpClient(connectionManager, params)

		http.routePlanner = new ProxySelectorRoutePlanner(http.connectionManager.schemeRegistry, ProxySelector.default)

		recorder.insertTape("ignore hosts spec")
	}

	def cleanup() {
		recorder.restoreOriginalProxySettings()
	}

	def "proxy can intercept HTTPS requests"() {
		when: "an HTTPS request is made"
		def uri = endpoint.url.toURI()
		def response = http.execute(new HttpGet("https://$uri.host:$uri.port/"))

		then: "it is intercepted by the proxy"
		response.getFirstHeader(VIA)?.value == "Betamax"
	}

}

class DummySSLSocketFactory extends SSLSocketFactory {
	SSLContext sslContext = SSLContext.getInstance("TLS")

	public DummySSLSocketFactory(KeyStore trustStore) {
		super(trustStore)

		def trustManager = new X509TrustManager() {
			void checkClientTrusted(X509Certificate[] chain, String authType) { }

			void checkServerTrusted(X509Certificate[] chain, String authType) { }

			X509Certificate[] getAcceptedIssuers() {
				null
			}
		}

		sslContext.init(null, [trustManager] as TrustManager[], null)
	}

	@Override
	Socket createSocket(Socket socket, String host, int port, boolean autoClose) {
		sslContext.socketFactory.createSocket(socket, host, port, autoClose)
	}

	@Override
	Socket createSocket() throws IOException {
		sslContext.socketFactory.createSocket()
	}
}

class SimpleSecureServer extends SimpleServer {

	@Override
	protected Server createServer(int port) {
		def server = super.createServer(port)

		def connector = new SslSelectChannelConnector()

		String keystore = new File("src/test/resources/keystore").absolutePath

		connector.port = port
		connector.keystore = keystore
		connector.password = "password"
		connector.keyPassword = "password"

		server.connectors = [connector] as Connector[]

		server
	}
}
