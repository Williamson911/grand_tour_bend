package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.expenses.requests.ExpenseRequest;
import be.technifutur.grandtourbend.models.expenses.responses.ExpensesResponse;

import java.util.List;
import java.util.UUID;

public interface ExpensesService {
    List<ExpensesResponse> getAll(UUID userId);

    UUID create(UUID userId, UUID eventId, ExpenseRequest request);

    void delete(UUID id, UUID userId);

}
