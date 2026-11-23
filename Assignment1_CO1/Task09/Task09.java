/** TASK 9 - Abstraction: abstract class + interfaces (multiple behaviours, default and static methods). */
interface Chargeable {
    double RATE_PER_KWH = 12;                              // interface constant
    double chargingFee(int kWh);
    default double chargingFeeWithGst(int kWh) { return chargingFee(kWh) * 1.18; }      // default method: shared behaviour
    static boolean validKwh(int kWh) { return kWh > 0 && kWh <= 100; }                   // static utility on the interface
}
interface Payable {
    double amountDue();
    default String receipt() { return "Receipt: Rs " + amountDue(); }
}
abstract class Vehicle {                                    // abstract class: STATE (regNo), constructor, partial implementation
    private final String regNo;
    protected Vehicle(String regNo) { this.regNo = regNo; }
    String regNo() { return regNo; }
    protected abstract double ratePerHour();                // WHAT must be provided; HOW is left to subclasses
    double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }
}
class Car extends Vehicle {
    Car(String r) { super(r); }
    @Override protected double ratePerHour() { return 30; }
}
class ElectricCar extends Car implements Chargeable, Payable {   // one class, several capabilities
    private final int minutes, kWh;
    ElectricCar(String r, int minutes, int kWh) { super(r); this.minutes = minutes; this.kWh = kWh; }
    @Override public double chargingFee(int k) { return k * RATE_PER_KWH; }
    @Override public double amountDue() { return calculateFee(minutes) + chargingFee(kWh); }
}
public class Task09 {
    public static void main(String[] args) {
        ElectricCar ev = new ElectricCar("TN03C3", 130, 10);
        System.out.println(ev.receipt());
        System.out.println("Charging with GST: " + ev.chargingFeeWithGst(10));
        System.out.println("Valid kWh 150? " + Chargeable.validKwh(150) + ", 10? " + Chargeable.validKwh(10));
        Payable p = ev; Chargeable c = ev; Vehicle v = ev;
        System.out.println("Same object seen as Payable/Chargeable/Vehicle: " + (p == c) + " " + (c == v));
        // new Vehicle("X");   // compile error: Vehicle is abstract; cannot be instantiated
        System.out.println("Abstract class chosen for Vehicle (shared state + constructor); interfaces for capabilities that unrelated classes can share.");
        System.out.println("If Vehicle were an interface: no instance fields/constructor; if Chargeable were an abstract class: EV could not also extend Car.");
        System.out.println("Method-call cost is the same (invokevirtual vs invokeinterface, both ~O(1)); interfaces add no per-object space.");
    }
}
