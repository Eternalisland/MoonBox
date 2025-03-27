package com.alibaba.jvm.sandbox.repeater.plugin.core.serialize;

import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.kryo.SandboxKryoSerializer;
import com.esotericsoftware.kryo.Kryo;

public class KryoSerializer extends AbstractSerializerAdapter {
    @Override
    public Type type() {
        return Type.KRYO;
    }

    @Override
    public byte[] serialize(Object object, ClassLoader classLoader) throws SerializeException {
        return null;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type, ClassLoader classLoader) throws SerializeException {
        return null;
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializeException {
        return null;
    }
}
