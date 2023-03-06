package com.somecode.core.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.somecode.common.spi.core.Serialize;

import java.io.ByteArrayOutputStream;

/**
 * Kryo序列化
 */
public class KryoSerialize implements Serialize {

    @Override
    public byte[] serialize(Object object) {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, object);
        output.flush();
        return bos.toByteArray();
    }

    @Override
    public Object deserialize(byte[] bytes) {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        return kryo.readClassAndObject(new Input(bytes));
    }
}
