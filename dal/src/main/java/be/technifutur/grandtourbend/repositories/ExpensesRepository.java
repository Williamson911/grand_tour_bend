package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, UUID> {
    List<Expenses> findByUser_Id(UUID userId);

    void deleteAllByUser_Id(UUID userId);
}
