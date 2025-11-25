package com.edufelip.meer.core.store;

import jakarta.persistence.*;

@Entity
public class ThriftStorePhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "thrift_store_id")
    private ThriftStore thriftStore;

    @Column(nullable = false)
    private String url;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public ThriftStorePhoto() {}

    public ThriftStorePhoto(ThriftStore thriftStore, String url, Integer displayOrder) {
        this.thriftStore = thriftStore;
        this.url = url;
        this.displayOrder = displayOrder;
    }

    public Integer getId() { return id; }
    public ThriftStore getThriftStore() { return thriftStore; }
    public String getUrl() { return url; }
    public Integer getDisplayOrder() { return displayOrder; }

    public void setId(Integer id) { this.id = id; }
    public void setThriftStore(ThriftStore thriftStore) { this.thriftStore = thriftStore; }
    public void setUrl(String url) { this.url = url; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
