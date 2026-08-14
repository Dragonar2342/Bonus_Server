package ru.zolotuhin.bonus_server.exception;

public class OperationNotFoundException extends RuntimeException {
    public OperationNotFoundException(Long operationId) {
        super("Бонусная операция с идентификатором " + operationId + " не найдена.");
    }
}
