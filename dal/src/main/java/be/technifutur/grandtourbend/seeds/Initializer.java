package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Address;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.enums.EventType;
import be.technifutur.grandtourbend.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Initializer implements CommandLineRunner {

    private final EventRepository eventRepository;

    @Override
    public void run(String... args) throws Exception {

        if(eventRepository.count() == 0) {
            var events = List.of(
                    new Event(
                            "Gladius",
                            EventType.REGIONAL,
                            LocalDate.now().plusMonths(1),
                            new Address("Liège", "Belgium", "Technifutur", 50.6326, 5.5797),
                            null
                    )
            );


            eventRepository.saveAll(events);
        }
    }
}