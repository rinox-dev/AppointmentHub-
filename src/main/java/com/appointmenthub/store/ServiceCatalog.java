package com.appointmenthub.store;

import com.appointmenthub.model.Service;
import java.util.List;
import java.util.Optional;

/**
 * Preconfigured service catalog (PDR sections 3.3 and 6.3).
 *
 * <p>Services are part of the initial configuration instead of being managed
 * through an administrative portal, which is explicitly out of scope.</p>
 *
 * <p>Service 5 is deliberately inactive: it exists to verify BR-03
 * ("only active services may be booked").</p>
 */
public final class ServiceCatalog {

    private final List<Service> services;

    public ServiceCatalog() {
        this(defaultCatalog());
    }

    public ServiceCatalog(List<Service> services) {
        this.services = List.copyOf(services);
    }

    /** The catalog shipped with the first release. */
    public static List<Service> defaultCatalog() {
        return List.of(
                new Service(1, "General Consultation", 30, true),
                new Service(2, "Dental Cleaning", 30, true),
                new Service(3, "Physiotherapy Session", 60, true),
                new Service(4, "Nutrition Assessment", 45, true),
                new Service(5, "Legacy Wellness Check", 30, false));
    }

    public List<Service> all() {
        return services;
    }

    /** Active services, ordered by identifier (FR-01). */
    public List<Service> active() {
        return services.stream().filter(Service::active).toList();
    }

    public Optional<Service> findById(int id) {
        return services.stream().filter(service -> service.id() == id).findFirst();
    }
}
