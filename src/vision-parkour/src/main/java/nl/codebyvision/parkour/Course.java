package nl.codebyvision.parkour;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

/** A parkour: pressure plates for start, checkpoints (in order) and finish. Locations are block locations. */
final class Course {

    final String name;
    Location start;
    final List<Location> checkpoints = new ArrayList<>();
    Location finish;

    Course(String name) {
        this.name = name;
    }

    boolean isReady() {
        return start != null && finish != null;
    }
}
