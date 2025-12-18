package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.store.ThriftStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ThriftStoreRepository extends JpaRepository<ThriftStore, UUID> {

    @Query("select distinct t from ThriftStore t join t.categories c where lower(c) = lower(:categoryId)")
    Page<ThriftStore> findByCategoryId(@Param("categoryId") String categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "photos")
    java.util.List<ThriftStore> findTop10ByOrderByCreatedAtDesc();

    /**
     * Preferred KNN using PostGIS geography (requires postgis extension).
     */
    @Query(
            value = """
                SELECT * FROM thrift_store ts
                WHERE ts.latitude IS NOT NULL AND ts.longitude IS NOT NULL
                ORDER BY geography(ST_SetSRID(ST_MakePoint(ts.longitude, ts.latitude), 4326))
                         <-> geography(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
                """,
            countQuery = """
                SELECT count(*) FROM thrift_store ts
                WHERE ts.latitude IS NOT NULL AND ts.longitude IS NOT NULL
                """,
            nativeQuery = true)
    Page<ThriftStore> findNearbyGeography(@Param("lat") double lat, @Param("lng") double lng, Pageable pageable);

    /**
     * Fast path for Postgres using KNN on point; expects a GiST index on point(longitude, latitude).
     */
    @Query(
            value = """
                SELECT * FROM thrift_store ts
                WHERE ts.latitude IS NOT NULL AND ts.longitude IS NOT NULL
                ORDER BY point(ts.longitude, ts.latitude) <-> point(:lng, :lat)
                """,
            countQuery = """
                SELECT count(*) FROM thrift_store ts
                WHERE ts.latitude IS NOT NULL AND ts.longitude IS NOT NULL
                """,
            nativeQuery = true)
    Page<ThriftStore> findNearbyKnn(@Param("lat") double lat, @Param("lng") double lng, Pageable pageable);

    /**
     * Portable fallback using Haversine; works on H2 and Postgres but without KNN index acceleration.
     */
    @Query(
            value = """
                SELECT * FROM thrift_store ts
                WHERE ts.latitude IS NOT NULL AND ts.longitude IS NOT NULL
                ORDER BY (6371 * acos(
                    LEAST(1, cos(radians(:lat)) * cos(radians(ts.latitude)) *
                              cos(radians(ts.longitude) - radians(:lng)) +
                              sin(radians(:lat)) * sin(radians(ts.latitude))
                    ))) ASC
                """,
            countQuery = """
                SELECT count(*) FROM thrift_store ts
                WHERE ts.latitude IS NOT NULL AND ts.longitude IS NOT NULL
                """,
            nativeQuery = true)
    Page<ThriftStore> findNearbyHaversine(@Param("lat") double lat, @Param("lng") double lng, Pageable pageable);

    @Query("""
        select t from ThriftStore t
        where lower(t.name) like lower(concat('%', :q, '%'))
           or lower(t.tagline) like lower(concat('%', :q, '%'))
           or lower(t.description) like lower(concat('%', :q, '%'))
           or lower(t.neighborhood) like lower(concat('%', :q, '%'))
        """)
    Page<ThriftStore> search(@Param("q") String q, Pageable pageable);

    @Query("""
        select t from ThriftStore t
        where lower(t.name) like lower(concat('%', :q, '%'))
           or lower(t.tagline) like lower(concat('%', :q, '%'))
           or lower(t.description) like lower(concat('%', :q, '%'))
           or lower(t.neighborhood) like lower(concat('%', :q, '%'))
        order by
            case
                when lower(t.name) = lower(:q) then 0
                when lower(t.name) like lower(concat(:q, '%')) then 1
                when locate(lower(:q), lower(t.name)) > 0 then 2
                when lower(t.tagline) like lower(concat('%', :q, '%')) then 3
                when lower(t.description) like lower(concat('%', :q, '%')) then 4
                when lower(t.neighborhood) like lower(concat('%', :q, '%')) then 5
                else 6
            end,
            locate(lower(:q), lower(t.name)),
            length(t.name),
            t.createdAt desc
        """)
    Page<ThriftStore> searchRanked(@Param("q") String q, Pageable pageable);

    List<ThriftStore> findByOwnerId(UUID ownerId);
}
