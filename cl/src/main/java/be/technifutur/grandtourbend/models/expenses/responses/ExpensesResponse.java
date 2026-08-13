package be.technifutur.grandtourbend.models.expenses.responses;

import be.technifutur.grandtourbend.entities.Expenses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpensesResponse(
        UUID id,
        UUID userId,
        UUID eventId,
        String category,
        BigDecimal amount,
        String  currency,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExpensesResponse fromExpenses(Expenses e) {
    return new ExpensesResponse(
            e.getId(),
            e.getUser().getId(),
            e.getEvent().getId(),
            e.getCategory(),
            e.getAmount(),
            e.getCurrency(),
            e.getNotes(),
            e.getCreatedAt(),
            e.getUpdatedAt()
    );
}
}
