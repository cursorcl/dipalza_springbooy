package cl.eos.dipalza.exceptions;

public class MissingDataException extends RuntimeException {

	
	private static final long serialVersionUID = 1L;

	public MissingDataException(String message) {
        super(message);
    }
}
