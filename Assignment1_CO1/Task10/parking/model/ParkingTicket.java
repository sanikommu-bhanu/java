package parking.model;

/** Model layer: data + validation. Fields are private; the only way in is a validating constructor/setter. */
public class ParkingTicket {
    private final int ticketNo;            // private  : only this class
    private final String regNo;
    private int floor, slot;
    private int durationMinutes;
    boolean printed;                       // default (package-private): only classes in parking.model
    protected String note = "";            // protected: subclasses (and the package)

    public ParkingTicket(int ticketNo, String regNo, int floor, int slot) {
        if (ticketNo <= 0) throw new IllegalArgumentException("ticketNo must be positive");
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("regNo required");
        this.ticketNo = ticketNo; this.regNo = regNo; setSlotPosition(floor, slot);
    }
    public void setSlotPosition(int floor, int slot) {
        if (floor < 0 || slot < 0) throw new IllegalArgumentException("invalid slot position " + floor + "," + slot);
        this.floor = floor; this.slot = slot;
    }
    public void setDurationMinutes(int m) {
        if (m < 0) throw new IllegalArgumentException("duration cannot be negative: " + m);
        this.durationMinutes = m;
    }
    public int getTicketNo() { return ticketNo; }
    public String getRegNo() { return regNo; }
    public int getDurationMinutes() { return durationMinutes; }
    void markPrinted() { printed = true; }               // package-private helper
    @Override public String toString() { return "Ticket#" + ticketNo + " " + regNo + " F" + floor + "-S" + slot + " " + durationMinutes + "min"; }
}
