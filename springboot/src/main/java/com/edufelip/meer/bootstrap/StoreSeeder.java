package com.edufelip.meer.bootstrap;

import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds a small catalog of stores for local development so /home and /stores have data.
 */
@Component
@Profile({"local-db", "local"})
public class StoreSeeder implements CommandLineRunner {

    private final ThriftStoreRepository storeRepo;

    public StoreSeeder(ThriftStoreRepository storeRepo) {
        this.storeRepo = storeRepo;
    }

    @Override
    public void run(String... args) {
        if (storeRepo.count() > 0) return; // keep idempotent for dev restarts

        seed(featureStores());
        seed(nearbyStores());
    }

    private void seed(List<ThriftStore> stores) {
        storeRepo.saveAll(stores);
    }

    private List<ThriftStore> featureStores() {
        return List.of(
                store("Vintage Vibes",
                        "Garimpos curados com pegada retrô",
                        "https://images.unsplash.com/photo-1441123694162-e54a981ceba3?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Rua Augusta, 123 - Pinheiros",
                        -23.5617, -46.6589,
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuATD7G9oKF2W1aQjxAqHpYvVPamBIvCIZ6Q7I74RHrH7zwrJHn7iFRGMdMEHWLTMlP9DQ7oquk7Frb_j9QaIiT7ZSYMjZJhvTjFAJU7U-X73PmboiSxOHwS4QZ9mIBO-fJWAbwbdWu5yfwTrXn0c6HHGRpI5fDlZ_HckG3G5-IAsF_Vsh98T6DdyXbPl0bdG-iC9J2bjl6tqGgQIoeItBfJUqcnWgrKl9Y05nEY0VjB15UkZf5t6v0xiO0VVOuXFpoAn1Z7WNfG-dc",
                        "Seg a Sáb: 10:00 - 19:00",
                        "Fecha aos domingos",
                        new Social(null, "@vintagevibes", null, null),
                        List.of("feminino", "vintage", "acessorios"),
                        0.5, 5, "Pinheiros", "Mais amado",
                        "Peças icônicas dos anos 70 e 80, curadoria semanal."
                ),
                store("Secondhand Chic",
                        "Peças de grife em segunda mão",
                        "https://images.unsplash.com/photo-1542293787938-4d273c37c1b8?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Av. Paulista, 456 - Bela Vista",
                        -23.5614, -46.6558,
                        null,
                        "Seg a Sex: 11:00 - 18:00",
                        null,
                        new Social(null, "@secondhandchic", null, null),
                        List.of("feminino", "luxo"),
                        1.2, 15, "Vila Madalena", null,
                        "Grifes seminovas autenticadas e em ótimo estado."
                ),
                store("Thrift Haven",
                        "Achadinhos baratos e estilosos",
                        "https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1524606894343-1e1a4c71d692?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Rua 7 de Abril, 90 - Centro",
                        -23.546, -46.638,
                        null,
                        "Ter a Dom: 10:00 - 20:00",
                        null,
                        new Social(null, "@thrifthaven", null, null),
                        List.of("vintage", "casual"),
                        2.4, 28, "Centro", null,
                        "Racks sempre renovados com preços acessíveis."
                ),
                store("Eco Trends",
                        "Slow fashion e peças eco-friendly",
                        "https://images.unsplash.com/photo-1503341455253-b2e723bb3dbb?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Alameda dos Maracatins, 300 - Moema",
                        -23.6101, -46.6675,
                        null,
                        "Seg a Sáb: 11:00 - 19:00",
                        null,
                        new Social(null, "@ecotrends", null, null),
                        List.of("sustentavel", "unissex"),
                        3.1, 35, "Augusta", null,
                        "Curadoria eco-friendly com marcas locais."
                )
        );
    }

    private List<ThriftStore> nearbyStores() {
        return List.of(
                store("Garimpo Urbano",
                        "Street + vintage na Consolação",
                        "https://images.unsplash.com/photo-1503341455253-b2e723bb3dbb?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Rua da Consolação, 210",
                        null, null,
                        null,
                        "Seg a Sáb: 10:00 - 19:30",
                        null,
                        new Social(null, "@garimpourbano", null, null),
                        List.of("street", "vintage"),
                        0.6, 8, "Augusta", null,
                        null
                ),
                store("Querido Brechó",
                        "Curadoria feminina e genderless",
                        "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1524606894343-1e1a4c71d692?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Rua Sabará, 41",
                        null, null,
                        null,
                        "Seg a Sex: 11:00 - 18:30",
                        null,
                        new Social(null, "@queridobrecho", null, null),
                        List.of("feminino", "genderless"),
                        1.2, 15, "Pinheiros", null,
                        null
                ),
                store("Revive Vintage",
                        "Peças clássicas restauradas",
                        "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=1600&q=80",
                        List.of(
                                "https://images.unsplash.com/photo-1503341455253-b2e723bb3dbb?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80",
                                "https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80"
                        ),
                        "Rua São Carlos do Pinhal, 15",
                        null, null,
                        null,
                        "Qua a Dom: 12:00 - 20:00",
                        null,
                        new Social(null, "@revivevintage", null, null),
                        List.of("vintage", "restauracao"),
                        2.1, 25, "Centro", null,
                        "Peças clássicas restauradas."
                )
        );
    }

    private ThriftStore store(String name,
                              String tagline,
                              String coverImageUrl,
                              List<String> galleryUrls,
                              String addressLine,
                              Double lat,
                              Double lng,
                              String mapImageUrl,
                              String openingHours,
                              String openingHoursNotes,
                              Social social,
                              List<String> categories,
                              Double distanceKm,
                              Integer walkTimeMinutes,
                              String neighborhood,
                              String badgeLabel,
                              String description) {
        ThriftStore s = new ThriftStore();
        s.setName(name);
        s.setTagline(tagline);
        s.setCoverImageUrl(coverImageUrl);
        s.setGalleryUrls(galleryUrls);
        s.setAddressLine(addressLine);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setMapImageUrl(mapImageUrl);
        s.setOpeningHours(openingHours);
        s.setOpeningHoursNotes(openingHoursNotes);
        s.setSocial(social);
        s.setCategories(categories);
        s.setDistanceKm(distanceKm);
        s.setWalkTimeMinutes(walkTimeMinutes);
        s.setNeighborhood(neighborhood);
        s.setBadgeLabel(badgeLabel);
        s.setDescription(description);
        return s;
    }
}
