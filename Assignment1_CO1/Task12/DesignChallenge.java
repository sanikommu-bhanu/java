import java.util.*;
import java.util.function.Function;

/**
 * TASK 12 - Day-4 Design Challenge: e-bikes, delivery vans, one-day visitor pass, 5 floors with different slot counts.
 *
 * DECISION: MODIFY (not redesign). The Task 11 hierarchy and jagged array are kept.
 *  CHANGED existing classes : Vehicle (+rule(), +overload calculateFee(entryMinuteOfDay,min), registry replaces switch),
 *                             Ticket (stores user + pass instead of a boolean), ParkingLot (passes, time of day)   -> 3 classes
 *  UNCHANGED                : TwoWheeler, Car, ElectricCar, ParkingFacility (already jagged), UserType, Chargeable
 *  NEW                      : PricingRule, HourlyRule, FlatRule, PeakRule, EBike, DeliveryVan, Pass, MonthlyPass, OneDayPass
 * Adding ANOTHER vehicle type later = 1 new subclass + 1 register(...) line; no existing class edited.
 */
// ---------- NEW: pricing abstraction ----------
interface PricingRule { double fee(int entryMinuteOfDay, int minutes); }

class HourlyRule implements PricingRule {          // the original Task 11 rule, extracted
    private final double rate;
    HourlyRule(double rate) { this.rate = rate; }
    public double fee(int entry, int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        int days = minutes / 1440, rem = minutes % 1440;
        double max = 8 * rate;
        return days * max + Math.min(((rem + 59) / 60) * rate, max);
    }
}
class FlatRule implements PricingRule {
    private final double flat;
    FlatRule(double flat) { this.flat = flat; }
    public double fee(int entry, int minutes) { return minutes <= 0 ? 0 : flat; }
}
class PeakRule implements PricingRule {            // multiplier applies to every started hour whose start lies in [from,to)
    private final double rate, multiplier; private final int from, to;
    PeakRule(double rate, double multiplier, int fromHour, int toHour) { this.rate = rate; this.multiplier = multiplier; from = fromHour * 60; to = toHour * 60; }
    public double fee(int entry, int minutes) {
        double total = 0;
        for (int offset = 0; offset < minutes; offset += 60) {           // one block per started hour
            int start = (entry + offset) % 1440;
            total += (start >= from && start < to) ? rate * multiplier : rate;
        }
        return total;
    }
}
// ---------- NEW: passes ----------
enum UserType { STUDENT, STAFF, VISITOR }
interface Pass { boolean covers(UserType user, long day); double price(); }
class MonthlyPass implements Pass {
    public boolean covers(UserType u, long day) { return u == UserType.STUDENT || u == UserType.STAFF; }
    public double price() { return 0; }
}
class OneDayPass implements Pass {
    private final long validDay; OneDayPass(long validDay) { this.validDay = validDay; }
    public boolean covers(UserType u, long day) { return u == UserType.VISITOR && day == validDay; }
    public double price() { return 100; }
}
// ---------- Vehicles ----------
interface Chargeable { double chargingFee(int kWh); }
abstract class Vehicle {                                   // MODIFIED
    private static final Map<String, Function<String, Vehicle>> REGISTRY = new HashMap<>();
    static void register(String type, Function<String, Vehicle> f) { REGISTRY.put(type.toUpperCase(), f); }
    static Vehicle create(String type, String regNo) {
        Function<String, Vehicle> f = REGISTRY.get(type.toUpperCase());
        if (f == null) throw new IllegalArgumentException("Unknown vehicle type: " + type);
        return f.apply(regNo);
    }
    private final String regNo;
    protected Vehicle(String regNo) { if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("regNo required"); this.regNo = regNo; }
    String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    protected PricingRule rule() { return new HourlyRule(ratePerHour()); }            // default keeps old subclasses unchanged
    double calculateFee(int minutes) { return rule().fee(0, minutes); }
    double calculateFee(int entryMinuteOfDay, int minutes) { return rule().fee(entryMinuteOfDay, minutes); }   // overload
}
class TwoWheeler extends Vehicle { TwoWheeler(String r) { super(r); } @Override protected double ratePerHour() { return 10; } }
class Car extends Vehicle { Car(String r) { super(r); } @Override protected double ratePerHour() { return 30; } }
class ElectricCar extends Car implements Chargeable { ElectricCar(String r) { super(r); } public double chargingFee(int k) { return k * 12; } }
class EBike extends Vehicle {                               // NEW: flat Rs 15 per visit
    EBike(String r) { super(r); }
    @Override protected double ratePerHour() { return 0; }
    @Override protected PricingRule rule() { return new FlatRule(15); }
}
class DeliveryVan extends Vehicle {                         // NEW: double car rate 9 AM - 5 PM
    DeliveryVan(String r) { super(r); }
    @Override protected double ratePerHour() { return 30; }
    @Override protected PricingRule rule() { return new PeakRule(30, 2, 9, 17); }
}
// ---------- Facility (UNCHANGED) ----------
class ParkingFacility {
    private final String[][] slots;
    ParkingFacility(int[] slotsPerFloor) { slots = new String[slotsPerFloor.length][]; for (int f = 0; f < slotsPerFloor.length; f++) slots[f] = new String[slotsPerFloor[f]]; }
    int[] allocate(String regNo) { for (int f = 0; f < slots.length; f++) for (int s = 0; s < slots[f].length; s++) if (slots[f][s] == null) { slots[f][s] = regNo; return new int[]{f, s}; } return null; }
    boolean release(int f, int s) { if (f < 0 || f >= slots.length || s < 0 || s >= slots[f].length || slots[f][s] == null) return false; slots[f][s] = null; return true; }
}
class Ticket {                                              // MODIFIED
    final int no; final Vehicle vehicle; final int floor, slot; final long entryMinute; final UserType user; final Pass pass;
    Ticket(int no, Vehicle v, int f, int s, long e, UserType u, Pass p) { this.no = no; vehicle = v; floor = f; slot = s; entryMinute = e; user = u; pass = p; }
}
class ParkingLot {                                          // MODIFIED
    private final ParkingFacility facility; private final Map<String, Ticket> active = new HashMap<>();
    private int counter = 1000; private double collection = 0;
    ParkingLot(int[] slotsPerFloor) { facility = new ParkingFacility(slotsPerFloor); }
    void sellPass(Pass p) { collection += p.price(); }
    Ticket enter(String reg, String type, UserType user, Pass pass, long minute) {
        if (active.containsKey(reg)) throw new IllegalArgumentException("Duplicate registration number: " + reg);
        Vehicle v = Vehicle.create(type, reg);
        int[] pos = facility.allocate(reg);
        if (pos == null) throw new IllegalStateException("Facility full");
        Ticket t = new Ticket(++counter, v, pos[0], pos[1], minute, user, pass);
        active.put(reg, t); return t;
    }
    double exit(String reg, long minute, int kWh) {
        Ticket t = active.remove(reg);
        if (t == null) throw new NoSuchElementException("No active ticket for " + reg);
        facility.release(t.floor, t.slot);
        boolean free = t.pass != null && t.pass.covers(t.user, t.entryMinute / 1440);
        double fee = free ? 0 : t.vehicle.calculateFee((int) (t.entryMinute % 1440), (int) (minute - t.entryMinute));
        if (t.vehicle instanceof Chargeable c && kWh > 0) fee += c.chargingFee(kWh);
        collection += fee; return fee;
    }
    double getCollection() { return collection; }
}
public class DesignChallenge {
    static int pass = 0, fail = 0;
    static void check(String id, String what, Object exp, Object act) {
        boolean ok = String.valueOf(exp).equals(String.valueOf(act)); if (ok) pass++; else fail++;
        System.out.printf("%-4s %-58s expected=%-10s actual=%-10s %s%n", id, what, exp, act, ok ? "PASS" : "FAIL");
    }
    public static void main(String[] args) {
        // New categories are registered WITHOUT touching Vehicle's code
        Vehicle.register("2W", TwoWheeler::new); Vehicle.register("CAR", Car::new); Vehicle.register("EV", ElectricCar::new);
        Vehicle.register("EBIKE", EBike::new); Vehicle.register("VAN", DeliveryVan::new);

        int[] floors = {20, 35, 50, 40, 60};                    // 5 floors, different slot counts = 205 slots
        ParkingLot big = new ParkingLot(floors);
        for (int i = 0; i < 205; i++) big.enter("R" + i, "CAR", UserType.VISITOR, null, 0);
        check("N1", "205 slots on 5 uneven floors all allocatable", "accepted", "accepted");
        try { big.enter("R205", "CAR", UserType.VISITOR, null, 0); check("N2", "206th vehicle rejected", "full", "accepted"); }
        catch (IllegalStateException e) { check("N2", "206th vehicle rejected", "Facility full", e.getMessage()); }

        ParkingLot lot = new ParkingLot(floors);
        // regression: old behaviour unchanged
        lot.enter("TW", "2W", UserType.VISITOR, null, 600); check("N3", "Regression: two-wheeler 130 min", 30.0, lot.exit("TW", 730, 0));
        lot.enter("EV", "EV", UserType.VISITOR, null, 600); check("N4", "Regression: EV 130 min + 10 kWh", 210.0, lot.exit("EV", 730, 10));
        // e-bike flat fee
        lot.enter("EB1", "EBIKE", UserType.VISITOR, null, 600); check("N5", "E-bike, 130 min = flat Rs 15", 15.0, lot.exit("EB1", 730, 0));
        lot.enter("EB2", "EBIKE", UserType.VISITOR, null, 600); check("N6", "E-bike, 600 min = still flat Rs 15", 15.0, lot.exit("EB2", 1200, 0));
        // delivery van, time of day
        lot.enter("V1", "VAN", UserType.VISITOR, null, 600);  check("N7", "Van 10:00, 130 min (3 peak hours x 60)", 180.0, lot.exit("V1", 730, 0));
        lot.enter("V2", "VAN", UserType.VISITOR, null, 480);  check("N8", "Van 08:00, 120 min (30 + 60)", 90.0, lot.exit("V2", 600, 0));
        lot.enter("V3", "VAN", UserType.VISITOR, null, 990);  check("N9", "Van 16:30, 100 min (60 + 30)", 90.0, lot.exit("V3", 1090, 0));
        lot.enter("V4", "VAN", UserType.VISITOR, null, 1200); check("N10", "Van 20:00, 60 min off-peak", 30.0, lot.exit("V4", 1260, 0));
        // one-day pass
        ParkingLot d = new ParkingLot(floors);
        Pass day0 = new OneDayPass(0); d.sellPass(day0);
        d.enter("VIS1", "CAR", UserType.VISITOR, day0, 60);   check("N11", "Visitor with one-day pass parks free on day 0", 0.0, d.exit("VIS1", 660, 0));
        d.enter("VIS2", "CAR", UserType.VISITOR, day0, 1500); check("N12", "Same pass on day 1 is invalid -> pays (130 min)", 90.0, d.exit("VIS2", 1630, 0));
        check("N13", "Pass sale (100) + day-1 parking (90) collected", 190.0, d.getCollection());
        try { lot.enter("Q", "BUS", UserType.VISITOR, null, 0); } catch (IllegalArgumentException e) { check("N14", "Unknown type still rejected", "Unknown vehicle type: BUS", e.getMessage()); }

        // Hand-calculated day: 15 (e-bike) + 180 (van 10:00,130) + 100 (pass sale) + 0 (pass user) + 90 (car 130) = 385
        ParkingLot h = new ParkingLot(floors); Pass p = new OneDayPass(0); h.sellPass(p);
        h.enter("H1", "EBIKE", UserType.VISITOR, null, 600); h.exit("H1", 730, 0);
        h.enter("H2", "VAN", UserType.VISITOR, null, 600);   h.exit("H2", 730, 0);
        h.enter("H3", "CAR", UserType.VISITOR, p, 600);      h.exit("H3", 1000, 0);
        h.enter("H4", "CAR", UserType.VISITOR, null, 600);   h.exit("H4", 730, 0);
        System.out.println("Hand calculation: 15 + 180 + 100 + 0 + 90 = 385");
        check("N15", "Day total", 385.0, h.getCollection());
        System.out.println("Passed " + pass + " / " + (pass + fail));
        System.out.println("Existing classes changed: 3 (Vehicle, Ticket, ParkingLot); unchanged: TwoWheeler, Car, ElectricCar, ParkingFacility, UserType, Chargeable");
        System.out.println("Limitation: PeakRule/FlatRule apply no daily maximum; a DailyCapRule decorator could wrap any PricingRule.");
    }
}
