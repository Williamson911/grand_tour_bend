package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Address;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.EventType;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Initializer implements CommandLineRunner {

    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;

    @Override
    public void run(String... args) throws Exception {

        if (eventTypeRepository.count() == 0) {
            eventTypeRepository.saveAll(List.of(
                    new EventType("Regional", "🏆", "#d4af37"),
                    new EventType("Finals", "🥇", "#c0392b")
            ));
        }

        if(eventRepository.count() == 0) {
            EventType regional = eventTypeRepository.findAll().getFirst();

            var events = List.of(
                    new Event(
                            "Gladius",
                            regional,
                            LocalDate.now().plusMonths(1),
                            new Address("Liège", "Belgium", "Technifutur", 50.6326, 5.5797),
                            null
                    )
            );


            eventRepository.saveAll(events);
        }
    }
}