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

import org.apache.commons.lang.StringUtils
import org.owasp.proxy.http.MutableBufferedResponse
import betamax.proxy.*
import static org.apache.http.HttpHeaders.*

class ResponseImpl extends AbstractMessage implements WritableResponse {

	private final MutableBufferedResponse delegate

	ResponseImpl(MutableBufferedResponse delegate) {
		this.delegate = delegate
	}

	void setStatus(int status) {
		delegate.status = status.toString()
	}

	void setError(int status, String reason) {
		delegate.status = status.toString()
		delegate.reason = reason
	}

	int getStatus() {
		delegate.status.toInteger()
	}

	@Override
	String getContentType() {
		StringUtils.substringBefore(getHeader(CONTENT_TYPE), ";")
	}

	@Override
	String getCharset() {
		getHeader(CONTENT_TYPE)?.find(/charset=(.*)/) { match, charset ->
			charset
		}
	}

	@Override
	String getEncoding() {
		getHeader(CONTENT_ENCODING)
	}

	Map<String, String> getHeaders() {
		def headers = [:]
		for (header in delegate.headers) {
			headers[header.name] = header.value
		}
		headers.asImmutable()
	}

	@Override
	protected OutputStream initOutputStream() {
		new ByteArrayOutputStream() {
			@Override
			void close() {
				super.close()
				delegate.content = toByteArray()
			}
		}
	}

	void addHeader(String name, String value) {
		delegate.addHeader(name, value)
	}

	boolean hasBody() {
		delegate.content.size() > 0
	}

	InputStream getBodyAsBinary() {
		new ByteArrayInputStream(delegate.content)
	}

}
