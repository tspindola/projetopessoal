package com.bravado.util;

import java.nio.charset.Charset;
import org.jpos.iso.ISOUtil;

public class StringUtil {
	private static final Charset CHARSET  = Charset.forName("ISO8859_1");

	public static String hex2str(String s) {
		byte[] b = ISOUtil.hex2byte(s);
		return new String(b, CHARSET);
	}
	
	public static String getMaskCpfCnpj(String cpf_cnpj) {
		String maskCpfCnpj;
		
		if (cpf_cnpj.length() == 11) {
			maskCpfCnpj = cpf_cnpj.substring(0, 3) + "." + cpf_cnpj.substring(3, 6) + "." + cpf_cnpj.substring(6, 9) + "-"
					+ cpf_cnpj.substring(9);
		} else if (cpf_cnpj.length() == 14){
			maskCpfCnpj = cpf_cnpj.substring(0, 2) + "." + cpf_cnpj.substring(2, 5) + "." + cpf_cnpj.substring(5, 8) + "/"
					+ cpf_cnpj.substring(8, 12) + "-" + cpf_cnpj.substring(12);
		} else {
			maskCpfCnpj = cpf_cnpj;
		}
		return maskCpfCnpj;
	}	
}