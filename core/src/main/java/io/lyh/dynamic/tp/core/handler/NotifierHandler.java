package io.lyh.dynamic.tp.core.handler;

import io.lyh.dynamic.tp.common.dto.DtpMainProp;
import io.lyh.dynamic.tp.common.dto.NotifyItem;
import io.lyh.dynamic.tp.common.em.NotifyTypeEnum;
import io.lyh.dynamic.tp.core.context.DtpContextHolder;
import io.lyh.dynamic.tp.core.notify.Notifier;
import io.lyh.dynamic.tp.core.notify.ding.DtpDingNotifier;
import io.lyh.dynamic.tp.core.notify.wechat.DtpWechatNotifier;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * NotifierHandler related
 *
 * @author: yanhom
 * @since 1.0.0
 **/
@Slf4j
public class NotifierHandler {

    private static final Map<String, Notifier> NOTIFIERS = new HashMap<>();

    private static class NotifierHandlerHolder {
        private static final NotifierHandler INSTANCE = new NotifierHandler();
    }

    private NotifierHandler() {
        ServiceLoader<Notifier> loader = ServiceLoader.load(Notifier.class);
        for (Notifier notifier : loader) {
            NOTIFIERS.put(notifier.platform(), notifier);
        }

        Notifier dingNotifier = new DtpDingNotifier();
        Notifier wechatNotifier = new DtpWechatNotifier();
        NOTIFIERS.put(dingNotifier.platform(), dingNotifier);
        NOTIFIERS.put(wechatNotifier.platform(), wechatNotifier);
    }

    public static NotifierHandler getInstance() {
        return NotifierHandlerHolder.INSTANCE;
    }

    public void sendNotice(DtpMainProp prop, List<String> diffs) {

        try {
            NotifyItem notifyItem = DtpContextHolder.get().getNotifyItem();
            for (String platform : notifyItem.getPlatforms()) {
                Notifier notifier = NOTIFIERS.get(platform.toLowerCase());
                if (notifier != null) {
                    notifier.sendChangeMsg(prop, diffs);
                }
            }
        } finally {
            DtpContextHolder.remove();
        }
    }

    public void sendAlarm(NotifyTypeEnum typeEnum) {
        try {
            NotifyItem notifyItem = DtpContextHolder.get().getNotifyItem();
            for (String platform : notifyItem.getPlatforms()) {
                Notifier notifier = NOTIFIERS.get(platform.toLowerCase());
                if (notifier != null) {
                    notifier.sendAlarmMsg(typeEnum);
                }
            }
        } finally {
            DtpContextHolder.remove();
        }
    }
}
