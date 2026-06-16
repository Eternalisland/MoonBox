/*
Copyright 2022 vivo Communication Technology Co., Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.vivo.internet.moonbox.common.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RecordPullRequest - {@link RecordPullRequest}
 *
 * @author yanjiang.liu
 * @version 1.0
 * @since 2022/8/22 17:06
 */
public class RecordPullRequest implements Serializable {

    private static final long serialVersionUID = -2647359692813964492L;

    public RecordPullRequest() {
    }

    public RecordPullRequest(String scrollId, String replayTaskRunId, String recordTaskRunId, String organizationId, String datahubCustomerId, String messageId, String traceId, int pageIndex, int pageSize) {
        this.scrollId = scrollId;
        this.replayTaskRunId = replayTaskRunId;
        this.recordTaskRunId = recordTaskRunId;
        this.organizationId = organizationId;
        this.datahubCustomerId = datahubCustomerId;
        this.messageId = messageId;
        this.traceId = traceId;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    private String scrollId;

    private String replayTaskRunId;

    private String recordTaskRunId;
    /**
     * 组织机构
     */
    private String organizationId;
    /**
     * 按照消息集 来查询
     */
    private String datahubCustomerId;
    /**
     * 按照 messageId 来查询
     */
    private String messageId;

    /**
     * 按照 traceId 来回放
     */
    private String traceId;
    /**
     *
     */
    private int pageIndex = 1;
    /**
     *
     */
    private int pageSize = 100;

    public static RecordPullRequestBuilder builder() {
        return new RecordPullRequestBuilder();
    }


    public static final class RecordPullRequestBuilder {
        private RecordPullRequest recordPullRequest;

        public RecordPullRequestBuilder() {
            recordPullRequest = new RecordPullRequest();
        }

        public RecordPullRequestBuilder(RecordPullRequest recordPullRequest) {
            this.recordPullRequest = recordPullRequest;
        }

        public static RecordPullRequestBuilder builder() {
            return new RecordPullRequestBuilder();
        }

        public RecordPullRequestBuilder scrollId(String scrollId) {
            recordPullRequest.setScrollId(scrollId);
            return this;
        }

        public RecordPullRequestBuilder replayTaskRunId(String replayTaskRunId) {
            recordPullRequest.setReplayTaskRunId(replayTaskRunId);
            return this;
        }

        public RecordPullRequestBuilder recordTaskRunId(String recordTaskRunId) {
            recordPullRequest.setRecordTaskRunId(recordTaskRunId);
            return this;
        }

        public RecordPullRequestBuilder organizationId(String organizationId) {
            recordPullRequest.setOrganizationId(organizationId);
            return this;
        }

        public RecordPullRequestBuilder datahubCustomerId(String datahubCustomerId) {
            recordPullRequest.setDatahubCustomerId(datahubCustomerId);
            return this;
        }

        public RecordPullRequestBuilder messageId(String messageId) {
            recordPullRequest.setMessageId(messageId);
            return this;
        }

        public RecordPullRequestBuilder traceId(String traceId) {
            recordPullRequest.setTraceId(traceId);
            return this;
        }

        public RecordPullRequestBuilder pageIndex(int pageIndex) {
            recordPullRequest.setPageIndex(pageIndex);
            return this;
        }

        public RecordPullRequestBuilder pageSize(int pageSize) {
            recordPullRequest.setPageSize(pageSize);
            return this;
        }

        public RecordPullRequest build() {
            return recordPullRequest;
        }
    }

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    public String getReplayTaskRunId() {
        return replayTaskRunId;
    }

    public void setReplayTaskRunId(String replayTaskRunId) {
        this.replayTaskRunId = replayTaskRunId;
    }

    public String getRecordTaskRunId() {
        return recordTaskRunId;
    }

    public void setRecordTaskRunId(String recordTaskRunId) {
        this.recordTaskRunId = recordTaskRunId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getDatahubCustomerId() {
        return datahubCustomerId;
    }

    public void setDatahubCustomerId(String datahubCustomerId) {
        this.datahubCustomerId = datahubCustomerId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
