package com.appointmenthub.service;

import com.appointmenthub.config.AppConfig;
import com.appointmenthub.error.ApiException;
import com.appointmenthub.error.ErrorCode;
import com.appointmenthub.json.Json;
import com.appointmenthub.model.Appointment;
import com.appointmenthub.model.AppointmentStatus;
import com.appointmenthub.model.AvailabilityResponse;
import com.appointmenthub.model.CreateAppointmentRequest;
import com.appointmenthub.model.Service;
import com.appointmenthub.model.TimeSlot;
import com.appointmenthub.store.AppointmentRepository;
import com.appointmenthub.store.ServiceCatalog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business logic of AppointmentHub: services, availability, booking and
 * cancellation.
 *
 * <p>All business rules (PDR section 9) are enforced here, on the server, so they
 * hold even when the web interface is bypassed (NFR-07). The class has no
 * knowledge of HTTP: the API layer only translates requests into calls and
 * {@link ApiException}s into responses.</p>
 *
 * <p><strong>Resource model.</strong> The first release models a single shared
 * resource: a confirmed appointment occupies its time interval for every
 * service, which is how BR-08 ("conflicting appointments for the same provider
 * or resource are not permitted") is interpreted.</p>
 *
 * <p><strong>Validation order</strong> (documented so tests can predict the exact
 * error): request body, required fields, field formats, service existence and
 * activity, date rules, time rules, then availability.</p>
 */
public final class BookingService {

    /** Documented email rule (BR-02). */
    public static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 100;

    private final AppConfig config;
    private final ServiceCatalog catalog;
    private final AppointmentRepository repository;

    public BookingService(AppConfig config, ServiceCatalog catalog, AppointmentRepository repository) {
        this.config = config;
        this.catalog = catalog;
        this.repository = repository;
    }

    public AppConfig config() {
        return config;
    }

    /** Services exposed by the API; inactive ones only when explicitly requested. */
    public List<Service> services(boolean includeInactive) {
        return includeInactive ? catalog.all() : catalog.active();
    }

    /** Available time slots for a service and date (FR-02). */
    public AvailabilityResponse availability(String rawServiceId, String rawDate) {
        int serviceId = parseRequiredInt(rawServiceId, "serviceId");
        LocalDate date = parseRequiredDate(rawDate, "date");
        Service service = requireActiveService(serviceId);
        return new AvailabilityResponse(service.id(), service.name(), date, service.durationMinutes(),
                availableSlots(service, date));
    }

    /** Creates an appointment after validating every business rule (FR-03, FR-04). */
    public Appointment create(CreateAppointmentRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.MISSING_BODY, "A JSON request body is required.");
        }
        String name = requireName(request.customerName());
        String email = requireEmail(request.customerEmail());
        if (request.serviceId() == null) {
            throw ApiException.validation("serviceId is required.");
        }
        LocalDate date = parseRequiredDate(request.appointmentDate(), "appointmentDate");
        LocalTime start = parseRequiredTime(request.startTime(), "startTime");

        Service service = requireActiveService(request.serviceId());
        int duration = service.durationMinutes();
        LocalDate today = LocalDate.now(config.clock());

        if (date.isBefore(today)) {
            throw new ApiException(ErrorCode.PAST_DATE,
                    "Appointments cannot be booked for a past date (" + date + ").");
        }

        List<LocalTime> alignedStarts = alignedStartTimes(service);
        if (!alignedStarts.contains(start)) {
            if (start.isBefore(config.businessOpen())
                    || start.plusMinutes(duration).isAfter(config.businessClose())) {
                throw new ApiException(ErrorCode.OUTSIDE_BUSINESS_HOURS,
                        "Appointment must fall within business hours ("
                                + config.businessOpen() + "-" + config.businessClose() + ").");
            }
            throw new ApiException(ErrorCode.SLOT_NOT_ALIGNED,
                    "startTime must match an available slot boundary of " + config.slotMinutes() + " minutes.");
        }

        LocalTime end = start.plusMinutes(duration);
        synchronized (repository) {
            if (isSlotOccupied(date, start, end)) {
                throw new ApiException(ErrorCode.SLOT_NOT_AVAILABLE,
                        "The time slot " + start + " on " + date + " is already occupied.");
            }
            if (date.isEqual(today) && !start.isAfter(LocalTime.now(config.clock()))) {
                throw new ApiException(ErrorCode.SLOT_NOT_AVAILABLE,
                        "The time slot " + start + " on " + date + " is no longer available because it has started.");
            }
            Appointment appointment = new Appointment(repository.nextId(), name, email, service.id(),
                    date, start, end, AppointmentStatus.CONFIRMED, Instant.now(config.clock()));
            return repository.save(appointment);
        }
    }

    /** Cancels a confirmed appointment (FR-07, FR-14). */
    public Appointment cancel(int id) {
        synchronized (repository) {
            Appointment appointment = repository.findById(id).orElseThrow(() -> new ApiException(
                    ErrorCode.APPOINTMENT_NOT_FOUND, "Appointment " + id + " does not exist."));
            if (appointment.status() == AppointmentStatus.CANCELLED) {
                throw new ApiException(ErrorCode.APPOINTMENT_ALREADY_CANCELLED,
                        "Appointment " + id + " is already cancelled.");
            }
            return repository.save(appointment.withStatus(AppointmentStatus.CANCELLED));
        }
    }

    /** Retrieves one appointment (FR-05). */
    public Appointment findById(int id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                ErrorCode.APPOINTMENT_NOT_FOUND, "Appointment " + id + " does not exist."));
    }

    /**
     * Retrieves registered appointments (FR-06).
     *
     * @param customerEmail optional case-insensitive filter; {@code null} returns everything
     */
    public List<Appointment> findAll(String customerEmail) {
        List<Appointment> appointments = repository.findAll();
        if (customerEmail == null || customerEmail.isBlank()) {
            return appointments;
        }
        String filter = customerEmail.trim().toLowerCase(Locale.ROOT);
        return appointments.stream()
                .filter(appointment -> appointment.customerEmail().toLowerCase(Locale.ROOT).equals(filter))
                .toList();
    }

    /** Number of stored appointments; used by tests and diagnostics. */
    public int storedAppointmentCount() {
        return repository.count();
    }

    /** Exposed for diagnostics: whether an appointment exists, without throwing. */
    public Optional<Appointment> maybeFindById(int id) {
        return repository.findById(id);
    }

    /**
     * Slot boundaries generated from the configured business hours and the
     * service duration (BR-05, BR-06).
     */
    private List<LocalTime> alignedStartTimes(Service service) {
        List<LocalTime> starts = new ArrayList<>();
        LocalTime candidate = config.businessOpen();
        while (!candidate.plusMinutes(service.durationMinutes()).isAfter(config.businessClose())) {
            starts.add(candidate);
            candidate = candidate.plusMinutes(config.slotMinutes());
        }
        return starts;
    }

    /** Available slots: within business hours, free, and not already started. */
    private List<TimeSlot> availableSlots(Service service, LocalDate date) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDate today = LocalDate.now(config.clock());
        if (date.isBefore(today)) {
            return slots;
        }
        LocalTime now = LocalTime.now(config.clock());
        for (LocalTime start : alignedStartTimes(service)) {
            LocalTime end = start.plusMinutes(service.durationMinutes());
            if (date.isEqual(today) && !start.isAfter(now)) {
                continue;
            }
            if (isSlotOccupied(date, start, end)) {
                continue;
            }
            slots.add(new TimeSlot(start, end));
        }
        return slots;
    }

    /** True when a confirmed appointment overlaps the requested interval (BR-07, BR-08). */
    private boolean isSlotOccupied(LocalDate date, LocalTime start, LocalTime end) {
        return repository.findAll().stream()
                .filter(appointment -> appointment.status() == AppointmentStatus.CONFIRMED)
                .filter(appointment -> appointment.appointmentDate().equals(date))
                .anyMatch(appointment -> appointment.overlaps(start, end));
    }

    private Service requireActiveService(int serviceId) {
        Service service = catalog.findById(serviceId).orElseThrow(() -> new ApiException(
                ErrorCode.SERVICE_NOT_FOUND, "Service " + serviceId + " does not exist."));
        if (!service.active()) {
            throw new ApiException(ErrorCode.SERVICE_INACTIVE,
                    "Service " + serviceId + " (" + service.name() + ") is not active and cannot be booked.");
        }
        return service;
    }

    private String requireName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.validation("customerName is required.");
        }
        String name = raw.trim();
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw ApiException.validation("customerName must contain between " + MIN_NAME_LENGTH
                    + " and " + MAX_NAME_LENGTH + " characters.");
        }
        return name;
    }

    private String requireEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.validation("customerEmail is required.");
        }
        String email = raw.trim();
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw ApiException.validation("customerEmail must be a valid email address.");
        }
        return email;
    }

    private int parseRequiredInt(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.validation(field + " is required.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw ApiException.validation(field + " must be an integer.");
        }
    }

    private LocalDate parseRequiredDate(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.validation(field + " is required.");
        }
        try {
            return LocalDate.parse(raw.trim(), Json.DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.INVALID_DATE_FORMAT,
                    field + " must use the format yyyy-MM-dd (received '" + raw + "').");
        }
    }

    private LocalTime parseRequiredTime(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.validation(field + " is required.");
        }
        try {
            return LocalTime.parse(raw.trim(), Json.TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.INVALID_TIME_FORMAT,
                    field + " must use the format HH:mm (received '" + raw + "').");
        }
    }
}
