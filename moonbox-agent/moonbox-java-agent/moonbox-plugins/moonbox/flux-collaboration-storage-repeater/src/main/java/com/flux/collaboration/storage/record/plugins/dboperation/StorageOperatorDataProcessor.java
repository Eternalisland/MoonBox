package com.flux.collaboration.storage.record.plugins.dboperation;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.ContextResourceClear;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRecordCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ProtobufUtil;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yijiakang
 * @date 2025/1/28 13:04
 * @description 数据存储 功能描述
 */
public class StorageOperatorDataProcessor extends DefaultInvocationProcessor {

    private ThreadLocal<Map<String,String>> extraLocal = new ThreadLocal<>();
    /**
     * 执行类型
     *
     * @param type
     */
    public StorageOperatorDataProcessor(InvokeType type) {
        super(type);
    }

    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        // 查询逻辑标识
        if (event.javaMethodName.equals("getDBRecordBind")) {
            try {
                List<Object> values = (List<Object>) MethodUtils.invokeMethod(event.argumentArray[0], "getValues");
                if (values != null && !values.isEmpty()) {
                    List<String> extraValues = new ArrayList<>();
                    for (Object value : values) {
                        if (value == null) {
                            extraValues.add("");
                        } else {
                            extraValues.add(value.toString());
                        }
                    }
                    getExtra().put("values", StringUtils.join(extraValues.iterator(), "-"));
                }
                getExtra().put("selectSql", URLEncoder.encode((String) event.argumentArray[1], "utf-8"));
            } catch (Exception e) {}
        }
        Identity identity = super.assembleIdentity(event);
        extraLocal.remove();
        return identity;
    }

    /**
     *  获取执行信息
     * @return
     */
    public Map<String, String> getExtra() {
        Map<String, String> extra = extraLocal.get();
        if(extra == null ) {
            extra = new LinkedHashMap<>();
            extraLocal.set(extra);
        }
        return extra;
    }


    @Override
    public Object assembleResponse(Event event) {
        // 查询逻辑标识
        if (event.type == Event.Type.RETURN) {
            return ((ReturnEvent) event).object;
        }
        return null;
    }


    @Override
    public Object[] assembleRequest(BeforeEvent event) {
        if (event.argumentArray == null || event.argumentArray.length < 1) {
            return null;
        }

        if(event.javaMethodName.equals("getDBRecordBind")) {
            Object[] argumentArrays = new Object[2];
            argumentArrays[0] = event.argumentArray[0];
            argumentArrays[1] = event.argumentArray[1];
            return argumentArrays;
        }
        return event.argumentArray;
    }


    @Override
    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
        try {
            if(event.javaMethodName.equals("getDBRecordBind")) {
                Object resultBiz = event.argumentArray[2];
                Object response = invocation.getResponse();
                MethodUtils.invokeMethod(resultBiz, true, "fromResultSet", response, System.getProperty("file.encoding")) ;
            }else {
                return invocation.getResponse();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
