package nl.codebyvision.parkour;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Courses (courses.yml) and best times (times.yml). */
final class CourseManager {

    record Time(UUID player, String name, long millis) {
    }

    private final VisionParkour plugin;
    private final File coursesFile;
    private final File timesFile;
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private YamlConfiguration times;

    CourseManager(VisionParkour plugin) {
        this.plugin = plugin;
        this.coursesFile = new File(plugin.getDataFolder(), "courses.yml");
        this.timesFile = new File(plugin.getDataFolder(), "times.yml");
        load();
    }

    void load() {
        courses.clear();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(coursesFile);
        for (String name : data.getKeys(false)) {
            Course course = new Course(name);
            course.start = data.getLocation(name + ".start");
            course.finish = data.getLocation(name + ".finish");
            for (Object checkpoint : data.getList(name + ".checkpoints", List.of())) {
                if (checkpoint instanceof Location location) {
                    course.checkpoints.add(location);
                }
            }
            courses.put(name, course);
        }
        times = YamlConfiguration.loadConfiguration(timesFile);
    }

    Map<String, Course> all() {
        return courses;
    }

    Course get(String name) {
        return courses.get(name);
    }

    Course create(String name) {
        Course course = new Course(name);
        courses.put(name, course);
        saveCourses();
        return course;
    }

    boolean delete(String name) {
        boolean removed = courses.remove(name) != null;
        if (removed) {
            saveCourses();
            times.set(name, null);
            saveTimes();
        }
        return removed;
    }

    void saveCourses() {
        YamlConfiguration data = new YamlConfiguration();
        for (Course course : courses.values()) {
            data.set(course.name + ".start", course.start);
            data.set(course.name + ".finish", course.finish);
            data.set(course.name + ".checkpoints", new ArrayList<>(course.checkpoints));
        }
        save(data, coursesFile);
    }

    long best(String course, UUID player) {
        return times.getLong(course + "." + player + ".millis", -1);
    }

    boolean hasFinished(String course, UUID player) {
        return times.contains(course + "." + player);
    }

    /** Stores the time when it is the player's best. Returns true for a new personal best. */
    boolean record(String course, UUID player, String name, long millis) {
        long best = best(course, player);
        if (best >= 0 && best <= millis) {
            return false;
        }
        times.set(course + "." + player + ".name", name);
        times.set(course + "." + player + ".millis", millis);
        saveTimes();
        return true;
    }

    List<Time> top(String course, int limit) {
        ConfigurationSection section = times.getConfigurationSection(course);
        if (section == null) {
            return List.of();
        }
        List<Time> list = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            list.add(new Time(UUID.fromString(key), section.getString(key + ".name", "?"), section.getLong(key + ".millis")));
        }
        list.sort(Comparator.comparingLong(Time::millis));
        return list.subList(0, Math.min(limit, list.size()));
    }

    private void saveTimes() {
        save(times, timesFile);
    }

    private void save(YamlConfiguration data, File file) {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
