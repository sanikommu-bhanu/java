/**
 * TASK 6 - Inheritance hierarchy
 *
 * UML (hierarchical + multilevel):
 *                 +------------------+
 *                 | Vehicle (abstract)|  regNo, ratePerHour()
 *                 +---------+--------+
 *             +-------------+--------------+
 *      +------+-----+                +-----+-----+
 *      | TwoWheeler |                |    Car    |  (rate 30)
 *      +------------+                +-----+-----+
 *                                          |
 *                                    +-----+------+
 *                                    | ElectricCar|  + chargingFee(kWh)
 *                                    +------------+
 *   User (abstract) <|-- Student, Staff, Visitor        (hierarchical)
 */
abstract class Vehicle {
    private final String regNo;
    protected Vehicle(String regNo) { this.regNo = regNo; System.out.println("  Vehicle constructor  (regNo=" + regNo + ")"); }
    public String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    public double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }
    public String describe() { return getClass().getSimpleName() + " " + regNo; }
}
class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo); System.out.println("  TwoWheeler constructor"); }
    @Override protected double ratePerHour() { return 10; }
}
class Car extends Vehicle {
    protected final int seats;
    Car(String regNo) { super(regNo); this.seats = 5; System.out.println("  Car constructor"); }
    @Override protected double ratePerHour() { return 30; }
}
class ElectricCar extends Car {
    private final int batteryKwh;
    ElectricCar(String regNo, int batteryKwh) { super(regNo); this.batteryKwh = batteryKwh; System.out.println("  ElectricCar constructor"); }
    double chargingFee(int kWh) { return kWh * 12; }
    @Override public String describe() { return super.describe() + " (battery " + batteryKwh + " kWh, seats " + seats + ")"; } // super.method()
}
abstract class User {
    final String name; User(String name) { this.name = name; }
    abstract boolean parksFree();
}
class Student extends User { Student(String n) { super(n); } boolean parksFree() { return true; } }
class Staff extends User { Staff(String n) { super(n); } boolean parksFree() { return true; } }
class Visitor extends User { Visitor(String n) { super(n); } boolean parksFree() { return false; } }

public class Task06 {
    public static void main(String[] args) {
        System.out.println("Creating ElectricCar -> constructor execution order (superclass first):");
        ElectricCar ev = new ElectricCar("TN03C3", 40);
        System.out.println(ev.describe());
        System.out.println("Fee for 130 min: Rs " + ev.calculateFee(130) + " + charging 10 kWh: Rs " + ev.chargingFee(10));
        User[] users = { new Student("Arun"), new Staff("Divya"), new Visitor("Kiran") };
        for (User u : users) System.out.println(u.getClass().getSimpleName() + " " + u.name + " parks free: " + u.parksFree());
        System.out.println("Space per vehicle: inheritance = fields of the chain only; composition adds one VehicleType reference (4 B).");
    }
}
