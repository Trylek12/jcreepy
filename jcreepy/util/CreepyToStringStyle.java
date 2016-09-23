
package jcreepy.util;

import jcreepy.util.Named;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CreepyToStringStyle
extends ToStringStyle {
    public static final CreepyToStringStyle INSTANCE = new CreepyToStringStyle();
    private static final long serialVersionUID = 1;

    private CreepyToStringStyle() {
        this.setUseClassName(false);
        this.setUseShortClassName(true);
        this.setArrayContentDetail(false);
        this.setUseIdentityHashCode(false);
        this.setContentStart("{");
        this.setContentEnd("}");
        this.setArrayStart("arr[");
        this.setArrayEnd("]");
    }

    @Override
    public void appendStart(StringBuffer buffer, Object object) {
        if (object != null) {
            if (object instanceof Named) {
                buffer.append(((Named)object).getName());
            } else {
                buffer.append(object.getClass().getSimpleName());
            }
            this.appendContentStart(buffer);
        }
    }
}

