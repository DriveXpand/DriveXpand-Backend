package com.example.drivebackend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.drivebackend.dto.TimeBucket;
import com.example.drivebackend.dto.TripDetailsResponse;
import com.example.drivebackend.entities.DeviceEntity;
import com.example.drivebackend.entities.TelemetryEntity;
import com.example.drivebackend.entities.TripEntity;
import com.example.drivebackend.repository.TelemetrySampleRepository;
import com.example.drivebackend.repository.TripRepository;
import com.example.drivebackend.services.TelemetryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TripController Tests")
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripRepository tripRepository;

    @MockBean
    private TelemetrySampleRepository telemetrySampleRepository;

    @MockBean
    private TelemetryService telemetryService;

    private String testDeviceId;
    private UUID testTripId;
    private TripEntity testTrip;
    private DeviceEntity testDevice;
    private Instant testStartTime;
    private Instant testEndTime;

    @BeforeEach
    void setUp() {
        testDeviceId = "device-001";
        testTripId = UUID.randomUUID();
        testStartTime = Instant.now().minusSeconds(3600);
        testEndTime = Instant.now();

        // Setup test device
        testDevice = new DeviceEntity();
        testDevice.setDeviceId(testDeviceId);

        // Setup test trip
        testTrip = new TripEntity();
        testTrip.setId(testTripId);
        testTrip.setDevice(testDevice);
        testTrip.setStartTime(testStartTime);
        testTrip.setEndTime(testEndTime);
        testTrip.setStartLocation("Start Location");
        testTrip.setEndLocation("End Location");
        testTrip.setNote("Test Note");
        testTrip.setTrip_distance_km(25.5);
    }

    @Test
    @DisplayName("GET /api/trips/weekday - Should return trip counts by weekday")
    void testGetTripsPerWeekday() throws Exception {
        // Arrange
        Map<UUID, List<com.example.drivebackend.dto.TelemetryResponse>> mockTrips = new HashMap<>();
        mockTrips.put(testTripId, createMockTelemetryResponseList());

        when(telemetryService.fetchTelemetryGroupedByTrip(testDeviceId, null, null))
                .thenReturn(mockTrips);

        // Act & Assert
        mockMvc.perform(get("/api/trips/weekday")
                .param("deviceId", testDeviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(telemetryService, times(1)).fetchTelemetryGroupedByTrip(testDeviceId, null, null);
    }

    @Test
    @DisplayName("GET /api/trips/weekday - Should return trip counts with time filters")
    void testGetTripsPerWeekdayWithTimeFilter() throws Exception {
        // Arrange
        Instant since = testStartTime;
        Instant end = testEndTime;

        Map<UUID, List<com.example.drivebackend.dto.TelemetryResponse>> mockTrips = new HashMap<>();
        mockTrips.put(testTripId, createMockTelemetryResponseList());

        when(telemetryService.fetchTelemetryGroupedByTrip(testDeviceId, since, end))
                .thenReturn(mockTrips);

        // Act & Assert
        mockMvc.perform(get("/api/trips/weekday")
                .param("deviceId", testDeviceId)
                .param("since", since.toString())
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).fetchTelemetryGroupedByTrip(testDeviceId, since, end);
    }

    @Test
    @DisplayName("GET /api/trips/time-of-day - Should return trips per hour distribution")
    void testGetTripsPerHour() throws Exception {
        // Arrange
        List<TimeBucket> mockTimeBuckets = createMockTimeBuckets();

        when(telemetryService.fetchTripsPerHour(testDeviceId, null, null))
                .thenReturn(mockTimeBuckets);

        // Act & Assert
        mockMvc.perform(get("/api/trips/time-of-day")
                .param("deviceId", testDeviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(telemetryService, times(1)).fetchTripsPerHour(testDeviceId, null, null);
    }

    @Test
    @DisplayName("GET /api/trips/time-of-day - Should return trips per hour with time filters")
    void testGetTripsPerHourWithTimeFilter() throws Exception {
        // Arrange
        Instant since = testStartTime;
        Instant end = testEndTime;
        List<TimeBucket> mockTimeBuckets = createMockTimeBuckets();

        when(telemetryService.fetchTripsPerHour(testDeviceId, since, end))
                .thenReturn(mockTimeBuckets);

        // Act & Assert
        mockMvc.perform(get("/api/trips/time-of-day")
                .param("deviceId", testDeviceId)
                .param("since", since.toString())
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).fetchTripsPerHour(testDeviceId, since, end);
    }

    @Test
    @DisplayName("GET /api/trips/list - Should return all trips for device without pagination")
    void testGetTripsNoPagination() throws Exception {
        // Arrange
        Page<TripEntity> tripPage = new PageImpl<>(Collections.singletonList(testTrip));

        when(tripRepository.findByDevice_DeviceId(eq(testDeviceId), any(Pageable.class)))
                .thenReturn(tripPage);

        // Act & Assert
        mockMvc.perform(get("/api/trips/list")
                .param("deviceId", testDeviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testTripId.toString()));

        verify(tripRepository, times(1)).findByDevice_DeviceId(eq(testDeviceId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/trips/list - Should return trips with pagination")
    void testGetTripsWithPagination() throws Exception {
        // Arrange
        int page = 0;
        int pageSize = 10;
        Page<TripEntity> tripPage = new PageImpl<>(Collections.singletonList(testTrip));

        when(tripRepository.findByDevice_DeviceId(eq(testDeviceId), any(Pageable.class)))
                .thenReturn(tripPage);

        // Act & Assert
        mockMvc.perform(get("/api/trips/list")
                .param("deviceId", testDeviceId)
                .param("page", String.valueOf(page))
                .param("pageSize", String.valueOf(pageSize))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(tripRepository, times(1)).findByDevice_DeviceId(eq(testDeviceId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/trips/list - Should return trips filtered by since parameter")
    void testGetTripsFilteredBySince() throws Exception {
        // Arrange
        Instant since = testStartTime;
        Page<TripEntity> tripPage = new PageImpl<>(Collections.singletonList(testTrip));

        when(tripRepository.findByDevice_DeviceIdAndStartTimeAfter(eq(testDeviceId), eq(since), any(Pageable.class)))
                .thenReturn(tripPage);

        // Act & Assert
        mockMvc.perform(get("/api/trips/list")
                .param("deviceId", testDeviceId)
                .param("since", since.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(tripRepository, times(1)).findByDevice_DeviceIdAndStartTimeAfter(eq(testDeviceId), eq(since), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/trips/list - Should return trips filtered by end parameter")
    void testGetTripsFilteredByEnd() throws Exception {
        // Arrange
        Instant end = testEndTime;
        Page<TripEntity> tripPage = new PageImpl<>(Collections.singletonList(testTrip));

        when(tripRepository.findByDevice_DeviceIdAndStartTimeBefore(eq(testDeviceId), eq(end), any(Pageable.class)))
                .thenReturn(tripPage);

        // Act & Assert
        mockMvc.perform(get("/api/trips/list")
                .param("deviceId", testDeviceId)
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(tripRepository, times(1)).findByDevice_DeviceIdAndStartTimeBefore(eq(testDeviceId), eq(end), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/trips/list - Should return trips filtered by since and end parameters")
    void testGetTripsFilteredBySinceAndEnd() throws Exception {
        // Arrange
        Instant since = testStartTime;
        Instant end = testEndTime;
        Page<TripEntity> tripPage = new PageImpl<>(Collections.singletonList(testTrip));

        when(tripRepository.findByDevice_DeviceIdAndStartTimeBetween(eq(testDeviceId), eq(since), eq(end), any(Pageable.class)))
                .thenReturn(tripPage);

        // Act & Assert
        mockMvc.perform(get("/api/trips/list")
                .param("deviceId", testDeviceId)
                .param("since", since.toString())
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(tripRepository, times(1)).findByDevice_DeviceIdAndStartTimeBetween(eq(testDeviceId), eq(since), eq(end), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/trips/{tripId} - Should return trip details with telemetry")
    void testGetTelemetryByTrip() throws Exception {
        // Arrange
        List<TelemetryEntity> telemetryList = createMockTelemetryList();

        when(telemetrySampleRepository.findByTrip_IdAndDevice_DeviceId(testTripId, testDeviceId))
                .thenReturn(telemetryList);

        // Act & Assert
        mockMvc.perform(get("/api/trips/{tripId}", testTripId)
                .param("deviceId", testDeviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(testTripId.toString()))
                .andExpect(jsonPath("$.deviceId").value(testDeviceId));

        verify(telemetrySampleRepository, times(1)).findByTrip_IdAndDevice_DeviceId(testTripId, testDeviceId);
    }

    @Test
    @DisplayName("GET /api/trips/{tripId} - Should return 404 when trip not found")
    void testGetTelemetryByTripNotFound() throws Exception {
        // Arrange
        when(telemetrySampleRepository.findByTrip_IdAndDevice_DeviceId(testTripId, testDeviceId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/trips/{tripId}", testTripId)
                .param("deviceId", testDeviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(telemetrySampleRepository, times(1)).findByTrip_IdAndDevice_DeviceId(testTripId, testDeviceId);
    }

    @Test
    @DisplayName("GET /api/trips - Should return trips with details grouped by trip ID")
    void testFetchTelemetryGroupedByTrip() throws Exception {
        // Arrange
        TripDetailsResponse tripDetails = createMockTripDetailsResponse();
        Map<UUID, TripDetailsResponse> tripMap = new HashMap<>();
        tripMap.put(testTripId, tripDetails);

        when(telemetryService.fetchTripDetails(testDeviceId, null, null))
                .thenReturn(tripMap);

        // Act & Assert
        mockMvc.perform(get("/api/trips")
                .param("deviceId", testDeviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(telemetryService, times(1)).fetchTripDetails(testDeviceId, null, null);
    }

    @Test
    @DisplayName("GET /api/trips - Should return trips with time filters")
    void testFetchTelemetryGroupedByTripWithTimeFilter() throws Exception {
        // Arrange
        Instant since = testStartTime;
        Instant end = testEndTime;
        TripDetailsResponse tripDetails = createMockTripDetailsResponse();
        Map<UUID, TripDetailsResponse> tripMap = new HashMap<>();
        tripMap.put(testTripId, tripDetails);

        when(telemetryService.fetchTripDetails(testDeviceId, since, end))
                .thenReturn(tripMap);

        // Act & Assert
        mockMvc.perform(get("/api/trips")
                .param("deviceId", testDeviceId)
                .param("since", since.toString())
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).fetchTripDetails(testDeviceId, since, end);
    }

    @Test
    @DisplayName("PATCH /api/trips/{tripId} - Should update trip start location")
    void testUpdateTripStartLocation() throws Exception {
        // Arrange
        String updatedLocation = "Updated Start Location";
        String updatePayload = "{\"startLocation\": \"" + updatedLocation + "\"}";

        TripEntity updatedTrip = new TripEntity();
        updatedTrip.setId(testTripId);
        updatedTrip.setDevice(testDevice);
        updatedTrip.setStartTime(testStartTime);
        updatedTrip.setEndTime(testEndTime);
        updatedTrip.setStartLocation(updatedLocation);
        updatedTrip.setEndLocation(testTrip.getEndLocation());
        updatedTrip.setNote(testTrip.getNote());
        updatedTrip.setTrip_distance_km(testTrip.getTrip_distance_km());

        when(tripRepository.findById(testTripId))
                .thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(TripEntity.class)))
                .thenReturn(updatedTrip);

        // Act & Assert
        mockMvc.perform(patch("/api/trips/{tripId}", testTripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startLocation").value(updatedLocation));

        verify(tripRepository, times(1)).findById(testTripId);
        verify(tripRepository, times(1)).save(any(TripEntity.class));
    }

    @Test
    @DisplayName("PATCH /api/trips/{tripId} - Should update trip end location")
    void testUpdateTripEndLocation() throws Exception {
        // Arrange
        String updatedLocation = "Updated End Location";
        String updatePayload = "{\"endLocation\": \"" + updatedLocation + "\"}";

        TripEntity updatedTrip = new TripEntity();
        updatedTrip.setId(testTripId);
        updatedTrip.setDevice(testDevice);
        updatedTrip.setStartTime(testStartTime);
        updatedTrip.setEndTime(testEndTime);
        updatedTrip.setStartLocation(testTrip.getStartLocation());
        updatedTrip.setEndLocation(updatedLocation);
        updatedTrip.setNote(testTrip.getNote());
        updatedTrip.setTrip_distance_km(testTrip.getTrip_distance_km());

        when(tripRepository.findById(testTripId))
                .thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(TripEntity.class)))
                .thenReturn(updatedTrip);

        // Act & Assert
        mockMvc.perform(patch("/api/trips/{tripId}", testTripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endLocation").value(updatedLocation));

        verify(tripRepository, times(1)).findById(testTripId);
        verify(tripRepository, times(1)).save(any(TripEntity.class));
    }

    @Test
    @DisplayName("PATCH /api/trips/{tripId} - Should update trip note")
    void testUpdateTripNote() throws Exception {
        // Arrange
        String updatedNote = "Updated Note";
        String updatePayload = "{\"note\": \"" + updatedNote + "\"}";

        TripEntity updatedTrip = new TripEntity();
        updatedTrip.setId(testTripId);
        updatedTrip.setDevice(testDevice);
        updatedTrip.setStartTime(testStartTime);
        updatedTrip.setEndTime(testEndTime);
        updatedTrip.setStartLocation(testTrip.getStartLocation());
        updatedTrip.setEndLocation(testTrip.getEndLocation());
        updatedTrip.setNote(updatedNote);
        updatedTrip.setTrip_distance_km(testTrip.getTrip_distance_km());

        when(tripRepository.findById(testTripId))
                .thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(TripEntity.class)))
                .thenReturn(updatedTrip);

        // Act & Assert
        mockMvc.perform(patch("/api/trips/{tripId}", testTripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(updatedNote));

        verify(tripRepository, times(1)).findById(testTripId);
        verify(tripRepository, times(1)).save(any(TripEntity.class));
    }

    @Test
    @DisplayName("PATCH /api/trips/{tripId} - Should update multiple trip fields")
    void testUpdateTripMultipleFields() throws Exception {
        // Arrange
        String newStartLocation = "New Start";
        String newEndLocation = "New End";
        String newNote = "New Note";
        String updatePayload = "{\"startLocation\": \"" + newStartLocation + "\", " +
                "\"endLocation\": \"" + newEndLocation + "\", " +
                "\"note\": \"" + newNote + "\"}";

        TripEntity updatedTrip = new TripEntity();
        updatedTrip.setId(testTripId);
        updatedTrip.setDevice(testDevice);
        updatedTrip.setStartTime(testStartTime);
        updatedTrip.setEndTime(testEndTime);
        updatedTrip.setStartLocation(newStartLocation);
        updatedTrip.setEndLocation(newEndLocation);
        updatedTrip.setNote(newNote);
        updatedTrip.setTrip_distance_km(testTrip.getTrip_distance_km());

        when(tripRepository.findById(testTripId))
                .thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(TripEntity.class)))
                .thenReturn(updatedTrip);

        // Act & Assert
        mockMvc.perform(patch("/api/trips/{tripId}", testTripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startLocation").value(newStartLocation))
                .andExpect(jsonPath("$.endLocation").value(newEndLocation))
                .andExpect(jsonPath("$.note").value(newNote));

        verify(tripRepository, times(1)).findById(testTripId);
        verify(tripRepository, times(1)).save(any(TripEntity.class));
    }

    @Test
    @DisplayName("PATCH /api/trips/{tripId} - Should return 404 when trip not found")
    void testUpdateTripNotFound() throws Exception {
        // Arrange
        String updatePayload = "{\"startLocation\": \"New Location\"}";

        when(tripRepository.findById(testTripId))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(patch("/api/trips/{tripId}", testTripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isNotFound());

        verify(tripRepository, times(1)).findById(testTripId);
        verify(tripRepository, never()).save(any(TripEntity.class));
    }

    // Helper methods

    private List<com.example.drivebackend.dto.TelemetryResponse> createMockTelemetryResponseList() {
        List<com.example.drivebackend.dto.TelemetryResponse> responses = new ArrayList<>();
        responses.add(new com.example.drivebackend.dto.TelemetryResponse(
                testStartTime,
                testEndTime,
                25.5,
                1800,
                100.0,
                85.0
        ));
        return responses;
    }

    private List<TimeBucket> createMockTimeBuckets() {
        List<TimeBucket> buckets = new ArrayList<>();
        buckets.add(new TimeBucket("Morning (6-12)", 25.0));
        buckets.add(new TimeBucket("Midday (12-15)", 30.0));
        buckets.add(new TimeBucket("Afternoon (15-18)", 35.0));
        buckets.add(new TimeBucket("Evening (18-23)", 10.0));
        return buckets;
    }

    private List<TelemetryEntity> createMockTelemetryList() {
        List<TelemetryEntity> telemetryList = new ArrayList<>();

        TelemetryEntity telemetry = new TelemetryEntity();
        telemetry.setId(UUID.randomUUID());
        telemetry.setTrip(testTrip);
        telemetry.setDevice(testDevice);
        telemetry.setTimestamp(testStartTime);

        Map<String, Object> aggregatedData = new HashMap<>();
        aggregatedData.put("distance_km", 25.5);
        aggregatedData.put("max_speed", 120.0);

        Map<String, Object> timedData = new HashMap<>();
        timedData.put("speed", 100.0);
        timedData.put("rpm", 3000);

        telemetry.setAggregated_data(aggregatedData);
        telemetry.setTimed_data(timedData);

        telemetryList.add(telemetry);
        return telemetryList;
    }

    private TripDetailsResponse createMockTripDetailsResponse() {
        List<Map<String, Object>> timedData = new ArrayList<>();
        List<Map<String, Object>> aggregatedData = new ArrayList<>();

        Map<String, Object> timed = new HashMap<>();
        timed.put("speed", 100.0);
        timedData.add(timed);

        Map<String, Object> aggregated = new HashMap<>();
        aggregated.put("distance_km", 25.5);
        aggregatedData.add(aggregated);

        return new TripDetailsResponse(
                testTripId,
                testDeviceId,
                testStartTime,
                testEndTime,
                testTrip.getStartLocation(),
                testTrip.getEndLocation(),
                testTrip.getNote(),
                timedData,
                aggregatedData,
                testTrip.getTrip_distance_km()
        );
    }
}

