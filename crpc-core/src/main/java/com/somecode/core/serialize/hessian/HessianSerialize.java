package com.somecode.core.serialize.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.somecode.common.spi.core.Serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Hessian序列化方式
 */
public class HessianSerialize implements Serialize {

    /**
     * 序列化
     */
    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            throw new NullPointerException("序列化的对象不能为空！");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HessianOutput hessianOutput = new HessianOutput(os);
        try {
            hessianOutput.writeObject(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return os.toByteArray();
    }

    /**
     * 反序列化
     */
    @Override
    public Object deserialize(byte[] bytes) {
        if (bytes == null){
            throw new NullPointerException();
        }
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        HessianInput hi = new HessianInput(is);
        Object obj = null;
        try {
            obj = hi.readObject();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

}
