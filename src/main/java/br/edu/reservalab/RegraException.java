package br.edu.reservalab;

public class RegraException extends RuntimeException {
    private final int httpStatus;
    public RegraException(int httpStatus, String mensagem) { super(mensagem); this.httpStatus = httpStatus; }
    public int httpStatus() { return httpStatus; }
}
