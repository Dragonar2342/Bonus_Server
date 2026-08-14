package ru.zolotuhin.bonus_server.exception;

public class OperationPersistenceException extends RuntimeException {
    public OperationPersistenceException(Throwable cause) {
        super("Не удалось сохранить бонусную операцию.", cause);
    }
}
