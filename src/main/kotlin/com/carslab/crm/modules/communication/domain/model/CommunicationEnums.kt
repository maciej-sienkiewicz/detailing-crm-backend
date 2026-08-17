package com.carslab.crm.modules.communication.domain.model

enum class MailProviderType {
    GOOGLE_API,
    MS_GRAPH,
    IMAP_SMTP
}

enum class MailAuthType {
    OAUTH2,
    PASSWORD,
    APP_PASSWORD
}

enum class MailAccountStatus {
    ACTIVE,
    AUTH_FAILED,
    DISABLED
}

enum class MessageDirection {
    INBOUND,
    OUTBOUND
}

enum class MessageSendStatus {
    RECEIVED,
    OUTBOX_PENDING,
    SENT,
    FAILED
}

enum class ThreadClassification {
    PENDING,
    INQUIRY,
    OTHER,
    SPAM,
    UNSURE
}

enum class LeadStage {
    NEW,
    QUOTED,
    TO_CONFIRM,
    SCHEDULED,
    WON,
    LOST
}

enum class LeadFactKey {
    VEHICLE,
    SERVICE,
    PHONE,
    PREFERRED_DATE
}
