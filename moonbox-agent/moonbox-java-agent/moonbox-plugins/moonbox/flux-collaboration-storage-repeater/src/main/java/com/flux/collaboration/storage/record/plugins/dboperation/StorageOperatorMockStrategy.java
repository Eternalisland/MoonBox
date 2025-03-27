package com.flux.collaboration.storage.record.plugins.dboperation;

import com.alibaba.jvm.sandbox.repeater.plugin.Comparable;
import com.alibaba.jvm.sandbox.repeater.plugin.ComparableFactory;
import com.alibaba.jvm.sandbox.repeater.plugin.CompareResult;
import com.alibaba.jvm.sandbox.repeater.plugin.Difference;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractReflectCompareStrategy;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.MoonboxContext;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockRequest;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.SelectResult;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.MockStrategy;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.UriMatchUtils;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vivo.internet.moonbox.common.api.constants.DiffScope;
import com.vivo.internet.moonbox.common.api.model.FieldDiffConfig;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import com.vivo.internet.moonbox.common.api.util.ComparableGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.MetaInfServices;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @date 2025/2/8 14:42
 * @description 处理逻辑 功能描述
 */
@Slf4j
@MetaInfServices(MockStrategy.class)
public class StorageOperatorMockStrategy  extends AbstractReflectCompareStrategy {

    /**
     *
     */
    private static final RepeaterConfig REPEATER_CONFIG = MoonboxContext.getInstance().getConfig();

    @Override
    public String invokeType() {
        return InvokeType.DATAHUB_STORAGE_OPERATOR.name();
    }

    /**
     *
     * @param request mock request
     * @return
     */
    @Override
    protected SelectResult select(MockRequest request) {
        SelectResult select = this.doSelect(request);

        SelectResult selectResult = SelectResult.builder()
                .match(true)
                .invocation(select.getInvocation())
                .cost(select.getCost())
                .build();
        return selectResult;
    }

    /**
     *
     * @param request
     * @return
     */
    protected SelectResult doSelect(MockRequest request) {

        final List<Invocation> subInvocations = request.getRecordModel().getSubInvocations();
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (CollectionUtils.isEmpty(subInvocations)
                && CollectionUtils.isEmpty(request.getRecordModel().getRemoveSubInvocations())) {
            return SelectResult.builder().match(false).cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
        }
        List<Invocation> target = selectTargets(request);
        if (CollectionUtils.isEmpty(target)) {
            log.error("can't find any sub invocation, strategy={}, identity={}", type().name(), request.getIdentity().getUri());
            return SelectResult.builder().match(false).cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
        }

        InvokeType invokeType = request.getType();
        Map<Integer, Pair<Invocation, List<Difference>>> invocationMap = Maps.newHashMap();
        // step2: 反射对比；默认忽略时间戳
        for (Invocation invocation : target) {
            Comparable comparable = generateComparator(request);
            CompareResult result;
            try {
                result = comparable.compare(getCompareParamFromOrigin(invocation, request),
                        getCompareParamFromCurrent(invocation, request));
            } catch (Throwable e) {
                log.error("compare error", e);
                return SelectResult.builder().match(false).invocation(invocation)
                        .cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
            }
            if (!result.hasDifference()) {
                log.info("find target invocation by {},index={},identity={}", type().name(), request.getIndex(),
                        request.getIdentity().getUri());
                try {
                    Iterator<Invocation> ite = subInvocations.iterator();
                    while (ite.hasNext()) {
                        if (invocation.equals(ite.next())) {
                            {
                                ite.remove();
                                // 将数据添加到删除列表里面，如果只是新增了相同的流程，后续可以继续做比对使用
                                invocation.setReplayAlreadyMatch(Boolean.TRUE);
                                request.getRecordModel().getRemoveSubInvocations().add(invocation);
                            }
                            break;
                        }
                    }
                    return SelectResult.builder().match(true).invocation(invocation)
                            .cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            Pair<Invocation, List<Difference>> pair = ImmutablePair.of(invocation, result.getDifferences());
            invocationMap.put(result.getDifferences().size(), pair);
        }

        // 如果是按照key对比的没有找到对应的key直接报错未匹配子调用而不是入参对比异常
        if (InvokeType.isKeyInvocation(invokeType)) {
            return SelectResult.builder().match(false).cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
        }

        // 如果没有找到，返回差异最少的一条
        List<Integer> scores = new ArrayList<>(invocationMap.keySet());
        scores.sort((o1, o2) -> {
            if (o1.equals(o2)) {
                return 0;
            }
            return o1 - o2 > 0 ? 1 : -1;
        });

        for (Integer diffCount : scores) {
            Pair<Invocation, List<Difference>> pair = invocationMap.get(diffCount);
            Invocation invocation = pair.getLeft();
            if (invocation == null || invocation.getReplayAlreadyMatch()) {
                continue;
            }
            invocation.setDiffs(pair.getRight());
            return SelectResult.builder().match(false).invocation(invocation)
                    .cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
        }
        log.error("find only alreadyMatch invocation but has difference strategy={}, identity={}", type().name(),
                request.getIdentity().getUri());
        return SelectResult.builder().match(false).cost(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)).build();
    }


    /**
     *
     * @param request
     * @return
     */
    private Comparable generateComparator(MockRequest request) {
        String uri = request.getIdentity().getUri();
        if(uri.indexOf("?") > -1) {
            uri = uri.substring(0,uri.indexOf("?")) ;
        }
        List<FieldDiffConfig> diffConfigs = REPEATER_CONFIG.getFieldDiffConfigs() == null ? new ArrayList<>() : REPEATER_CONFIG.getFieldDiffConfigs() ;
        FieldDiffConfig f1 = new FieldDiffConfig();
        f1.setScope(DiffScope.APP_SCOPE.getCode());
        f1.setFieldPath("$[0].messageGroupSysId");
        f1.setUri(uri);
        diffConfigs.add(f1);
        if (CollectionUtils.isEmpty(diffConfigs)) {
            return ComparableFactory.instance()
                    .create(com.alibaba.jvm.sandbox.repeater.plugin.comparator.Comparator.CompareMode.LENIENT_DATES);
        }
        // 判断子调用uri是否命中配置规则
        String subUri = request.getIdentity().getUri();
        subUri = subUri.endsWith("/") ? subUri.substring(0, subUri.length() - 1) : subUri;
        if(subUri.indexOf("?") > -1) {
            subUri = subUri.substring(0,subUri.indexOf("?")) ;
        }
        //Map<String, FieldDiffConfig> diffConfigMap = diffConfigs.stream()
        //        .collect(Collectors.toMap(FieldDiffConfig::getUri, a -> a, (k1, k2) -> k1));
        //修复如果子调用如果忽略多个字段时，只有一个子调用字段忽略生效的问题。
        Map<String, List<FieldDiffConfig>> diffConfigMap = Maps.newHashMap();
        diffConfigs.forEach(fieldDiffConfig -> {
            List<FieldDiffConfig> middleFieldDiffConfig = diffConfigMap.computeIfAbsent(fieldDiffConfig.getUri(),
                    k -> Lists.newArrayList());
            middleFieldDiffConfig.add(fieldDiffConfig);
        });
        log.info("generate comparator: subUri:{}", request.getIdentity().getUri());
        Pair<Boolean, String> pair = matchUri(diffConfigMap.keySet(), subUri);
        if (!pair.getLeft()) {
            return ComparableFactory.instance()
                    .create(com.alibaba.jvm.sandbox.repeater.plugin.comparator.Comparator.CompareMode.LENIENT_DATES);
        }
        return ComparableGenerator.generate(Lists.newArrayList(diffConfigMap.get(pair.getRight())),
                com.alibaba.jvm.sandbox.repeater.plugin.comparator.Comparator.CompareMode.LENIENT_DATES,
                Objects.equals(request.getIdentity().getScheme(), InvokeType.HTTP.name()));
    }


    private Pair<Boolean, String> matchUri(Set<String> uriSet, String uri) {
        if (null == uriSet) {
            return ImmutablePair.of(Boolean.FALSE, null);
        }
        if (uriSet.contains(uri)) {
            return ImmutablePair.of(Boolean.TRUE, uri);
        }

        String[] uriArr = uri.split("/");
        for (String item : uriSet) {
            if (StringUtils.isBlank(item)) {
                continue;
            }
            if (!item.contains("{") || !item.contains("}")) {
                continue;
            }
            String[] itemArr = item.split("/");

            if (itemArr.length != uriArr.length) {
                continue;
            }
            if (UriMatchUtils.match(uriArr, uri)) {
                return ImmutablePair.of(Boolean.TRUE, item);
            }
        }
        return ImmutablePair.of(Boolean.FALSE, null);
    }

}
