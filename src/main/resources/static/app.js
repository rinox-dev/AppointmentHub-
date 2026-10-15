/**
 * AppointmentHub web interface.
 *
 * The interface is intentionally thin: it collects input, calls the REST API and
 * renders whatever the server returns. It never decides on its own whether a
 * booking is valid (NFR-07, BR-12): the displayed result always comes from the
 * API response.
 *
 * Stable `data-testid` attributes are provided for the Selenium tests.
 */
(function () {
  'use strict';

  var state = {
    services: [],
    selectedService: null,
    selectedSlot: null
  };

  function testId(name) {
    return document.querySelector('[data-testid="' + name + '"]');
  }

  function pad(value) {
    return String(value).padStart(2, '0');
  }

  function todayIso() {
    var now = new Date();
    return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate());
  }

  /** Calls the API and returns {status, ok, payload}. Never throws on HTTP errors. */
  function api(method, path, body) {
    var options = { method: method, headers: {} };
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }
    return fetch(path, options).then(function (response) {
      return response.text().then(function (text) {
        var payload = null;
        if (text) {
          try {
            payload = JSON.parse(text);
          } catch (ignored) {
            payload = null;
          }
        }
        return { status: response.status, ok: response.ok, payload: payload };
      });
    });
  }

  function clearMessages() {
    var success = testId('message-success');
    var error = testId('message-error');
    success.hidden = true;
    success.textContent = '';
    error.hidden = true;
    error.textContent = '';
  }

  function showSuccess(message) {
    clearMessages();
    var success = testId('message-success');
    success.textContent = message;
    success.hidden = false;
  }

  function showError(message) {
    clearMessages();
    var error = testId('message-error');
    error.textContent = message;
    error.hidden = false;
  }

  function errorMessage(result, fallback) {
    if (result.payload && result.payload.message) {
      return result.payload.message;
    }
    return fallback + ' (HTTP ' + result.status + ')';
  }

  function loadServices() {
    return api('GET', '/api/services').then(function (result) {
      var container = testId('service-list');
      container.innerHTML = '';
      if (!result.ok || !Array.isArray(result.payload)) {
        container.appendChild(hint('Unable to load services.'));
        return;
      }
      state.services = result.payload;
      if (state.services.length === 0) {
        container.appendChild(hint('No active services are available.'));
        return;
      }
      state.services.forEach(function (service) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'service-item';
        button.setAttribute('data-testid', 'service-item');
        button.setAttribute('data-service-id', service.id);
        button.setAttribute('data-service-name', service.name);
        button.setAttribute('aria-pressed', 'false');
        button.textContent = service.name;
        var duration = document.createElement('span');
        duration.className = 'duration';
        duration.textContent = service.durationMinutes + ' minutes';
        button.appendChild(duration);
        button.addEventListener('click', function () {
          selectService(service);
        });
        container.appendChild(button);
      });
    });
  }

  function hint(text) {
    var element = document.createElement('p');
    element.className = 'hint';
    element.textContent = text;
    return element;
  }

  function selectService(service) {
    state.selectedService = service;
    state.selectedSlot = null;
    testId('service-selected').textContent = 'Selected service: ' + service.name
      + ' (' + service.durationMinutes + ' minutes)';
    testId('selected-slot').textContent = 'No time slot selected yet.';
    document.querySelectorAll('[data-testid="service-item"]').forEach(function (button) {
      button.setAttribute('aria-pressed',
        String(Number(button.getAttribute('data-service-id')) === service.id));
    });
    clearMessages();
    loadAvailability();
  }

  function loadAvailability() {
    var container = testId('slot-list');
    var date = testId('date-input').value;
    if (!state.selectedService) {
      container.innerHTML = '';
      container.appendChild(hint('Select a service and a date to see available time slots.'));
      return Promise.resolve();
    }
    if (!date) {
      container.innerHTML = '';
      container.appendChild(hint('Select an appointment date.'));
      return Promise.resolve();
    }
    container.innerHTML = '';
    var loading = hint('Loading time slots...');
    loading.setAttribute('data-testid', 'slots-loading');
    container.appendChild(loading);
    state.selectedSlot = null;
    testId('selected-slot').textContent = 'No time slot selected yet.';
    return api('GET', '/api/availability?serviceId=' + state.selectedService.id + '&date=' + date)
      .then(function (result) {
        container.innerHTML = '';
        if (!result.ok) {
          container.appendChild(hint(errorMessage(result, 'Unable to load availability.')));
          return;
        }
        var slots = (result.payload && result.payload.slots) || [];
        if (slots.length === 0) {
          container.appendChild(hint('No time slots are available for the selected date.'));
          return;
        }
        slots.forEach(function (slot) {
          var button = document.createElement('button');
          button.type = 'button';
          button.className = 'slot-item';
          button.setAttribute('data-testid', 'slot-item');
          button.setAttribute('data-start-time', slot.startTime);
          button.setAttribute('aria-pressed', 'false');
          button.textContent = slot.startTime + ' - ' + slot.endTime;
          button.addEventListener('click', function () {
            selectSlot(slot);
          });
          container.appendChild(button);
        });
      });
  }

  function selectSlot(slot) {
    state.selectedSlot = slot;
    testId('selected-slot').textContent = 'Selected time slot: ' + slot.startTime + ' - ' + slot.endTime;
    document.querySelectorAll('[data-testid="slot-item"]').forEach(function (button) {
      button.setAttribute('aria-pressed', String(button.getAttribute('data-start-time') === slot.startTime));
    });
    clearMessages();
  }

  /** Client-side checks keep the form friendly; the API remains the source of truth. */
  function validateBookingForm() {
    var name = testId('customer-name').value.trim();
    var email = testId('customer-email').value.trim();
    if (!state.selectedService) {
      return 'Please select a service before booking.';
    }
    if (!state.selectedSlot) {
      return 'Please select an available time slot.';
    }
    if (!name) {
      return 'Please enter the customer name.';
    }
    if (!email) {
      return 'Please enter the customer email.';
    }
    if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/.test(email)) {
      return 'Please enter a valid email address, for example ana@example.com.';
    }
    return null;
  }

  function bookAppointment(event) {
    event.preventDefault();
    var validationError = validateBookingForm();
    if (validationError) {
      showError(validationError);
      return;
    }
    var request = {
      customerName: testId('customer-name').value.trim(),
      customerEmail: testId('customer-email').value.trim(),
      serviceId: state.selectedService.id,
      appointmentDate: testId('date-input').value,
      startTime: state.selectedSlot.startTime
    };
    api('POST', '/api/appointments', request).then(function (result) {
      if (result.status === 201 && result.payload) {
        showSuccess('Appointment ' + result.payload.id + ' confirmed for '
          + result.payload.appointmentDate + ' at ' + result.payload.startTime
          + ' (' + state.selectedService.name + ').');
        testId('customer-name').value = '';
        testId('customer-email').value = '';
        state.selectedSlot = null;
        testId('selected-slot').textContent = 'No time slot selected yet.';
        loadAvailability();
        loadAppointments();
      } else {
        showError(errorMessage(result, 'The appointment could not be created.'));
      }
    });
  }

  function serviceName(serviceId) {
    var match = state.services.filter(function (service) {
      return service.id === serviceId;
    })[0];
    return match ? match.name : 'Service ' + serviceId;
  }

  function loadAppointments() {
    return api('GET', '/api/appointments').then(function (result) {
      var body = testId('appointments-body');
      body.innerHTML = '';
      var empty = testId('appointments-empty');
      if (!result.ok || !Array.isArray(result.payload)) {
        empty.hidden = false;
        empty.textContent = errorMessage(result, 'Unable to load appointments.');
        return;
      }
      empty.hidden = result.payload.length > 0;
      empty.textContent = 'No appointments registered yet.';
      result.payload.forEach(function (appointment) {
        body.appendChild(appointmentRow(appointment));
      });
    });
  }

  function appointmentRow(appointment) {
    var row = document.createElement('tr');
    row.setAttribute('data-testid', 'appointment-row');
    row.setAttribute('data-appointment-id', appointment.id);

    row.appendChild(cell(appointment.id, 'appointment-id'));
    row.appendChild(cell(appointment.customerName, 'appointment-customer'));
    row.appendChild(cell(appointment.customerEmail, 'appointment-email'));
    row.appendChild(cell(serviceName(appointment.serviceId), 'appointment-service'));
    row.appendChild(cell(appointment.appointmentDate, 'appointment-date'));
    row.appendChild(cell(appointment.startTime + ' - ' + appointment.endTime, 'appointment-time'));

    var statusCell = document.createElement('td');
    var status = document.createElement('span');
    status.setAttribute('data-testid', 'appointment-status');
    status.className = 'status-' + appointment.status;
    status.textContent = appointment.status;
    statusCell.appendChild(status);
    row.appendChild(statusCell);

    var actionCell = document.createElement('td');
    var cancelButton = document.createElement('button');
    cancelButton.type = 'button';
    cancelButton.className = 'secondary';
    cancelButton.setAttribute('data-testid', 'cancel-button');
    cancelButton.textContent = 'Cancel';
    cancelButton.disabled = appointment.status !== 'CONFIRMED';
    cancelButton.addEventListener('click', function () {
      cancelAppointment(appointment.id);
    });
    actionCell.appendChild(cancelButton);
    row.appendChild(actionCell);

    return row;
  }

  function cell(text, testIdName) {
    var element = document.createElement('td');
    element.setAttribute('data-testid', testIdName);
    element.textContent = text;
    return element;
  }

  function cancelAppointment(id) {
    api('DELETE', '/api/appointments/' + id).then(function (result) {
      if (result.ok && result.payload) {
        showSuccess('Appointment ' + id + ' was cancelled and its time slot has been released.');
        loadAppointments();
        loadAvailability();
      } else {
        showError(errorMessage(result, 'The appointment could not be cancelled.'));
      }
    });
  }

  function init() {
    testId('date-input').value = todayIso();
    testId('booking-form').addEventListener('submit', bookAppointment);
    testId('check-availability').addEventListener('click', function () {
      clearMessages();
      loadAvailability();
    });
    testId('refresh-appointments').addEventListener('click', function () {
      loadAppointments();
    });
    testId('date-input').addEventListener('change', function () {
      loadAvailability();
    });
    loadServices();
    loadAppointments();
  }

  document.addEventListener('DOMContentLoaded', init);
})();
