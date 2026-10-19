package com.example.live.service.impl;

import com.example.live.config.MediaProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MediaMTX 管理 API 客户端：查询活跃 path、踢掉推流连接。
 */
@Component
public class MediaApiClient {

    private final RestClient restClient;

    public MediaApiClient(RestClient.Builder builder, MediaProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        this.restClient = builder
                .baseUrl(properties.apiBase())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 返回当前 ready 的 path 名集合；MediaMTX 不可用时抛异常，由调用方跳过本轮。
     */
    public Set<String> activePaths() {
        PathList list = restClient.get()
                .uri("/v3/paths/list")
                .retrieve()
                .body(PathList.class);
        if (list == null || list.items() == null) {
            return Set.of();
        }
        return list.items().stream()
                .filter(PathInfo::ready)
                .map(PathInfo::name)
                .collect(Collectors.toSet());
    }

    /**
     * 踢掉指定 path 的 RTMP 推流连接；没有连接时静默返回。
     */
    public void kickPublisher(String path) {
        ConnList list = restClient.get()
                .uri("/v3/rtmpconns/list")
                .retrieve()
                .body(ConnList.class);
        if (list == null || list.items() == null) {
            return;
        }
        list.items().stream()
                .filter(conn -> path.equals(conn.path()))
                .map(ConnInfo::id)
                .forEach(id -> restClient.post()
                        .uri("/v3/rtmpconns/kick/{id}", id)
                        .retrieve()
                        .toBodilessEntity());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PathList(List<PathInfo> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PathInfo(String name, boolean ready) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConnList(List<ConnInfo> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConnInfo(String id, String path) {
    }
}
