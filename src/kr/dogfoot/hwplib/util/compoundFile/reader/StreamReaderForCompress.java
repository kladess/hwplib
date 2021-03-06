package kr.dogfoot.hwplib.util.compoundFile.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import kr.dogfoot.hwplib.object.fileheader.FileVersion;

import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;

/**
 * 압축된 스트림을 읽기 위한 객체
 * 
 * @author neolord
 */
public class StreamReaderForCompress extends StreamReader {
	/**
	 * 압축 풀린 데이터를 읽기 위한 InputStream
	 */
	private ByteArrayInputStream bis;

	/**
	 * 생성자. 압축된 스트림을 읽어 압축을 풀어서 압축 풀린 데이터로 InputStream을 만든다.
	 * 
	 * @param de
	 *            스트림을 가리키는 Apache POI 객체
	 * @param fileVersion
	 * @throws Exception
	 */
	public StreamReaderForCompress(DocumentEntry de, FileVersion fileVersion)
			throws Exception {
		setByteArrayInputStream(de);
		setFileVersion(fileVersion);
	}

	/**
	 * 압축된 스트림을 읽어 압축을 풀어서 압축 풀린 데이터로 InputStream을 만든다.
	 * 
	 * @param de
	 *            스트림을 가리키는 Apache POI 객체
	 * @throws Exception
	 */
	private void setByteArrayInputStream(DocumentEntry de) throws Exception {
		DocumentInputStream dis = new DocumentInputStream(de);
		byte[] compressed = getCompressedBytes(dis, de.getSize() - 8);
		dis.skip(4);
		int originSize = readOriginalSize(dis);
		dis.close();

		byte[] decompressed = decompress(compressed, originSize);
		if (originSize == 0x3ffff || originSize == decompressed.length) {
			bis = new ByteArrayInputStream(decompressed);
			setSize(decompressed.length);
		} else {
			throw new Exception("Decompressed bytes size is wrong.");
		}
	}

	/**
	 * 스트림에서 압축된 데이터를 읽는다.
	 * 
	 * @param dis
	 *            스트림을 읽기 위한 Apache POI InputStream 객체
	 * @param size
	 *            읽을 크기
	 * @return 압축된 데이터
	 * @throws IOException
	 */
	private byte[] getCompressedBytes(DocumentInputStream dis, int size)
			throws IOException {
		byte[] buffer = new byte[size];
		dis.read(buffer);
		return buffer;
	}

	/**
	 * 압축된 스트림에 끝에서 원본 데이터의 크기를 읽는다.
	 * 
	 * @param dis
	 *            스트림을 읽기 위한 InputStream 객체
	 * @return 원본 데이터의 크기
	 * @throws IOException
	 */
	private int readOriginalSize(DocumentInputStream dis) throws IOException {
		return dis.readInt();
	}

	/**
	 * 압축된 데이터를 풀어서 원본 데이터를 얻는다.
	 * 
	 * @param compressed
	 *            압축된 데이터
	 * @param originSize
	 *            원본 데이터 크기
	 * @return 원본 데이터
	 * @throws DataFormatException
	 * @throws IOException
	 */
	private byte[] decompress(byte[] compressed, int originSize)
			throws DataFormatException, IOException {
		byte[] result = new byte[originSize];
		Inflater decompresser = new Inflater(true);
		decompresser.setInput(compressed, 0, compressed.length);
		int resultLength = decompresser.inflate(result);
		decompresser.end();
		return Arrays.copyOfRange(result, 0, resultLength); 
	}

	@Override
	public void readBytes(byte[] buffer) throws IOException {
		forwardPosition(buffer.length);
		bis.read(buffer);
	}

	@Override
	public byte readSInt1() throws IOException {
		byte[] buffer = readBytes(1);
		return buffer[0];
	}

	/**
	 * n byte를 읽어서 byte 배열ㄴ을 반환한다.
	 * 
	 * @param n
	 *            읽을 바이트 수
	 * @return 새로 읽은 byte 배열
	 * @throws IOException
	 */
	private byte[] readBytes(int n) throws IOException {
		byte[] buffer = new byte[n];
		readBytes(buffer);
		return buffer;
	}

	@Override
	public short readSInt2() throws IOException {
		byte[] buffer = readBytes(2);
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
				.getShort();
	}

	@Override
	public int readSInt4() throws IOException {
		byte[] buffer = readBytes(4);
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	@Override
	public short readUInt1() throws IOException {
		return (short) (readSInt1() & 0xff);
	}

	@Override
	public int readUInt2() throws IOException {
		return readSInt2() & 0xffff;
	}

	@Override
	public long readUInt4() throws IOException {
		return readSInt4() & 0xffffffff;
	}

	@Override
	public double readDouble() throws IOException {
		byte[] buffer = readBytes(8);
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
				.getDouble();
	}

	@Override
	public float readFloat() throws IOException {
		byte[] buffer = readBytes(4);
		return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
				.getFloat();
	}

	@Override
	public void skip(long n) throws IOException {
		readBytes((int) n);
	}

	@Override
	public void close() throws IOException {
		bis.close();
		bis = null;
	}
}
