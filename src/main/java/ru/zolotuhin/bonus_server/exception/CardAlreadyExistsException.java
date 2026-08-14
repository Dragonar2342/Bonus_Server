package ru.zolotuhin.bonus_server.exception;

public class CardAlreadyExistsException extends RuntimeException {
    public CardAlreadyExistsException(String cardNumber) {
        super("Бонусная карта с номером " + cardNumber + " уже существует.");
    }
}
