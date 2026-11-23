package parking.app;

import parking.model.ParkingTicket;
import parking.service.ParkingService;

public class Main {
    public static void main(String[] args) {
        ParkingService svc = new ParkingService();
        ParkingTicket t = svc.issue("TN01AB1111", 0, 4);
        t.setDurationMinutes(130);
        System.out.println(t + " fee = Rs " + svc.fee(t, 30));
        try { t.setDurationMinutes(-5); } catch (IllegalArgumentException e) { System.out.println("Rejected: " + e.getMessage()); }
        try { t.setSlotPosition(-1, 3); } catch (IllegalArgumentException e) { System.out.println("Rejected: " + e.getMessage()); }
        // t.printed = true;        // compile error: 'printed' is not public in ParkingTicket; cannot be accessed from outside package
        // t.markPrinted();         // compile error for the same reason

        // Wrapper classes / autoboxing risk
        Integer a = 127, b = 127, c = 1000, d = 1000;
        System.out.println("127 == 127  (Integer cache): " + (a == b));
        System.out.println("1000 == 1000 (Integer objects): " + (c == d) + "   <-- compares references!");
        System.out.println("1000 equals 1000: " + c.equals(d));
        Integer nothing = null;
        try { int x = nothing; } catch (NullPointerException e) { System.out.println("Unboxing null -> NullPointerException"); }

        // Alternative: public fields + validation by callers -> the check must be repeated everywhere
        System.out.println("With public fields every caller must repeat 'if (m < 0)'; here the check exists once (setDurationMinutes). Cost per update O(1) either way.");
    }
}
