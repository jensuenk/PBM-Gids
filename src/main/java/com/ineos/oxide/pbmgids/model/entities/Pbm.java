package com.ineos.oxide.pbmgids.model.entities;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pbm")
public class Pbm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String brand;

    @Column(name = "type")
    private String typeName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "protects_against", columnDefinition = "text")
    private String protectsAgainst;

    @Column(name = "does_not_protect_against", columnDefinition = "text")
    private String doesNotProtectAgainst;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "usage_instructions", columnDefinition = "text")
    private String usageInstructions;

    @Column(columnDefinition = "text")
    private String distribution;

    @Column(columnDefinition = "text")
    private String standards;

    private String image;

    @Column(name = "published")
    private Boolean published;

    private Integer hits;

    @ManyToMany(mappedBy = "pbms")
    private Set<Category> categories = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "pbm_document", joinColumns = @JoinColumn(name = "pbm_id"), inverseJoinColumns = @JoinColumn(name = "document_id"))
    private Set<Document> documents = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "pbm_norm", joinColumns = @JoinColumn(name = "pbm_id"), inverseJoinColumns = @JoinColumn(name = "norm_id"))
    private Set<Norm> norms = new LinkedHashSet<>();

    @OneToMany(mappedBy = "pbm", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WarehouseItem> warehouseItems = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtectsAgainst() {
        return protectsAgainst;
    }

    public void setProtectsAgainst(String protectsAgainst) {
        this.protectsAgainst = protectsAgainst;
    }

    public String getDoesNotProtectAgainst() {
        return doesNotProtectAgainst;
    }

    public void setDoesNotProtectAgainst(String doesNotProtectAgainst) {
        this.doesNotProtectAgainst = doesNotProtectAgainst;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUsageInstructions() {
        return usageInstructions;
    }

    public void setUsageInstructions(String usageInstructions) {
        this.usageInstructions = usageInstructions;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getStandards() {
        return standards;
    }

    public void setStandards(String standards) {
        this.standards = standards;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public Set<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<Document> documents) {
        this.documents = documents;
    }

    public Set<Norm> getNorms() {
        return norms;
    }

    public void setNorms(Set<Norm> norms) {
        this.norms = norms;
    }

    public Set<WarehouseItem> getWarehouseItems() {
        return warehouseItems;
    }

    public void setWarehouseItems(Set<WarehouseItem> warehouseItems) {
        this.warehouseItems = warehouseItems;
    }
}
