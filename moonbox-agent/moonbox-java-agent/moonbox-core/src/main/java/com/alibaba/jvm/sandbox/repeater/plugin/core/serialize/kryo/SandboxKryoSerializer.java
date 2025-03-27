package com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Objects;

public class SandboxKryoSerializer {


    private final static ThreadLocal<Output> outputLocal = new ThreadLocal<>();
    private final static ThreadLocal<Input> inputLocal = new ThreadLocal<>();
    private final static ThreadLocal<Kryo> kryoLocal = new ThreadLocal<>();

//	private Class<?> ct = null;

    public static Kryo getKryo(Class<?> serializerClass) {
        Kryo kryo= null;
        if ((kryo = kryoLocal.get()) == null) {
            kryo = new Kryo();
            kryo.register(serializerClass);
            kryoLocal.set(kryo);
        }
        kryo.setReferences(false);
        kryo.setRegistrationRequired(false);
        return kryo;
    }

    /**
     * 共享部署需要 注册 WEB-INF/lib下的jar的对象
     * @param serializerClass
     * @param extSerializerClass
     * @return
     */
    public static Kryo getKryoExtClass(Class<?> serializerClass,Class<?> [] extSerializerClass) {
        Kryo kryo= null;
        if ((kryo = kryoLocal.get()) == null) {
            kryo = new Kryo();
            // 20230117 yijiakang  RecordMap kryo 序列化反序列化 会丢失config 参数，自定义序列化来 保存 config和还原 config
            kryo.register(serializerClass);
            if (Objects.nonNull(extSerializerClass)) {
                for (Class<?> aClass : extSerializerClass) {
                    kryo.register(aClass);
                }
            }
            kryoLocal.set(kryo);
        }
        return kryo;
    }

    public static Output getOutput() {
        Output output = null;
        if ((output = outputLocal.get()) == null) {
            output = new Output(1, -1);
            outputLocal.set(output);
        }
        //shangjian 20220525 kryo5.x版本方法变更修复
        output.reset();
        return output;
    }

    public static Input getInput() {
        Input input = null;
        if ((input = inputLocal.get()) == null) {
            input = new Input();
            inputLocal.set(input);
        }
        return input;
    }

    public static void remove() {
        outputLocal.remove();
        inputLocal.remove();
        kryoLocal.remove();

    }
}
