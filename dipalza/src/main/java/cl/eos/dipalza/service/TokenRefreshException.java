package cl.eos.dipalza.service;

public class TokenRefreshException extends Exception {

	private static final long serialVersionUID = 1L;

	public TokenRefreshException(String tokenHash, String text) {
		
		super(tokenHash + ":" + text);
	}

}
