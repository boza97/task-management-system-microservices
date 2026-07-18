package com.bozidar.tms.task_service.event;

public enum TaskEventType {
    TASK_CREATED,
    STATUS_CHANGED,
    ASSIGNEE_CHANGED,
    TITLE_CHANGED,
    DESCRIPTION_CHANGED,
    DUE_DATE_CHANGED,
    PRIORITY_CHANGED,
    COMMENT_ADDED,
    COMMENT_DELETED,
    TASK_DELETED
}
