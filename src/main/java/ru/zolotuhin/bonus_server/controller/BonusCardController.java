package ru.zolotuhin.bonus_server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.zolotuhin.bonus_server.dto.*;
import ru.zolotuhin.bonus_server.service.interfaces.BonusCardService;

@RestController
@RequestMapping("/api/v1/bonus-cards")
@RequiredArgsConstructor
@Validated
public class BonusCardController {

    private final BonusCardService bonusCardService;

    @PostMapping
    public ResponseEntity<BalanceResponse> createCard(
            @Valid @RequestBody CreateCardRequest request
    ) { return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bonusCardService.createCard(request));
    }

    @PostMapping("/{cardNumber}/accruals")
    public ResponseEntity<BalanceResponse> accrue(
            @PathVariable String cardNumber,
            @Valid @RequestBody MoneyRequest request
    ) { return ResponseEntity.ok( bonusCardService.accrue(cardNumber, request ));
    }

    @PostMapping("/{cardNumber}/debits")
    public ResponseEntity<BalanceResponse> debit(
            @PathVariable String cardNumber,
            @Valid @RequestBody MoneyRequest request
    ) { return ResponseEntity.ok( bonusCardService.debit(cardNumber, request ));
    }

    @PostMapping("/{cardNumber}/operations/{operationId}/reversal")
    public ResponseEntity<OperationResponse> reverse(
            @PathVariable String cardNumber,
            @PathVariable @Positive Long operationId
    ) { return ResponseEntity.ok( bonusCardService.reverse(cardNumber, operationId));
    }

    @GetMapping("/{cardNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable String cardNumber
    ) { return ResponseEntity.ok( bonusCardService.getBalance(cardNumber));
    }

    @GetMapping("/{cardNumber}/operations")
    public ResponseEntity<PageResponse<OperationResponse>> getOperations(
            @PathVariable String cardNumber, @RequestParam(defaultValue = "0")
            @Min(0) int page, @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size
    ) { return ResponseEntity.ok( bonusCardService.getOperations(cardNumber, page, size));
    }
}
