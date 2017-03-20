package com.bravado.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;
import org.jpos.iso.packager.XMLPackager;

import bsh.This;

public class FileReader {
	private static String readFileAsString(String filename) throws IOException {
		List<String> fileContents = Files.readAllLines(Paths.get(filename),
				Charset.defaultCharset());

		StringBuilder builder = new StringBuilder();
		for (String line : fileContents) {
			builder.append(line);
		}

		return builder.toString();
	}

	public static ISOMsg readXMLMsg(String filename) throws IOException,
			ISOException {
		String fileContents = readFileAsString(filename);

		XMLPackager packager = new XMLPackager();
		ISOMsg msg = packager.createISOMsg();
		packager.unpack(msg, fileContents.getBytes());

		return msg;
	}

	public static ISOMsg readRawMsg(String filename) throws IOException,
			ISOException {
		String fileContents = readFileAsString(filename);

		ISO87APackager packager = new ISO87APackager();
		ISOMsg msg = packager.createISOMsg();
		packager.unpack(msg, fileContents.getBytes());

		return msg;
	}

	public static ISOMsg readRawMsgString(String fileContents)
			throws IOException, ISOException {
		ISO87APackager packager = new ISO87APackager();
		ISOMsg msg = packager.createISOMsg();
		packager.unpack(msg, fileContents.getBytes());

		return msg;
	}

	/**
	 * Parser ISO8586 MSG From Raw String Content
	 * 
	 * @param args
	 * @throws IOException
	 * @throws ISOException
	 */
	public static void main(String[] args) throws IOException, ISOException {
		String fileContent = args[0];
		ISOMsg msg = FileReader.readRawMsgString(fileContent);
		PrintStream s = new PrintStream(new File("iso.xml"));
		msg.dump(System.out, "	");
		msg.dump(s, "	");
		s.flush();
		s.close();
	}

}
