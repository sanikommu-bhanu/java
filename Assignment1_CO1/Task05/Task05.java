import java.lang.ref.WeakReference;

/** TASK 5 - static members and object life cycle. */
class Facility {
    private static int ticketCounter;                 // ONE counter for the whole university
    private static int totalOccupancy;                // class-wide statistic
    static final int MAX_HOURS_PER_DAY;               // initialised once by the static block
    static { MAX_HOURS_PER_DAY = 8; System.out.println("[static block] Facility class loaded"); }

    private final String name;
    private int instanceCounter;                      // WRONG choice for ticket numbers (alternative concept)
    Facility(String name) { this.name = name; }

    int issueTicket() { totalOccupancy++; return ++ticketCounter; }
    int issueInstanceTicket() { return 1000 + (++instanceCounter); }
    static int getTotalOccupancy() { return totalOccupancy; }     // static method: needs no object
}

class Ticket { int no; Ticket(int no) { this.no = no; } }

public class Task05 {
    static void modify(Ticket t) { t.no = 999; t = new Ticket(111); }   // changes object; reassigning local reference has no effect
    static Ticket create(int n) { return new Ticket(n); }               // object returned by reference

    public static void main(String[] args) {
        Facility north = new Facility("North"), south = new Facility("South");
        System.out.println("static counter  -> north: " + north.issueTicket() + ", south: " + south.issueTicket() + ", north: " + north.issueTicket());
        System.out.println("instance counter-> north: " + north.issueInstanceTicket() + ", south: " + south.issueInstanceTicket()
                + "  (two different tickets numbered 1001 would exist - duplicates!)");
        System.out.println("Total occupancy (static method, no object): " + Facility.getTotalOccupancy() + ", MAX_HOURS=" + Facility.MAX_HOURS_PER_DAY);

        Ticket t = create(5); modify(t);
        System.out.println("After modify(): t.no = " + t.no + " (state changed, reference not replaced)");

        WeakReference<Ticket> ref = new WeakReference<>(new Ticket(42));     // ticket after the vehicle leaves
        System.out.println("Before GC, ticket reachable: " + (ref.get() != null));
        System.gc();                                                          // only a hint
        System.out.println("After no strong reference + GC hint, ticket collected: " + (ref.get() == null));
        System.out.println("Space of counter: static = O(1) total; instance = O(k) for k facilities; tickets O(n) either way.");
    }
}
