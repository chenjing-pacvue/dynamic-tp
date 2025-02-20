/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.dynamictp.core.notifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import lombok.val;
import org.dromara.dynamictp.common.em.NotifyPlatformEnum;
import org.dromara.dynamictp.common.notifier.Notifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dromara.dynamictp.common.util.JsonUtil;
import org.dromara.dynamictp.core.notifier.context.BaseNotifyCtx;
import org.dromara.dynamictp.core.notifier.context.DtpNotifyCtxHolder;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.adapter.ExecutorAdapter;
import org.dromara.dynamictp.core.support.task.runnable.DtpRunnable;
import org.dromara.dynamictp.core.support.task.runnable.NamedFuture;
import org.dromara.dynamictp.core.support.task.runnable.NamedRunnable;

/**
 * DtpDingNotifier related
 *
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
public class DtpDingNotifier extends AbstractDtpNotifier {

    public DtpDingNotifier(Notifier notifier) {
        super(notifier);
    }

    @Override
    public String platform() {
        return NotifyPlatformEnum.DING.name().toLowerCase();
    }



    protected String getNoticeTemplate() {
        return "<font color=#5AB030>【通知】</font> 动态线程池参数变更 \n\n <font color=#664B4B size=2>服务名称：%s</font> \n\n <font color=#664B4B size=2>实例信息：%s</font> \n\n <font color=#664B4B size=2>环境：%s</font> \n\n <font color=#664B4B size=2>线程池名称：%s</font> \n\n <font color=corePoolSize size=2>核心线程数：%s => %s</font> \n\n <font color=maxPoolSize size=2>最大线程数：%s => %s</font> \n\n <font color=allowCoreThreadTimeOut size=2>允许核心线程超时：%s => %s</font> \n\n <font color=keepAliveTime size=2>线程存活时间：%ss => %ss</font> \n\n <font color=#664B4B size=2>队列类型：%s</font> \n\n <font color=queueCapacity size=2>队列容量：%s => %s</font> \n\n <font color=rejectType size=2>拒绝策略：%s => %s</font> \n\n <font color=#664B4B size=2>接收人：@%s</font> \n\n<font color=#664B4B size=2>通知时间：%s</font> \n\n";
    }

    protected String getAlarmTemplate() {
        return "<font color=#EA9F00>【报警】 </font> 动态线程池运行告警 \n\n<font color=#664B4B size=2>服务名称：%s</font> \n\n <font color=#664B4B size=2>实例信息：%s</font> \n\n <font color=#664B4B size=2>环境：%s</font> \n\n <font color=#664B4B size=2>线程池名称：%s</font> \n\n <font color=alarmType size=2>报警项：%s</font> \n\n <font color=alarmValue size=2>报警阈值 / 当前值：%s</font> \n\n <font color=corePoolSize size=2>核心线程数：%d</font> \n\n <font color=maximumPoolSize size=2>最大线程数：%d</font> \n\n <font color=poolSize size=2>当前线程数：%d</font> \n\n <font color=activeCount size=2>活跃线程数：%d</font> \n\n <font color=#664B4B size=2>历史最大线程数：%d</font> \n\n <font color=#664B4B size=2>任务总数：%d</font> \n\n <font color=#664B4B size=2>执行完成任务数：%d</font> \n\n <font color=#664B4B size=2>等待执行任务数：%d</font> \n\n <font color=queueType size=2>队列类型：%s</font> \n\n <font color=queueCapacity size=2>队列容量：%d</font> \n\n <font color=queueSize size=2>队列任务数量：%d</font> \n\n <font color=queueRemaining size=2>队列剩余容量：%d</font> \n\n <font color=rejectType size=2>拒绝策略：%s</font> \n\n<font color=rejectCount size=2>总拒绝任务数量：%s</font> \n\n <font color=runTimeoutCount size=2>总执行超时任务数量：%s</font> \n\n <font color=queueTimeoutCount size=2>总等待超时任务数量：%s</font> \n\n <font color=#664B4B size=2>上次报警时间：%s</font> \n\n<font color=#664B4B size=2>报警时间：%s</font> \n\n<font color=#664B4B size=2>接收人：@%s</font> \n\n<font color=#664B4B size=2>trace 信息：%s</font> \n\n<font color=#22B838 size=2>报警间隔：%ss</font> \n\n<font color=#664B4B size=2>扩展信息：%s</font> \n\n";
    }

    protected Pair<String, String> getColors() {
        return new ImmutablePair("#EA9F00", "#664B4B");
    }

    @Override
    protected String getExtInfo() {
        String extInfo = super.getExtInfo();
        //todo 配置是否开启队列任务信息获取
        return extInfo + "; 当前队列中等待的任务：" + getWaitTasks();
    }

    public String getWaitTasks() {
        BaseNotifyCtx context = DtpNotifyCtxHolder.get();
        ExecutorWrapper executorWrapper = context.getExecutorWrapper();
        ExecutorAdapter<?> executor0 = executorWrapper.getExecutor();

        val statProvider = executorWrapper.getThreadPoolStatProvider();
        ExecutorAdapter<?> executor = statProvider.getExecutorWrapper().getExecutor();

        //todo 增加获取当前系统的内存信息，cpu信息
        //todo 获取告警类型，实现自动扩容（或者通过一个xxljob，定时进行扫描信息扩容）
        //获取在运行的任务名称
        List<String> waitTasks = new ArrayList<>();
        BlockingQueue<Runnable> queue = executor.getQueue();
        if (queue == null) {
            return "无等待队列";
        }
        if (queue.size() == 0) {
            return "队列为空";
        }

        List<Runnable> list = queue.stream().toList();
        list.forEach(wrapRunnable -> {

            // 使用反射获取 final 字段
            DtpRunnable wrapRunnable1 = (DtpRunnable) wrapRunnable;

            Field field = null;
            try {
                field = DtpRunnable.class.getDeclaredField("originRunnable");
                field.setAccessible(true); // 允许访问私有字段
                Runnable extractedRunnable = (Runnable) field.get(wrapRunnable1);
                String taskName2 = "";
                if (extractedRunnable instanceof NamedRunnable) {
                    taskName2 = (extractedRunnable instanceof NamedRunnable) ? ((NamedRunnable) extractedRunnable).getName() : null;
                }
                if (extractedRunnable instanceof NamedFuture) {
                    taskName2 = (extractedRunnable instanceof NamedFuture) ? ((NamedFuture) extractedRunnable).getName() : null;
                }


                waitTasks.add(taskName2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });


        return JsonUtil.toJson(waitTasks);
    }



}
