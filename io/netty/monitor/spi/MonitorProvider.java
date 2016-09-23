
package io.netty.monitor.spi;

import java.io.Serializable;

public final class MonitorProvider
implements Serializable,
Comparable<MonitorProvider> {
    private static final long serialVersionUID = -6549490566242173389L;
    private final String name;

    public static MonitorProvider named(String name) {
        return new MonitorProvider(name);
    }

    private MonitorProvider(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (name.length() < 1) {
            throw new IllegalArgumentException("Argument 'name' must not be blank");
        }
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int compareTo(MonitorProvider o) {
        return this.name.compareTo(o.name);
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
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
        MonitorProvider other = (MonitorProvider)obj;
        if (this.name == null ? other.name != null : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "MonitorProvider(" + this.name + ')';
    }
}

