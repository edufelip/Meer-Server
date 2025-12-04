package com.edufelip.meer.bootstrap;

import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Instant;
import java.util.ArrayList;

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

        var feature = featureStores();
        var nearby = nearbyStores();
        seed(feature);
        seed(nearby);
        seedContents(feature);
    }

    private void seed(List<ThriftStore> stores) {
        storeRepo.saveAll(stores);
    }

    private List<ThriftStore> featureStores() {
        return List.of(
                store("Vintage Vibes",
                        "Garimpos curados com pegada retrô",
                        "https://storage.googleapis.com/meerbucket/stores/seed/vintage/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/vintage/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/vintage/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/vintage/3.jpg"
                        ),
                        "Rua Augusta, 123 - Pinheiros",
                        -23.5617, -46.6589,
                        "Seg a Sáb: 10:00 - 19:00",
                        new Social(null, "@vintagevibes", null),
                        List.of("feminino", "vintage", "acessorios"),
                        "Pinheiros", "Mais amado",
                        "Peças icônicas dos anos 70 e 80, curadoria semanal."
                ),
                store("Secondhand Chic",
                        "Peças de grife em segunda mão",
                        "https://storage.googleapis.com/meerbucket/stores/seed/secondhand/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/secondhand/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/secondhand/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/secondhand/3.jpg"
                        ),
                        "Av. Paulista, 456 - Bela Vista",
                        -23.5614, -46.6558,
                        "Seg a Sex: 11:00 - 18:00",
                        new Social(null, "@secondhandchic", null),
                        List.of("feminino", "luxo"),
                        "Vila Madalena", null,
                        "Grifes seminovas autenticadas e em ótimo estado."
                ),
                store("Thrift Haven",
                        "Achadinhos baratos e estilosos",
                        "https://storage.googleapis.com/meerbucket/stores/seed/thrift/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/thrift/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/thrift/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/thrift/3.jpg"
                        ),
                        "Rua 7 de Abril, 90 - Centro",
                        -23.546, -46.638,
                        "Ter a Dom: 10:00 - 20:00",
                        new Social(null, "@thrifthaven", null),
                        List.of("vintage", "casual"),
                        "Centro", null,
                        "Racks sempre renovados com preços acessíveis."
                ),
                store("Eco Trends",
                        "Slow fashion e peças eco-friendly",
                        "https://storage.googleapis.com/meerbucket/stores/seed/eco/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/eco/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/eco/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/eco/3.jpg"
                        ),
                        "Alameda dos Maracatins, 300 - Moema",
                        -23.6101, -46.6675,
                        "Seg a Sáb: 11:00 - 19:00",
                        new Social(null, "@ecotrends", null),
                        List.of("sustentavel", "unissex"),
                        "Augusta", null,
                        "Curadoria eco-friendly com marcas locais."
                )
        );
    }

    private List<ThriftStore> nearbyStores() {
        return List.of(
                store("Garimpo Urbano",
                        "Street + vintage na Consolação",
                        "https://storage.googleapis.com/meerbucket/stores/seed/garimpo/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/garimpo/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/garimpo/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/garimpo/3.jpg"
                        ),
                        "Rua da Consolação, 210",
                        null, null,
                        "Seg a Sáb: 10:00 - 19:30",
                        new Social(null, "@garimpourbano", null),
                        List.of("street", "vintage"),
                        "Augusta", null,
                        null
                ),
                store("Querido Brechó",
                        "Curadoria feminina e genderless",
                        "https://storage.googleapis.com/meerbucket/stores/seed/querido/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/querido/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/querido/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/querido/3.jpg"
                        ),
                        "Rua Sabará, 41",
                        null, null,
                        "Seg a Sex: 11:00 - 18:30",
                        new Social(null, "@queridobrecho", null),
                        List.of("feminino", "genderless"),
                        "Pinheiros", null,
                        null
                ),
                store("Revive Vintage",
                        "Peças clássicas restauradas",
                        "https://storage.googleapis.com/meerbucket/stores/seed/revive/cover.jpg",
                        List.of(
                                "https://storage.googleapis.com/meerbucket/stores/seed/revive/1.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/revive/2.jpg",
                                "https://storage.googleapis.com/meerbucket/stores/seed/revive/3.jpg"
                        ),
                        "Rua São Carlos do Pinhal, 15",
                        null, null,
                        "Qua a Dom: 12:00 - 20:00",
                        new Social(null, "@revivevintage", null),
                        List.of("vintage", "restauracao"),
                        "Centro", null,
                        "Peças clássicas restauradas."
                )
        );
    }

    private ThriftStore store(String name,
                              String tagline,
                              String coverImageUrl,
                              List<String> photoUrls,
                              String addressLine,
                              Double lat,
                              Double lng,
                              String openingHours,
                              Social social,
                              List<String> categories,
                              String neighborhood,
                              String badgeLabel,
                              String description) {
        ThriftStore s = new ThriftStore();
        s.setName(name);
        s.setTagline(tagline);
        s.setCoverImageUrl(coverImageUrl);
        s.setGalleryUrls(null); // legacy field unused in responses
        s.setAddressLine(addressLine);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setOpeningHours(openingHours);
        s.setSocial(social);
        s.setCategories(categories);
        s.setNeighborhood(neighborhood);
        s.setBadgeLabel(badgeLabel);
        s.setDescription(description);
        Instant now = Instant.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);

        // Populate photos list for API responses (ordered)
        var photos = new ArrayList<ThriftStorePhoto>();
        int order = 0;
        if (coverImageUrl != null && !coverImageUrl.isBlank()) {
            photos.add(new ThriftStorePhoto(s, coverImageUrl, order++));
        }
        if (photoUrls != null) {
            for (String url : photoUrls) {
                photos.add(new ThriftStorePhoto(s, url, order++));
            }
        }
        s.setPhotos(photos);
        if (!photos.isEmpty()) {
            s.setCoverImageUrl(photos.get(0).getUrl());
        }
        return s;
    }

    private void seedContents(List<ThriftStore> featureStores) {
        if (featureStores.isEmpty()) return;
        ThriftStore ref = featureStores.get(0);
        var contents = List.of(
                new com.edufelip.meer.core.content.GuideContent(
                        null,
                        "Dicas para um garimpo de sucesso",
                        "Aprenda a encontrar as melhores peças em brechós.",
                        "Guia de estilo",
                        "article",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuASh-dosAr4TVNex49PUkBKFWcLJ5g7HOQJC7p6SaRZyNznaks3TiQuWOksGvrnYi6IeO5sMPBjaerUTI7HzO4xaF5jyAX9NkZS80VOX-lpdnJIHTDaM3nL8ANPzyy3T2OEPfbqu2cuQlC-_PdB_tFutmEey75ynvkAcO3CQis8asojk9mkENmn1Hg88uqHJEOxr2z8LyIELsQfsWo_vVdfdLbws8VFobNPLNE5cMP-Snp3CsMplvntxVg4BQTHBAk7pgXTv1Px3Ls",
                        ref
                ),
                new com.edufelip.meer.core.content.GuideContent(
                        null,
                        "Tour pelo brechó: novidades da semana",
                        "Veja os achados que chegaram nesta semana no nosso espaço.",
                        "Vídeo",
                        "video",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuBmfZnwoz2FyAt09OQcXxSOWFu-nzy3A0N7Y8dSUZmyVxMgOCUCY3KYMJkp8VWQmVt_eEPzL69-jCYmwaqq52hdiViVNBeuOIy35QjmobKVUKmv0XujKAj04kCQYbEijnAQ84NSgvQ2618bew_ido3S_RsVHz9SiI3adBYqbWWILbs5CkJk5nZnxcQK4mTayuijLFbB_TIx3KsrW2ONKeAeFmq2bGqv-C1QFs-D3V9stPlgaG84Q31ythQGOQjNwpsW6FmOwIdo2us",
                        ref
                ),
                new com.edufelip.meer.core.content.GuideContent(
                        null,
                        "Como cuidar das suas peças vintage",
                        "Guia rápido para conservar tecidos delicados e couro antigo.",
                        "Dica Rápida",
                        "article",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuBmfZnwoz2FyAt09OQcXxSOWFu-nzy3A0N7Y8dSUZmyVxMgOCUCY3KYMJkp8VWQmVt_eEPzL69-jCYmwaqq52hdiViVNBeuOIy35QjmobKVUKmv0XujKAj04kCQYbEijnAQ84NSgvQ2618bew_ido3S_RsVHz9SiI3adBYqbWWILbs5CkJk5nZnxcQK4mTayuijLFbB_TIx3KsrW2ONKeAeFmq2bGqv-C1QFs-D3V9stPlgaG84Q31ythQGOQjNwpsW6FmOwIdo2us",
                        ref
                ),
                new com.edufelip.meer.core.content.GuideContent(
                        null,
                        "Look do dia com achados do brechó",
                        "Inspire-se com combinações práticas para o cotidiano.",
                        "Post",
                        "post",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuBmfZnwoz2FyAt09OQcXxSOWFu-nzy3A0N7Y8dSUZmyVxMgOCUCY3KYMJkp8VWQmVt_eEPzL69-jCYmwaqq52hdiViVNBeuOIy35QjmobKVUKmv0XujKAj04kCQYbEijnAQ84NSgvQ2618bew_ido3S_RsVHz9SiI3adBYqbWWILbs5CkJk5nZnxcQK4mTayuijLFbB_TIx3KsrW2ONKeAeFmq2bGqv-C1QFs-D3V9stPlgaG84Q31ythQGOQjNwpsW6FmOwIdo2us",
                        ref
                )
        );
        // attach and persist
        ref.setContents(contents);
        storeRepo.save(ref);
    }
}
