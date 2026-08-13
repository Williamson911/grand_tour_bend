package be.technifutur.grandtourbend.models.expenses.requests;

import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.Expenses;
import be.technifutur.grandtourbend.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ExpenseRequest(
        @NotBlank String category,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        String notes
) {

    public Expenses toExpenses(User user, Event event) {
        return new Expenses(user, event, category, amount, currency, notes);
    }
}
