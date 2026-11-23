/** TASK 4 - Classes, objects, constructors, this, chaining, copy constructor, memory. */
class ParkingTicket {
    private static int counter = 1000;               // shared by all tickets (see Task 5)
    private final int ticketNo;
    private final String regNo, vehicleType;
    private final int floor, slot;
    private final long entryMinute;

    // Master (parameterised) constructor - ALL validation lives here
    ParkingTicket(String regNo, String vehicleType, int floor, int slot, long entryMinute) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("regNo required");
        if (entryMinute < 0) throw new IllegalArgumentException("entryMinute < 0");
        this.ticketNo = ++counter;
        this.regNo = regNo; this.vehicleType = vehicleType;
        this.floor = floor; this.slot = slot; this.entryMinute = entryMinute;
    }
    // Overloaded constructors chain to the master with this(...) -> no repeated code
    ParkingTicket(String regNo, int floor, int slot) { this(regNo, "CAR", floor, slot, 0); }
    ParkingTicket() { this("UNASSIGNED", -1, -1); }              // default constructor: walk-in, no slot yet
    // Copy constructor: duplicate ticket re-issue keeps the SAME ticket number
    ParkingTicket(ParkingTicket o) {
        this.ticketNo = o.ticketNo; this.regNo = o.regNo; this.vehicleType = o.vehicleType;
        this.floor = o.floor; this.slot = o.slot; this.entryMinute = o.entryMinute;
    }
    // Static factory (alternative concept)
    static ParkingTicket of(String regNo, String type, int floor, int slot, long entry) {
        return new ParkingTicket(regNo, type, floor, slot, entry);
    }
    int getTicketNo() { return ticketNo; }
    @Override public String toString() {
        return "Ticket#" + ticketNo + "[" + regNo + "," + vehicleType + ",F" + floor + "-S" + slot + ",t=" + entryMinute + "]";
    }
}

class ParkingTicketMutable {                      // alternative: default constructor + setters (can be invalid midway)
    String regNo; int floor, slot;
    void setRegNo(String r) { regNo = r; } void setFloor(int f) { floor = f; } void setSlot(int s) { slot = s; }
}

public class Task04 {
    public static void main(String[] args) {
        ParkingTicket a = new ParkingTicket("TN01AB1111", "EV", 0, 3, 480);
        ParkingTicket b = new ParkingTicket("TN02CD2222", 1, 7);       // chained
        ParkingTicket c = new ParkingTicket();                           // default
        ParkingTicket d = new ParkingTicket(a);                          // copy (duplicate ticket)
        System.out.println(a); System.out.println(b); System.out.println(c); System.out.println(d);
        System.out.println("a == d ? " + (a == d) + "   same ticket number ? " + (a.getTicketNo() == d.getTicketNo()));
        try { new ParkingTicket("", 0, 0); } catch (IllegalArgumentException e) { System.out.println("Rejected: " + e.getMessage()); }
        ParkingTicket e1 = ParkingTicket.of("TN05XX0001", "2W", 2, 1, 600);
        System.out.println(e1);

        // Memory: a, b, c, d, e1 are REFERENCE variables on the stack; the ticket objects live on the heap.
        // 'ParkingTicket x = a;' copies the reference, not the object.
        ParkingTicket x = a; System.out.println("x == a (same object) ? " + (x == a));

        // Cost of creating n tickets: constructor vs factory vs default+setters
        int n = 2_000_000; long t0 = System.nanoTime(); long sink = 0;
        for (int i = 0; i < n; i++) sink += new ParkingTicket("R" + (i & 7), "CAR", 0, i & 15, i).getTicketNo();
        long tCons = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) sink += ParkingTicket.of("R" + (i & 7), "CAR", 0, i & 15, i).getTicketNo();
        long tFact = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) { ParkingTicketMutable m = new ParkingTicketMutable(); m.setRegNo("R" + (i & 7)); m.setFloor(0); m.setSlot(i & 15); sink += m.slot; }
        long tSet = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("Creating " + n + " tickets: constructor " + tCons + " ms, factory " + tFact + " ms, default+setters " + tSet + " ms (all O(n) time, O(n) space)");
        System.out.println("(sink=" + (sink > 0) + ")");
    }
}
