import java.io.*;
import java.sql.Connection;
import java.util.*;

/** A4 - serialization of Hostel -> Room -> Resident. Usage: java HostelSerialization save|load <file> */
class Resident implements Serializable {
    private static final long serialVersionUID = 1L;                  // @UID
    private final String name, regNo;
    private transient String password;                                // never persisted
    private transient Connection connection;                          // live resource, never persisted
    private String phone;                                             // @PHONE
    Resident(String name, String regNo, String password) { this.name = name; this.regNo = regNo; this.password = password; }
    void setPhone(String p) { phone = p; }                            // @PHONE
    @Override public String toString() { return name + "/" + regNo + " password=" + password + " connection=" + connection; }
    String phoneInfo() { try { java.lang.reflect.Field f = getClass().getDeclaredField("phone"); f.setAccessible(true); return "phone=" + f.get(this); } catch (ReflectiveOperationException e) { return "(class has no phone field)"; } }
}
class Room implements Serializable { final int no; final List<Resident> residents = new ArrayList<>(); Room(int no) { this.no = no; } }
class Hostel implements Serializable { final List<Room> rooms = new ArrayList<>(); }

public class HostelSerialization {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) { System.out.println("usage: save|load <file>"); return; }
        File file = new File(args[1]);
        if (args[0].equals("save")) {
            Hostel h = new Hostel(); Room r = new Room(101); Resident a = new Resident("Arun", "192311001", "s3cret");
            a.setPhone("9000000001");                                  // @PHONE
            r.residents.add(a); r.residents.add(new Resident("Divya", "192311002", "pw2")); h.rooms.add(r); h.rooms.add(new Room(102));
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) { o.writeObject(h); }
            System.out.println("saved " + h.rooms.size() + " rooms, file " + file.length() + " bytes");
        } else {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                Hostel h = (Hostel) in.readObject();
                for (Room r : h.rooms) { System.out.println("Room " + r.no + ": " + r.residents.size() + " residents"); for (Resident x : r.residents) System.out.println("   " + x + " " + x.phoneInfo()); }
            } catch (InvalidClassException e) { System.out.println("InvalidClassException: " + e.getMessage()); }
        }
    }
}
