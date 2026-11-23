package parking.service;

import parking.model.ParkingTicket;

/** Service layer: business rules. Uses only the public API of the model. */
public class ParkingService {
    private int nextTicket = 1000;                          // private state
    public ParkingTicket issue(String regNo, int floor, int slot) { return new ParkingTicket(++nextTicket, regNo, floor, slot); }
    public double fee(ParkingTicket t, double ratePerHour) { return ((t.getDurationMinutes() + 59) / 60) * ratePerHour; }
}
