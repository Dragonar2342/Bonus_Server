package ru.zolotuhin.bonus_server.service.interfaces;

import ru.zolotuhin.bonus_server.dto.*;

public interface BonusCardService {
    BalanceResponse createCard(CreateCardRequest request);
    BalanceResponse accrue(
            String cardNumber,
            MoneyRequest request
    );
    BalanceResponse debit(
            String cardNumber,
            MoneyRequest request
    );
    OperationResponse reverse(
            String cardNumber,
            Long operationId
    );
    BalanceResponse getBalance(String cardNumber);
    PageResponse<OperationResponse> getOperations(
            String cardNumber,
            int page,
            int size
    );
}
