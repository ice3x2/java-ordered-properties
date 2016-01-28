package nu.studer.java.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This class provides an alternative to the JDK's {@link Properties} class. It
 * fixes the design flaw of using inheritance over composition, while keeping up
 * the same APIs as the original class. Keys and values are guaranteed to be of
 * type {@link String}.
 * <p/>
 * This class is not synchronized, contrary to the original implementation.
 * <p/>
 * As additional functionality, this class keeps its properties in a
 * well-defined order. By default, the order is the one in which the individual
 * properties have been added, either through explicit API calls or through
 * reading them top-to-bottom from a properties file.
 * <p/>
 * Also, an optional flag can be set to omit the comment that contains the
 * current date when storing the properties to a properties file.
 * <p/>
 * Currently, this class does not support the concept of default properties,
 * contrary to the original implementation.
 * <p/>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access ordered properties concurrently, and at least one of
 * the threads modifies the ordered properties structurally, it <em>must</em> be
 * synchronized externally. This is typically accomplished by synchronizing on
 * some object that naturally encapsulates the properties.
 * <p/>
 * Note that the actual (and quite complex) logic of parsing and storing
 * properties from and to a stream is delegated to the {@link Properties} class
 * from the JDK.
 *
 * @see Properties
 */

public final class OrderedProperties implements Serializable {

	private static final long serialVersionUID = 1L;

	private transient Map<String, String> properties;
	private transient boolean suppressDate;

	/**
	 * Creates a new instance that will keep the properties in the order they
	 * have been added. Other than the ordering of the keys, this instance
	 * behaves like an instance of the {@link Properties} class.
	 */
	public OrderedProperties() {
		this(new LinkedHashMap<String, String>(), false);
	}

	private OrderedProperties(Map<String, String> properties, boolean suppressDate) {
		this.properties = properties;
		this.suppressDate = suppressDate;
	}

	/**
	 * See {@link Properties#getProperty(String)}.
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * See {@link Properties#getProperty(String, String)}.
	 */
	public String getProperty(String key, String defaultValue) {
		String value = properties.get(key);
		return (value == null) ? defaultValue : value;
	}

	/**
	 * See {@link Properties#setProperty(String, String)}.
	 */
	public String setProperty(String key, String value) {
		return properties.put(key, value);
	}

	/**
	 * Removes the property with the specified key
	 * <p/>
	 * Currently, this class does not support the concept of default properties,
	 * contrary to the original implementation.
	 * <p/>
	 * <strong>Note that this implementation is not synchronized.</strong> If
	 * multiple threads access ordered properties concurrently, and at least one
	 * of the threads modifies the ordered properties structurally, it
	 * <em>must</em> be synchronized externally. This is typically accomplished
	 * by synchronizing on some object that naturally encapsulates the
	 * properties.
	 * <p/>
	 * , if it is present. Returns the value of the property, or <tt>null</tt>
	 * if there was no property with the specified key.
	 *
	 * @param key
	 *            the key of the property to remove
	 * @return the previous value of the property, or <tt>null</tt> if there was
	 *         no property with the specified key
	 */
	public String removeProperty(String key) {
		return properties.remove(key);
	}

	/**
	 * Returns <tt>true</tt> if there is a property with the specified key.
	 *
	 * @param key
	 *            the key whose presence is to be tested
	 */
	public boolean containsProperty(String key) {
		return properties.containsKey(key);
	}

	/**
	 * See {@link Properties#size()}.
	 */
	public int size() {
		return properties.size();
	}

	/**
	 * See {@link Properties#isEmpty()}.
	 */
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	/**
	 * See {@link Properties#propertyNames()}.
	 */
	public Enumeration<String> propertyNames() {
		return new Vector<String>(properties.keySet()).elements();
	}

	/**
	 * See {@link Properties#stringPropertyNames()}.
	 */
	public Set<String> stringPropertyNames() {
		return new LinkedHashSet<String>(properties.keySet());
	}

	/**
	 * See {@link Properties#entrySet()}.
	 */
	public Set<Map.Entry<String, String>> entrySet() {
		return new LinkedHashSet<Map.Entry<String, String>>(properties.entrySet());
	}

	/**
	 * See {@link Properties#load(InputStream)}.
	 */
	public void load(InputStream stream) throws IOException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		customProperties.load(stream);
	}

	public void load(Reader reader) throws IOException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		load0(customProperties, new LineReader(reader));
	}

	private void load0(CustomProperties customProperties, LineReader lr) throws IOException {
		char[] convtBuf = new char[1024];
		int limit;
		int keyLen;
		int valueStart;
		char c;
		boolean hasSep;
		boolean precedingBackslash;

		while ((limit = lr.readLine()) >= 0) {
			c = 0;
			keyLen = 0;
			valueStart = limit;
			hasSep = false;

			// System.out.println("line=<" + new String(lineBuf, 0, limit) +
			// ">");
			precedingBackslash = false;
			while (keyLen < limit) {
				c = lr.lineBuf[keyLen];
				// need check if escaped.
				if ((c == '=' || c == ':') && !precedingBackslash) {
					valueStart = keyLen + 1;
					hasSep = true;
					break;
				} else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
					valueStart = keyLen + 1;
					break;
				}
				if (c == '\\') {
					precedingBackslash = !precedingBackslash;
				} else {
					precedingBackslash = false;
				}
				keyLen++;
			}
			while (valueStart < limit) {
				c = lr.lineBuf[valueStart];
				if (c != ' ' && c != '\t' && c != '\f') {
					if (!hasSep && (c == '=' || c == ':')) {
						hasSep = true;
					} else {
						break;
					}
				}
				valueStart++;
			}
			String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
			String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
			customProperties.put(key, value);
		}
	}

	private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
		if (convtBuf.length < len) {
			int newLen = len * 2;
			if (newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		char aChar;
		char[] out = convtBuf;
		int outLen = 0;
		int end = off + len;

		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = in[off++];
						switch (aChar) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							value = (value << 4) + aChar - '0';
							break;
						case 'a':
						case 'b':
						case 'c':
						case 'd':
						case 'e':
						case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A':
						case 'B':
						case 'C':
						case 'D':
						case 'E':
						case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
							throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar == 't')
						aChar = '\t';
					else if (aChar == 'r')
						aChar = '\r';
					else if (aChar == 'n')
						aChar = '\n';
					else if (aChar == 'f')
						aChar = '\f';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = (char) aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	/**
	 * See {@link Properties#loadFromXML(InputStream)}.
	 */
	public void loadFromXML(InputStream stream) throws IOException, InvalidPropertiesFormatException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		customProperties.loadFromXML(stream);
	}

	/**
	 * See {@link Properties#store(OutputStream, String)}.
	 */
	public void store(OutputStream stream, String comments) throws IOException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		if (suppressDate) {

			Writer writer = new DateSuppressingPropertiesBufferedWriter(new OutputStreamWriter(stream, "8859_1"));
			store0(customProperties, toBufferedWriterIfNeed(writer), comments, false);

		} else {
			customProperties.store(stream, comments);
		}
	}

	private void store0(CustomProperties customProperties, BufferedWriter bw, String comments, boolean escUnicode)
			throws IOException {
		if (comments != null) {
			writeComments(bw, comments);
		}
		bw.write("#" + new Date().toString());
		bw.newLine();
		synchronized (this) {
			for (Enumeration<?> e = customProperties.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				String val = (String) customProperties.get(key);
				key = saveConvert(key, true, escUnicode);
				/*
				 * No need to escape embedded and trailing spaces for value,
				 * hence pass false to flag.
				 */
				val = saveConvert(val, false, escUnicode);
				bw.write(key + "=" + val);
				bw.newLine();
			}
		}
		bw.flush();
	}

	private static void writeComments(BufferedWriter bw, String comments) throws IOException {
		bw.write("#");
		int len = comments.length();
		int current = 0;
		int last = 0;
		char[] uu = new char[6];
		uu[0] = '\\';
		uu[1] = 'u';
		while (current < len) {
			char c = comments.charAt(current);
			if (c > '\u00ff' || c == '\n' || c == '\r') {
				if (last != current)
					bw.write(comments.substring(last, current));
				if (c > '\u00ff') {
					uu[2] = toHex((c >> 12) & 0xf);
					uu[3] = toHex((c >> 8) & 0xf);
					uu[4] = toHex((c >> 4) & 0xf);
					uu[5] = toHex(c & 0xf);
					bw.write(new String(uu));
				} else {
					bw.newLine();
					if (c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n') {
						current++;
					}
					if (current == len - 1
							|| (comments.charAt(current + 1) != '#' && comments.charAt(current + 1) != '!'))
						bw.write("#");
				}
				last = current + 1;
			}
			current++;
		}
		if (last != current)
			bw.write(comments.substring(last, current));
		bw.newLine();
	}

	private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

	private static char toHex(int nibble) {
		return hexDigit[(nibble & 0xF)];
	}

	/** A table of hex digits */
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
			'F' };

	/**
	 * See {@link Properties#store(Writer, String)}.
	 */
	public void store(Writer writer, String comments) throws IOException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		if (suppressDate) {
			Writer DateSuppressingPropertiesBufferedWriter = new DateSuppressingPropertiesBufferedWriter(writer);

			store0(customProperties, toBufferedWriterIfNeed(DateSuppressingPropertiesBufferedWriter), comments, false);
		} else {
			store0(customProperties, toBufferedWriterIfNeed(writer), comments, false);
		}
	}

	public BufferedWriter toBufferedWriterIfNeed(Writer writer) {
		return (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer);
	}

	/**
	 * See {@link Properties#storeToXML(OutputStream, String)}.
	 */
	public void storeToXML(OutputStream stream, String comment) throws IOException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		customProperties.storeToXML(stream, comment);
	}

	/**
	 * See {@link Properties#storeToXML(OutputStream, String, String)}.
	 */
	public void storeToXML(OutputStream stream, String comment, String encoding) throws IOException {
		CustomProperties customProperties = new CustomProperties(this.properties);
		customProperties.storeToXML(stream, comment, encoding);
	}

	/**
	 * See {@link Properties#list(PrintStream)}.
	 */
	public void list(PrintStream stream) {
		CustomProperties customProperties = new CustomProperties(this.properties);
		customProperties.list(stream);
	}

	/**
	 * See {@link Properties#list(PrintWriter)}.
	 */
	public void list(PrintWriter writer) {
		CustomProperties customProperties = new CustomProperties(this.properties);
		customProperties.list(writer);
	}

	/**
	 * Convert this instance to a {@link Properties} instance.
	 *
	 * @return the {@link Properties} instance
	 * 
	 * 
	 */
	public Properties toJdkProperties() {
		Properties jdkProperties = new Properties();
		for (Map.Entry<String, String> entry : this.entrySet()) {
			jdkProperties.put(entry.getKey(), entry.getValue());
		}
		return jdkProperties;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other == null || getClass() != other.getClass()) {
			return false;
		}

		OrderedProperties that = (OrderedProperties) other;
		return Arrays.equals(properties.entrySet().toArray(), that.properties.entrySet().toArray());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(properties.entrySet().toArray());
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeObject(properties);
		stream.writeBoolean(suppressDate);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		properties = (Map<String, String>) stream.readObject();
		suppressDate = stream.readBoolean();
	}

	@SuppressWarnings("unused")
	private void readObjectNoData() throws InvalidObjectException {
		throw new InvalidObjectException("Stream data required");
	}

	/**
	 * See {@link Properties#toString()}.
	 */
	@Override
	public String toString() {
		return properties.toString();
	}

	/**
	 * Creates a new instance that will have both the same property entries and
	 * the same behavior as the given source.
	 * <p/>
	 * Note that the source instance and the copy instance will share the same
	 * comparator instance if a custom ordering had been configured on the
	 * source.
	 *
	 * @param source
	 *            the source to copy from
	 * @return the copy
	 */

	public static OrderedProperties copyOf(OrderedProperties source) {
		// create a copy that has the same behaviour
		OrderedPropertiesBuilder builder = new OrderedPropertiesBuilder();
		builder.withSuppressDateInComment(source.suppressDate);
		if (source.properties instanceof TreeMap) {
			builder.withOrdering(((TreeMap<String, String>) source.properties).comparator());
		}
		OrderedProperties result = builder.build();

		// copy the properties from the source to the target
		for (Map.Entry<String, String> entry : source.entrySet()) {
			result.setProperty(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Builder for {@link OrderedProperties} instances.
	 */
	public static final class OrderedPropertiesBuilder {

		private Comparator<? super String> comparator;
		private boolean suppressDate;

		/**
		 * Use a custom ordering of the keys.
		 *
		 * @param comparator
		 *            the ordering to apply on the keys
		 * @return the builder
		 */
		public OrderedPropertiesBuilder withOrdering(Comparator<? super String> comparator) {
			this.comparator = comparator;
			return this;
		}

		/**
		 * Suppress the comment that contains the current date when storing the
		 * properties.
		 *
		 * @param suppressDate
		 *            whether to suppress the comment that contains the current
		 *            date
		 * @return the builder
		 */
		public OrderedPropertiesBuilder withSuppressDateInComment(boolean suppressDate) {
			this.suppressDate = suppressDate;
			return this;
		}

		/**
		 * Builds a new {@link OrderedProperties} instance.
		 *
		 * @return the new instance
		 */
		public OrderedProperties build() {
			Map<String, String> properties = (this.comparator != null) ? new TreeMap<String, String>(comparator)
					: new LinkedHashMap<String, String>();
			return new OrderedProperties(properties, suppressDate);
		}

	}

	/**
	 * Custom {@link Properties} that delegates reading, writing, and
	 * enumerating properties to the backing {@link OrderedProperties}
	 * instance's properties.
	 */
	private static final class CustomProperties extends Properties {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3232895110726619657L;
		private final Map<String, String> targetProperties;

		private CustomProperties(Map<String, String> targetProperties) {
			this.targetProperties = targetProperties;
		}

		@Override
		public Object get(Object key) {
			return targetProperties.get(key);
		}

		@Override
		public Object put(Object key, Object value) {
			return targetProperties.put((String) key, (String) value);
		}

		@Override
		public String getProperty(String key) {
			return targetProperties.get(key);
		}

		@Override
		public Enumeration<Object> keys() {
			return new Vector<Object>(targetProperties.keySet()).elements();
		}

		@Override
		public Set<Object> keySet() {
			return new LinkedHashSet<Object>(targetProperties.keySet());
		}

	}

	/**
	 * Custom {@link BufferedWriter} for storing properties that will write all
	 * leading lines of comments except the last comment line. Using the JDK
	 * Properties class to store properties, the last comment line always
	 * contains the current date which is what we want to filter out.
	 */
	private static final class DateSuppressingPropertiesBufferedWriter extends BufferedWriter {

		private final String LINE_SEPARATOR = System.getProperty("line.separator");

		private StringBuilder currentComment;
		private String previousComment;

		private DateSuppressingPropertiesBufferedWriter(Writer out) {
			super(out);
		}

		@Override
		public void write(String string) throws IOException {
			if (currentComment != null) {
				currentComment.append(string);
				if (string.endsWith(LINE_SEPARATOR)) {
					if (previousComment != null) {
						super.write(previousComment);
					}

					previousComment = currentComment.toString();
					currentComment = null;
				}
			} else if (string.startsWith("#")) {
				currentComment = new StringBuilder(string);
			} else {
				super.write(string);
			}
		}

	}

	class LineReader {
		public LineReader(InputStream inStream) {
			this.inStream = inStream;
			inByteBuf = new byte[8192];
		}

		public LineReader(Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}

		byte[] inByteBuf;
		char[] inCharBuf;
		char[] lineBuf = new char[1024];
		int inLimit = 0;
		int inOff = 0;
		InputStream inStream;
		Reader reader;

		int readLine() throws IOException {
			int len = 0;
			char c = 0;

			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;

			while (true) {
				if (inOff >= inLimit) {
					inLimit = (inStream == null) ? reader.read(inCharBuf) : inStream.read(inByteBuf);
					inOff = 0;
					if (inLimit <= 0) {
						if (len == 0 || isCommentLine) {
							return -1;
						}
						return len;
					}
				}
				if (inStream != null) {
					// The line below is equivalent to calling a
					// ISO8859-1 decoder.
					c = (char) (0xff & inByteBuf[inOff++]);
				} else {
					c = inCharBuf[inOff++];
				}
				if (skipLF) {
					skipLF = false;
					if (c == '\n') {
						continue;
					}
				}
				if (skipWhiteSpace) {
					if (c == ' ' || c == '\t' || c == '\f') {
						continue;
					}
					if (!appendedLineBegin && (c == '\r' || c == '\n')) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				if (isNewLine) {
					isNewLine = false;
					if (c == '#' || c == '!') {
						isCommentLine = true;
						continue;
					}
				}

				if (c != '\n' && c != '\r') {
					lineBuf[len++] = c;
					if (len == lineBuf.length) {
						int newLength = lineBuf.length * 2;
						if (newLength < 0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					// flip the preceding backslash flag
					if (c == '\\') {
						precedingBackslash = !precedingBackslash;
					} else {
						precedingBackslash = false;
					}
				} else {
					// reached EOL
					if (isCommentLine || len == 0) {
						isCommentLine = false;
						isNewLine = true;
						skipWhiteSpace = true;
						len = 0;
						continue;
					}
					if (inOff >= inLimit) {
						inLimit = (inStream == null) ? reader.read(inCharBuf) : inStream.read(inByteBuf);
						inOff = 0;
						if (inLimit <= 0) {
							return len;
						}
					}
					if (precedingBackslash) {
						len -= 1;
						// skip the leading whitespace characters in following
						// line
						skipWhiteSpace = true;
						appendedLineBegin = true;
						precedingBackslash = false;
						if (c == '\r') {
							skipLF = true;
						}
					} else {
						return len;
					}
				}
			}
		}
	}

}
