package com.zfabrik.test.dev.z2unit;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.zfabrik.z2unit.impl.Protocol;

public class ProtocolTests {

	@Test
	public void bytesToBytes() throws Exception {
		byte[] payload = "Hallo".getBytes(StandardCharsets.ISO_8859_1);
		String inter = Protocol.toBase64Chunk(payload);
		byte[] result = Protocol.fromBase64Chunk(new StringReader(inter));
		Assert.assertArrayEquals(payload, result);
	}
	
}
