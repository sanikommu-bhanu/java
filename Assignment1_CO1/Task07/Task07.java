import java.util.*;

/** TASK 7 - Method overriding, final, equals/hashCode/toString. */
abstract class Vehicle {
    private final String regNo;                         // final field: registration number never changes
    protected Vehicle(String regNo) { this.regNo = regNo; }
    public final String getRegNo() { return regNo; }    // final method: subclasses may not change identity access
    protected abstract double ratePerHour();
    public double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }
    @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + "]"; }
    @Override public boolean equals(Object o) { return o instanceof Vehicle v && regNo.equals(v.regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }   // same field as equals => contract kept
}
class TwoWheeler extends Vehicle {
    TwoWheeler(String r) { super(r); }
    @Override protected double ratePerHour() { return 10; }
}
class Car extends Vehicle {
    Car(String r) { super(r); }
    @Override protected double ratePerHour() { return 30; }
}
class ElectricCar extends Car {
    ElectricCar(String r) { super(r); }
    @Override public double calculateFee(int minutes) { return super.calculateFee(minutes) * 1.0 + 20; }   // EV pays Rs 20 service fee
}
final class Bus extends Vehicle {                        // final class: cannot be extended
    Bus(String r) { super(r); }
    @Override protected double ratePerHour() { return 80; }
}

public class Task07 {
    public static void main(String[] args) {
        Vehicle[] vs = { new TwoWheeler("TN01A1"), new Car("TN02B2"), new ElectricCar("TN03C3"), new Bus("TN04D4") };
        for (Vehicle v : vs) System.out.println(v + " fee(130 min) = Rs " + v.calculateFee(130));
        Set<Vehicle> set = new HashSet<>();
        set.add(new Car("TN02B2")); set.add(new Car("TN02B2")); set.add(new TwoWheeler("TN02B2"));
        System.out.println("HashSet size after adding 3 vehicles with the same regNo: " + set.size());
        System.out.println("equals(): " + new Car("X1").equals(new ElectricCar("X1")) + ", hashCodes equal: " + (new Car("X1").hashCode() == new ElectricCar("X1").hashCode()));
        System.out.println("Overriding rules: same signature, return type same/covariant, access same or wider, @Override makes the compiler check it.");
        System.out.println("Alternative (switch on type field): time O(1) with k cases (jump table) or O(k) with if-chain; adding a type edits Vehicle + every switch.");
    }
}
