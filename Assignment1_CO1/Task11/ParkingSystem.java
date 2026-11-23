import java.util.*;

/** TASK 11 - Complete Campus Smart-Parking System with tests (Encapsulation, Inheritance, Polymorphism, Abstraction). */
interface Chargeable { double chargingFee(int kWh); }

abstract class Vehicle {
    private final String regNo;
    protected Vehicle(String regNo) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("regNo required");
        this.regNo = regNo;
    }
    String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    double dailyMax() { return 8 * ratePerHour(); }
    /** Every started hour billed; daily maximum applies per 24 h; 0 minutes = free. */
    double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        int days = minutes / 1440, rem = minutes % 1440;
        double remFee = Math.min(((rem + 59) / 60) * ratePerHour(), dailyMax());
        return days * dailyMax() + remFee;
    }
    static Vehicle create(String type, String regNo) {
        switch (type.toUpperCase()) {
            case "2W":  return new TwoWheeler(regNo);
            case "CAR": return new Car(regNo);
            case "EV":  return new ElectricCar(regNo);
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
    @Override public boolean equals(Object o) { return o instanceof Vehicle v && regNo.equals(v.regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
class TwoWheeler extends Vehicle { TwoWheeler(String r) { super(r); } @Override protected double ratePerHour() { return 10; } }
class Car extends Vehicle { Car(String r) { super(r); } @Override protected double ratePerHour() { return 30; } }
class ElectricCar extends Car implements Chargeable {
    ElectricCar(String r) { super(r); }
    @Override public double chargingFee(int kWh) { return kWh * 12; }
}
enum UserType { STUDENT, STAFF, VISITOR }

class Ticket {
    final int ticketNo; final Vehicle vehicle; final int floor, slot; final long entryMinute; final boolean freeParking;
    Ticket(int no, Vehicle v, int f, int s, long entry, boolean free) { ticketNo = no; vehicle = v; floor = f; slot = s; entryMinute = entry; freeParking = free; }
}

class ParkingFacility {
    private final String[][] slots;
    ParkingFacility(int[] slotsPerFloor) {
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) slots[f] = new String[slotsPerFloor[f]];
    }
    int[] allocate(String regNo) {
        for (int f = 0; f < slots.length; f++)
            for (int s = 0; s < slots[f].length; s++)
                if (slots[f][s] == null) { slots[f][s] = regNo; return new int[]{f, s}; }
        return null;
    }
    boolean release(int f, int s) {
        if (f < 0 || f >= slots.length || s < 0 || s >= slots[f].length || slots[f][s] == null) return false;
        slots[f][s] = null; return true;
    }
}

class ParkingLot {
    private final ParkingFacility facility;
    private final Map<String, Ticket> active = new HashMap<>();   // regNo -> ticket : O(1) exit lookup
    private int ticketCounter = 1000;
    private double collection = 0;
    ParkingLot(int[] slotsPerFloor) { facility = new ParkingFacility(slotsPerFloor); }

    Ticket enter(String regNo, String type, UserType user, boolean hasPass, long minute) {
        if (active.containsKey(regNo)) throw new IllegalArgumentException("Duplicate registration number: " + regNo);
        Vehicle v = Vehicle.create(type, regNo);                       // unknown type fails BEFORE a slot is taken
        int[] pos = facility.allocate(regNo);
        if (pos == null) throw new IllegalStateException("Facility full");
        Ticket t = new Ticket(++ticketCounter, v, pos[0], pos[1], minute, user != UserType.VISITOR && hasPass);
        active.put(regNo, t);
        return t;
    }
    double exit(String regNo, long minute, int kWh) {
        Ticket t = active.remove(regNo);
        if (t == null) throw new NoSuchElementException("No active ticket for " + regNo);
        facility.release(t.floor, t.slot);
        double fee = t.freeParking ? 0 : t.vehicle.calculateFee((int) (minute - t.entryMinute));   // dynamic dispatch
        if (t.vehicle instanceof Chargeable c && kWh > 0) fee += c.chargingFee(kWh);
        collection += fee;
        return fee;
    }
    boolean releaseSlot(int f, int s) { return facility.release(f, s); }
    double getCollection() { return collection; }
}

public class ParkingSystem {
    static int pass = 0, fail = 0;
    static void check(String id, String what, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-52s expected=%-28s actual=%-28s %s%n", id, what, expected, actual, ok ? "PASS" : "FAIL");
    }
    static String tryEnter(ParkingLot lot, String reg, String type) {
        try { lot.enter(reg, type, UserType.VISITOR, false, 0); return "accepted"; }
        catch (RuntimeException e) { return e.getClass().getSimpleName() + ": " + e.getMessage(); }
    }

    public static void main(String[] args) {
        System.out.println("ID   Test                                                 Expected / Actual / Result");
        ParkingLot lot = new ParkingLot(new int[]{3, 2});
        // ---- Normal operation ----
        lot.enter("TW1", "2W", UserType.VISITOR, false, 0);
        check("T1", "Two-wheeler 130 min (3 h x 10)", 30.0, lot.exit("TW1", 130, 0));
        lot.enter("C1", "CAR", UserType.VISITOR, false, 0);
        check("T2", "Car 130 min (3 h x 30)", 90.0, lot.exit("C1", 130, 0));
        lot.enter("E1", "EV", UserType.VISITOR, false, 0);
        check("T3", "EV 130 min + 10 kWh (90 + 120)", 210.0, lot.exit("E1", 130, 10));
        lot.enter("S1", "CAR", UserType.STUDENT, true, 0);
        check("T4", "Student with monthly pass parks free", 0.0, lot.exit("S1", 500, 0));
        // ---- Boundary cases ----
        lot.enter("B0", "CAR", UserType.VISITOR, false, 100);
        check("T5", "Zero-minute stay", 0.0, lot.exit("B0", 100, 0));
        lot.enter("B60", "CAR", UserType.VISITOR, false, 0);
        check("T6", "Exactly 60 min (slab boundary) = 1 h", 30.0, lot.exit("B60", 60, 0));
        lot.enter("B61", "CAR", UserType.VISITOR, false, 0);
        check("T7", "61 min = 2 started hours", 60.0, lot.exit("B61", 61, 0));
        lot.enter("BMAX", "CAR", UserType.VISITOR, false, 0);
        check("T8", "600 min capped at daily maximum (8 x 30)", 240.0, lot.exit("BMAX", 600, 0));
        lot.enter("B24", "CAR", UserType.VISITOR, false, 0);
        check("T9", "Exactly 1440 min = one full day", 240.0, lot.exit("B24", 1440, 0));
        ParkingLot tiny = new ParkingLot(new int[]{1});
        check("T10", "Last free slot is allocated", "accepted", tryEnter(tiny, "L1", "CAR"));
        check("T11", "Facility full after last slot", "IllegalStateException: Facility full", tryEnter(tiny, "L2", "CAR"));
        // ---- Invalid cases ----
        check("T12", "Unknown vehicle type BUS", "IllegalArgumentException: Unknown vehicle type: BUS", tryEnter(lot, "X1", "BUS"));
        lot.enter("D1", "CAR", UserType.VISITOR, false, 0);
        check("T13", "Duplicate registration number", "IllegalArgumentException: Duplicate registration number: D1", tryEnter(lot, "D1", "CAR"));
        check("T14", "Release invalid slot (9,9)", false, lot.releaseSlot(9, 9));
        try { lot.exit("GHOST", 10, 0); check("T15", "Exit of vehicle never parked", "exception", "no exception"); }
        catch (NoSuchElementException e) { check("T15", "Exit of vehicle never parked", "NoSuchElementException", e.getClass().getSimpleName()); }
        lot.exit("D1", 10, 0);

        // ---- Hand-calculated day: collection ----
        ParkingLot day = new ParkingLot(new int[]{5, 5});
        day.enter("D-2W", "2W", UserType.VISITOR, false, 0);   // 130 min -> 3 x 10 = 30
        day.enter("D-CAR", "CAR", UserType.VISITOR, false, 0); // 130 min -> 3 x 30 = 90
        day.enter("D-EV", "EV", UserType.VISITOR, false, 0);   // 130 min -> 90 + 10 kWh x 12 = 210
        day.enter("D-STF", "CAR", UserType.STAFF, true, 0);    // pass -> 0
        day.enter("D-61", "CAR", UserType.VISITOR, false, 0);  // 61 min -> 2 x 30 = 60
        day.enter("D-LONG", "CAR", UserType.VISITOR, false, 0);// 600 min -> capped 240
        day.exit("D-2W", 130, 0); day.exit("D-CAR", 130, 0); day.exit("D-EV", 130, 10);
        day.exit("D-STF", 300, 0); day.exit("D-61", 61, 0); day.exit("D-LONG", 600, 0);
        System.out.println("Hand calculation: 30 + 90 + 210 + 0 + 60 + 240 = 630");
        check("T16", "Total collection for the day", 630.0, day.getCollection());
        System.out.println("Passed " + pass + " / " + (pass + fail) + (fail == 0 ? "  -> ALL TESTS PASSED" : "  -> FAILURES"));
    }
}
