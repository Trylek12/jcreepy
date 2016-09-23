
package jcreepy.protocol.packet.entity;

import java.util.List;
import jcreepy.protocol.packet.entity.EntityPacket;
import jcreepy.protocol.util.Parameter;

public final class EntityMetadataPacket
extends EntityPacket {
    private final List<Parameter<?>> parameters;

    public EntityMetadataPacket(int id, List<Parameter<?>> parameters) {
        super(id);
        this.parameters = parameters;
    }

    public List<Parameter<?>> getParameters() {
        return this.parameters;
    }

    public static enum Parameters {
        META_INFLAMED(0, 1),
        META_CROUCHED(0, 2),
        META_MOBRIDER(0, 4),
        META_SPRINTING(0, 8),
        META_RIGHTCLICKACTION(0, 16),
        META_FULLDROWNINGCOUNTER(1, 300),
        META_DROWNINGCOUNTEDDEPLETED(1, -19),
        META_STARTDROWNING(1, 0),
        META_NOPOTIONEFFECT(8, 0),
        META_BABYANIMALSTAGE(12, -23999),
        META_PARENTANIMALSTAGE(12, 6000),
        META_BREEDANIMALSTAGE(12, 0);
        
        private Parameter<?> parameter;

        private Parameters(int index, int value) {
            this.parameter = new Parameter<Integer>(2, index, value);
        }

        private Parameters(int index, short value) {
            this.parameter = new Parameter<Short>(1, index, value);
        }

        private Parameters(int index, byte value) {
            this.parameter = new Parameter<Byte>(0, index, Byte.valueOf(value));
        }

        public Parameter<?> get() {
            return this.parameter;
        }
    }

}

