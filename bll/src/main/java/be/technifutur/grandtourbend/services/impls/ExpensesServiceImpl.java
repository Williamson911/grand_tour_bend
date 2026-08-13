package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.ExpensesService;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.Expenses;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.EventNotFoundException;
import be.technifutur.grandtourbend.exceptions.ExpenseNotFoundException;
import be.technifutur.grandtourbend.models.expenses.requests.ExpenseRequest;
import be.technifutur.grandtourbend.models.expenses.responses.ExpensesResponse;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.ExpensesRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpensesServiceImpl implements ExpensesService {

    private final ExpensesRepository expensesRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ExpensesResponse> getAll(UUID userId) {
        return expensesRepository.findByUser_Id(userId)
                .stream()
                .map(ExpensesResponse::fromExpenses)
                .toList();
    }

    @Override
    public UUID create(UUID userId, UUID eventId, ExpenseRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Authenticated user " + userId + " not found")
        );

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new EventNotFoundException("Event with id " + eventId + " not found")
        );

        Expenses expenses = request.toExpenses(user, event);

        return expensesRepository.save(expenses).getId();
    }

    @Override
    public void delete(UUID id, UUID userId) {
        Expenses expenses = expensesRepository.findById(id).orElseThrow(() ->
                new ExpenseNotFoundException("Expense with id " + id + " not found")
        );

        if (!expenses.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this expense");
        }

        expensesRepository.delete(expenses);
    }
}
