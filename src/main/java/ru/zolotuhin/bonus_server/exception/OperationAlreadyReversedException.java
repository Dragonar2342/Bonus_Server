package ru.zolotuhin.bonus_server.exception;

public class OperationAlreadyReversedException extends RuntimeException {
    public OperationAlreadyReversedException(Long operationId) {
        super("Операция с идентификатором " + operationId + " уже отменена");
    }
}
