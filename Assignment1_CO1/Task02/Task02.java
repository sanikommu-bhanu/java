import java.util.*;

/** TASK 2 - Choose the data representation (types, arrays, casting, input). */
public class Task02 {
    public static void main(String[] args) {
        // Input: Scanner over a String keeps the demo repeatable (use new Scanner(System.in) for real input)
        Scanner sc = new Scanner("TN01AB1234 130\n");
        String regNo = sc.next();          // String  : alphanumeric registration number
        int minutes = sc.nextInt();        // int     : duration in minutes (max ~2^31 min, plenty)
        double rate = 30;                  // double  : Rs per hour (may have paise)
        long entryEpochMs = 1_700_000_000_000L;  // long : milliseconds since 1970 do NOT fit in int

        // ---- Casting / integer-division risk ----
        double wrong1 = (minutes / 60) * rate;                 // 130/60 = 2 (integer division) -> 60.0  (loses 1 started hour)
        double wrong2 = (double) (minutes / 60) * rate;        // cast applied AFTER division: still 60.0
        double wrong3 = Math.ceil(minutes / 60) * rate;        // minutes/60 already int: ceil(2) = 2 -> 60.0
        int hours = (minutes + 59) / 60;                       // integer ceiling, no floating point
        double right = hours * rate;                           // 3 * 30 = 90.0
        double right2 = Math.ceil(minutes / 60.0) * rate;      // 60.0 forces floating division -> also 90.0
        System.out.println("Vehicle " + regNo + ", " + minutes + " min, rate " + rate + "/h");
        System.out.println("minutes/60 * rate          = " + wrong1 + "   <-- WRONG (integer division)");
        System.out.println("(double)(minutes/60)*rate  = " + wrong2 + "   <-- WRONG (cast too late)");
        System.out.println("ceil(minutes/60)*rate      = " + wrong3 + "   <-- WRONG (division already truncated)");
        System.out.println("(minutes+59)/60 * rate     = " + right + "   <-- CORRECT (used in design)");
        System.out.println("ceil(minutes/60.0) * rate  = " + right2 + "   <-- CORRECT (alternative)");
        System.out.println("int overflow check: (int) entryEpochMs = " + (int) entryEpochMs + "  vs long = " + entryEpochMs);
        System.out.println("double precision: 0.1 + 0.2 = " + (0.1 + 0.2) + "  -> keep money in paise (long) when exactness matters");

        // ---- Jagged array: floors with different slot counts ----
        int[] slotsPerFloor = {20, 35, 50};
        String[][] jagged = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) jagged[f] = new String[slotsPerFloor[f]];
        jagged[1][34] = "TN09ZZ9999";
        System.out.println("Jagged array lengths: " + jagged[0].length + ", " + jagged[1].length + ", " + jagged[2].length);

        // ---- ALTERNATIVE 1: 1-D array, index = offset[floor] + slot (offsets needed for uneven floors) ----
        int[] offset = new int[slotsPerFloor.length + 1];
        for (int f = 0; f < slotsPerFloor.length; f++) offset[f + 1] = offset[f] + slotsPerFloor[f];
        String[] flat = new String[offset[slotsPerFloor.length]];
        flat[offset[1] + 34] = "TN09ZZ9999";
        System.out.println("Flat array size " + flat.length + "; floor*slotsPerFloor formula would be wrong here, offset[1]+34 = " + (offset[1] + 34));

        // ---- ALTERNATIVE 2: ArrayList<String[]> ----
        List<String[]> floors = new ArrayList<>();
        for (int s : slotsPerFloor) floors.add(new String[s]);
        floors.add(new String[10]);  // a new floor can be added at run time (arrays cannot grow)
        System.out.println("ArrayList<String[]> now has " + floors.size() + " floors (grown at run time)");
        System.out.println("Locate slot: jagged O(1), flat O(1) (+offset table), ArrayList O(1) (+1 indirection).");
        System.out.println("Space: jagged = F array objects; flat = 1 array + F+1 offsets; ArrayList = F arrays + list overhead.");
    }
}
