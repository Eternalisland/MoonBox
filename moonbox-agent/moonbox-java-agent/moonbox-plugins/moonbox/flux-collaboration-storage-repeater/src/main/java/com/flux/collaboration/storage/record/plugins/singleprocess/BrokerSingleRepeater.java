package com.flux.collaboration.storage.record.plugins.singleprocess;

import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.ClassloaderBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractRepeater;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.MoonboxContext;
import com.alibaba.jvm.sandbox.repeater.plugin.core.spring.SpringContextAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.MethodSignatureParser;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.exception.RepeatException;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.Repeater;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import com.vivo.internet.moonbox.common.api.model.JavaRecordInterface;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;

import java.lang.reflect.Method;
import java.util.List;

@MetaInfServices(Repeater.class)
public class BrokerSingleRepeater extends AbstractRepeater {
    @Override
    protected Object executeRepeat(RepeatContext context) throws Exception {
        Invocation invocation = context.getRecordModel().getEntranceInvocation();
        if (!getType().equals(invocation.getType())) {
            throw new RepeatException("invoke type miss match, required invoke type is: " + invocation.getType());
        }
        Identity identity = invocation.getIdentity();
        String[] array = identity.getEndpoint().split("~");

        // array[0]=/methodName
        String methodName = getTargetMethod(array[0]);

        Object bean = getTargetBean(identity, methodName);

        Class<?> beanClass = bean.getClass();
        Class<?> AopUtilsClass = ClassloaderBridge.instance().findClassInstance("org.springframework.aop.support.AopUtils");;
        boolean isAopPoxy = (Boolean) MethodUtils.invokeStaticMethod(AopUtilsClass, "isAopProxy", bean);
        if(isAopPoxy) {
            beanClass = (Class<?>) MethodUtils.invokeStaticMethod(AopUtilsClass, "getTargetClass", bean);
        }

        ClassLoader classLoader = ClassloaderBridge.instance().decode(invocation.getSerializeToken());
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        // fix issue#9 int.class基本类型被解析成包装类型，通过java方法签名来规避这类问题
        // array[1]=javaMethodDesc
        // 这里需要兼容 没有参数的方法 的情况，例如 public void nonArgsMethod(){}，此时 array[1] 会抛出数组越界的情况
        Method method;
        if (array.length > 1) {
            MethodSignatureParser.MethodSpec methodSpec = MethodSignatureParser.parseIdentifier(array[1]);
            Class<?>[] parameterTypes = MethodSignatureParser.loadClass(methodSpec.getParamIdentifiers(), classLoader);
            method = beanClass.getDeclaredMethod(methodName, parameterTypes);
        } else {
            if (array.length == 1) {
                String paramTypeStr = StringUtils.substringAfter(StringUtils.substringBefore(array[0], ")"), "(");
                String[] paramTypeStrs = paramTypeStr.split(",");
                Class<?>[] parameterTypes = new Class[paramTypeStrs.length];
                int index = 0;
                for (String tmpParamTypeStr : paramTypeStrs) {
                    Class<?> paramType = ClassloaderBridge.instance().findClassInstance(tmpParamTypeStr);
                    parameterTypes[index] = paramType;
                    index++;
                }
                method = beanClass.getDeclaredMethod(methodName, parameterTypes);
            } else {
                method = beanClass.getDeclaredMethod(methodName);
            }
        }
        // 这里没法办像HTTP、DUBBO透传traceId，因此在执行前先执行Trace.start()，根据traceId创建好TraceContext，避免在
        Tracer.start(context.getTraceId());
        // 开始invoke
        return method.invoke(bean, invocation.getRequest());
    }

    /**
     * 获得即将回放的java方法
     */
    private String getTargetMethod(String methodName) {
        String method = methodName.substring(1);
        // 这里 method 可能为 method()，不知道是不是录制的地方有地方冗余了，这里先简单处理下
        int leftBracketIndex = method.indexOf("(");
        if (leftBracketIndex != -1) {
            method = method.substring(0, leftBracketIndex);
        }
        return method;
    }

    /**
     * 获得回放目标 bean
     */
    private Object getTargetBean(Identity identity, String methodName) throws Exception {
        Object bean = SpringContextAdapter.getBeanByType(identity.getLocation());
        if (bean == null) {
            RepeaterConfig repeaterConfig = MoonboxContext.getInstance().getConfig();
            List<JavaRecordInterface> javaRecordInterfaces = repeaterConfig.getJavaRecordInterfaces();
            String contextMethod = null;
            for (JavaRecordInterface javaRecordInterface : javaRecordInterfaces) {
                if (javaRecordInterface.getClassPattern().contains(identity.getLocation())) {
                    String[] methodPatterns = javaRecordInterface.getMethodPatterns();
                    for (String methodPattern : methodPatterns) {
                        if (methodPattern.equals(methodName)) {
                            contextMethod = javaRecordInterface.getObtainApplicationContextMethod();
                            break;
                        }
                    }
                }
            }
            if (contextMethod != null) {
                int lastIndexOfDot = contextMethod.lastIndexOf(".");
                Class<?> applicationContext = ClassloaderBridge.instance().findClassInstance(contextMethod.substring(0, lastIndexOfDot));
                Object applicationContextObject = MethodUtils.invokeStaticMethod(applicationContext, contextMethod.substring(lastIndexOfDot + 1));
                Method getBeanMethod = applicationContextObject.getClass().getMethod("getBean", String.class);
                int lastDotIndex = identity.getLocation().lastIndexOf(".");
                String className = identity.getLocation().substring(lastDotIndex + 1);
                bean = getBeanMethod.invoke(applicationContextObject, StringUtils.uncapitalize(className));
            }
        }

        if (bean == null) {
            throw new RepeatException("no bean found in context, className=" + identity.getLocation());
        }

        return bean;
    }
    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_BROKER_SINGLE;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_BROKER_SINGLE.getInvokeName() ;
    }
}
