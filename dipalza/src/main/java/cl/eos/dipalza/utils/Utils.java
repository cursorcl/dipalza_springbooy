package cl.eos.dipalza.utils;

import java.time.ZoneId;

public class Utils {
	
	public static ZoneId ZONE = ZoneId.systemDefault();

	/**
	 * Convierte el número a string colocando tantos ceros (0) a la izquierda como
	 * sean necesarios para alcanzar el largo solicitado.
	 * 
	 * @param number El número a convertir
	 * @param len El largo total del resultado
	 * @return el string representando al número con ceros a la izquierda
	 */
	public static String putZeroesAtBegin(int number, int len) {
		String mask = "%0" + len + "d";
		return String.format(mask, number);
	}

	public static String putStrAtBegin(String source, char str, int len) {
		String result = "";

		result = source;
		if (source.length() < len) {
			for (int n = 0; n < len - source.length(); ++n) {
				result = String.valueOf(str) + result;
			}
		}
		return result;
	}
}
