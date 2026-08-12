package be.technifutur.grandtourbend.models.address.responses;

import be.technifutur.grandtourbend.entities.Address;

public record AddressResponse(
        String city,
        String country,
        String venue,
        Double lat,
        Double lng
) {

    public static AddressResponse fromAddress(Address a) {
        return new AddressResponse(
                a.getCity(),
                a.getCountry(),
                a.getVenue(),
                a.getLat(),
                a.getLng()
        );
    }
}
