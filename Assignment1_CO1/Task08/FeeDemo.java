import java.time.LocalTime;

interface Chargeable {
    double chargingFee(int kWh);
}
abstract class Vehicle {
    private final String regNo;
    protected Vehicle(String regNo) { this.regNo = regNo; }
    public String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    /** Every started hour is charged in full. */
    public double calculateFee(int minutes) {
        // TODO 1: Compute billable hours as the ceiling of minutes / 60 using integer
        // arithmetic only, and return hours × ratePerHour()
        int hours = (minutes + 59) / 60;
        return hours * ratePerHour();
    }
    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
}
class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 10; }
}
class Car extends Vehicle {
    // TODO 2: Write the constructor using super, and override ratePerHour() to return
    // 30
    Car(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return 30; }
}
class ElectricCar extends Car implements Chargeable {
    ElectricCar(String regNo) { super(regNo); }
    // TODO 3: Implement chargingFee() at Rs 12 per kWh
    @Override public double chargingFee(int kWh) { return kWh * 12; }
}
public class FeeDemo {
    public static void main(String[] args) {
        Vehicle[] parked = { new TwoWheeler("TN01A1"), new Car("TN02B2"),
                new ElectricCar("TN03C3") };
        for (Vehicle v : parked) {
            double fee = v.calculateFee(130); // dynamic method dispatch
            if (v instanceof Chargeable c) fee += c.chargingFee(10);
            System.out.println(v.getClass().getSimpleName() + " " + v.getRegNo()
                    + " -> Rs " + fee);
        }
        System.out.println(new Car("TN02B2").equals(new ElectricCar("TN02B2")));
    }
}

/** Extra demo for Task 8: compile-time overloading, upcasting and a traced dynamic dispatch. Run: java Overloads */
class Overloads {
    // Overloaded methods: the COMPILER picks by the argument types (compile-time polymorphism)
    static double fee(Vehicle v, int minutes) { return v.calculateFee(minutes); }
    static double fee(Vehicle v, LocalTime in, LocalTime out) {
        int minutes = (int) java.time.Duration.between(in, out).toMinutes();
        return v.calculateFee(minutes);
    }
    public static void main(String[] args) {
        Vehicle v = new ElectricCar("TN03C3");             // declared type Vehicle, actual type ElectricCar (upcasting)
        System.out.println("fee(Vehicle,int)               -> " + fee(v, 130));
        System.out.println("fee(Vehicle,LocalTime,LocalTime)-> " + fee(v, LocalTime.of(9, 0), LocalTime.of(11, 10)));
        System.out.println("TRACE: v.calculateFee(130) | declared type = Vehicle | actual type = "
                + v.getClass().getSimpleName() + " | method found = Vehicle.calculateFee (inherited) | inside it ratePerHour() dispatches to Car.ratePerHour() = "
                + 30.0);
        Vehicle[] all = { new TwoWheeler("A"), new Car("B"), new ElectricCar("C") };
        for (Vehicle x : all) System.out.println(x.getClass().getSimpleName() + " -> " + x.calculateFee(61));
        System.out.println("No if-else chain on the vehicle type was needed.");
    }
}
