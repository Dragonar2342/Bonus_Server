package ru.zolotuhin.bonus_server.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String username) {
        super("Пользователь с логином " + username + " уже существует.");
    }
}
