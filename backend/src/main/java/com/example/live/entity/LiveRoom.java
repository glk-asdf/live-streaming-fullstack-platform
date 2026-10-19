package com.example.live.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * 直播间实体。
 */
@TableName("t_live_room")
public class LiveRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long anchorId;

    private Long categoryId;

    private String title;

    private String cover;

    // 推流密钥只允许通过主播专用接口返回，实体误序列化时必须隐藏
    @JsonIgnore
    private String streamKey;

    private Integer status;

    private LocalDateTime liveStartTime;

    private LocalDateTime liveEndTime;

    private Integer viewerCount;

    private Integer peakViewer;

    private Long likeCount;

    private Integer hotScore;

    private Integer replayStatus;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnchorId() {
        return anchorId;
    }

    public void setAnchorId(Long anchorId) {
        this.anchorId = anchorId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLiveStartTime() {
        return liveStartTime;
    }

    public void setLiveStartTime(LocalDateTime liveStartTime) {
        this.liveStartTime = liveStartTime;
    }

    public LocalDateTime getLiveEndTime() {
        return liveEndTime;
    }

    public void setLiveEndTime(LocalDateTime liveEndTime) {
        this.liveEndTime = liveEndTime;
    }

    public Integer getViewerCount() {
        return viewerCount;
    }

    public void setViewerCount(Integer viewerCount) {
        this.viewerCount = viewerCount;
    }

    public Integer getPeakViewer() {
        return peakViewer;
    }

    public void setPeakViewer(Integer peakViewer) {
        this.peakViewer = peakViewer;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getHotScore() {
        return hotScore;
    }

    public void setHotScore(Integer hotScore) {
        this.hotScore = hotScore;
    }

    public Integer getReplayStatus() {
        return replayStatus;
    }

    public void setReplayStatus(Integer replayStatus) {
        this.replayStatus = replayStatus;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
