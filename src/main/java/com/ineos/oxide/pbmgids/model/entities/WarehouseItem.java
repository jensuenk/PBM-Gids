package com.ineos.oxide.pbmgids.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse_item")
public class WarehouseItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pbm_id")
    private Pbm pbm;

    @Column(name = "warehouse_number")
    private String warehouseNumber;

    @Column(name = "variant_text")
    private String variantText;

    @Column(name = "published")
    private Boolean published;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Pbm getPbm() {
        return pbm;
    }

    public void setPbm(Pbm pbm) {
        this.pbm = pbm;
    }

    public String getWarehouseNumber() {
        return warehouseNumber;
    }

    public void setWarehouseNumber(String warehouseNumber) {
        this.warehouseNumber = warehouseNumber;
    }

    public String getVariantText() {
        return variantText;
    }

    public void setVariantText(String variantText) {
        this.variantText = variantText;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }
}
