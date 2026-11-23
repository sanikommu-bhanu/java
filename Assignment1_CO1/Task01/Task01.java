import java.util.*;

/**
 * TASK 1 - Understand the problem and identify the objects (Campus Smart-Parking System)
 *
 * INPUTS : vehicle reg-no, vehicle type (2W / CAR / EV), entry time, exit time, pass status, kWh charged
 * OUTPUTS: allocated slot (floor, slot), ticket number, parking fee, "facility full" message
 *
 * NOUNS -> CLASSES : Vehicle (+ TwoWheeler, Car, ElectricCar), ParkingSlot, Floor, ParkingFacility,
 *                    ParkingTicket, User (Student/Staff/Visitor), MonthlyPass
 * VERBS -> METHODS : park(), allocate(), issueTicket(), release(), calculateFee(), isFree()
 *
 * BUSINESS RULES  : R1 a slot cannot hold two vehicles   R2 a ticket number is never reused
 *                   R3 a vehicle cannot be parked twice  R4 pass holders (student/staff) park free
 *                   R5 exit time >= entry time           R6 only EVs may use charging slots
 * ASSUMPTIONS     : time measured in whole minutes; every started hour is billed; one vehicle per slot;
 *                   slot numbers start at 0; ticket numbers start at 1001.
 */
public class Task01 {

    // ---- candidate classes (minimal versions to demonstrate the business rules) ----
    static class Vehicle {
        final String regNo, type; final long entry;
        Vehicle(String regNo, String type, long entry) { this.regNo = regNo; this.type = type; this.entry = entry; }
    }
    static class Slot {
        final int floor, number; private Vehicle occupant;
        Slot(int floor, int number) { this.floor = floor; this.number = number; }
        boolean isFree() { return occupant == null; }
        void park(Vehicle v) {
            if (!isFree()) throw new IllegalStateException("R1 violated: slot " + floor + "-" + number + " already holds " + occupant.regNo);
            occupant = v;
        }
    }
    static class TicketIssuer {
        private static int last = 1000;
        static int issue() { return ++last; }          // static + only increases => never reused (R2)
    }

    public static void main(String[] args) {
        System.out.println("--- Business rule R1: a slot cannot hold two vehicles ---");
        Slot s = new Slot(0, 1);
        s.park(new Vehicle("TN01AB1111", "CAR", 0));
        try { s.park(new Vehicle("TN02CD2222", "2W", 5)); }
        catch (IllegalStateException e) { System.out.println("Rejected -> " + e.getMessage()); }

        System.out.println("--- Business rule R2: ticket numbers are never reused ---");
        System.out.println("Tickets issued: " + TicketIssuer.issue() + ", " + TicketIssuer.issue() + ", " + TicketIssuer.issue());

        // ---- ALTERNATIVE CONCEPT: parallel String arrays vs objects ----
        int n = 200_000;
        String[] regNo = new String[n]; String[] type = new String[n]; long[] entry = new long[n];
        Vehicle[] objs = new Vehicle[n];
        for (int i = 0; i < n; i++) {
            regNo[i] = "TN" + i; type[i] = (i % 3 == 0) ? "2W" : "CAR"; entry[i] = i;
            objs[i] = new Vehicle(regNo[i], type[i], i);
        }
        String target = "TN" + (n - 1);                  // worst case: last element
        int reps = 200;
        long t0 = System.nanoTime(); int found1 = -1;
        for (int r = 0; r < reps; r++) for (int i = 0; i < n; i++) if (regNo[i].equals(target)) { found1 = i; break; }
        long parallel = (System.nanoTime() - t0) / reps;
        t0 = System.nanoTime(); Vehicle found2 = null;
        for (int r = 0; r < reps; r++) for (Vehicle v : objs) if (v.regNo.equals(target)) { found2 = v; break; }
        long object = (System.nanoTime() - t0) / reps;
        System.out.println("--- Search by registration number, n = " + n + " (both O(n)) ---");
        System.out.println("Parallel arrays : index " + found1 + ", avg " + parallel + " ns");
        System.out.println("Vehicle objects : " + found2.regNo + ", avg " + object + " ns");
        System.out.println("Adding a new attribute: parallel arrays -> new array + edit every method that shifts/copies arrays;");
        System.out.println("                        objects        -> add ONE field to Vehicle, existing code unchanged.");
        System.out.println("Space per vehicle: parallel = 3 array slots (~4+4+8 B) ; object = 12 B header + fields (~32 B) + 1 reference.");
    }
}
