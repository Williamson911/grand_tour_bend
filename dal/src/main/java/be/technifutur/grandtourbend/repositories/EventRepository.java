package be.technifutur.grandtourbend.repositories;

import be.technifutur.grandtourbend.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    @Query("select count(e) > 0 from Event e where e.name ilike :name")
    boolean existsByName(String name);

    @Query("select count(e) > 0 from Event e where e.name ilike :name and e.id <> :id")
    boolean existsByNameAndIdNot(String name, UUID id);
}
