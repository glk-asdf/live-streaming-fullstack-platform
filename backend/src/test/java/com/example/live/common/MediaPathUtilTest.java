package com.example.live.common;

import com.example.live.common.util.MediaPathUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaPathUtilTest {

    @Test
    void parsesRoomIdFromMediaPath() {
        assertThat(MediaPathUtil.roomId("live/42")).isEqualTo(42L);
        assertThat(MediaPathUtil.roomId("live/42/extra")).isNull();
        assertThat(MediaPathUtil.roomId("other/42")).isNull();
        assertThat(MediaPathUtil.roomId("live/abc")).isNull();
        assertThat(MediaPathUtil.roomId(null)).isNull();
    }

    @Test
    void extractsStreamKeyFromQuery() {
        assertThat(MediaPathUtil.streamKey("key=abc_123")).isEqualTo("abc_123");
        assertThat(MediaPathUtil.streamKey("foo=1&key=abc%2Bdef&x=2")).isEqualTo("abc+def");
        assertThat(MediaPathUtil.streamKey("foo=1")).isNull();
        assertThat(MediaPathUtil.streamKey(null)).isNull();
    }
}
