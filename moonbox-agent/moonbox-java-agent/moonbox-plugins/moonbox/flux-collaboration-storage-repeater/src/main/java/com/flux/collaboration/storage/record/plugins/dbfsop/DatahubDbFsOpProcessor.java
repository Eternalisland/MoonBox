package com.flux.collaboration.storage.record.plugins.dbfsop;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


@Slf4j
public class DatahubDbFsOpProcessor  extends DefaultInvocationProcessor {

    private ThreadLocal<Map<String,String>> extraLocal = new ThreadLocal<>();
    /**
     *
     * @param type
     */
    public DatahubDbFsOpProcessor(InvokeType type) {
        super(type);
    }


    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        // 查询逻辑标识
        Object mongoParamUtils = event.argumentArray[0];
        try {
            String collectionName = (String) MethodUtils.invokeMethod(mongoParamUtils, true,"getCollectionName");
            String organizationId = (String) MethodUtils.invokeMethod(mongoParamUtils, true,"getOrganizationId");
            Map<String, Object> fieldValues = (Map<String, Object>) MethodUtils.invokeMethod(mongoParamUtils, true,"getFieldValues");
            List<Map<String, Object>> rowsRecords = (List<Map<String, Object>>) MethodUtils.invokeMethod(mongoParamUtils, true,"getRowsRecord");
            Map<String, Object> rowsRecord = rowsRecords.isEmpty()? new HashMap<>() : rowsRecords.get(0) ;
            String datahubCustomerId = (String) fieldValues.get("datahubCustomerId");
            String messageId = (String) fieldValues.get("messageId");
            String jndi = (String) fieldValues.get("jndi");
            String filePath = StringUtils.defaultIfEmpty ((String) rowsRecord.get("filePath"), (String) fieldValues.get("filePath"));
            if(!StringUtils.isEmpty(organizationId)) {
                getExtra().put("organizationId",organizationId);
            }
            if(!StringUtils.isEmpty(collectionName)) {
                getExtra().put("collectionName",collectionName);
            }
            if(!StringUtils.isEmpty(datahubCustomerId)) {
                getExtra().put("datahubCustomerId",datahubCustomerId);
            }
            if(!StringUtils.isEmpty(messageId)) {
                getExtra().put("messageId",messageId);
            }
            if(!StringUtils.isEmpty(jndi)) {
                getExtra().put("jndi",jndi);
            }
            Identity identity = super.assembleIdentity(event);

            if(!StringUtils.isEmpty(filePath)) {
                getExtra().put("filePath",filePath);
            }
            return identity;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }

        return super.assembleIdentity(event);
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
    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
        Map<String, String> responseDataMap = (Map<String, String>) invocation.getResponse();
        if (responseDataMap == null || responseDataMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> fsNames = new ArrayList<>();
        Object mongoParamUtils = event.argumentArray[0];
        try {
            Map<String, Object> fieldValues = (Map<String, Object>) MethodUtils.invokeMethod(mongoParamUtils, true, "getFieldValues");
            List<Map<String, Object>> rowsRecords = (List<Map<String, Object>>) MethodUtils.invokeMethod(mongoParamUtils, true,"getRowsRecord");
            Map<String, Object> rowsRecord = rowsRecords.isEmpty()? new HashMap<>() : rowsRecords.get(0) ;
            String filePath = (String) rowsRecord.get("filePath");
            if(StringUtils.isEmpty(filePath)) {
                String organizationId = (String) fieldValues.get("organizationId");
                String datahubCustomerId = (String) fieldValues.get("datahubCustomerId");
                String messageId = (String) fieldValues.get("messageId");
                filePath = getFilePath(organizationId, datahubCustomerId, messageId);
            }
            File fsDir = new File(filePath);
            if (!fsDir.exists()) {
                if (fsDir.mkdirs()) {
                    throw new RuntimeException("mkdirs error " + filePath);
                }
            }
            for (String fsName : responseDataMap.keySet()) {
                File fs = new File(filePath, fsName);
                try (FileOutputStream os = new FileOutputStream(fs)) {
                    os.write(Base64.getDecoder().decode(responseDataMap.get(fsName)));
                    os.flush();
                    fsNames.add(fsName);
                } catch (Exception ee) {
                    log.error(ee.getMessage(), ee);
                }
            }
        } catch (Exception ee) {
            log.error(ee.getMessage(), ee);
        }
        return fsNames;
    }


    @Override
    public Object assembleResponse(Event event) {
        Map<String, String> extraMap = extraLocal.get();
        //
        Object response = null;
        if (event.type == Event.Type.RETURN) {
            response =  ((ReturnEvent) event).object;
            Map<String,String> responseDataMap = new HashMap<>();
            if(extraMap != null) {
                String filePath = extraMap.get("filePath");
                if(StringUtils.isEmpty(filePath)) {
                    String organizationId = (String) extraMap.get("organizationId");
                    String datahubCustomerId = (String) extraMap.get("datahubCustomerId");
                    String messageId = (String) extraMap.get("messageId");
                    filePath = getFilePath(organizationId, datahubCustomerId, messageId);
                }
                // 需要文件路径
                List<String> fileNames = List.class.cast(response);
                if(!fileNames.isEmpty()) {
                    for (String fileName : fileNames) {
                        String fileNamePath = filePath + fileName;
                        File fs = new File(fileNamePath);
                        if(fs.exists()) {
                            try {
                                byte[] fileBytes = FileUtils.readFileToByteArray(fs);
                                responseDataMap.put(fileName, Base64.getEncoder().encodeToString(fileBytes));
                            } catch (IOException e) {
                                log.error(e.getMessage(),e);
                            }
                        }
                    }
                }
            }
            return responseDataMap;
        }else {
            return  null ;
        }
    }

    /**
     *
     * @param organizationId
     * @param datahubCustomerId
     * @param messageId
     * @return
     */
    private String getFilePath(String organizationId,String datahubCustomerId, String messageId) {
        StringBuilder tempDir = new StringBuilder(50);

        tempDir.append(System.getProperty("logFiles.location.root", "")+"DATAHUB"+ File.pathSeparator);
        tempDir.append(File.pathSeparator);
        tempDir.append("TMP");

        if(StringUtils.isNotEmpty(organizationId)) {
            tempDir.append(File.pathSeparator);
            tempDir.append(organizationId);
        }
        if(StringUtils.isNotEmpty(datahubCustomerId)) {
            tempDir.append(File.pathSeparator);
            tempDir.append(datahubCustomerId);
        }
        if(StringUtils.isNotEmpty(messageId)) {
            tempDir.append(File.pathSeparator);
            tempDir.append(messageId);
        }
        return tempDir.toString();
    }

}
