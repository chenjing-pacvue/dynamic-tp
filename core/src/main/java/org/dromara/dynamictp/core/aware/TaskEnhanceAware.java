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

package org.dromara.dynamictp.core.aware;

import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.core.support.task.runnable.DtpRunnable;
import org.dromara.dynamictp.core.support.task.runnable.NamedFuture;
import org.dromara.dynamictp.core.support.task.runnable.NamedRunnable;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * TaskEnhanceAware related
 *
 * @author yanhom
 * @since 1.1.4
 **/
public interface TaskEnhanceAware extends DtpAware {

    /**
     * 获取线程池里面实际正在执行的task，可能被封装了N层
     * @param futureTask
     * @return
     */
    static Object extractTaskFromFutureTask(FutureTask<?> futureTask) {
        try {
            // 1. 获取 FutureTask 内部的 "callable" 字段（该字段为 private final Callable<V> callable;）
            Field callableField = FutureTask.class.getDeclaredField("callable");
            callableField.setAccessible(true);
            Object callableObj = callableField.get(futureTask);

            // 2. 如果 callableObj 是由 RunnableAdapter 包装的，则其内部通常有一个 "task" 字段
            if (callableObj != null && callableObj.getClass().getName().contains("RunnableAdapter")) {
                Field taskField = callableObj.getClass().getDeclaredField("task");
                taskField.setAccessible(true);
                Object originalTask = taskField.get(callableObj);
                return originalTask;
            }
            // 如果不是 RunnableAdapter，则直接返回 callableObj
            return callableObj;
        } catch (Exception e) {
            throw new RuntimeException("无法提取内部 task", e);
        }
    }

    static String getRunnableName(Runnable wrapRunnable) {
        String taskName = "";
        if (wrapRunnable instanceof NamedRunnable) {
            taskName = (wrapRunnable instanceof NamedRunnable) ? ((NamedRunnable) wrapRunnable).getName() : null;
        }
        if (wrapRunnable instanceof NamedFuture) {
            taskName = (wrapRunnable instanceof NamedFuture) ? ((NamedFuture) wrapRunnable).getName() : null;
        }
        return taskName;
    }

    /**
     * Enhance task
     *
     * @param command      command
     * @param taskWrappers task wrappers
     * @return enhanced task
     */
    default Runnable getEnhancedTask(Runnable command, List<TaskWrapper> taskWrappers) {
        Runnable wrapRunnable = command;

        String taskName = "";
        try {
            if (wrapRunnable instanceof FutureTask) {
                //反射拿到里面的原runnable
                Runnable o = (Runnable) extractTaskFromFutureTask((FutureTask) command);
                taskName = getRunnableName(o);
            } else {
                taskName = getRunnableName(wrapRunnable);
            }
        } catch (Exception ex) {

        }


        if (CollectionUtils.isNotEmpty(taskWrappers)) {
            for (TaskWrapper t : taskWrappers) {
                wrapRunnable = t.wrap(wrapRunnable);
            }
        }
        return new DtpRunnable(command, wrapRunnable, taskName);
    }

    /**
     * Enhance task
     *
     * @param command command
     * @return enhanced task
     */
    default Runnable getEnhancedTask(Runnable command) {
        return getEnhancedTask(command, getTaskWrappers());
    }

    /**
     * Get task wrappers
     *
     * @return task wrappers
     */
    List<TaskWrapper> getTaskWrappers();

    /**
     * Set task wrappers
     *
     * @param taskWrappers task wrappers
     */
    void setTaskWrappers(List<TaskWrapper> taskWrappers);
}
