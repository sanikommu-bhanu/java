/*
 * TASK 3 - Control logic.   PSEUDOCODE (SIMATS style)
 *
 * ALGORITHM Allocate(vehicleNo)
 *   INPUT : vehicleNo     OUTPUT: {floor, slot} or NULL
 *   FOR floor <- 0 TO numberOfFloors - 1 DO
 *       FOR slot <- 0 TO slotsOnFloor(floor) - 1 DO
 *           IF slots[floor][slot] = NULL THEN
 *               slots[floor][slot] <- vehicleNo
 *               RETURN {floor, slot}            // search stops at the first free slot
 *           END IF
 *       END FOR
 *   END FOR
 *   RETURN NULL                                 // facility full
 * END ALGORITHM
 *
 * ALGORITHM ComputeFee(minutes, rate, hasPass)
 *   IF hasPass THEN RETURN 0
 *   IF minutes <= 0 THEN RETURN 0
 *   hours <- (minutes + 59) DIV 60
 *   fee   <- hours * rate
 *   IF fee > 8 * rate THEN fee <- 8 * rate      // daily maximum
 *   RETURN fee
 * END ALGORITHM
 */
public class ParkingFacility {
    // slots[floor][slot] holds a vehicle number, or null if the slot is free
    private final String[][] slots;
    public ParkingFacility(int[] slotsPerFloor) {
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            slots[f] = new String[slotsPerFloor[f]];
        }
    }
    /** Returns {floor, slot} of the first free slot, or null if it is full. */
    public int[] allocate(String vehicleNo) {
        // TODO 1: Search floor by floor and slot by slot; place vehicleNo in the first
        // free slot and return {floor, slot}
        for (int f = 0; f < slots.length; f++) {
            for (int s = 0; s < slots[f].length; s++) {
                if (slots[f][s] == null) {
                    slots[f][s] = vehicleNo;
                    return new int[]{f, s};
                }
            }
        }
        return null;
    }
    /** Frees a slot. Returns false if the position is invalid or already free. */
    public boolean release(int floor, int slot) {
        // TODO 2: Validate floor and slot, free the slot and return true; otherwise
        // return false
        if (floor < 0 || floor >= slots.length) return false;
        if (slot < 0 || slot >= slots[floor].length) return false;
        if (slots[floor][slot] == null) return false;
        slots[floor][slot] = null;
        return true;
    }
    public static void main(String[] args) {
        // floor 0 has 2 slots, floor 1 has 1 slot
        ParkingFacility p = new ParkingFacility(new int[]{2, 1});
        System.out.println(java.util.Arrays.toString(p.allocate("TN01AB1111")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN02CD2222")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN03EF3333")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN04GH4444")));
        System.out.println(p.release(0, 1) + " " + p.release(0, 1) + " "
                + p.release(5, 0));
        System.out.println(java.util.Arrays.toString(p.allocate("TN04GH4444")));
    }
}
