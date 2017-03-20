package com.bravado.util;

import java.nio.charset.Charset;
import org.jpos.iso.ISOUtil;

public class StringUtil {
	private static final Charset CHARSET  = Charset.forName("ISO8859_1");

	public static String hex2str(String s) {
		byte[] b = ISOUtil.hex2byte(s);
		return new String(b, CHARSET);
	}
}