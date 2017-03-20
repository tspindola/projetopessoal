package com.bravado.util;

import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

public class CardUtil {

	private static LuhnCheckDigit luhnCheckDigit = new LuhnCheckDigit();

	private static String substringBefore(String str, String separator) {
		if (str.isEmpty() || separator.isEmpty()) {
			return str;
		}
		int pos = str.indexOf(separator);
		if (pos == -1) {
			return str;
		}
		return str.substring(0, pos);
	}

	public static String extractPanFromTrack(String track2Data) {
		final String delimiter = "=";
		return substringBefore(track2Data, delimiter);
	}

	public static String stripPadFromPan(String panData) {
		final String padChar = "F";
		int padIndex = panData.indexOf(padChar);
		return panData.substring(0, padIndex);
	}

	public static String extractBinFromTrack(String track2Data) {
		final int binSize = 6;
		return track2Data.substring(0, binSize);
	}

	public static CardIssuer getIssuerFromBin(String bin) {
		String bandeira = bin.substring(0, 1);
		if (bandeira.equals("4")) {
			return CardIssuer.Visa;
		} else if (bandeira.equals("6")) {
			return CardIssuer.Maestro;
		} else {
			return CardIssuer.Mastercard;
		}
	}

	public static boolean validateCreditCardNumberLuhn(String pan) {
		return luhnCheckDigit.isValid(pan);
	}
}
