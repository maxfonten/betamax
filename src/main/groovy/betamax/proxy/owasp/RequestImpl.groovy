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

import betamax.proxy.Request
import org.owasp.proxy.http.MutableBufferedRequest

class RequestImpl implements Request {

	private final MutableBufferedRequest delegate

	RequestImpl(MutableBufferedRequest delegate) {
		this.delegate = delegate
	}

	String getMethod() {
		delegate.method
	}

	URI getUri() {
		"http://$delegate.target$delegate.resource".toURI()
	}

	Map<String, String> getHeaders() {
		def headers = [:]
		for (header in delegate.headers) {
			headers[header.name] = header.value
		}
		headers.asImmutable()
	}

	boolean hasBody() {
		delegate.content.size() > 0
	}

	InputStream getBodyAsBinary() {
		new ByteArrayInputStream(delegate.content)
	}

	String getHeader(String name) {
		delegate.getHeader(name)
	}

	Reader getBodyAsText() {
		new InputStreamReader(new ByteArrayInputStream(delegate.decodedContent))
	}
}
