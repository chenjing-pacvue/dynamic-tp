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

package org.dromara.dynamictp.common.notifier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import static org.dromara.dynamictp.common.constant.DingNotifyConst.ACCESS_TOKEN_PARAM;
import static org.dromara.dynamictp.common.constant.DingNotifyConst.DING_NOTICE_TITLE;
import static org.dromara.dynamictp.common.constant.DingNotifyConst.SIGN_PARAM;
import static org.dromara.dynamictp.common.constant.DingNotifyConst.TIMESTAMP_PARAM;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.common.constant.DingNotifyConst;
import org.dromara.dynamictp.common.em.NotifyPlatformEnum;
import org.dromara.dynamictp.common.entity.MarkdownReq;
import org.dromara.dynamictp.common.entity.NotifyPlatform;
import org.dromara.dynamictp.common.util.DingSignUtil;
import org.dromara.dynamictp.common.util.JsonUtil;

/**
 * DingNotifier related
 *
 * @author yanhom
 * @author Kyao
 * @since 1.0.0
 **/
@Slf4j
public class DingNotifier extends AbstractHttpNotifier {

    private static final String ALL = "all";

    @Override
    public String platform() {
        return NotifyPlatformEnum.DING.name().toLowerCase();
    }


    protected String buildMsgBody(NotifyPlatform platform, String content) {
        MarkdownReq.Markdown markdown = new MarkdownReq.Markdown();
        markdown.setTitle(DingNotifyConst.DING_NOTICE_TITLE);
        markdown.setText(content);

        MarkdownReq.At at = new MarkdownReq.At();

        List<String> mobiles = Lists.newArrayList(platform.getReceivers().split(","));
        at.setAtMobiles(mobiles);
        if (mobiles.contains(ALL) || CollectionUtils.isEmpty(mobiles)) {
            at.setIsAtAll(true);
        }

        MarkdownReq markdownReq = new MarkdownReq();
        markdownReq.setMsgtype("markdown");
        markdownReq.setMarkdown(markdown);
        markdownReq.setAt(at);
        return JsonUtil.toJson(markdownReq);
    }

    protected String buildUrl(NotifyPlatform platform) {
        String webhook = Optional.ofNullable(platform.getWebhook()).orElse(DingNotifyConst.DING_WEBHOOK);
        return getTargetUrl(platform.getSecret(), platform.getUrlKey(), webhook);
    }

    /**
     * Build target url.
     *
     * @param secret      secret
     * @param accessToken accessToken
     * @param webhook     webhook
     * @return url
     */
    public String getTargetUrl(String secret, String accessToken, String webhook) {
        StringBuilder urlBuilder = new StringBuilder(webhook);

        // 如果 webhook 中不包含查询参数，则添加一个 '?'
        if (!webhook.contains("?")) {
            urlBuilder.append("?");
        } else if (!webhook.endsWith("&") && !webhook.endsWith("?")) {
            urlBuilder.append("&");
        }

        // 如果 accessToken 非空，且 URL 中未包含 access_token 参数，则添加
        if (isNotBlank(accessToken) && !webhook.contains(DingNotifyConst.ACCESS_TOKEN_PARAM + "=")) {
            appendQueryParam(urlBuilder, DingNotifyConst.ACCESS_TOKEN_PARAM, accessToken);
        }

        // 如果 secret 非空，计算时间戳和签名
        if (isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis();
            appendQueryParam(urlBuilder, DingNotifyConst.TIMESTAMP_PARAM, String.valueOf(timestamp));
            String sign = DingSignUtil.dingSign(secret, timestamp); // 自定义签名逻辑
            appendQueryParam(urlBuilder, DingNotifyConst.SIGN_PARAM, sign);
        }

        // 返回构建后的完整 URL
        return urlBuilder.toString();
    }

    // 判断字符串是否非空
    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 添加查询参数到 URL
    private void appendQueryParam(StringBuilder urlBuilder, String key, String value) {
        try {
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.name());
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            if (urlBuilder.charAt(urlBuilder.length() - 1) != '?' && urlBuilder.charAt(urlBuilder.length() - 1) != '&') {
                urlBuilder.append("&");
            }
            urlBuilder.append(encodedKey).append("=").append(encodedValue);
        } catch (Exception e) {
            throw new RuntimeException("Error encoding query parameters", e);
        }
    }

}
