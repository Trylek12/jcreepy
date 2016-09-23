
package io.netty.channel.socket;

import com.sun.nio.sctp.Notification;

public final class SctpNotificationEvent {
    private final Notification notification;
    private final Object attachment;

    public SctpNotificationEvent(Notification notification, Object attachment) {
        this.notification = notification;
        this.attachment = attachment;
    }

    public Notification notification() {
        return this.notification;
    }

    public Object attachment() {
        return this.attachment;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        SctpNotificationEvent that = (SctpNotificationEvent)o;
        if (!this.attachment.equals(that.attachment)) {
            return false;
        }
        if (!this.notification.equals(that.notification)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = this.notification.hashCode();
        result = 31 * result + this.attachment.hashCode();
        return result;
    }

    public String toString() {
        return "SctpNotification{notification=" + this.notification + ", attachment=" + this.attachment + '}';
    }
}

