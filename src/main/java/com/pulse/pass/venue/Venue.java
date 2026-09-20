package com.pulse.pass.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Recinto donde se celebran los eventos.
 * Regla de negocio: BR-002 (un Venue puede albergar cero o muchos Event).
 */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", length = 200)
    private String address;

    // La regla "capacity > 0" (FR-VEN-003) se refuerza mediante CHECK en PostgreSQL (V1),
    // no con Bean Validation, para que sea la base de datos quien la garantice (NFR-001).
    @NotNull
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Venue() {
        // Requerido por JPA
    }

    public Venue(String code, String name, String city, String address, Integer capacity, boolean active) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.capacity = capacity;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Venue{id=%d, code='%s', name='%s', city='%s', capacity=%d}"
                .formatted(id, code, name, city, capacity);
    }
}
