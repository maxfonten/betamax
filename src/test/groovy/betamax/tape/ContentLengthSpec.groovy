package betamax.tape

import betamax.Betamax
import betamax.Recorder
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.ProxySelectorRoutePlanner
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Specification
import static betamax.proxy.RecordAndPlaybackProxyInterceptor.X_BETAMAX
import static org.apache.http.HttpHeaders.CONTENT_LENGTH
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpResponse

class ContentLengthSpec extends Specification {

	@Rule Recorder recorder = new Recorder()
	@Shared DefaultHttpClient http = new DefaultHttpClient()

	def setupSpec() {
		http.routePlanner = new ProxySelectorRoutePlanner(http.connectionManager.schemeRegistry, ProxySelector.default)
	}

	@Betamax(tape = "wrong content length tape")
	def "content length header is overwritten when playing back from tape"() {
		when:
		HttpResponse response = http.execute(new HttpGet("http://robfletcher.github.com/betamax"))

		then:
		response.getFirstHeader(X_BETAMAX)?.value == "PLAY"
		response.entity.contentLength == response.entity.content.bytes.length
		response.getFirstHeader(CONTENT_LENGTH)?.value == response.entity.content.bytes.length
	}

}
