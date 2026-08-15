package ru.zolotuhin.bonus_server.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Неверный логин или пароль.");
    }
}
