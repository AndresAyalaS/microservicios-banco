package com.devsu.cuenta.exception;

public class InsufficientBalanceException extends BusinessRuleException {
    public InsufficientBalanceException() {
        super("Saldo no disponible");
    }
}
