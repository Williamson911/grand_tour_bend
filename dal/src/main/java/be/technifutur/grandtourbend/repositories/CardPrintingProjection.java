package be.technifutur.grandtourbend.repositories;

import java.util.UUID;

public interface CardPrintingProjection {
    UUID getCardId();
    UUID getVariantId();
    String getName();
    String getBackName();
    String getCardType();
    String getColor();
    String getCardNumber();
    String getSeries();
    String getRarity();
    String getImgLink();
}
