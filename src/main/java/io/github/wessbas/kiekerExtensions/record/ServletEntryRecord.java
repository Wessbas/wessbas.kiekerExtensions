/***************************************************************************
 * Copyright 2015 Kieker Project (http://kieker-monitoring.net)
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
 ***************************************************************************/

package io.github.wessbas.kiekerExtensions.record;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import kieker.common.record.AbstractMonitoringRecord;
import kieker.common.record.IMonitoringRecord;
import kieker.common.util.registry.IRegistry;
import kieker.common.util.Version;


/**
 * @author Andre van Hoorn
 * 
 * @since 1.12
 */
public class ServletEntryRecord extends AbstractMonitoringRecord implements IMonitoringRecord.Factory, IMonitoringRecord.BinaryFactory {
	/** Descriptive definition of the serialization size of the record. */
	public static final int SIZE = TYPE_SIZE_LONG // ServletEntryRecord.traceId
			 + TYPE_SIZE_STRING // ServletEntryRecord.uri
			 + TYPE_SIZE_INT // ServletEntryRecord.port
			 + TYPE_SIZE_STRING // ServletEntryRecord.host
			 + TYPE_SIZE_STRING // ServletEntryRecord.protocol
			 + TYPE_SIZE_STRING // ServletEntryRecord.method
			 + TYPE_SIZE_STRING // ServletEntryRecord.queryString
			 + TYPE_SIZE_STRING // ServletEntryRecord.encoding
	;
	private static final long serialVersionUID = 2574704311317656583L;
	
	public static final Class<?>[] TYPES = {
		long.class, // ServletEntryRecord.traceId
		String.class, // ServletEntryRecord.uri
		int.class, // ServletEntryRecord.port
		String.class, // ServletEntryRecord.host
		String.class, // ServletEntryRecord.protocol
		String.class, // ServletEntryRecord.method
		String.class, // ServletEntryRecord.queryString
		String.class, // ServletEntryRecord.encoding
	};
	
	/* user-defined constants */
	public static final int NO_TRACE_ID = -1;
	public static final String NO_URI = "<no-uri>";
	public static final int NO_PORT = -1;
	public static final String NO_HOST = "<no-host>";
	public static final String NO_PROTOCOL = "<no-protocol>";
	public static final String NO_METHOD = "<no-method>";
	public static final String NO_QUERY_STRING = "<no-query-string>";
	public static final String NO_ENCODING = "<no-encoding>";
	/* default constants */
	public static final long TRACE_ID = NO_TRACE_ID;
	public static final String URI = NO_URI;
	public static final int PORT = NO_PORT;
	public static final String HOST = NO_HOST;
	public static final String PROTOCOL = NO_PROTOCOL;
	public static final String METHOD = NO_METHOD;
	public static final String QUERY_STRING = NO_QUERY_STRING;
	public static final String ENCODING = NO_ENCODING;
	/* property declarations */
	private final long traceId;
	private final String uri;
	private final int port;
	private final String host;
	private final String protocol;
	private final String method;
	private final String queryString;
	private final String encoding;

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param traceId
	 *            traceId
	 * @param uri
	 *            uri
	 * @param port
	 *            port
	 * @param host
	 *            host
	 * @param protocol
	 *            protocol
	 * @param method
	 *            method
	 * @param queryString
	 *            queryString
	 * @param encoding
	 *            encoding
	 */
	public ServletEntryRecord(final long traceId, final String uri, final int port, final String host, final String protocol, final String method, final String queryString, final String encoding) {
		this.traceId = traceId;
		this.uri = uri == null?NO_URI:uri;
		this.port = port;
		this.host = host == null?NO_HOST:host;
		this.protocol = protocol == null?NO_PROTOCOL:protocol;
		this.method = method == null?NO_METHOD:method;
		this.queryString = queryString == null?NO_QUERY_STRING:queryString;
		this.encoding = encoding == null?NO_ENCODING:encoding;
	}

	/**
	 * This constructor converts the given array into a record.
	 * It is recommended to use the array which is the result of a call to {@link #toArray()}.
	 * 
	 * @param values
	 *            The values for the record.
	 */
	public ServletEntryRecord(final Object[] values) { // NOPMD (direct store of values)
		AbstractMonitoringRecord.checkArray(values, TYPES);
		this.traceId = (Long) values[0];
		this.uri = (String) values[1];
		this.port = (Integer) values[2];
		this.host = (String) values[3];
		this.protocol = (String) values[4];
		this.method = (String) values[5];
		this.queryString = (String) values[6];
		this.encoding = (String) values[7];
	}
	
	/**
	 * This constructor uses the given array to initialize the fields of this record.
	 * 
	 * @param values
	 *            The values for the record.
	 * @param valueTypes
	 *            The types of the elements in the first array.
	 */
	protected ServletEntryRecord(final Object[] values, final Class<?>[] valueTypes) { // NOPMD (values stored directly)
		AbstractMonitoringRecord.checkArray(values, valueTypes);
		this.traceId = (Long) values[0];
		this.uri = (String) values[1];
		this.port = (Integer) values[2];
		this.host = (String) values[3];
		this.protocol = (String) values[4];
		this.method = (String) values[5];
		this.queryString = (String) values[6];
		this.encoding = (String) values[7];
	}

	/**
	 * This constructor converts the given array into a record.
	 * 
	 * @param buffer
	 *            The bytes for the record.
	 * 
	 * @throws BufferUnderflowException
	 *             if buffer not sufficient
	 */
	public ServletEntryRecord(final ByteBuffer buffer, final IRegistry<String> stringRegistry) throws BufferUnderflowException {
		this.traceId = buffer.getLong();
		this.uri = stringRegistry.get(buffer.getInt());
		this.port = buffer.getInt();
		this.host = stringRegistry.get(buffer.getInt());
		this.protocol = stringRegistry.get(buffer.getInt());
		this.method = stringRegistry.get(buffer.getInt());
		this.queryString = stringRegistry.get(buffer.getInt());
		this.encoding = stringRegistry.get(buffer.getInt());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] toArray() {
		return new Object[] {
			this.getTraceId(),
			this.getUri(),
			this.getPort(),
			this.getHost(),
			this.getProtocol(),
			this.getMethod(),
			this.getQueryString(),
			this.getEncoding()
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeBytes(final ByteBuffer buffer, final IRegistry<String> stringRegistry) throws BufferOverflowException {
		buffer.putLong(this.getTraceId());
		buffer.putInt(stringRegistry.get(this.getUri()));
		buffer.putInt(this.getPort());
		buffer.putInt(stringRegistry.get(this.getHost()));
		buffer.putInt(stringRegistry.get(this.getProtocol()));
		buffer.putInt(stringRegistry.get(this.getMethod()));
		buffer.putInt(stringRegistry.get(this.getQueryString()));
		buffer.putInt(stringRegistry.get(this.getEncoding()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] getValueTypes() {
		return TYPES; // NOPMD
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSize() {
		return SIZE;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated This record uses the {@link kieker.common.record.IMonitoringRecord.Factory} mechanism. Hence, this method is not implemented.
	 */
	@Override
	@Deprecated
	public void initFromArray(final Object[] values) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated This record uses the {@link kieker.common.record.IMonitoringRecord.BinaryFactory} mechanism. Hence, this method is not implemented.
	 */
	@Override
	@Deprecated
	public void initFromBytes(final ByteBuffer buffer, final IRegistry<String> stringRegistry) throws BufferUnderflowException {
		throw new UnsupportedOperationException();
	}

	public final long getTraceId() {
		return this.traceId;
	}
	
	public final String getUri() {
		return this.uri;
	}
	
	public final int getPort() {
		return this.port;
	}
	
	public final String getHost() {
		return this.host;
	}
	
	public final String getProtocol() {
		return this.protocol;
	}
	
	public final String getMethod() {
		return this.method;
	}
	
	public final String getQueryString() {
		return this.queryString;
	}
	
	public final String getEncoding() {
		return this.encoding;
	}
	
}
