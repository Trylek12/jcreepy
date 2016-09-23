
package io.netty.monitor;

public final class MonitorName {
    private final String group;
    private final String type;
    private final String name;
    private final String instance;

    public MonitorName(Class<?> monitoredClass, String name) {
        this(monitoredClass.getPackage() != null ? monitoredClass.getPackage().getName() : "", monitoredClass.getSimpleName().replaceAll("\\$$", ""), name, null);
    }

    public MonitorName(Class<?> monitoredClass, String name, String instance) {
        this(monitoredClass.getPackage().getName(), monitoredClass.getSimpleName(), name, instance);
    }

    public MonitorName(String group, String type, String name) {
        this(group, type, name, null);
    }

    public MonitorName(String group, String type, String name, String instance) {
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.group = group;
        this.type = type;
        this.name = name;
        this.instance = instance;
    }

    public MonitorName ofInstance(String instance) {
        if (instance == null) {
            throw new NullPointerException("instance");
        }
        if (instance.equals(this.instance)) {
            return this;
        }
        return new MonitorName(this.group, this.type, this.name, instance);
    }

    public String getGroup() {
        return this.group;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getInstance() {
        return this.instance;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.group == null ? 0 : this.group.hashCode());
        result = 31 * result + (this.instance == null ? 0 : this.instance.hashCode());
        result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
        result = 31 * result + (this.type == null ? 0 : this.type.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        MonitorName other = (MonitorName)obj;
        if (this.group == null ? other.group != null : !this.group.equals(other.group)) {
            return false;
        }
        if (this.instance == null ? other.instance != null : !this.instance.equals(other.instance)) {
            return false;
        }
        if (this.name == null ? other.name != null : !this.name.equals(other.name)) {
            return false;
        }
        if (this.type == null ? other.type != null : !this.type.equals(other.type)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.instance != null ? "Monitor(" + this.group + '/' + this.type + '/' + this.name + '/' + this.instance + ')' : "Monitor(" + this.group + '/' + this.type + '/' + this.name + ')';
    }
}

