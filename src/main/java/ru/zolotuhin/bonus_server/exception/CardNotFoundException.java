package ru.zolotuhin.bonus_server.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String cardNumber) {
        super("Бонусная карта с номером " + cardNumber + " не найдена.");
    }
}
