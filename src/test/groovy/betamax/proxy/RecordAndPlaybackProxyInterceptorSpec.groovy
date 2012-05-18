package betamax.proxy

import spock.lang.Specification
import betamax.*
import betamax.util.message.*
import static java.net.HttpURLConnection.HTTP_FORBIDDEN

class RecordAndPlaybackProxyInterceptorSpec extends Specification {

	Recorder recorder = Mock(Recorder)
	VetoingProxyInterceptor interceptor = new RecordAndPlaybackProxyInterceptor(recorder)
	Request request = new BasicRequest("GET", "http://robfletcher.github.com/betamax")
	Response response = new BasicResponse(status: 200, reason: "OK")

	def "does not veto a request when no matching recording is found on tape"() {
		given:
		def tape = Mock(Tape)
		tape.isReadable() >> true
		tape.isWritable() >> true
		tape.matchesRequest(_) >> false
		recorder.getTape() >> tape

		when:
		boolean veto = interceptor.interceptRequest(request, response)

		then:
		!veto

		and:
		0 * tape.play(_)
	}
	
	def "vetos a request when a matching recording is found on tape"() {
		given:
		def tape = Mock(Tape)
		tape.isReadable() >> true
		tape.matchesRequest(_) >> true
		recorder.getTape() >> tape

		when:
		boolean veto = interceptor.interceptRequest(request, response)

		then:
		veto

		and:
		1 * tape.play(request, response)
	}

	def "vetos a request and sets failing response code if no tape is inserted"() {
		given:
		recorder.getTape() >> null

		when:
		boolean veto = interceptor.interceptRequest(request, response)

		then:
		veto

		and:
		response.status == HTTP_FORBIDDEN
	}

	def "does not veto a request if the tape is not readable"() {
		given:
		def tape = Mock(Tape)
		tape.isReadable() >> false
		tape.isWritable() >> true
		recorder.getTape() >> tape

		when:
		boolean veto = interceptor.interceptRequest(request, response)

		then:
		!veto

		and: "the tape is positioned on an existing matching recording so it gets overwritten in WRITE_ONLY mode"
		1 * tape.matchesRequest(_) >> true
	}

	def "vetos a request and sets failing response code if the tape is not writable"() {
		given:
		def tape = Mock(Tape)
		tape.isReadable() >> true
		tape.matchesRequest(request) >> false
		tape.isWritable() >> false
		recorder.getTape() >> tape

		when:
		boolean veto = interceptor.interceptRequest(request, response)

		then:
		veto

		and:
		response.status == HTTP_FORBIDDEN
	}

	def "records response when intercepted"() {
		given:
		def tape = Mock(Tape)
		recorder.getTape() >> tape

		when:
		interceptor.interceptResponse(request, response)

		then:
		1 * tape.record(request, response)
	}

}
